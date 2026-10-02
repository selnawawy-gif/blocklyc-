package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioTranscriberManager
import com.example.data.BlocklySketch
import com.example.data.SketchDatabase
import com.example.data.SketchRepository
import com.example.engine.ArduinoCodeGenerator
import com.example.engine.BlocklyXmlGenerator
import com.example.engine.BlocklyXmlParser
import com.example.model.*
import com.example.simulator.CircuitSimulatorEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class AppTab(val title: String, val iconName: String) {
    BLOCKS("Visual Blocks", "Widgets"),
    XML("Blockly XML", "Code"),
    CPP("Arduino C++", "DataObject"),
    SIMULATOR("Circuit Sim", "PrecisionManufacturing"),
    PAST_CODES("Past Codes", "History"),
    AUDIO("Live Audio", "Mic"),
    DIAGNOSTICS("Diagnostics", "Warning")
}

data class BlocklyUiState(
    val sketchName: String = "My Arduino Project",
    val sketchDescription: String = "Automated microcontroller sketch",
    val currentSketchId: Long? = null,
    val selectedBoard: ArduinoBoardType = ArduinoBoardType.UNO,
    val blocks: List<BlocklyBlock> = emptyList(),
    val xmlText: String = "",
    val sketchOutput: ArduinoSketchOutput = ArduinoSketchOutput(cppCode = ""),
    val currentTab: AppTab = AppTab.BLOCKS,
    val focusedBlockId: String? = null,
    val userNotification: String? = null,
    val isAddBlockSheetOpen: Boolean = false,
    val isTemplatesDialogOpen: Boolean = false,
    val isSavedSketchesDialogOpen: Boolean = false,
    val isSaveDialogOpen: Boolean = false,
    val isHardwarePinoutVisible: Boolean = false
)

class BlocklyViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SketchRepository
    private val xmlParser = BlocklyXmlParser()
    private val xmlGenerator = BlocklyXmlGenerator()
    private val codeGenerator = ArduinoCodeGenerator()

    val simulatorEngine = CircuitSimulatorEngine(viewModelScope)
    val audioTranscriber: AudioTranscriberManager

    private val _uiState = MutableStateFlow(BlocklyUiState())
    val uiState: StateFlow<BlocklyUiState> = _uiState.asStateFlow()

    val savedSketches: StateFlow<List<BlocklySketch>>

    init {
        val db = SketchDatabase.getDatabase(application)
        repository = SketchRepository(db.sketchDao())
        savedSketches = repository.allSketches.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        audioTranscriber = AudioTranscriberManager(application) { voiceAction ->
            handleVoiceCommand(voiceAction)
        }

        // Initialize with default Blink template
        loadTemplate(SketchTemplates.TEMPLATES.first())
    }

    fun selectTab(tab: AppTab) {
        _uiState.update { it.copy(currentTab = tab) }
        if (tab == AppTab.SIMULATOR) {
            simulatorEngine.updateBlocks(_uiState.value.blocks)
        }
    }

    fun setBoard(board: ArduinoBoardType) {
        _uiState.update { it.copy(selectedBoard = board) }
        recompile()
    }

    fun setSketchName(name: String) {
        _uiState.update { it.copy(sketchName = name) }
    }

    fun setSketchDescription(desc: String) {
        _uiState.update { it.copy(sketchDescription = desc) }
    }

    // --- Block Manipulation ---

    fun addBlock(type: String, scope: BlockScope? = null) {
        val newBlock = BlockRegistry.createBlock(type, scope)
        val updated = _uiState.value.blocks.toMutableList().apply { add(newBlock) }
        _uiState.update {
            it.copy(
                blocks = updated,
                focusedBlockId = newBlock.id,
                isAddBlockSheetOpen = false,
                userNotification = "Added ${newBlock.title}"
            )
        }
        recompile()
    }

    fun updateBlockField(blockId: String, fieldName: String, value: String) {
        val updated = updateFieldRecursive(_uiState.value.blocks, blockId, fieldName, value)
        _uiState.update { it.copy(blocks = updated) }
        recompile()
    }

    private fun updateFieldRecursive(
        list: List<BlocklyBlock>,
        targetId: String,
        fieldName: String,
        value: String
    ): List<BlocklyBlock> {
        return list.map { block ->
            if (block.id == targetId) {
                val newFields = HashMap(block.fields)
                newFields[fieldName] = value
                block.copy(fields = newFields)
            } else {
                block.copy(
                    childBlocks = updateFieldRecursive(block.childBlocks, targetId, fieldName, value).toMutableList(),
                    elseBlocks = updateFieldRecursive(block.elseBlocks, targetId, fieldName, value).toMutableList()
                )
            }
        }
    }

    fun deleteBlock(blockId: String) {
        val updated = deleteRecursive(_uiState.value.blocks, blockId)
        _uiState.update {
            it.copy(
                blocks = updated,
                userNotification = "Block deleted"
            )
        }
        recompile()
    }

    private fun deleteRecursive(list: List<BlocklyBlock>, targetId: String): List<BlocklyBlock> {
        return list.filterNot { it.id == targetId }.map { block ->
            block.copy(
                childBlocks = deleteRecursive(block.childBlocks, targetId).toMutableList(),
                elseBlocks = deleteRecursive(block.elseBlocks, targetId).toMutableList()
            )
        }
    }

    fun duplicateBlock(blockId: String) {
        val target = findBlockById(_uiState.value.blocks, blockId) ?: return
        val clone = target.deepCopy()
        val index = _uiState.value.blocks.indexOfFirst { it.id == blockId }
        val updated = _uiState.value.blocks.toMutableList()
        if (index >= 0) {
            updated.add(index + 1, clone)
        } else {
            updated.add(clone)
        }
        _uiState.update {
            it.copy(
                blocks = updated,
                focusedBlockId = clone.id,
                userNotification = "Duplicated block"
            )
        }
        recompile()
    }

    fun moveBlock(blockId: String, direction: Int) {
        val list = _uiState.value.blocks.toMutableList()
        val index = list.indexOfFirst { it.id == blockId }
        if (index < 0) return
        val newIndex = index + direction
        if (newIndex in list.indices) {
            val item = list.removeAt(index)
            list.add(newIndex, item)
            _uiState.update { it.copy(blocks = list) }
            recompile()
        }
    }

    fun focusBlock(blockId: String) {
        _uiState.update {
            it.copy(
                focusedBlockId = blockId,
                currentTab = AppTab.BLOCKS
            )
        }
    }

    // --- XML Import / Export ---

    fun updateXmlText(newXml: String) {
        _uiState.update { it.copy(xmlText = newXml) }
    }

    fun parseAndApplyXml(xmlString: String = _uiState.value.xmlText) {
        val parseResult = xmlParser.parse(xmlString)
        if (parseResult.blocks.isNotEmpty() || parseResult.diagnostics.isEmpty()) {
            _uiState.update {
                it.copy(
                    blocks = parseResult.blocks,
                    xmlText = xmlString,
                    userNotification = "Imported ${parseResult.blocks.size} blocks from XML"
                )
            }
            recompile()
        } else {
            // Parser had critical syntax errors: update diagnostics without blowing away workspace
            val existing = _uiState.value.sketchOutput
            val combinedDiags = parseResult.diagnostics + existing.diagnostics
            _uiState.update {
                it.copy(
                    sketchOutput = existing.copy(diagnostics = combinedDiags),
                    userNotification = "XML Parse Error: ${parseResult.diagnostics.firstOrNull()?.message}"
                )
            }
        }
    }

    // --- Templates & Persistence ---

    fun loadTemplate(template: SketchTemplate) {
        _uiState.update {
            it.copy(
                sketchName = template.title,
                sketchDescription = template.description,
                selectedBoard = template.targetBoard,
                blocks = template.blocks.map { b -> b.deepCopy() },
                isTemplatesDialogOpen = false,
                userNotification = "Loaded '${template.title}' template"
            )
        }
        recompile()
    }

    fun newEmptySketch() {
        _uiState.update {
            it.copy(
                sketchName = "Untitled Sketch",
                sketchDescription = "New Arduino project",
                currentSketchId = null,
                blocks = emptyList(),
                xmlText = "<xml></xml>",
                userNotification = "Created new empty sketch"
            )
        }
        recompile()
    }

    fun saveCurrentSketch(name: String = _uiState.value.sketchName) {
        viewModelScope.launch {
            val sketch = BlocklySketch(
                id = _uiState.value.currentSketchId ?: 0,
                name = name,
                description = _uiState.value.sketchDescription,
                boardType = _uiState.value.selectedBoard.name,
                xmlContent = _uiState.value.xmlText,
                cppContent = _uiState.value.sketchOutput.cppCode,
                blockCount = _uiState.value.blocks.size,
                updatedAt = System.currentTimeMillis()
            )
            val newId = repository.saveSketch(sketch)
            _uiState.update {
                it.copy(
                    currentSketchId = newId,
                    sketchName = name,
                    isSaveDialogOpen = false,
                    userNotification = "Saved sketch '$name'"
                )
            }
        }
    }

    fun loadSavedSketch(saved: BlocklySketch) {
        val board = try {
            ArduinoBoardType.valueOf(saved.boardType)
        } catch (_: Exception) {
            ArduinoBoardType.UNO
        }
        val parseResult = xmlParser.parse(saved.xmlContent)
        _uiState.update {
            it.copy(
                sketchName = saved.name,
                sketchDescription = saved.description,
                currentSketchId = saved.id,
                selectedBoard = board,
                blocks = parseResult.blocks,
                xmlText = saved.xmlContent,
                isSavedSketchesDialogOpen = false,
                userNotification = "Loaded '${saved.name}'"
            )
        }
        recompile()
    }

    fun deleteSavedSketch(id: Long) {
        viewModelScope.launch {
            repository.deleteSketchById(id)
            _uiState.update { it.copy(userNotification = "Sketch deleted from database") }
        }
    }

    // --- Dialogs & Panels ---

    fun setAddBlockSheetOpen(open: Boolean) = _uiState.update { it.copy(isAddBlockSheetOpen = open) }
    fun setTemplatesDialogOpen(open: Boolean) = _uiState.update { it.copy(isTemplatesDialogOpen = open) }
    fun setSavedSketchesDialogOpen(open: Boolean) = _uiState.update { it.copy(isSavedSketchesDialogOpen = open) }
    fun setSaveDialogOpen(open: Boolean) = _uiState.update { it.copy(isSaveDialogOpen = open) }
    fun togglePinoutVisible() = _uiState.update { it.copy(isHardwarePinoutVisible = !it.isHardwarePinoutVisible) }
    fun clearNotification() = _uiState.update { it.copy(userNotification = null) }

    // --- Compilation Engine ---

    private fun recompile() {
        val blocks = _uiState.value.blocks
        val board = _uiState.value.selectedBoard
        val output = codeGenerator.generate(blocks, board)
        val xml = xmlGenerator.generate(blocks)

        _uiState.update {
            it.copy(
                sketchOutput = output,
                xmlText = xml
            )
        }
        simulatorEngine.updateBlocks(blocks)
    }

    private fun handleVoiceCommand(voiceAction: String) {
        when {
            voiceAction.contains("Digital Write Pin 13 HIGH") -> {
                addBlock("arduino_digital_write")
            }
            voiceAction.contains("Delay") -> {
                addBlock("arduino_delay")
            }
            voiceAction.contains("Servo") -> {
                addBlock("servo_write")
            }
            voiceAction.contains("Analog Read") -> {
                addBlock("arduino_analog_read")
            }
            voiceAction.contains("Tone") -> {
                addBlock("tone_buzzer")
            }
            voiceAction.contains("Serial") -> {
                addBlock("serial_println")
            }
            voiceAction.contains("Sonar") -> {
                addBlock("sensor_ultrasonic_distance")
            }
        }
    }

    private fun findBlockById(list: List<BlocklyBlock>, targetId: String): BlocklyBlock? {
        for (b in list) {
            if (b.id == targetId) return b
            val inChild = findBlockById(b.childBlocks, targetId)
            if (inChild != null) return inChild
            val inElse = findBlockById(b.elseBlocks, targetId)
            if (inElse != null) return inElse
        }
        return null
    }

    override fun onCleared() {
        super.onCleared()
        simulatorEngine.pause()
        audioTranscriber.stopListening()
    }
}

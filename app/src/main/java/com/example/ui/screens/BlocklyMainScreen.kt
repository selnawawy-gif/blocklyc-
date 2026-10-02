package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DiagnosticSeverity
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.AppTab
import com.example.viewmodel.BlocklyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlocklyMainScreen(
    viewModel: BlocklyViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val savedSketches by viewModel.savedSketches.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.userNotification) {
        uiState.userNotification?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotification()
        }
    }

    val errorCount = uiState.sketchOutput.diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warningCount = uiState.sketchOutput.diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            // Scratch 3.0 Signature Blue Top Bar
            Surface(
                color = ScratchBlue,
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left: Scratch Title + Green Flag + Stop Sign
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Scratch Logo Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFAB19))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "SCRATCH",
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                color = Color.White,
                                letterSpacing = 1.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Arduino",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        // Scratch Green Flag (Run Simulation)
                        IconButton(
                            onClick = {
                                viewModel.selectTab(AppTab.SIMULATOR)
                                viewModel.simulatorEngine.start()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ScratchGreenFlag)
                                .testTag("scratch_green_flag")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run (Green Flag)", tint = Color.White, modifier = Modifier.size(20.dp))
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        // Scratch Red Stop Sign (Halt Simulation)
                        IconButton(
                            onClick = {
                                viewModel.simulatorEngine.pause()
                            },
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(ScratchRedStop)
                                .testTag("scratch_stop_sign")
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Right: Actions (Pinout, Templates, Past Codes, Save)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.togglePinoutVisible() }) {
                            Icon(Icons.Default.DeveloperBoard, contentDescription = "Pinout", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setTemplatesDialogOpen(true) }) {
                            Icon(Icons.Default.FolderSpecial, contentDescription = "Templates", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.selectTab(AppTab.PAST_CODES) }) {
                            Icon(Icons.Default.History, contentDescription = "Past Codes", tint = Color.White)
                        }
                        IconButton(onClick = { viewModel.setSaveDialogOpen(true) }) {
                            Icon(Icons.Default.Save, contentDescription = "Save", tint = Color.White)
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (uiState.currentTab == AppTab.BLOCKS) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.setAddBlockSheetOpen(true) },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add Block") },
                    text = { Text("Add Block") },
                    containerColor = ScratchOrange,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("fab_add_block")
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(ScratchBlockArea)
        ) {
            // Optional Hardware Pinout Inspector
            if (uiState.isHardwarePinoutVisible) {
                Box(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                    ArduinoPinoutView(
                        board = uiState.selectedBoard,
                        usedPins = uiState.sketchOutput.usedPins
                    )
                }
            }

            // Scratch Navigation Bar (Code, Costumes/Sim, Sounds/Audio, C++, Past Codes, Diagnostics)
            ScrollableTabRow(
                selectedTabIndex = uiState.currentTab.ordinal,
                edgePadding = 8.dp,
                containerColor = Color.White,
                contentColor = ScratchBlue,
                divider = { Divider(color = ScratchBorder) }
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = uiState.currentTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { viewModel.selectTab(tab) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when (tab) {
                                        AppTab.BLOCKS -> "🧩 Blocks"
                                        AppTab.XML -> "📄 XML"
                                        AppTab.CPP -> "💻 C++ Code"
                                        AppTab.SIMULATOR -> "🎮 Circuit Sim"
                                        AppTab.PAST_CODES -> "📜 Past Codes"
                                        AppTab.AUDIO -> "🎙️ Live Audio"
                                        AppTab.DIAGNOSTICS -> "⚠️ Diagnostics"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = if (isSelected) ScratchBlue else Color(0xFF576075)
                                )
                                if (tab == AppTab.DIAGNOSTICS && (errorCount > 0 || warningCount > 0)) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(if (errorCount > 0) ScratchRed else ScratchOrange)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (errorCount > 0) "$errorCount" else "$warningCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // Tab Content Body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (uiState.currentTab) {
                    AppTab.BLOCKS -> {
                        VisualBlocksTab(
                            viewModel = viewModel,
                            uiState = uiState
                        )
                    }

                    AppTab.XML -> {
                        BlocklyXmlTab(
                            viewModel = viewModel,
                            xmlText = uiState.xmlText
                        )
                    }

                    AppTab.CPP -> {
                        ArduinoCodeViewer(
                            output = uiState.sketchOutput,
                            selectedBoard = uiState.selectedBoard,
                            onSelectBoard = { viewModel.setBoard(it) },
                            onOpenDiagnostics = { viewModel.selectTab(AppTab.DIAGNOSTICS) }
                        )
                    }

                    AppTab.SIMULATOR -> {
                        CircuitCanvasView(
                            simulatorEngine = viewModel.simulatorEngine
                        )
                    }

                    AppTab.PAST_CODES -> {
                        PastCodesTab(
                            viewModel = viewModel,
                            savedSketches = savedSketches
                        )
                    }

                    AppTab.AUDIO -> {
                        LiveAudioTranscriberView(
                            audioManager = viewModel.audioTranscriber,
                            onAddBlockFromVoice = { action ->
                                when {
                                    action.contains("Pin 13 HIGH") -> viewModel.addBlock("arduino_digital_write")
                                    action.contains("Delay") -> viewModel.addBlock("arduino_delay")
                                    action.contains("Servo") -> viewModel.addBlock("servo_write")
                                    action.contains("Analog") -> viewModel.addBlock("arduino_analog_read")
                                    action.contains("Tone") -> viewModel.addBlock("tone_buzzer")
                                    action.contains("Serial") -> viewModel.addBlock("serial_println")
                                    action.contains("Sonar") -> viewModel.addBlock("sensor_ultrasonic_distance")
                                }
                            }
                        )
                    }

                    AppTab.DIAGNOSTICS -> {
                        DiagnosticsPanel(
                            diagnostics = uiState.sketchOutput.diagnostics,
                            onFocusBlock = { blockId -> viewModel.focusBlock(blockId) }
                        )
                    }
                }
            }
        }
    }

    // Modal Add Block Sheet
    if (uiState.isAddBlockSheetOpen) {
        AddBlockSheet(
            onDismiss = { viewModel.setAddBlockSheetOpen(false) },
            onSelectBlockType = { type -> viewModel.addBlock(type) }
        )
    }

    // Templates Dialog
    if (uiState.isTemplatesDialogOpen) {
        TemplateLibraryDialog(
            onDismiss = { viewModel.setTemplatesDialogOpen(false) },
            onSelectTemplate = { viewModel.loadTemplate(it) }
        )
    }

    // Save Dialog
    if (uiState.isSaveDialogOpen) {
        SaveSketchDialog(
            initialName = uiState.sketchName,
            initialDesc = uiState.sketchDescription,
            onDismiss = { viewModel.setSaveDialogOpen(false) },
            onSave = { name, desc ->
                viewModel.setSketchDescription(desc)
                viewModel.saveCurrentSketch(name)
            }
        )
    }

    // Saved Sketches Dialog
    if (uiState.isSavedSketchesDialogOpen) {
        SketchesListDialog(
            sketches = savedSketches,
            onDismiss = { viewModel.setSavedSketchesDialogOpen(false) },
            onSelectSketch = { viewModel.loadSavedSketch(it) },
            onDeleteSketch = { viewModel.deleteSavedSketch(it) }
        )
    }
}

@Composable
private fun VisualBlocksTab(
    viewModel: BlocklyViewModel,
    uiState: com.example.viewmodel.BlocklyUiState
) {
    if (uiState.blocks.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = ScratchBlue.copy(alpha = 0.6f),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Scratch Arduino Workspace",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E384D)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Snap together puzzle blocks to generate Arduino C++ code!",
                    fontSize = 13.sp,
                    color = Color(0xFF576075)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { viewModel.setAddBlockSheetOpen(true) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScratchOrange)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Puzzle Block")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.blocks, key = { it.id }) { block ->
                BlockCard(
                    block = block,
                    diagnostics = uiState.sketchOutput.diagnostics,
                    isFocused = block.id == uiState.focusedBlockId,
                    onUpdateField = { field, value ->
                        viewModel.updateBlockField(block.id, field, value)
                    },
                    onDelete = { viewModel.deleteBlock(block.id) },
                    onDuplicate = { viewModel.duplicateBlock(block.id) },
                    onMoveUp = { viewModel.moveBlock(block.id, -1) },
                    onMoveDown = { viewModel.moveBlock(block.id, 1) }
                )
            }
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }
}

@Composable
private fun BlocklyXmlTab(
    viewModel: BlocklyViewModel,
    xmlText: String
) {
    var editableXml by remember(xmlText) { mutableStateOf(xmlText) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Blockly XML Source",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color(0xFF2E384D)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { editableXml = xmlText },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("Reset", fontSize = 12.sp)
                }

                Button(
                    onClick = { viewModel.parseAndApplyXml(editableXml) },
                    colors = ButtonDefaults.buttonColors(containerColor = ScratchBlue),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Parse & Sync", fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = editableXml,
            onValueChange = {
                editableXml = it
                viewModel.updateXmlText(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .testTag("xml_editor_field"),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            ),
            placeholder = { Text("Paste or edit Blockly XML here...") }
        )
    }
}

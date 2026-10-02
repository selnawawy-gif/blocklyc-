package com.example.simulator

import com.example.model.BlockScope
import com.example.model.BlocklyBlock
import com.example.model.SimulatedCircuitState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CircuitSimulatorEngine(
    private val scope: CoroutineScope
) {
    private val _simState = MutableStateFlow(SimulatedCircuitState())
    val simState: StateFlow<SimulatedCircuitState> = _simState.asStateFlow()

    private var simJob: Job? = null
    private var currentBlocks: List<BlocklyBlock> = emptyList()

    fun updateBlocks(blocks: List<BlocklyBlock>) {
        currentBlocks = blocks
    }

    fun start() {
        if (simJob?.isActive == true) return
        _simState.update { it.copy(isRunning = true) }

        simJob = scope.launch(Dispatchers.Default) {
            // First run setup blocks once
            runSetupCycle()

            // Then repeatedly run loop blocks
            while (isActive) {
                runLoopCycle()
                delay(_simState.value.executionSpeedMs)
            }
        }
    }

    fun pause() {
        simJob?.cancel()
        simJob = null
        _simState.update { it.copy(isRunning = false) }
    }

    fun step() {
        scope.launch(Dispatchers.Default) {
            runLoopCycle()
        }
    }

    fun reset() {
        pause()
        _simState.value = SimulatedCircuitState(
            serialLog = listOf("--- Circuit Simulation Reset ---")
        )
    }

    fun setSpeedMs(delayMs: Long) {
        _simState.update { it.copy(executionSpeedMs = delayMs) }
    }

    // Interactive user inputs
    fun setButtonPressed(pressed: Boolean) {
        _simState.update { it.copy(buttonPressed = pressed) }
    }

    fun setSlideSwitch(on: Boolean) {
        _simState.update { it.copy(slideSwitchOn = on) }
    }

    fun setPotentiometer(value: Int) {
        _simState.update { it.copy(potentiometerValue = value.coerceIn(0, 1023)) }
    }

    fun setLdrLight(value: Int) {
        _simState.update { it.copy(ldrLightLevel = value.coerceIn(0, 1023)) }
    }

    fun setUltrasonicDistance(cm: Float) {
        _simState.update { it.copy(ultrasonicDistanceCm = cm.coerceIn(2f, 400f)) }
    }

    fun setAmbientTemperature(tempC: Float) {
        _simState.update { it.copy(ambientTemperatureC = tempC) }
    }

    fun setAmbientHumidity(humidityPercent: Float) {
        _simState.update { it.copy(ambientHumidityPercent = humidityPercent) }
    }

    fun setPirMotion(detected: Boolean) {
        _simState.update { it.copy(pirMotionDetected = detected) }
    }

    fun clearSerialLog() {
        _simState.update { it.copy(serialLog = emptyList()) }
    }

    fun sendSerialInput(text: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = "[$time] TX >> $text"
        _simState.update {
            val list = it.serialLog.takeLast(100).toMutableList()
            list.add(logEntry)
            list.add("[$time] RX << Echo: $text (OK)")
            it.copy(serialLog = list)
        }
    }

    private fun runSetupCycle() {
        val setupBlocks = currentBlocks.filter { it.scope == BlockScope.SETUP }
        for (block in setupBlocks) {
            executeBlock(block)
        }
    }

    private fun runLoopCycle() {
        val loopBlocks = currentBlocks.filter { it.scope == BlockScope.LOOP }
        for (block in loopBlocks) {
            executeBlock(block)
        }

        // Record oscilloscope trace for Pin 13 (or probe)
        val p13Val = _simState.value.digitalPinOutputs[13] ?: 0
        val normP13 = if (p13Val > 0) 5.0f else 0.0f

        _simState.update {
            val history = (it.oscilloscopeSignalHistory + normP13).takeLast(40)
            it.copy(
                cycleCount = it.cycleCount + 1,
                oscilloscopeSignalHistory = history
            )
        }
    }

    private fun executeBlock(block: BlocklyBlock) {
        if (!block.isSupportedOnArduino || block.disabled) return

        when (block.type) {
            "arduino_digital_write" -> {
                val pin = block.fields["PIN"]?.toIntOrNull() ?: 13
                val state = block.fields["STATE"] ?: "HIGH"
                val level = if (state == "HIGH") 255 else 0
                _simState.update {
                    val pins = it.digitalPinOutputs.toMutableMap()
                    pins[pin] = level
                    it.copy(digitalPinOutputs = pins)
                }
            }

            "arduino_analog_write" -> {
                val pin = block.fields["PIN"]?.toIntOrNull() ?: 9
                val value = block.fields["VALUE"]?.toIntOrNull()?.coerceIn(0, 255) ?: 128
                _simState.update {
                    val pins = it.digitalPinOutputs.toMutableMap()
                    pins[pin] = value
                    it.copy(digitalPinOutputs = pins)
                }
            }

            "actuator_dc_motor_speed" -> {
                val speed = block.fields["SPEED"]?.toIntOrNull()?.coerceIn(0, 255) ?: 200
                val rpm = (speed / 255.0f) * 3200.0f
                _simState.update { it.copy(dcMotorRpm = rpm) }
            }

            "actuator_relay_switch" -> {
                val state = block.fields["STATE"] ?: "HIGH"
                val closed = (state == "HIGH")
                _simState.update { it.copy(relayClosed = closed) }
            }

            "servo_write" -> {
                val angle = block.fields["ANGLE"]?.toFloatOrNull()?.coerceIn(0f, 180f) ?: 90f
                _simState.update { it.copy(servoAngleDeg = angle) }
            }

            "tone_buzzer" -> {
                val freq = block.fields["FREQ"]?.toIntOrNull() ?: 440
                _simState.update { it.copy(buzzerFrequencyHz = freq, buzzerIsActive = true) }
            }

            "notone_buzzer" -> {
                _simState.update { it.copy(buzzerIsActive = false) }
            }

            "display_lcd_i2c_print" -> {
                val line = block.fields["LINE"]?.toIntOrNull() ?: 0
                val text = block.fields["TEXT"] ?: ""
                _simState.update {
                    if (line == 0) it.copy(lcdLine1 = text) else it.copy(lcdLine2 = text)
                }
            }

            "display_neopixel_set_color" -> {
                val r = block.fields["RED"]?.toIntOrNull()?.coerceIn(0, 255) ?: 255
                val g = block.fields["GREEN"]?.toIntOrNull()?.coerceIn(0, 255) ?: 0
                val b = block.fields["BLUE"]?.toIntOrNull()?.coerceIn(0, 255) ?: 0
                val argb = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                _simState.update { it.copy(rgbColor = argb.toLong()) }
            }

            "serial_println" -> {
                val msg = block.fields["MESSAGE"] ?: ""
                logSerial(msg)
            }

            "controls_if", "controls_ifelse" -> {
                val conditionMet = evaluateCondition(block)
                if (conditionMet) {
                    for (child in block.childBlocks) {
                        executeBlock(child)
                    }
                } else if (block.elseBlocks.isNotEmpty()) {
                    for (elseChild in block.elseBlocks) {
                        executeBlock(elseChild)
                    }
                }
            }

            "controls_repeat_ext" -> {
                val times = block.fields["TIMES"]?.toIntOrNull()?.coerceIn(1, 10) ?: 3
                for (i in 0 until times) {
                    for (child in block.childBlocks) {
                        executeBlock(child)
                    }
                }
            }
        }
    }

    private fun evaluateCondition(block: BlocklyBlock): Boolean {
        val op = block.fields["OP"] ?: "=="
        val val1Str = block.fields["VAL1"] ?: ""
        val val2Str = block.fields["VAL2"] ?: ""

        val leftVal: Double = when {
            val1Str.contains("digitalRead(2)", ignoreCase = true) -> if (_simState.value.buttonPressed) 0.0 else 1.0
            val1Str.contains("analogRead(A0)", ignoreCase = true) -> _simState.value.potentiometerValue.toDouble()
            val1Str.contains("analogRead(A1)", ignoreCase = true) -> _simState.value.ldrLightLevel.toDouble()
            else -> val1Str.toDoubleOrNull() ?: 1.0
        }

        val rightVal: Double = when {
            val2Str.equals("HIGH", ignoreCase = true) -> 1.0
            val2Str.equals("LOW", ignoreCase = true) -> 0.0
            else -> val2Str.toDoubleOrNull() ?: 0.0
        }

        return when (op) {
            "==" -> kotlin.math.abs(leftVal - rightVal) < 0.001
            "!=" -> kotlin.math.abs(leftVal - rightVal) >= 0.001
            "<" -> leftVal < rightVal
            ">" -> leftVal > rightVal
            "<=" -> leftVal <= rightVal
            ">=" -> leftVal >= rightVal
            else -> true
        }
    }

    private fun logSerial(text: String) {
        val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val logEntry = "[$time] $text"
        _simState.update {
            val list = it.serialLog.takeLast(100).toMutableList()
            list.add(logEntry)
            it.copy(serialLog = list)
        }
    }
}

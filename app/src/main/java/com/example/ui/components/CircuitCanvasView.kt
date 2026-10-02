package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SimulatedCircuitState
import com.example.simulator.CircuitSimulatorEngine
import com.example.ui.theme.*

@Composable
fun CircuitCanvasView(
    simulatorEngine: CircuitSimulatorEngine,
    modifier: Modifier = Modifier
) {
    val simState by simulatorEngine.simState.collectAsState()
    var selectedToolCategory by remember { mutableStateOf("All") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("circuit_simulation_tab")
    ) {
        // Master Simulation Control Toolbar
        SimulationControlBar(
            simState = simState,
            onStart = { simulatorEngine.start() },
            onPause = { simulatorEngine.pause() },
            onStep = { simulatorEngine.step() },
            onReset = { simulatorEngine.reset() },
            onSpeedChange = { simulatorEngine.setSpeedMs(it) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Circuit Workbench Layout (Scrollable panels)
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Digital Oscilloscope Probe & Logic Analyzer
            item {
                OscilloscopeCard(simState = simState)
            }

            // 2. 16x2 Character LCD (I2C) Display
            item {
                Lcd1602Card(simState = simState)
            }

            // 3. Actuators: SG90 Micro Servo & DC Motor Fan
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ServoMotorCard(simState = simState)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DcMotorFanCard(simState = simState)
                    }
                }
            }

            // 4. Outputs: LEDs, Traffic Light & RGB NeoPixel
            item {
                LedOutputsCard(simState = simState)
            }

            // 5. Acoustic Piezo Buzzer & 5V Relay Module
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        PiezoBuzzerCard(simState = simState)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        RelayModuleCard(simState = simState)
                    }
                }
            }

            // 6. Interactive Inputs: Push Button, Slide Switch & Potentiometer
            item {
                InteractiveInputsCard(
                    simState = simState,
                    onButtonToggle = { simulatorEngine.setButtonPressed(it) },
                    onSlideSwitchToggle = { simulatorEngine.setSlideSwitch(it) },
                    onPotentiometerChange = { simulatorEngine.setPotentiometer(it) }
                )
            }

            // 7. Interactive Sensors: HC-SR04 Ultrasonic Sonar & LDR Light Sensor
            item {
                InteractiveSensorsCard(
                    simState = simState,
                    onDistanceChange = { simulatorEngine.setUltrasonicDistance(it) },
                    onLdrChange = { simulatorEngine.setLdrLight(it) },
                    onTempChange = { simulatorEngine.setAmbientTemperature(it) },
                    onHumidityChange = { simulatorEngine.setAmbientHumidity(it) },
                    onMotionTrigger = { simulatorEngine.setPirMotion(!simState.pirMotionDetected) }
                )
            }

            // 8. Virtual Serial Monitor Terminal
            item {
                SerialMonitorCard(
                    simState = simState,
                    onSend = { simulatorEngine.sendSerialInput(it) },
                    onClear = { simulatorEngine.clearSerialLog() }
                )
            }
        }
    }
}

// -------------------------------------------------------------
// Component Cards
// -------------------------------------------------------------

@Composable
private fun SimulationControlBar(
    simState: SimulatedCircuitState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStep: () -> Unit,
    onReset: () -> Unit,
    onSpeedChange: (Long) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Run / Pause / Step / Reset Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (simState.isRunning) {
                    FilledTonalButton(
                        onClick = onPause,
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = WarningOrange, contentColor = Color.White),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = "Pause", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Pause", fontSize = 12.sp)
                    }
                } else {
                    Button(
                        onClick = onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onStep,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FastForward, contentDescription = "Step", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Step", fontSize = 12.sp)
                }

                IconButton(onClick = onReset, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                }
            }

            // Speed & Clock Cycles
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Cycles: ${simState.cycleCount}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                AssistChip(
                    onClick = {
                        val nextSpeed = when (simState.executionSpeedMs) {
                            200L -> 100L
                            100L -> 50L
                            50L -> 500L
                            else -> 200L
                        }
                        onSpeedChange(nextSpeed)
                    },
                    label = {
                        Text(
                            text = when (simState.executionSpeedMs) {
                                50L -> "4x Fast"
                                100L -> "2x Fast"
                                200L -> "1x Norm"
                                else -> "0.5x Slow"
                            },
                            fontSize = 11.sp
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun OscilloscopeCard(simState: SimulatedCircuitState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF00FF66)))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Real-Time Logic Oscilloscope (Probe: Pin 13)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
                val currentVolts = if ((simState.digitalPinOutputs[13] ?: 0) > 0) "5.0V (HIGH)" else "0.0V (LOW)"
                Text(
                    text = currentVolts,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color(0xFF00FF66),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Waveform
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFF020617), RoundedCornerShape(6.dp))
            ) {
                val history = simState.oscilloscopeSignalHistory
                if (history.size < 2) return@Canvas

                val stepX = size.width / (history.size - 1).coerceAtLeast(1)
                val path = Path()

                history.forEachIndexed { index, volts ->
                    val x = index * stepX
                    // 5V is top, 0V is bottom
                    val y = size.height - (volts / 5.0f * (size.height - 12f)) - 6f
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = Color(0xFF00FF66),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun Lcd1602Card(simState: SimulatedCircuitState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "16x2 Character LCD (I2C Address 0x27)",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Blue Retro LCD Screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp)),
                color = Color(0xFF0040A0)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = simState.lcdLine1.padEnd(16, ' ').take(16),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0F2FE),
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = simState.lcdLine2.padEnd(16, ' ').take(16),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE0F2FE),
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ServoMotorCard(simState: SimulatedCircuitState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SG90 Micro Servo (Pin 9)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Rotating Servo Horn
            Canvas(
                modifier = Modifier.size(90.dp)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                // Draw servo body circle
                drawCircle(color = Color(0xFF0284C7), radius = size.width / 2.5f, center = center)
                drawCircle(color = Color(0xFF0369A1), radius = size.width / 5f, center = center)

                // Draw rotating arm
                val angleRad = Math.toRadians((simState.servoAngleDeg - 90.0).toDouble())
                val armLength = size.width / 2.2f
                val endX = center.x + armLength * Math.cos(angleRad).toFloat()
                val endY = center.y + armLength * Math.sin(angleRad).toFloat()

                drawLine(
                    color = Color.White,
                    start = center,
                    end = Offset(endX, endY),
                    strokeWidth = 6.dp.toPx()
                )
                drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(endX, endY))
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Angle: ${simState.servoAngleDeg.toInt()}°",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ArduinoTeal
            )
        }
    }
}

@Composable
private fun DcMotorFanCard(simState: SimulatedCircuitState) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = (1000f / ((simState.dcMotorRpm / 100f).coerceAtLeast(0.1f))).toInt().coerceIn(40, 2000),
                easing = LinearEasing
            )
        )
    )

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "DC Motor Fan (PWM Pin 3)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Rotating Fan Blades
            Canvas(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .rotate(if (simState.dcMotorRpm > 0) rotation else 0f)
            ) {
                val center = Offset(size.width / 2, size.height / 2)
                val bladeLen = size.width / 2.3f
                drawCircle(color = if (simState.dcMotorRpm > 0) Color(0xFFE47128) else Color.Gray, radius = 7.dp.toPx(), center = center)
                for (i in 0 until 4) {
                    val angle = Math.toRadians((i * 90.0))
                    val endX = center.x + bladeLen * Math.cos(angle).toFloat()
                    val endY = center.y + bladeLen * Math.sin(angle).toFloat()
                    drawLine(
                        color = if (simState.dcMotorRpm > 0) Color(0xFFFF9800) else Color.LightGray,
                        start = center,
                        end = Offset(endX, endY),
                        strokeWidth = 10.dp.toPx()
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Speed: ${simState.dcMotorRpm.toInt()} RPM",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = ArduinoAmber
            )
        }
    }
}

@Composable
private fun LedOutputsCard(simState: SimulatedCircuitState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "LEDs & Visual Signaling Components",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Built-in LED 13
                val p13On = (simState.digitalPinOutputs[13] ?: 0) > 0
                LedBulbWidget(
                    name = "Builtin LED",
                    pin = "Pin 13",
                    activeColor = Color(0xFFF59E0B),
                    isOn = p13On
                )

                // Red LED
                val p12On = (simState.digitalPinOutputs[12] ?: 0) > 0 || (simState.digitalPinOutputs[10] ?: 0) > 0
                LedBulbWidget(
                    name = "Red LED",
                    pin = "Pin 10/12",
                    activeColor = Color(0xFFEF4444),
                    isOn = p12On
                )

                // Yellow LED
                val p9On = (simState.digitalPinOutputs[9] ?: 0) > 0
                LedBulbWidget(
                    name = "Yellow LED",
                    pin = "Pin 9",
                    activeColor = Color(0xFFEAB308),
                    isOn = p9On
                )

                // Green LED
                val p8On = (simState.digitalPinOutputs[8] ?: 0) > 0
                LedBulbWidget(
                    name = "Green LED",
                    pin = "Pin 8",
                    activeColor = Color(0xFF22C55E),
                    isOn = p8On
                )

                // NeoPixel RGB
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(simState.rgbColor))
                            .border(2.dp, Color.LightGray, CircleShape)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("NeoPixel", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("Pin 5", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun LedBulbWidget(
    name: String,
    pin: String,
    activeColor: Color,
    isOn: Boolean
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isOn) activeColor else Color(0xFF475569))
                .border(2.dp, if (isOn) activeColor else Color.Transparent, CircleShape)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(name, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Text(pin, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun PiezoBuzzerCard(simState: SimulatedCircuitState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Piezo Buzzer (Pin 11)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = if (simState.buzzerIsActive) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                contentDescription = null,
                tint = if (simState.buzzerIsActive) ErrorRed else Color.Gray,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (simState.buzzerIsActive) "${simState.buzzerFrequencyHz} Hz Tone" else "Silent",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (simState.buzzerIsActive) ErrorRed else Color.Gray
            )
        }
    }
}

@Composable
private fun RelayModuleCard(simState: SimulatedCircuitState) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "5V Relay Module (Pin 4)",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Icon(
                imageVector = if (simState.relayClosed) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                contentDescription = null,
                tint = if (simState.relayClosed) SuccessGreen else Color.Gray,
                modifier = Modifier.size(42.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (simState.relayClosed) "CLOSED (NO ON)" else "OPEN (NC ON)",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (simState.relayClosed) SuccessGreen else Color.Gray
            )
        }
    }
}

@Composable
private fun InteractiveInputsCard(
    simState: SimulatedCircuitState,
    onButtonToggle: (Boolean) -> Unit,
    onSlideSwitchToggle: (Boolean) -> Unit,
    onPotentiometerChange: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Interactive Switches & Potentiometer",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tactile Push Button (Pin 2)
                Button(
                    onClick = { onButtonToggle(!simState.buttonPressed) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (simState.buttonPressed) ErrorRed else ArduinoTeal
                    ),
                    modifier = Modifier.testTag("push_button_pin2")
                ) {
                    Icon(Icons.Default.TouchApp, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (simState.buttonPressed) "Button Pressed (LOW)" else "Press Button (Pin 2)", fontSize = 12.sp)
                }

                // Slide Switch (Pin 4)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Switch (Pin 4):", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = simState.slideSwitchOn,
                        onCheckedChange = onSlideSwitchToggle
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 10k Potentiometer (Pin A0)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("10kΩ Potentiometer (Pin A0)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${simState.potentiometerValue} / 1023 (${(simState.potentiometerValue / 1023f * 5.0f).let { String.format("%.2f", it) }}V)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = ArduinoTeal,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = simState.potentiometerValue.toFloat(),
                    onValueChange = { onPotentiometerChange(it.toInt()) },
                    valueRange = 0f..1023f,
                    modifier = Modifier.testTag("potentiometer_slider")
                )
            }
        }
    }
}

@Composable
private fun InteractiveSensorsCard(
    simState: SimulatedCircuitState,
    onDistanceChange: (Float) -> Unit,
    onLdrChange: (Int) -> Unit,
    onTempChange: (Float) -> Unit,
    onHumidityChange: (Float) -> Unit,
    onMotionTrigger: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Sensor Telemetry & Obstacle Simulators",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(10.dp))

            // HC-SR04 Ultrasonic Sonar Distance (2 - 400 cm)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("HC-SR04 Sonar Distance (Trig: 11, Echo: 12)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${simState.ultrasonicDistanceCm.toInt()} cm",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ArduinoTeal
                    )
                }
                Slider(
                    value = simState.ultrasonicDistanceCm,
                    onValueChange = onDistanceChange,
                    valueRange = 2f..400f,
                    modifier = Modifier.testTag("ultrasonic_distance_slider")
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // LDR Light Sensor (Pin A1)
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("LDR Ambient Light Sensor (Pin A1)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${simState.ldrLightLevel} lux",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = ArduinoAmber,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = simState.ldrLightLevel.toFloat(),
                    onValueChange = { onLdrChange(it.toInt()) },
                    valueRange = 0f..1023f
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // DHT11 & PIR
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onMotionTrigger,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DirectionsWalk, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (simState.pirMotionDetected) "Motion Active" else "Trigger PIR (Pin 2)", fontSize = 11.sp)
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("DHT11 Sensor (Pin 7)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${simState.ambientTemperatureC}°C | ${simState.ambientHumidityPercent.toInt()}%",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = ArduinoTeal
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SerialMonitorCard(
    simState: SimulatedCircuitState,
    onSend: (String) -> Unit,
    onClear: () -> Unit
) {
    var textInput by remember { mutableStateOf("") }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Serial Monitor Terminal (@ 9600 baud)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = Color.White
                    )
                }
                IconButton(onClick = onClear, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Output Terminal Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF020617))
                    .padding(8.dp)
                    .verticalScroll(rememberScrollState(Int.MAX_VALUE))
            ) {
                Column {
                    for (log in simState.serialLog) {
                        Text(
                            text = log,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = if (log.contains("TX >>")) Color(0xFF38BDF8) else Color(0xFF4ADE80)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Send command row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Send serial command...", color = Color.Gray, fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color(0xFF334155)
                    ),
                    singleLine = true
                )
                Spacer(modifier = Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = {
                        if (textInput.isNotBlank()) {
                            onSend(textInput.trim())
                            textInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Default.Send, contentDescription = "Send", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

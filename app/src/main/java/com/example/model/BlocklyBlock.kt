package com.example.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.*
import java.util.UUID

enum class BlockScope {
    SETUP,
    LOOP,
    GLOBAL
}

enum class BlockCategory(
    val title: String,
    val color: Color,
    val iconName: String
) {
    ARDUINO_IO("Arduino I/O", CategoryIO, "Memory"),
    TIME("Timing", CategoryTime, "Schedule"),
    SERIAL("Serial Monitor", CategorySerial, "Terminal"),
    CONTROL("Control & Logic", CategoryControl, "AltRoute"),
    MATH("Math & Numbers", CategoryMath, "Calculate"),
    ACTUATORS("Actuators & Motors", CategoryActuators, "PrecisionManufacturing"),
    SENSORS("Sensors & Inputs", CategorySensors, "Sensors"),
    DISPLAYS("Displays & LEDs", CategoryIO, "Tv"),
    VARIABLES("Variables", CategoryVariables, "DataObject"),
    INCOMPATIBLE("Incompatible (Web/DOM)", ErrorRed, "Warning")
}

data class BlockDefinition(
    val type: String,
    val category: BlockCategory,
    val title: String,
    val defaultScope: BlockScope,
    val defaultFields: Map<String, String> = emptyMap(),
    val requiredSockets: List<String> = emptyList(),
    val isStatement: Boolean = true,
    val allowsChildren: Boolean = false,
    val isSupportedOnArduino: Boolean = true,
    val incompatibilityReason: String? = null
)

data class BlocklyBlock(
    val id: String = UUID.randomUUID().toString().take(8),
    val type: String,
    val category: BlockCategory,
    val title: String,
    val scope: BlockScope = BlockScope.LOOP,
    val fields: MutableMap<String, String> = mutableMapOf(),
    val childBlocks: MutableList<BlocklyBlock> = mutableListOf(),
    val elseBlocks: MutableList<BlocklyBlock> = mutableListOf(),
    val isCollapsed: Boolean = false,
    val disabled: Boolean = false,
    val customXml: String? = null,
    val isSupportedOnArduino: Boolean = true,
    val incompatibilityReason: String? = null,
    val allowsChildren: Boolean = false
) {
    fun deepCopy(): BlocklyBlock {
        return copy(
            id = UUID.randomUUID().toString().take(8),
            fields = HashMap(fields),
            childBlocks = childBlocks.map { it.deepCopy() }.toMutableList(),
            elseBlocks = elseBlocks.map { it.deepCopy() }.toMutableList(),
            allowsChildren = allowsChildren
        )
    }
}

object BlockRegistry {
    val DEFINITIONS = mapOf(
        // Arduino I/O
        "arduino_digital_write" to BlockDefinition(
            type = "arduino_digital_write",
            category = BlockCategory.ARDUINO_IO,
            title = "Digital Write",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "13", "STATE" to "HIGH"),
            requiredSockets = listOf("PIN", "STATE")
        ),
        "arduino_digital_read" to BlockDefinition(
            type = "arduino_digital_read",
            category = BlockCategory.ARDUINO_IO,
            title = "Digital Read",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "2"),
            requiredSockets = listOf("PIN"),
            isStatement = false
        ),
        "arduino_analog_write" to BlockDefinition(
            type = "arduino_analog_write",
            category = BlockCategory.ARDUINO_IO,
            title = "Analog Write (PWM)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "9", "VALUE" to "128"),
            requiredSockets = listOf("PIN", "VALUE")
        ),
        "arduino_analog_read" to BlockDefinition(
            type = "arduino_analog_read",
            category = BlockCategory.ARDUINO_IO,
            title = "Analog Read (0-1023)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "A0"),
            requiredSockets = listOf("PIN"),
            isStatement = false
        ),
        "arduino_pin_mode" to BlockDefinition(
            type = "arduino_pin_mode",
            category = BlockCategory.ARDUINO_IO,
            title = "Pin Mode",
            defaultScope = BlockScope.SETUP,
            defaultFields = mapOf("PIN" to "13", "MODE" to "OUTPUT"),
            requiredSockets = listOf("PIN", "MODE")
        ),

        // Timing
        "arduino_delay" to BlockDefinition(
            type = "arduino_delay",
            category = BlockCategory.TIME,
            title = "Delay Milliseconds",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("TIME" to "1000"),
            requiredSockets = listOf("TIME")
        ),
        "arduino_delay_microseconds" to BlockDefinition(
            type = "arduino_delay_microseconds",
            category = BlockCategory.TIME,
            title = "Delay Microseconds",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("TIME" to "50"),
            requiredSockets = listOf("TIME")
        ),
        "arduino_millis" to BlockDefinition(
            type = "arduino_millis",
            category = BlockCategory.TIME,
            title = "Current Millis()",
            defaultScope = BlockScope.LOOP,
            isStatement = false
        ),

        // Serial
        "serial_begin" to BlockDefinition(
            type = "serial_begin",
            category = BlockCategory.SERIAL,
            title = "Serial.begin(baud)",
            defaultScope = BlockScope.SETUP,
            defaultFields = mapOf("BAUD" to "9600"),
            requiredSockets = listOf("BAUD")
        ),
        "serial_println" to BlockDefinition(
            type = "serial_println",
            category = BlockCategory.SERIAL,
            title = "Serial Printline",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("MESSAGE" to "System running!"),
            requiredSockets = listOf("MESSAGE")
        ),
        "serial_print" to BlockDefinition(
            type = "serial_print",
            category = BlockCategory.SERIAL,
            title = "Serial Print (inline)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("MESSAGE" to "Pin Val: "),
            requiredSockets = listOf("MESSAGE")
        ),

        // Control & Logic
        "controls_if" to BlockDefinition(
            type = "controls_if",
            category = BlockCategory.CONTROL,
            title = "If Condition",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("VAL1" to "digitalRead(2)", "OP" to "==", "VAL2" to "HIGH"),
            requiredSockets = listOf("CONDITION"),
            allowsChildren = true
        ),
        "controls_ifelse" to BlockDefinition(
            type = "controls_ifelse",
            category = BlockCategory.CONTROL,
            title = "If / Else Condition",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("VAL1" to "analogRead(A0)", "OP" to ">", "VAL2" to "500"),
            requiredSockets = listOf("CONDITION"),
            allowsChildren = true
        ),
        "controls_repeat_ext" to BlockDefinition(
            type = "controls_repeat_ext",
            category = BlockCategory.CONTROL,
            title = "Repeat N Times",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("TIMES" to "5"),
            requiredSockets = listOf("TIMES"),
            allowsChildren = true
        ),
        "controls_whileUntil" to BlockDefinition(
            type = "controls_whileUntil",
            category = BlockCategory.CONTROL,
            title = "While Loop",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("CONDITION" to "digitalRead(2) == LOW"),
            requiredSockets = listOf("CONDITION"),
            allowsChildren = true
        ),

        // Math & Logic
        "math_number" to BlockDefinition(
            type = "math_number",
            category = BlockCategory.MATH,
            title = "Number Literal",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("NUM" to "0"),
            isStatement = false
        ),
        "math_arithmetic" to BlockDefinition(
            type = "math_arithmetic",
            category = BlockCategory.MATH,
            title = "Arithmetic Math",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("A" to "10", "OP" to "ADD", "B" to "5"),
            requiredSockets = listOf("A", "B"),
            isStatement = false
        ),
        "arduino_map_range" to BlockDefinition(
            type = "arduino_map_range",
            category = BlockCategory.MATH,
            title = "Map Range map(val, from, to)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("VAL" to "analogRead(A0)", "FROM_LOW" to "0", "FROM_HIGH" to "1023", "TO_LOW" to "0", "TO_HIGH" to "255"),
            requiredSockets = listOf("VAL"),
            isStatement = false
        ),

        // Actuators
        "servo_attach" to BlockDefinition(
            type = "servo_attach",
            category = BlockCategory.ACTUATORS,
            title = "Servo.attach(pin)",
            defaultScope = BlockScope.SETUP,
            defaultFields = mapOf("PIN" to "9", "NAME" to "myServo"),
            requiredSockets = listOf("PIN")
        ),
        "servo_write" to BlockDefinition(
            type = "servo_write",
            category = BlockCategory.ACTUATORS,
            title = "Servo.write(angle 0-180)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "9", "NAME" to "myServo", "ANGLE" to "90"),
            requiredSockets = listOf("ANGLE")
        ),
        "actuator_dc_motor_speed" to BlockDefinition(
            type = "actuator_dc_motor_speed",
            category = BlockCategory.ACTUATORS,
            title = "DC Motor Speed (PWM)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "3", "SPEED" to "200"),
            requiredSockets = listOf("PIN", "SPEED")
        ),
        "actuator_relay_switch" to BlockDefinition(
            type = "actuator_relay_switch",
            category = BlockCategory.ACTUATORS,
            title = "Relay Switch (NO/NC)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "4", "STATE" to "HIGH"),
            requiredSockets = listOf("PIN", "STATE")
        ),
        "tone_buzzer" to BlockDefinition(
            type = "tone_buzzer",
            category = BlockCategory.ACTUATORS,
            title = "Piezo Tone(freq, dur)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "11", "FREQ" to "440", "DURATION" to "300"),
            requiredSockets = listOf("PIN", "FREQ")
        ),
        "notone_buzzer" to BlockDefinition(
            type = "notone_buzzer",
            category = BlockCategory.ACTUATORS,
            title = "Stop Tone (noTone)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "11"),
            requiredSockets = listOf("PIN")
        ),

        // Sensors
        "sensor_ultrasonic_distance" to BlockDefinition(
            type = "sensor_ultrasonic_distance",
            category = BlockCategory.SENSORS,
            title = "HC-SR04 Ultrasonic Sonar",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("TRIG_PIN" to "11", "ECHO_PIN" to "12", "VAR" to "distanceCm"),
            requiredSockets = listOf("TRIG_PIN", "ECHO_PIN")
        ),
        "sensor_potentiometer_read" to BlockDefinition(
            type = "sensor_potentiometer_read",
            category = BlockCategory.SENSORS,
            title = "Read Potentiometer (A0-A5)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "A0", "VAR" to "potVal")
        ),
        "sensor_ldr_read" to BlockDefinition(
            type = "sensor_ldr_read",
            category = BlockCategory.SENSORS,
            title = "Read LDR Light Sensor",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "A1", "VAR" to "lightLux")
        ),
        "sensor_dht11_read" to BlockDefinition(
            type = "sensor_dht11_read",
            category = BlockCategory.SENSORS,
            title = "DHT11 Temp & Humidity",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "7", "TEMP_VAR" to "temperature", "HUM_VAR" to "humidity")
        ),

        // Displays & LEDs
        "display_lcd_i2c_print" to BlockDefinition(
            type = "display_lcd_i2c_print",
            category = BlockCategory.DISPLAYS,
            title = "16x2 LCD Print Text",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("LINE" to "0", "COL" to "0", "TEXT" to "Temp: 24C"),
            requiredSockets = listOf("TEXT")
        ),
        "display_neopixel_set_color" to BlockDefinition(
            type = "display_neopixel_set_color",
            category = BlockCategory.DISPLAYS,
            title = "WS2812 RGB NeoPixel Color",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("PIN" to "5", "PIXEL" to "0", "RED" to "0", "GREEN" to "255", "BLUE" to "128"),
            requiredSockets = listOf("PIN", "RED", "GREEN", "BLUE")
        ),

        // Variables
        "variable_declare" to BlockDefinition(
            type = "variable_declare",
            category = BlockCategory.VARIABLES,
            title = "Declare Global Variable",
            defaultScope = BlockScope.GLOBAL,
            defaultFields = mapOf("TYPE" to "int", "VAR" to "myCounter", "VALUE" to "0"),
            requiredSockets = listOf("VAR")
        ),
        "variable_set" to BlockDefinition(
            type = "variable_set",
            category = BlockCategory.VARIABLES,
            title = "Set Variable Value",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("VAR" to "myCounter", "VALUE" to "myCounter + 1"),
            requiredSockets = listOf("VAR", "VALUE")
        ),

        // Incompatible Web/DOM Blocks (Trigger robust error handling as requested)
        "web_fetch" to BlockDefinition(
            type = "web_fetch",
            category = BlockCategory.INCOMPATIBLE,
            title = "Web Fetch (HTTP GET)",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("URL" to "https://api.example.com/data"),
            isSupportedOnArduino = false,
            incompatibilityReason = "Browser DOM/Fetch API cannot run on standard Arduino C++ microcontroller runtime without WiFi/Ethernet shield."
        ),
        "document_getElementById" to BlockDefinition(
            type = "document_getElementById",
            category = BlockCategory.INCOMPATIBLE,
            title = "DOM Element Query",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("ID" to "buttonElement"),
            isSupportedOnArduino = false,
            incompatibilityReason = "DOM query block belongs to web browsers, not Arduino microcontrollers. Use digitalRead(pin) instead."
        ),
        "window_alert" to BlockDefinition(
            type = "window_alert",
            category = BlockCategory.INCOMPATIBLE,
            title = "Window Alert Dialog",
            defaultScope = BlockScope.LOOP,
            defaultFields = mapOf("TEXT" to "Motion detected!"),
            isSupportedOnArduino = false,
            incompatibilityReason = "GUI Alert dialog is incompatible with bare-metal Arduino hardware. Use Serial.println() or LCD 16x2 print instead."
        )
    )

    fun createBlock(type: String, scope: BlockScope? = null): BlocklyBlock {
        val def = DEFINITIONS[type] ?: BlockDefinition(
            type = type,
            category = BlockCategory.INCOMPATIBLE,
            title = "Unknown Block ($type)",
            defaultScope = scope ?: BlockScope.LOOP,
            isSupportedOnArduino = false,
            incompatibilityReason = "Unknown block type '$type' is not recognized by Arduino C++ compiler."
        )

        return BlocklyBlock(
            id = UUID.randomUUID().toString().take(8),
            type = def.type,
            category = def.category,
            title = def.title,
            scope = scope ?: def.defaultScope,
            fields = HashMap(def.defaultFields),
            isSupportedOnArduino = def.isSupportedOnArduino,
            incompatibilityReason = def.incompatibilityReason,
            allowsChildren = def.allowsChildren
        )
    }
}

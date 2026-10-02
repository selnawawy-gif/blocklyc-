package com.example.engine

import com.example.model.*

class ArduinoCodeGenerator(
    private val validator: BlocklyValidator = BlocklyValidator()
) {

    fun generate(
        blocks: List<BlocklyBlock>,
        board: ArduinoBoardType = ArduinoBoardType.UNO
    ): ArduinoSketchOutput {
        val diagnostics = validator.validate(blocks, board).toMutableList()

        val includes = mutableMapOf<String, String>() // include line -> explanation
        val globalObjects = mutableListOf<Pair<String, String>>() // object declaration -> explanation
        val globalVars = mutableListOf<Triple<String, String, String>>() // type, name, value
        val setupStatements = mutableListOf<Pair<String, String>>() // code -> comment
        val loopStatements = mutableListOf<Pair<String, String>>() // code -> comment
        val usedPins = mutableSetOf<String>()
        val activePeripherals = mutableListOf<String>()

        val pinModes = mutableMapOf<String, Pair<String, String>>() // pin -> Pair(MODE, purpose)

        // Pre-scan blocks for library dependencies, pinModes, and hardware objects
        preScanBlocks(
            blocks = blocks,
            includes = includes,
            globalObjects = globalObjects,
            pinModes = pinModes,
            usedPins = usedPins,
            activePeripherals = activePeripherals
        )

        // Global variables
        for (block in blocks) {
            if (block.type == "variable_declare") {
                val type = block.fields["TYPE"] ?: "int"
                val varName = block.fields["VAR"] ?: "variable"
                val initialVal = block.fields["VALUE"] ?: "0"
                globalVars.add(Triple(type, varName, initialVal))
            }
        }

        // Add auto-detected pinModes to setup
        for ((pin, modePair) in pinModes) {
            val (mode, reason) = modePair
            setupStatements.add(Pair("pinMode($pin, $mode);", "Configure Pin $pin as $mode ($reason)"))
        }

        // Process setup-scoped blocks
        for (block in blocks) {
            if (block.scope == BlockScope.SETUP && block.type != "variable_declare") {
                translateBlockWithComment(block, indentLevel = 1, targetList = setupStatements)
            }
        }

        // Process loop-scoped blocks
        for (block in blocks) {
            if (block.scope == BlockScope.LOOP && block.type != "variable_declare") {
                translateBlockWithComment(block, indentLevel = 1, targetList = loopStatements)
            }
        }

        // Generate High-Level Summary
        val summaryText = generateHighLevelSummary(blocks, usedPins, activePeripherals, board)

        // Build formatted C++ source code with section explanations
        val sb = StringBuilder()
        sb.appendLine("/*")
        sb.appendLine(" * ===========================================================================")
        sb.appendLine(" *  ARDUINO C++ SKETCH - GENERATED FROM BLOCKLY VISUAL PROGRAM")
        sb.appendLine(" * ===========================================================================")
        sb.appendLine(" *  Target Board   : ${board.displayName}")
        sb.appendLine(" *  Microcontroller: ${board.mcu}")
        sb.appendLine(" *  Total Blocks   : ${countAllBlocks(blocks)}")
        sb.appendLine(" *  Active Pins    : ${if (usedPins.isEmpty()) "None" else usedPins.sorted().joinToString(", ")}")
        sb.appendLine(" *")
        sb.appendLine(" *  HIGH-LEVEL FUNCTIONALITY SUMMARY:")
        for (line in summaryText.lines()) {
            sb.appendLine(" *  $line")
        }
        sb.appendLine(" * ===========================================================================")
        sb.appendLine(" */")
        sb.appendLine()

        // 1. Library Includes Section
        sb.appendLine("// ===========================================================================")
        sb.appendLine("// SECTION 1: HARDWARE & LIBRARY INCLUDES")
        sb.appendLine("// Purpose: Imports external device drivers and hardware abstraction libraries")
        sb.appendLine("// ===========================================================================")
        if (includes.isEmpty()) {
            sb.appendLine("// (No external libraries required; using native Arduino core peripherals)")
        } else {
            for ((inc, desc) in includes) {
                sb.appendLine("$inc // $desc")
            }
        }
        sb.appendLine()

        // 2. Global Peripherals & Objects
        sb.appendLine("// ===========================================================================")
        sb.appendLine("// SECTION 2: HARDWARE OBJECT INSTANCES")
        sb.appendLine("// Purpose: Instantiates driver objects for servos, displays, and sensor buses")
        sb.appendLine("// ===========================================================================")
        if (globalObjects.isEmpty()) {
            sb.appendLine("// (No hardware driver objects declared)")
        } else {
            for ((objDecl, desc) in globalObjects) {
                sb.appendLine("$objDecl // $desc")
            }
        }
        sb.appendLine()

        // 3. Global Variables Section
        sb.appendLine("// ===========================================================================")
        sb.appendLine("// SECTION 3: GLOBAL VARIABLES & STATE")
        sb.appendLine("// Purpose: Stores runtime states, sensor measurements, and timing counters")
        sb.appendLine("// ===========================================================================")
        if (globalVars.isEmpty()) {
            sb.appendLine("// (No user-defined global variables declared)")
        } else {
            for ((type, name, initVal) in globalVars) {
                sb.appendLine("$type $name = $initVal; // Global variable '$name' initialized to $initVal")
            }
        }
        sb.appendLine()

        // 4. Setup Routine
        sb.appendLine("// ===========================================================================")
        sb.appendLine("// SECTION 4: SETUP FUNCTION (Runs Once on Microcontroller Boot)")
        sb.appendLine("// Purpose: Configures GPIO pin modes (INPUT/OUTPUT), initializes UART Serial")
        sb.appendLine("// communication, and attaches actuators to their designated hardware pins.")
        sb.appendLine("// ===========================================================================")
        sb.appendLine("void setup() {")
        if (setupStatements.isEmpty()) {
            sb.appendLine("  // No hardware setup actions required for this sketch")
        } else {
            for ((code, comment) in setupStatements) {
                if (comment.isNotBlank()) {
                    sb.appendLine("  // $comment")
                }
                for (codeLine in code.lines()) {
                    sb.appendLine("  $codeLine")
                }
            }
        }
        sb.appendLine("}")
        sb.appendLine()

        // 5. Loop Routine
        sb.appendLine("// ===========================================================================")
        sb.appendLine("// SECTION 5: MAIN EXECUTION LOOP (Runs Continuously)")
        sb.appendLine("// Purpose: Microcontroller execution cycle. Reads sensor inputs, computes")
        sb.appendLine("// control logic, updates actuator outputs, and logs telemetry repeatedly.")
        sb.appendLine("// ===========================================================================")
        sb.appendLine("void loop() {")
        if (loopStatements.isEmpty()) {
            sb.appendLine("  // Workspace loop is currently empty. Add blocks to execute periodic tasks.")
        } else {
            for ((code, comment) in loopStatements) {
                if (comment.isNotBlank()) {
                    sb.appendLine("  // $comment")
                }
                for (codeLine in code.lines()) {
                    sb.appendLine("  $codeLine")
                }
                sb.appendLine()
            }
        }
        sb.appendLine("}")

        return ArduinoSketchOutput(
            cppCode = sb.toString(),
            diagnostics = diagnostics,
            usedPins = usedPins,
            activePeripherals = activePeripherals.distinct()
        )
    }

    private fun generateHighLevelSummary(
        blocks: List<BlocklyBlock>,
        usedPins: Set<String>,
        peripherals: List<String>,
        board: ArduinoBoardType
    ): String {
        val features = mutableListOf<String>()

        val hasDigitalWrite = blocks.any { hasBlockType(it, "arduino_digital_write") }
        val hasDigitalRead = blocks.any { hasBlockType(it, "arduino_digital_read") }
        val hasAnalogWrite = blocks.any { hasBlockType(it, "arduino_analog_write") || hasBlockType(it, "actuator_dc_motor_speed") }
        val hasAnalogRead = blocks.any { hasBlockType(it, "arduino_analog_read") || hasBlockType(it, "sensor_potentiometer_read") || hasBlockType(it, "sensor_ldr_read") }
        val hasDelay = blocks.any { hasBlockType(it, "arduino_delay") }
        val hasSerial = blocks.any { hasBlockType(it, "serial_begin") || hasBlockType(it, "serial_println") || hasBlockType(it, "serial_print") }
        val hasServo = blocks.any { hasBlockType(it, "servo_attach") || hasBlockType(it, "servo_write") }
        val hasSonar = blocks.any { hasBlockType(it, "sensor_ultrasonic_distance") }
        val hasBuzzer = blocks.any { hasBlockType(it, "tone_buzzer") || hasBlockType(it, "notone_buzzer") }
        val hasLcd = blocks.any { hasBlockType(it, "display_lcd_i2c_print") }
        val hasNeoPixel = blocks.any { hasBlockType(it, "display_neopixel_set_color") }
        val hasRelay = blocks.any { hasBlockType(it, "actuator_relay_switch") }
        val hasLogic = blocks.any { hasBlockType(it, "controls_if") || hasBlockType(it, "controls_ifelse") }
        val hasLoops = blocks.any { hasBlockType(it, "controls_repeat_ext") || hasBlockType(it, "controls_whileUntil") }

        if (hasDigitalWrite) features.add("toggling digital outputs (LEDs/actuators)")
        if (hasDigitalRead) features.add("monitoring digital switch/button inputs")
        if (hasAnalogRead) features.add("reading analog potentiometer or light level signals")
        if (hasAnalogWrite) features.add("generating analog PWM signals for variable LED brightness or motor speed")
        if (hasDelay) features.add("utilizing controlled millisecond timing delays")
        if (hasSerial) features.add("transmitting debug messages and telemetry over hardware UART Serial")
        if (hasServo) features.add("controlling angular position of an SG90 micro servo motor")
        if (hasSonar) features.add("measuring object proximity in centimeters using HC-SR04 sonar pulses")
        if (hasBuzzer) features.add("producing acoustic audio frequencies and melodies via piezo buzzer")
        if (hasLcd) features.add("displaying alphanumeric text on a 16x2 LiquidCrystal I2C display")
        if (hasNeoPixel) features.add("driving individually addressable WS2812B RGB NeoPixel LEDs")
        if (hasRelay) features.add("switching high-voltage loads via electromechanical relay contacts")
        if (hasLogic) features.add("evaluating conditional branches (if/else threshold decisions)")
        if (hasLoops) features.add("executing repeated programmatic loops")

        val summary = if (features.isEmpty()) {
            "Empty or baseline template sketch configured for ${board.displayName}."
        } else {
            "This Arduino sketch operates on ${board.displayName}. Its primary workload consists of " +
                    features.joinToString(", ") + ". " +
                    "Hardware interfacing touches pins [${if (usedPins.isEmpty()) "none" else usedPins.sorted().joinToString(", ")}], " +
                    "with ${peripherals.size} peripheral subsystem(s) active."
        }
        return summary
    }

    private fun hasBlockType(block: BlocklyBlock, type: String): Boolean {
        if (block.type == type) return true
        if (block.childBlocks.any { hasBlockType(it, type) }) return true
        if (block.elseBlocks.any { hasBlockType(it, type) }) return true
        return false
    }

    private fun preScanBlocks(
        blocks: List<BlocklyBlock>,
        includes: MutableMap<String, String>,
        globalObjects: MutableList<Pair<String, String>>,
        pinModes: MutableMap<String, Pair<String, String>>,
        usedPins: MutableSet<String>,
        activePeripherals: MutableList<String>
    ) {
        for (block in blocks) {
            when (block.type) {
                "arduino_digital_write" -> {
                    val pin = block.fields["PIN"] ?: "13"
                    pinModes[pin] = Pair("OUTPUT", "Digital output driver for Pin $pin")
                    usedPins.add(pin)
                    activePeripherals.add("Digital Output (Pin $pin)")
                }
                "arduino_digital_read" -> {
                    val pin = block.fields["PIN"] ?: "2"
                    pinModes[pin] = Pair("INPUT_PULLUP", "Button / sensor input with internal pull-up")
                    usedPins.add(pin)
                    activePeripherals.add("Digital Input (Pin $pin)")
                }
                "arduino_analog_write" -> {
                    val pin = block.fields["PIN"] ?: "9"
                    pinModes[pin] = Pair("OUTPUT", "PWM duty cycle driver on Pin $pin")
                    usedPins.add(pin)
                    activePeripherals.add("PWM Output (Pin $pin)")
                }
                "arduino_analog_read" -> {
                    val pin = block.fields["PIN"] ?: "A0"
                    usedPins.add(pin)
                    activePeripherals.add("Analog Input ($pin)")
                }
                "servo_attach", "servo_write" -> {
                    includes["#include <Servo.h>"] = "Standard Arduino RC servo motor control library"
                    val name = block.fields["NAME"] ?: "myServo"
                    val pin = block.fields["PIN"] ?: "9"
                    globalObjects.add(Pair("Servo $name;", "Servo motor controller instance"))
                    usedPins.add(pin)
                    activePeripherals.add("Servo Motor ($name on Pin $pin)")
                }
                "display_lcd_i2c_print" -> {
                    includes["#include <Wire.h>"] = "I2C synchronous serial communication library"
                    includes["#include <LiquidCrystal_I2C.h>"] = "HD44780 16x2 LCD display driver over I2C bus"
                    globalObjects.add(Pair("LiquidCrystal_I2C lcd(0x27, 16, 2);", "LCD display configured at I2C address 0x27"))
                    usedPins.add("A4")
                    usedPins.add("A5")
                    activePeripherals.add("16x2 LCD Display (I2C 0x27)")
                }
                "display_neopixel_set_color" -> {
                    includes["#include <Adafruit_NeoPixel.h>"] = "Driver for WS2812 / WS2812B individually addressable RGB LEDs"
                    val pin = block.fields["PIN"] ?: "5"
                    globalObjects.add(Pair("Adafruit_NeoPixel strip(16, $pin, NEO_GRB + NEO_KHZ800);", "16-pixel RGB NeoPixel strip"))
                    usedPins.add(pin)
                    activePeripherals.add("WS2812B NeoPixel RGB (Pin $pin)")
                }
                "tone_buzzer" -> {
                    val pin = block.fields["PIN"] ?: "11"
                    pinModes[pin] = Pair("OUTPUT", "Acoustic audio square-wave generator")
                    usedPins.add(pin)
                    activePeripherals.add("Piezo Buzzer (Pin $pin)")
                }
                "sensor_ultrasonic_distance" -> {
                    val trig = block.fields["TRIG_PIN"] ?: "11"
                    val echo = block.fields["ECHO_PIN"] ?: "12"
                    pinModes[trig] = Pair("OUTPUT", "HC-SR04 sonar trigger pulse emitter")
                    pinModes[echo] = Pair("INPUT", "HC-SR04 sonar echo pulse listener")
                    usedPins.add(trig)
                    usedPins.add(echo)
                    activePeripherals.add("HC-SR04 Sonar (Trig:$trig, Echo:$echo)")
                }
                "actuator_dc_motor_speed" -> {
                    val pin = block.fields["PIN"] ?: "3"
                    pinModes[pin] = Pair("OUTPUT", "DC motor PWM speed controller")
                    usedPins.add(pin)
                    activePeripherals.add("DC Motor Fan (PWM Pin $pin)")
                }
                "actuator_relay_switch" -> {
                    val pin = block.fields["PIN"] ?: "4"
                    pinModes[pin] = Pair("OUTPUT", "Electromechanical relay coil driver")
                    usedPins.add(pin)
                    activePeripherals.add("Relay Switch (Pin $pin)")
                }
                "serial_begin" -> {
                    val baud = block.fields["BAUD"] ?: "9600"
                    activePeripherals.add("Hardware UART Serial ($baud baud)")
                }
            }

            if (block.childBlocks.isNotEmpty()) {
                preScanBlocks(block.childBlocks, includes, globalObjects, pinModes, usedPins, activePeripherals)
            }
            if (block.elseBlocks.isNotEmpty()) {
                preScanBlocks(block.elseBlocks, includes, globalObjects, pinModes, usedPins, activePeripherals)
            }
        }
    }

    private fun translateBlockWithComment(
        block: BlocklyBlock,
        indentLevel: Int,
        targetList: MutableList<Pair<String, String>>
    ) {
        val indent = "  ".repeat(indentLevel)

        if (!block.isSupportedOnArduino) {
            targetList.add(
                Pair(
                    "// ❌ [ERROR: Incompatible Block '${block.title}']",
                    "This block (${block.type}) cannot compile to Arduino C++: ${block.incompatibilityReason ?: "Unsupported block"}"
                )
            )
            return
        }

        when (block.type) {
            "arduino_digital_write" -> {
                val pin = block.fields["PIN"] ?: "13"
                val state = block.fields["STATE"] ?: "HIGH"
                val comment = "Set digital pin $pin to $state (${if (state == "HIGH") "5V HIGH voltage" else "0V LOW ground"})"
                targetList.add(Pair("digitalWrite($pin, $state);", comment))
            }

            "arduino_analog_write" -> {
                val pin = block.fields["PIN"] ?: "9"
                val value = block.fields["VALUE"] ?: "128"
                val comment = "Write PWM duty cycle $value / 255 to pin $pin"
                targetList.add(Pair("analogWrite($pin, $value);", comment))
            }

            "arduino_delay" -> {
                val time = block.fields["TIME"] ?: "1000"
                val comment = "Pause execution for $time milliseconds (${time.toDoubleOrNull()?.let { it / 1000.0 } ?: 1.0} seconds)"
                targetList.add(Pair("delay($time);", comment))
            }

            "arduino_delay_microseconds" -> {
                val time = block.fields["TIME"] ?: "50"
                targetList.add(Pair("delayMicroseconds($time);", "High-precision pause for $time microseconds"))
            }

            "arduino_pin_mode" -> {
                val pin = block.fields["PIN"] ?: "13"
                val mode = block.fields["MODE"] ?: "OUTPUT"
                targetList.add(Pair("pinMode($pin, $mode);", "Set pin $pin hardware direction to $mode"))
            }

            "serial_begin" -> {
                val baud = block.fields["BAUD"] ?: "9600"
                targetList.add(Pair("Serial.begin($baud);", "Open serial communication channel at $baud baud rate"))
            }

            "serial_println" -> {
                val msg = block.fields["MESSAGE"] ?: ""
                targetList.add(Pair("Serial.println(\"$msg\");", "Send text string over Serial Monitor terminated with newline"))
            }

            "serial_print" -> {
                val msg = block.fields["MESSAGE"] ?: ""
                targetList.add(Pair("Serial.print(\"$msg\");", "Send text string over Serial Monitor without newline"))
            }

            "servo_attach" -> {
                val name = block.fields["NAME"] ?: "myServo"
                val pin = block.fields["PIN"] ?: "9"
                targetList.add(Pair("$name.attach($pin);", "Bind servo instance '$name' to PWM pin $pin"))
            }

            "servo_write" -> {
                val name = block.fields["NAME"] ?: "myServo"
                val angle = block.fields["ANGLE"] ?: "90"
                targetList.add(Pair("$name.write($angle);", "Command servo '$name' to rotate to $angle degrees"))
            }

            "tone_buzzer" -> {
                val pin = block.fields["PIN"] ?: "11"
                val freq = block.fields["FREQ"] ?: "440"
                val dur = block.fields["DURATION"]
                if (dur != null && dur.isNotBlank()) {
                    targetList.add(Pair("tone($pin, $freq, $dur);", "Play audio tone at $freq Hz for $dur ms on pin $pin"))
                } else {
                    targetList.add(Pair("tone($pin, $freq);", "Play continuous audio tone at $freq Hz on pin $pin"))
                }
            }

            "notone_buzzer" -> {
                val pin = block.fields["PIN"] ?: "11"
                targetList.add(Pair("noTone($pin);", "Silence buzzer square wave on pin $pin"))
            }

            "actuator_dc_motor_speed" -> {
                val pin = block.fields["PIN"] ?: "3"
                val speed = block.fields["SPEED"] ?: "200"
                targetList.add(Pair("analogWrite($pin, $speed);", "Adjust DC motor fan speed to PWM duty cycle $speed / 255"))
            }

            "actuator_relay_switch" -> {
                val pin = block.fields["PIN"] ?: "4"
                val state = block.fields["STATE"] ?: "HIGH"
                targetList.add(Pair("digitalWrite($pin, $state);", "Toggle relay state to $state (${if (state == "HIGH") "Energized / Closed" else "De-energized / Open"})"))
            }

            "display_lcd_i2c_print" -> {
                val col = block.fields["COL"] ?: "0"
                val line = block.fields["LINE"] ?: "0"
                val text = block.fields["TEXT"] ?: ""
                val code = "lcd.setCursor($col, $line);\n${indent}lcd.print(\"$text\");"
                targetList.add(Pair(code, "Position LCD cursor at column $col, line $line and render \"$text\""))
            }

            "display_neopixel_set_color" -> {
                val pixel = block.fields["PIXEL"] ?: "0"
                val r = block.fields["RED"] ?: "255"
                val g = block.fields["GREEN"] ?: "0"
                val b = block.fields["BLUE"] ?: "0"
                val code = "strip.setPixelColor($pixel, strip.Color($r, $g, $b));\n${indent}strip.show();"
                targetList.add(Pair(code, "Set NeoPixel #$pixel RGB color to (R:$r, G:$g, B:$b) and latch data bus"))
            }

            "sensor_ultrasonic_distance" -> {
                val trig = block.fields["TRIG_PIN"] ?: "11"
                val echo = block.fields["ECHO_PIN"] ?: "12"
                val v = block.fields["VAR"] ?: "distanceCm"
                val code = """
digitalWrite($trig, LOW);
${indent}delayMicroseconds(2);
${indent}digitalWrite($trig, HIGH);
${indent}delayMicroseconds(10);
${indent}digitalWrite($trig, LOW);
${indent}long duration = pulseIn($echo, HIGH);
${indent}float $v = duration * 0.034 / 2.0;
                """.trimIndent()
                targetList.add(Pair(code, "Emit 10us ultrasonic pulse on Trig pin $trig and compute distance in cm from Echo pin $echo"))
            }

            "sensor_potentiometer_read" -> {
                val pin = block.fields["PIN"] ?: "A0"
                val v = block.fields["VAR"] ?: "potVal"
                targetList.add(Pair("int $v = analogRead($pin);", "Read 10-bit analog voltage (0 to 1023) from potentiometer on $pin"))
            }

            "sensor_ldr_read" -> {
                val pin = block.fields["PIN"] ?: "A1"
                val v = block.fields["VAR"] ?: "lightLux"
                targetList.add(Pair("int $v = analogRead($pin);", "Sample ambient light level from LDR photocell on $pin"))
            }

            "sensor_dht11_read" -> {
                val tVar = block.fields["TEMP_VAR"] ?: "temperature"
                val hVar = block.fields["HUM_VAR"] ?: "humidity"
                val pin = block.fields["PIN"] ?: "7"
                val code = "float $tVar = 24.5; // Simulated temperature in Celsius\n${indent}float $hVar = 55.0; // Simulated relative humidity percentage"
                targetList.add(Pair(code, "Poll DHT11 sensor on Pin $pin for ambient temperature and relative humidity"))
            }

            "variable_set" -> {
                val v = block.fields["VAR"] ?: "myVar"
                val valExpr = block.fields["VALUE"] ?: "0"
                targetList.add(Pair("$v = $valExpr;", "Update variable '$v' with new calculated value"))
            }

            "controls_if" -> {
                val cond = buildCondition(block)
                val innerList = mutableListOf<Pair<String, String>>()
                for (child in block.childBlocks) {
                    translateBlockWithComment(child, indentLevel + 1, innerList)
                }
                val body = innerList.joinToString("\n") { (c, cm) ->
                    val cmLine = if (cm.isNotBlank()) "$indent  // $cm\n" else ""
                    cmLine + c.lines().joinToString("\n") { "$indent  $it" }
                }
                val code = "if ($cond) {\n$body\n$indent}"
                targetList.add(Pair(code, "Decision branch: Execute nested statements if condition ($cond) is true"))
            }

            "controls_ifelse" -> {
                val cond = buildCondition(block)
                val ifList = mutableListOf<Pair<String, String>>()
                for (child in block.childBlocks) {
                    translateBlockWithComment(child, indentLevel + 1, ifList)
                }
                val elseList = mutableListOf<Pair<String, String>>()
                for (child in block.elseBlocks) {
                    translateBlockWithComment(child, indentLevel + 1, elseList)
                }

                val ifBody = ifList.joinToString("\n") { (c, cm) ->
                    val cmLine = if (cm.isNotBlank()) "$indent  // $cm\n" else ""
                    cmLine + c.lines().joinToString("\n") { "$indent  $it" }
                }
                val elseBody = elseList.joinToString("\n") { (c, cm) ->
                    val cmLine = if (cm.isNotBlank()) "$indent  // $cm\n" else ""
                    cmLine + c.lines().joinToString("\n") { "$indent  $it" }
                }

                val code = "if ($cond) {\n$ifBody\n$indent} else {\n$elseBody\n$indent}"
                targetList.add(Pair(code, "Conditional choice: Execute primary branch if ($cond), otherwise fallback to else branch"))
            }

            "controls_repeat_ext" -> {
                val times = block.fields["TIMES"] ?: "5"
                val innerList = mutableListOf<Pair<String, String>>()
                for (child in block.childBlocks) {
                    translateBlockWithComment(child, indentLevel + 1, innerList)
                }
                val body = innerList.joinToString("\n") { (c, cm) ->
                    val cmLine = if (cm.isNotBlank()) "$indent  // $cm\n" else ""
                    cmLine + c.lines().joinToString("\n") { "$indent  $it" }
                }
                val code = "for (int i = 0; i < $times; i++) {\n$body\n$indent}"
                targetList.add(Pair(code, "For loop: Repeat the enclosed block sequence exactly $times times"))
            }

            "controls_whileUntil" -> {
                val cond = block.fields["CONDITION"] ?: "true"
                val innerList = mutableListOf<Pair<String, String>>()
                for (child in block.childBlocks) {
                    translateBlockWithComment(child, indentLevel + 1, innerList)
                }
                val body = innerList.joinToString("\n") { (c, cm) ->
                    val cmLine = if (cm.isNotBlank()) "$indent  // $cm\n" else ""
                    cmLine + c.lines().joinToString("\n") { "$indent  $it" }
                }
                val code = "while ($cond) {\n$body\n$indent}"
                targetList.add(Pair(code, "While loop: Keep executing while ($cond) evaluates to true"))
            }

            else -> {
                targetList.add(Pair("// Block: ${block.title}", "Generic block translation"))
            }
        }
    }

    private fun buildCondition(block: BlocklyBlock): String {
        val customCond = block.fields["CONDITION"]
        if (!customCond.isNullOrBlank()) return customCond

        val val1 = block.fields["VAL1"] ?: "digitalRead(2)"
        val op = block.fields["OP"] ?: "=="
        val val2 = block.fields["VAL2"] ?: "HIGH"
        return "$val1 $op $val2"
    }

    private fun countAllBlocks(blocks: List<BlocklyBlock>): Int {
        var count = 0
        for (b in blocks) {
            count++
            count += countAllBlocks(b.childBlocks)
            count += countAllBlocks(b.elseBlocks)
        }
        return count
    }
}

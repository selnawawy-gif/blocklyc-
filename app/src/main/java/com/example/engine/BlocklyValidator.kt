package com.example.engine

import com.example.model.*

class BlocklyValidator {

    fun validate(
        blocks: List<BlocklyBlock>,
        board: ArduinoBoardType = ArduinoBoardType.UNO
    ): List<BlocklyDiagnostic> {
        val diagnostics = mutableListOf<BlocklyDiagnostic>()
        val declaredVariables = mutableSetOf<String>()
        val usedPins = mutableMapOf<String, String>() // pin to operation ("DIGITAL_OUT", "ANALOG_PWM", "SERIAL_TX", etc.)
        var hasSerialBegin = false
        var serialBaudRate: Int? = null

        // 1. Scan global variables first
        for (block in blocks) {
            if (block.type == "variable_declare") {
                val varName = block.fields["VAR"]?.trim() ?: ""
                validateVariableName(varName, block, diagnostics)
                declaredVariables.add(varName)
            }
        }

        // 2. Validate all blocks recursively
        validateBlockList(
            blocks = blocks,
            parentScope = BlockScope.LOOP,
            board = board,
            declaredVariables = declaredVariables,
            usedPins = usedPins,
            onSerialBegin = { baud ->
                hasSerialBegin = true
                serialBaudRate = baud
            },
            diagnostics = diagnostics
        )

        // 3. Post-validation checks: Check pin conflicts
        if (hasSerialBegin) {
            // Check if hardware serial pins 0 or 1 are also used for standard digital/analog IO
            if (usedPins.containsKey("0")) {
                diagnostics.add(
                    BlocklyDiagnostic(
                        severity = DiagnosticSeverity.WARNING,
                        title = "Hardware Serial Conflict (Pin 0/RX)",
                        message = "Pin 0 is configured as general I/O while Serial.begin() is also active. This will disrupt USB Serial communication.",
                        recommendation = "Use a different digital pin (e.g. Pin 2-13) for digital I/O instead of Pin 0.",
                        section = "Hardware Pinout"
                    )
                )
            }
            if (usedPins.containsKey("1")) {
                diagnostics.add(
                    BlocklyDiagnostic(
                        severity = DiagnosticSeverity.WARNING,
                        title = "Hardware Serial Conflict (Pin 1/TX)",
                        message = "Pin 1 is configured as general I/O while Serial.begin() is also active. This will disrupt Serial output.",
                        recommendation = "Use a different digital pin (e.g. Pin 2-13) for digital I/O instead of Pin 1.",
                        section = "Hardware Pinout"
                    )
                )
            }
        }

        return diagnostics
    }

    private fun validateBlockList(
        blocks: List<BlocklyBlock>,
        parentScope: BlockScope,
        board: ArduinoBoardType,
        declaredVariables: Set<String>,
        usedPins: MutableMap<String, String>,
        onSerialBegin: (Int) -> Unit,
        diagnostics: MutableList<BlocklyDiagnostic>
    ) {
        for ((index, block) in blocks.withIndex()) {
            val blockSection = if (block.scope == BlockScope.SETUP) "Setup Function" else "Main Loop"

            // Check if block is marked incompatible
            if (!block.isSupportedOnArduino) {
                diagnostics.add(
                    BlocklyDiagnostic(
                        severity = DiagnosticSeverity.ERROR,
                        title = "Incompatible Block: ${block.title}",
                        message = block.incompatibilityReason ?: "Block '${block.type}' cannot be compiled to Arduino C++.",
                        recommendation = "Remove this block or replace it with a supported Arduino hardware block.",
                        blockId = block.id,
                        blockType = block.type,
                        section = blockSection
                    )
                )
                continue
            }

            // Specific block validations
            when (block.type) {
                "arduino_digital_write" -> {
                    val pin = block.fields["PIN"]?.trim() ?: ""
                    val state = block.fields["STATE"]?.trim() ?: ""

                    if (pin.isEmpty()) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Missing Pin Number",
                                message = "Digital Write block (ID: ${block.id}) has no pin specified.",
                                recommendation = "Select a valid pin number (e.g., 2 to 13).",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    } else {
                        validatePinForBoard(pin, board, block, isDigital = true, diagnostics)
                        usedPins[pin] = "DIGITAL_WRITE"
                    }

                    if (state != "HIGH" && state != "LOW") {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Invalid Digital State",
                                message = "State '$state' in Digital Write block is invalid. Must be HIGH or LOW.",
                                recommendation = "Set state to either HIGH or LOW.",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    }
                }

                "arduino_analog_write" -> {
                    val pin = block.fields["PIN"]?.trim() ?: ""
                    val valueStr = block.fields["VALUE"]?.trim() ?: ""

                    if (pin.isEmpty()) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Missing PWM Pin",
                                message = "Analog Write (PWM) block (ID: ${block.id}) has no pin specified.",
                                recommendation = "Select a PWM-capable pin.",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    } else {
                        val pinNum = pin.toIntOrNull()
                        if (pinNum != null) {
                            if (!board.isValidPwmPin(pinNum)) {
                                diagnostics.add(
                                    BlocklyDiagnostic(
                                        severity = DiagnosticSeverity.ERROR,
                                        title = "Incompatible PWM Pin: Pin $pin",
                                        message = "Pin $pin on ${board.displayName} does not support hardware PWM (analogWrite). Supported PWM pins are: ${board.pwmPins.sorted().joinToString(", ")}.",
                                        recommendation = "Change pin to one of the PWM pins: ${board.pwmPins.sorted().joinToString(", ")}.",
                                        blockId = block.id,
                                        blockType = block.type,
                                        section = blockSection
                                    )
                                )
                            }
                        }
                        usedPins[pin] = "PWM_WRITE"
                    }

                    val intVal = valueStr.toIntOrNull()
                    if (intVal != null && (intVal < 0 || intVal > 255)) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.WARNING,
                                title = "PWM Value Out of Range",
                                message = "PWM value '$valueStr' is outside the 0-255 byte range.",
                                recommendation = "Use a value between 0 (0% duty cycle) and 255 (100% duty cycle).",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    }
                }

                "arduino_delay" -> {
                    val timeStr = block.fields["TIME"]?.trim() ?: ""
                    val time = timeStr.toLongOrNull()
                    if (time == null || time < 0) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Invalid Delay Duration",
                                message = "Delay block (ID: ${block.id}) duration '$timeStr' is invalid. Must be a positive integer in milliseconds.",
                                recommendation = "Enter a valid positive number in milliseconds (e.g. 500, 1000).",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    }
                }

                "serial_begin" -> {
                    val baudStr = block.fields["BAUD"]?.trim() ?: "9600"
                    val baud = baudStr.toIntOrNull()
                    if (baud == null || baud <= 0) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Invalid Serial Baud Rate",
                                message = "Baud rate '$baudStr' must be a positive integer.",
                                recommendation = "Select a standard baud rate: 9600, 19200, 38400, 57600, or 115200.",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    } else {
                        onSerialBegin(baud)
                    }
                }

                "math_arithmetic" -> {
                    val op = block.fields["OP"] ?: "ADD"
                    val b = block.fields["B"]?.trim() ?: ""
                    if ((op == "DIVIDE" || op == "MODULO") && b == "0") {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Division by Zero Error",
                                message = "Math block (ID: ${block.id}) contains division by constant zero, which causes microcontroller crash.",
                                recommendation = "Ensure denominator operand 'B' is not zero.",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    }
                }

                "servo_write" -> {
                    val pin = block.fields["PIN"]?.trim() ?: ""
                    val angleStr = block.fields["ANGLE"]?.trim() ?: ""
                    val angle = angleStr.toIntOrNull()
                    if (angle != null && (angle < 0 || angle > 180)) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.WARNING,
                                title = "Servo Angle Out of Range",
                                message = "Servo angle $angle° exceeds standard micro servo limits (0° to 180°).",
                                recommendation = "Clamp servo angle between 0 and 180 degrees.",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    }
                    if (pin.isNotEmpty()) {
                        usedPins[pin] = "SERVO"
                    }
                }

                "controls_if", "controls_ifelse" -> {
                    val val1 = block.fields["VAL1"]?.trim() ?: ""
                    val val2 = block.fields["VAL2"]?.trim() ?: ""
                    if (val1.isEmpty() && val2.isEmpty() && block.fields["CONDITION"].isNullOrBlank()) {
                        diagnostics.add(
                            BlocklyDiagnostic(
                                severity = DiagnosticSeverity.ERROR,
                                title = "Missing Condition Socket",
                                message = "If statement block (ID: ${block.id}) is missing its test condition socket.",
                                recommendation = "Provide a comparison expression (e.g. digitalRead(2) == HIGH).",
                                blockId = block.id,
                                blockType = block.type,
                                section = blockSection
                            )
                        )
                    }
                }
            }

            // Recursively validate nested statements
            if (block.childBlocks.isNotEmpty()) {
                validateBlockList(
                    blocks = block.childBlocks,
                    parentScope = block.scope,
                    board = board,
                    declaredVariables = declaredVariables,
                    usedPins = usedPins,
                    onSerialBegin = onSerialBegin,
                    diagnostics = diagnostics
                )
            }
            if (block.elseBlocks.isNotEmpty()) {
                validateBlockList(
                    blocks = block.elseBlocks,
                    parentScope = block.scope,
                    board = board,
                    declaredVariables = declaredVariables,
                    usedPins = usedPins,
                    onSerialBegin = onSerialBegin,
                    diagnostics = diagnostics
                )
            }
        }
    }

    private fun validateVariableName(name: String, block: BlocklyBlock, diagnostics: MutableList<BlocklyDiagnostic>) {
        if (name.isEmpty()) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Empty Variable Name",
                    message = "A variable declaration block has no name assigned.",
                    recommendation = "Provide a valid variable name, e.g., 'sensorReading'.",
                    blockId = block.id,
                    blockType = block.type,
                    section = "Global Scope"
                )
            )
            return
        }

        if (name[0].isDigit()) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Invalid Variable Name: '$name'",
                    message = "C++ variable names cannot begin with a numeric digit.",
                    recommendation = "Rename variable to start with a letter or underscore (e.g., 'val_$name').",
                    blockId = block.id,
                    blockType = block.type,
                    section = "Global Scope"
                )
            )
        }

        if (name.contains(" ")) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Whitespace in Variable Name: '$name'",
                    message = "C++ variable names cannot contain whitespace spaces.",
                    recommendation = "Use camelCase or snake_case without spaces (e.g., '${name.replace(" ", "_")}').",
                    blockId = block.id,
                    blockType = block.type,
                    section = "Global Scope"
                )
            )
        }

        val cppReserved = setOf("int", "float", "char", "bool", "void", "setup", "loop", "class", "switch", "case", "return", "if", "else", "for", "while")
        if (cppReserved.contains(name)) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Reserved Keyword Collision: '$name'",
                    message = "'$name' is a reserved C++ Arduino keyword and cannot be used as an identifier.",
                    recommendation = "Choose a descriptive name such as 'my_${name}_val'.",
                    blockId = block.id,
                    blockType = block.type,
                    section = "Global Scope"
                )
            )
        }
    }

    private fun validatePinForBoard(
        pin: String,
        board: ArduinoBoardType,
        block: BlocklyBlock,
        isDigital: Boolean,
        diagnostics: MutableList<BlocklyDiagnostic>
    ) {
        if (pin.startsWith("A", ignoreCase = true)) {
            if (!board.isValidAnalogPin(pin.uppercase())) {
                diagnostics.add(
                    BlocklyDiagnostic(
                        severity = DiagnosticSeverity.ERROR,
                        title = "Invalid Analog Pin: $pin",
                        message = "Analog pin $pin is not available on ${board.displayName}. Available analog pins are: ${board.analogPins.joinToString(", ")}.",
                        recommendation = "Change pin to one of: ${board.analogPins.joinToString(", ")}.",
                        blockId = block.id,
                        blockType = block.type,
                        section = "Hardware Pinout"
                    )
                )
            }
        } else {
            val pinNum = pin.toIntOrNull()
            if (pinNum == null || !board.isValidDigitalPin(pinNum)) {
                diagnostics.add(
                    BlocklyDiagnostic(
                        severity = DiagnosticSeverity.ERROR,
                        title = "Invalid Digital Pin: $pin",
                        message = "Digital pin '$pin' is out of range for ${board.displayName}. Valid digital pins are 0 to ${board.digitalPins.maxOrNull() ?: 13}.",
                        recommendation = "Select a valid pin between 0 and ${board.digitalPins.maxOrNull() ?: 13}.",
                        blockId = block.id,
                        blockType = block.type,
                        section = "Hardware Pinout"
                    )
                )
            }
        }
    }
}

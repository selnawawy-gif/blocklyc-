package com.example.model

data class SketchTemplate(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val blocks: List<BlocklyBlock>,
    val targetBoard: ArduinoBoardType = ArduinoBoardType.UNO
)

object SketchTemplates {

    val TEMPLATES: List<SketchTemplate> by lazy {
        listOf(
            createBlinkTemplate(),
            createTrafficLightTemplate(),
            createServoSweepTemplate(),
            createUltrasonicTemplate(),
            createSmartLdrNightLightTemplate(),
            createDcMotorFanTemplate(),
            createMelodyBuzzerTemplate(),
            createLcdTelemetryTemplate(),
            createNeoPixelTemplate(),
            createFaultyBlocklyErrorDemoTemplate() // Demonstrates robust error handling!
        )
    }

    private fun createBlinkTemplate(): SketchTemplate {
        val b1 = BlockRegistry.createBlock("arduino_digital_write").apply {
            fields["PIN"] = "13"
            fields["STATE"] = "HIGH"
        }
        val b2 = BlockRegistry.createBlock("arduino_delay").apply {
            fields["TIME"] = "1000"
        }
        val b3 = BlockRegistry.createBlock("arduino_digital_write").apply {
            fields["PIN"] = "13"
            fields["STATE"] = "LOW"
        }
        val b4 = BlockRegistry.createBlock("arduino_delay").apply {
            fields["TIME"] = "1000"
        }
        return SketchTemplate(
            id = "template_blink",
            title = "Blink Built-in LED",
            description = "The classic Arduino Hello World. Toggles onboard Pin 13 LED on and off every second.",
            category = "Basics",
            blocks = listOf(b1, b2, b3, b4)
        )
    }

    private fun createTrafficLightTemplate(): SketchTemplate {
        val rOn = BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "10"; fields["STATE"] = "HIGH" }
        val rWait = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "3000" }
        val rOff = BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "10"; fields["STATE"] = "LOW" }

        val yOn = BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "9"; fields["STATE"] = "HIGH" }
        val yWait = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "1000" }
        val yOff = BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "9"; fields["STATE"] = "LOW" }

        val gOn = BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "8"; fields["STATE"] = "HIGH" }
        val gWait = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "3000" }
        val gOff = BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "8"; fields["STATE"] = "LOW" }

        return SketchTemplate(
            id = "template_traffic_light",
            title = "Traffic Light Controller",
            description = "Sequences Red (Pin 10), Yellow (Pin 9), and Green (Pin 8) LEDs with real street timings.",
            category = "Lighting",
            blocks = listOf(rOn, rWait, rOff, yOn, yWait, yOff, gOn, gWait, gOff)
        )
    }

    private fun createServoSweepTemplate(): SketchTemplate {
        val setupAttach = BlockRegistry.createBlock("servo_attach", BlockScope.SETUP).apply {
            fields["PIN"] = "9"
            fields["NAME"] = "myServo"
        }
        val s0 = BlockRegistry.createBlock("servo_write").apply {
            fields["NAME"] = "myServo"
            fields["ANGLE"] = "0"
        }
        val d1 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "800" }
        val s90 = BlockRegistry.createBlock("servo_write").apply {
            fields["NAME"] = "myServo"
            fields["ANGLE"] = "90"
        }
        val d2 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "800" }
        val s180 = BlockRegistry.createBlock("servo_write").apply {
            fields["NAME"] = "myServo"
            fields["ANGLE"] = "180"
        }
        val d3 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "800" }

        return SketchTemplate(
            id = "template_servo_sweep",
            title = "SG90 Servo Sweep",
            description = "Sweeps an SG90 micro servo from 0° center 90° to 180° with Servo.h library.",
            category = "Motors",
            blocks = listOf(setupAttach, s0, d1, s90, d2, s180, d3)
        )
    }

    private fun createUltrasonicTemplate(): SketchTemplate {
        val sBegin = BlockRegistry.createBlock("serial_begin", BlockScope.SETUP).apply { fields["BAUD"] = "9600" }
        val sonar = BlockRegistry.createBlock("sensor_ultrasonic_distance").apply {
            fields["TRIG_PIN"] = "11"
            fields["ECHO_PIN"] = "12"
            fields["VAR"] = "distanceCm"
        }
        val logPrefix = BlockRegistry.createBlock("serial_print").apply { fields["MESSAGE"] = "Obstacle Distance (cm): " }
        val sDelay = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "250" }

        return SketchTemplate(
            id = "template_sonar",
            title = "HC-SR04 Distance Sensor",
            description = "Measures sonar pulse echo time and computes distance in centimeters.",
            category = "Sensors",
            blocks = listOf(sBegin, sonar, logPrefix, sDelay)
        )
    }

    private fun createSmartLdrNightLightTemplate(): SketchTemplate {
        val readLdr = BlockRegistry.createBlock("sensor_ldr_read").apply {
            fields["PIN"] = "A1"
            fields["VAR"] = "lightLux"
        }
        val ifElse = BlockRegistry.createBlock("controls_ifelse").apply {
            fields["VAL1"] = "lightLux"
            fields["OP"] = "<"
            fields["VAL2"] = "400"
            childBlocks.add(BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "13"; fields["STATE"] = "HIGH" })
            elseBlocks.add(BlockRegistry.createBlock("arduino_digital_write").apply { fields["PIN"] = "13"; fields["STATE"] = "LOW" })
        }
        val d = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "200" }

        return SketchTemplate(
            id = "template_ldr",
            title = "Smart LDR Night Light",
            description = "Automatic night lamp: automatically illuminates LED 13 when darkness falls below threshold.",
            category = "Smart Home",
            blocks = listOf(readLdr, ifElse, d)
        )
    }

    private fun createDcMotorFanTemplate(): SketchTemplate {
        val readPot = BlockRegistry.createBlock("sensor_potentiometer_read").apply {
            fields["PIN"] = "A0"
            fields["VAR"] = "potVal"
        }
        val motorSpeed = BlockRegistry.createBlock("actuator_dc_motor_speed").apply {
            fields["PIN"] = "3"
            fields["SPEED"] = "220"
        }
        val d = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "100" }

        return SketchTemplate(
            id = "template_fan",
            title = "DC Fan PWM Speed Controller",
            description = "Drives a variable-speed DC motor fan using hardware PWM duty cycles.",
            category = "Motors",
            blocks = listOf(readPot, motorSpeed, d)
        )
    }

    private fun createMelodyBuzzerTemplate(): SketchTemplate {
        val t1 = BlockRegistry.createBlock("tone_buzzer").apply { fields["PIN"] = "11"; fields["FREQ"] = "262"; fields["DURATION"] = "250" }
        val d1 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "300" }
        val t2 = BlockRegistry.createBlock("tone_buzzer").apply { fields["PIN"] = "11"; fields["FREQ"] = "330"; fields["DURATION"] = "250" }
        val d2 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "300" }
        val t3 = BlockRegistry.createBlock("tone_buzzer").apply { fields["PIN"] = "11"; fields["FREQ"] = "392"; fields["DURATION"] = "350" }
        val d3 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "800" }

        return SketchTemplate(
            id = "template_melody",
            title = "Piezo Buzzer Musical Chime",
            description = "Synthesizes musical notes (C4 262Hz, E4 330Hz, G4 392Hz) through an acoustic piezo transducer.",
            category = "Audio",
            blocks = listOf(t1, d1, t2, d2, t3, d3)
        )
    }

    private fun createLcdTelemetryTemplate(): SketchTemplate {
        val lcdPrint = BlockRegistry.createBlock("display_lcd_i2c_print").apply {
            fields["LINE"] = "0"
            fields["COL"] = "0"
            fields["TEXT"] = "Temp: 24.5C"
        }
        val lcdPrint2 = BlockRegistry.createBlock("display_lcd_i2c_print").apply {
            fields["LINE"] = "1"
            fields["COL"] = "0"
            fields["TEXT"] = "Humidity: 55%"
        }
        val d = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "1000" }

        return SketchTemplate(
            id = "template_lcd",
            title = "16x2 Character LCD Display",
            description = "Displays sensor readings on a LiquidCrystal display with I2C PCF8574 backpack.",
            category = "Displays",
            blocks = listOf(lcdPrint, lcdPrint2, d)
        )
    }

    private fun createNeoPixelTemplate(): SketchTemplate {
        val red = BlockRegistry.createBlock("display_neopixel_set_color").apply {
            fields["PIN"] = "5"; fields["PIXEL"] = "0"; fields["RED"] = "255"; fields["GREEN"] = "0"; fields["BLUE"] = "0"
        }
        val d1 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "500" }
        val green = BlockRegistry.createBlock("display_neopixel_set_color").apply {
            fields["PIN"] = "5"; fields["PIXEL"] = "0"; fields["RED"] = "0"; fields["GREEN"] = "255"; fields["BLUE"] = "0"
        }
        val d2 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "500" }
        val blue = BlockRegistry.createBlock("display_neopixel_set_color").apply {
            fields["PIN"] = "5"; fields["PIXEL"] = "0"; fields["RED"] = "0"; fields["GREEN"] = "0"; fields["BLUE"] = "255"
        }
        val d3 = BlockRegistry.createBlock("arduino_delay").apply { fields["TIME"] = "500" }

        return SketchTemplate(
            id = "template_neopixel",
            title = "WS2812B RGB NeoPixel Strip",
            description = "Cycles primary RGB color channels on an addressable smart LED strip.",
            category = "Displays",
            blocks = listOf(red, d1, green, d2, blue, d3)
        )
    }

    private fun createFaultyBlocklyErrorDemoTemplate(): SketchTemplate {
        // Intentionally invalid blocks to demonstrate robust error reporting
        val webBlock = BlockRegistry.createBlock("web_fetch").apply {
            fields["URL"] = "https://api.iot-cloud.com/data"
        }
        val domBlock = BlockRegistry.createBlock("document_getElementById").apply {
            fields["ID"] = "btnSwitch"
        }
        val badPwm = BlockRegistry.createBlock("arduino_analog_write").apply {
            fields["PIN"] = "7" // Pin 7 does not support PWM on Uno!
            fields["VALUE"] = "180"
        }
        val divZero = BlockRegistry.createBlock("math_arithmetic").apply {
            fields["A"] = "100"
            fields["OP"] = "DIVIDE"
            fields["B"] = "0" // Division by zero!
        }
        val badDelay = BlockRegistry.createBlock("arduino_delay").apply {
            fields["TIME"] = "-500" // Negative delay!
        }

        return SketchTemplate(
            id = "template_faulty_demo",
            title = "⚠️ Error Diagnostics Demo",
            description = "Demonstration sketch with incompatible web blocks, invalid PWM pins, and division by zero to showcase error detection.",
            category = "Diagnostics",
            blocks = listOf(webBlock, domBlock, badPwm, divZero, badDelay)
        )
    }
}

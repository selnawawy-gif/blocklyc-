package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.engine.ArduinoCodeGenerator
import com.example.engine.BlocklyValidator
import com.example.engine.BlocklyXmlParser
import com.example.model.ArduinoBoardType
import com.example.model.BlockRegistry
import com.example.model.DiagnosticSeverity
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Blockly to Arduino", appName)
    }

    @Test
    fun `test robust error handling for incompatible web blocks`() {
        val parser = BlocklyXmlParser()
        val xmlWithWebFetch = """
            <xml xmlns="https://developers.google.com/blockly/xml">
                <block type="web_fetch" id="block_test_1">
                    <field name="URL">https://example.com/api</field>
                </block>
            </xml>
        """.trimIndent()

        val parseResult = parser.parse(xmlWithWebFetch)
        assertTrue(parseResult.diagnostics.any { it.severity == DiagnosticSeverity.ERROR })
        val incompatibleDiag = parseResult.diagnostics.first { it.severity == DiagnosticSeverity.ERROR }
        assertEquals("block_test_1", incompatibleDiag.blockId)
        assertTrue(incompatibleDiag.title.contains("Incompatible Block", ignoreCase = true))

        val generator = ArduinoCodeGenerator()
        val output = generator.generate(parseResult.blocks, ArduinoBoardType.UNO)
        assertTrue(output.diagnostics.any { it.severity == DiagnosticSeverity.ERROR })
        assertTrue(output.cppCode.contains("ERROR: Incompatible Block"))
    }

    @Test
    fun `test arduino blink generation with explanatory comments and summary`() {
        val b1 = BlockRegistry.createBlock("arduino_digital_write").apply {
            fields["PIN"] = "13"
            fields["STATE"] = "HIGH"
        }
        val b2 = BlockRegistry.createBlock("arduino_delay").apply {
            fields["TIME"] = "1000"
        }
        val generator = ArduinoCodeGenerator()
        val output = generator.generate(listOf(b1, b2), ArduinoBoardType.UNO)

        assertTrue(output.isSuccess)
        assertTrue(output.cppCode.contains("HIGH-LEVEL FUNCTIONALITY SUMMARY:"))
        assertTrue(output.cppCode.contains("SECTION 4: SETUP FUNCTION"))
        assertTrue(output.cppCode.contains("SECTION 5: MAIN EXECUTION LOOP"))
        assertTrue(output.cppCode.contains("digitalWrite(13, HIGH);"))
        assertTrue(output.cppCode.contains("delay(1000);"))
        assertTrue(output.cppCode.contains("pinMode(13, OUTPUT);"))
    }

    @Test
    fun `test invalid pwm pin detection on Arduino Uno`() {
        // Pin 7 does not support PWM on Uno
        val b = BlockRegistry.createBlock("arduino_analog_write").apply {
            fields["PIN"] = "7"
            fields["VALUE"] = "128"
        }
        val validator = BlocklyValidator()
        val diags = validator.validate(listOf(b), ArduinoBoardType.UNO)
        assertTrue(diags.any { it.severity == DiagnosticSeverity.ERROR && it.title.contains("Incompatible PWM Pin") })
    }
}

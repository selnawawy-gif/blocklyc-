package com.example.engine

import android.util.Xml
import com.example.model.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.StringReader

data class BlocklyParseResult(
    val blocks: List<BlocklyBlock>,
    val diagnostics: List<BlocklyDiagnostic>
)

class BlocklyXmlParser {

    fun parse(xmlString: String): BlocklyParseResult {
        val diagnostics = mutableListOf<BlocklyDiagnostic>()
        val blocks = mutableListOf<BlocklyBlock>()

        val trimmed = xmlString.trim()
        if (trimmed.isEmpty()) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Empty Blockly Code",
                    message = "The provided Blockly XML is completely empty.",
                    recommendation = "Add blocks from the palette or paste valid Blockly XML.",
                    section = "XML Parser"
                )
            )
            return BlocklyParseResult(emptyList(), diagnostics)
        }

        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(trimmed))

            var eventType = parser.eventType
            var hasXmlRoot = false

            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        val tagName = parser.name
                        if (tagName == "xml") {
                            hasXmlRoot = true
                        } else if (tagName == "block") {
                            // Top-level block
                            val block = parseBlockTag(parser, diagnostics)
                            if (block != null) {
                                blocks.add(block)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            if (!hasXmlRoot && blocks.isEmpty()) {
                diagnostics.add(
                    BlocklyDiagnostic(
                        severity = DiagnosticSeverity.ERROR,
                        title = "Missing <xml> Root Tag",
                        message = "The Blockly code must start with an <xml> tag and end with </xml>.",
                        recommendation = "Ensure your Blockly XML is enclosed in <xml>...</xml> tags.",
                        section = "XML Parser",
                        line = 1
                    )
                )
            }
        } catch (e: XmlPullParserException) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Blockly XML Syntax Error",
                    message = "Malformed XML syntax: ${e.message?.substringBefore(" (position:") ?: e.localizedMessage}",
                    recommendation = "Check for unclosed tags, unmatched brackets, or illegal characters in the XML.",
                    section = "XML Parser",
                    line = e.lineNumber,
                    column = e.columnNumber
                )
            )
        } catch (e: Exception) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Blockly Parse Failure",
                    message = "Failed to parse Blockly code: ${e.localizedMessage}",
                    recommendation = "Verify that the Blockly code follows the standard Google Blockly XML schema.",
                    section = "XML Parser"
                )
            )
        }

        return BlocklyParseResult(blocks, diagnostics)
    }

    private fun parseBlockTag(
        parser: XmlPullParser,
        diagnostics: MutableList<BlocklyDiagnostic>
    ): BlocklyBlock? {
        val blockType = parser.getAttributeValue(null, "type")
        val blockId = parser.getAttributeValue(null, "id") ?: java.util.UUID.randomUUID().toString().take(8)
        val lineNumber = parser.lineNumber

        if (blockType.isNullOrBlank()) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Missing Block Type",
                    message = "Encountered a <block> tag without a required 'type' attribute.",
                    recommendation = "Specify a valid block type, e.g., type=\"arduino_digital_write\".",
                    blockId = blockId,
                    section = "Block Sockets",
                    line = lineNumber
                )
            )
            return null
        }

        val registeredDef = BlockRegistry.DEFINITIONS[blockType]
        val isSupported = registeredDef?.isSupportedOnArduino ?: (!isKnownWebBlock(blockType))
        val reason = registeredDef?.incompatibilityReason ?: if (!isSupported) {
            "Incompatible block '$blockType'. This block belongs to browser/web environments and cannot be converted to Arduino C++."
        } else null

        if (!isSupported) {
            diagnostics.add(
                BlocklyDiagnostic(
                    severity = DiagnosticSeverity.ERROR,
                    title = "Incompatible Block: '$blockType'",
                    message = reason ?: "Block type '$blockType' is not supported in Arduino C++.",
                    recommendation = "Remove this block or replace it with an equivalent Arduino hardware block (e.g., Serial, Digital I/O, or Math).",
                    blockId = blockId,
                    blockType = blockType,
                    section = registeredDef?.defaultScope?.name ?: "Main Loop",
                    line = lineNumber
                )
            )
        }

        val fields = mutableMapOf<String, String>()
        val children = mutableListOf<BlocklyBlock>()
        val elseChildren = mutableListOf<BlocklyBlock>()
        var currentFieldName: String? = null
        var inStatementDo = false
        var inStatementElse = false

        var depth = 1
        while (depth > 0) {
            val event = parser.next()
            when (event) {
                XmlPullParser.START_TAG -> {
                    when (parser.name) {
                        "block" -> {
                            val childBlock = parseBlockTag(parser, diagnostics)
                            if (childBlock != null) {
                                if (inStatementElse) {
                                    elseChildren.add(childBlock)
                                } else {
                                    children.add(childBlock)
                                }
                            }
                        }
                        "field" -> {
                            currentFieldName = parser.getAttributeValue(null, "name")
                        }
                        "statement" -> {
                            val statementName = parser.getAttributeValue(null, "name")
                            if (statementName?.contains("ELSE", ignoreCase = true) == true) {
                                inStatementElse = true
                                inStatementDo = false
                            } else {
                                inStatementDo = true
                                inStatementElse = false
                            }
                        }
                        "next" -> {
                            // Sibling chained next block
                        }
                        else -> {
                            depth++
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    if (currentFieldName != null) {
                        val text = parser.text.trim()
                        if (text.isNotEmpty()) {
                            fields[currentFieldName] = text
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "block" -> {
                            depth--
                        }
                        "field" -> {
                            currentFieldName = null
                        }
                        "statement" -> {
                            inStatementDo = false
                            inStatementElse = false
                        }
                        else -> {
                            depth--
                        }
                    }
                }
                XmlPullParser.END_DOCUMENT -> depth = 0
            }
        }

        // Apply defaults if fields missing
        registeredDef?.defaultFields?.forEach { (k, v) ->
            if (!fields.containsKey(k)) {
                fields[k] = v
            }
        }

        val category = registeredDef?.category ?: if (isSupported) BlockCategory.ARDUINO_IO else BlockCategory.INCOMPATIBLE
        val title = registeredDef?.title ?: "Custom Block ($blockType)"
        val scope = registeredDef?.defaultScope ?: BlockScope.LOOP

        return BlocklyBlock(
            id = blockId,
            type = blockType,
            category = category,
            title = title,
            scope = scope,
            fields = fields,
            childBlocks = children,
            elseBlocks = elseChildren,
            isSupportedOnArduino = isSupported,
            incompatibilityReason = reason
        )
    }

    private fun isKnownWebBlock(type: String): Boolean {
        val webBlocks = setOf(
            "web_fetch", "document_getElementById", "window_alert", "text_prompt_ext",
            "dom_create", "canvas_draw", "local_storage_set", "navigator_geolocation",
            "audio_play_web", "html_element", "fetch_api"
        )
        return webBlocks.contains(type)
    }
}

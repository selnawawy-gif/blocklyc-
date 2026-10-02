package com.example.engine

import com.example.model.BlockScope
import com.example.model.BlocklyBlock

class BlocklyXmlGenerator {

    fun generate(blocks: List<BlocklyBlock>): String {
        val sb = StringBuilder()
        sb.appendLine("<xml xmlns=\"https://developers.google.com/blockly/xml\">")

        for (block in blocks) {
            renderBlock(block, indent = 2, sb = sb)
        }

        sb.appendLine("</xml>")
        return sb.toString()
    }

    private fun renderBlock(block: BlocklyBlock, indent: Int, sb: StringBuilder) {
        val sp = " ".repeat(indent)
        sb.appendLine("$sp<block type=\"${block.type}\" id=\"${block.id}\">")

        // Fields
        for ((name, value) in block.fields) {
            val escaped = value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
            sb.appendLine("$sp  <field name=\"$name\">$escaped</field>")
        }

        // Statements: Child blocks
        if (block.childBlocks.isNotEmpty()) {
            sb.appendLine("$sp  <statement name=\"DO\">")
            for (child in block.childBlocks) {
                renderBlock(child, indent + 4, sb)
            }
            sb.appendLine("$sp  </statement>")
        }

        // Else statement
        if (block.elseBlocks.isNotEmpty()) {
            sb.appendLine("$sp  <statement name=\"ELSE\">")
            for (elseChild in block.elseBlocks) {
                renderBlock(elseChild, indent + 4, sb)
            }
            sb.appendLine("$sp  </statement>")
        }

        sb.appendLine("$sp</block>")
    }
}

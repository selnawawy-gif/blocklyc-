package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArduinoBoardType
import com.example.model.ArduinoSketchOutput
import com.example.model.DiagnosticSeverity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArduinoCodeViewer(
    output: ArduinoSketchOutput,
    selectedBoard: ArduinoBoardType,
    onSelectBoard: (ArduinoBoardType) -> Unit,
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isBoardMenuExpanded by remember { mutableStateOf(false) }

    val errorCount = output.diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warningCount = output.diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("arduino_code_viewer")
    ) {
        // Controls Row: Board Selector & Copy / Share Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Board Selector Button
            Box {
                OutlinedButton(
                    onClick = { isBoardMenuExpanded = true },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.DeveloperBoard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(selectedBoard.displayName.take(16) + "...", fontSize = 13.sp)
                }

                DropdownMenu(
                    expanded = isBoardMenuExpanded,
                    onDismissRequest = { isBoardMenuExpanded = false }
                ) {
                    ArduinoBoardType.values().forEach { board ->
                        DropdownMenuItem(
                            text = { Text(board.displayName) },
                            onClick = {
                                onSelectBoard(board)
                                isBoardMenuExpanded = false
                            }
                        )
                    }
                }
            }

            // Copy & Share buttons
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Arduino Sketch", output.cppCode)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Arduino C++ copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy C++", fontSize = 13.sp)
                }

                IconButton(
                    onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, output.cppCode)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share Arduino .ino Code"))
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Diagnostics Alert Banner if issues exist
        if (errorCount > 0 || warningCount > 0) {
            Surface(
                color = if (errorCount > 0) ErrorContainer else WarningContainer,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (errorCount > 0) ErrorRed else WarningOrange,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (errorCount > 0) "$errorCount Error(s) in Blockly conversion" else "$warningCount Warning(s) in sketch",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (errorCount > 0) ErrorRed else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    TextButton(onClick = onOpenDiagnostics) {
                        Text("View Details", fontSize = 12.sp, color = if (errorCount > 0) ErrorRed else WarningOrange)
                    }
                }
            }
        }

        // Active Peripherals / Used Pins Chips
        if (output.usedPins.isNotEmpty() || output.activePeripherals.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (pin in output.usedPins.sorted()) {
                    AssistChip(
                        onClick = {},
                        label = { Text("Pin $pin", fontSize = 11.sp) },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(ArduinoTeal, shape = RoundedCornerShape(4.dp))
                            )
                        }
                    )
                }
                for (peri in output.activePeripherals.take(3)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(peri, fontSize = 11.sp) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Code Editor Window (Syntax-Highlighted + Line Numbers)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(
                containerColor = ArduinoDarkBackground
            )
        ) {
            val lines = remember(output.cppCode) { output.cppCode.lines() }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                // Line Numbers Column
                Column(modifier = Modifier.padding(end = 12.dp)) {
                    for (i in 1..lines.size) {
                        Text(
                            text = i.toString().padStart(3, ' '),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Vertical Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color(0xFF334155))
                )

                Spacer(modifier = Modifier.width(12.dp))

                // Highlighted Code Column
                Column {
                    for (line in lines) {
                        Text(
                            text = highlightCppSyntax(line),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

// C++ Syntax highlighter
fun highlightCppSyntax(line: String): AnnotatedString {
    val trimmed = line.trimStart()

    // Comments
    if (trimmed.startsWith("//") || trimmed.startsWith("/*") || trimmed.startsWith("*")) {
        return AnnotatedString(
            text = line,
            spanStyle = SpanStyle(
                color = if (trimmed.contains("ERROR", ignoreCase = true)) Color(0xFFFF5252) else SyntaxComment,
                fontWeight = if (trimmed.contains("SECTION")) FontWeight.Bold else FontWeight.Normal
            )
        )
    }

    return buildAnnotatedString {
        val tokens = line.split(Regex("(?<=[\\s(),;{}])|(?=[\\s(),;{}])"))
        for (token in tokens) {
            when {
                token in setOf("void", "int", "float", "char", "bool", "long", "const", "boolean", "unsigned", "Servo", "LiquidCrystal_I2C", "Adafruit_NeoPixel") -> {
                    pushStyle(SpanStyle(color = SyntaxType, fontWeight = FontWeight.Bold))
                    append(token)
                    pop()
                }
                token in setOf("setup", "loop", "pinMode", "digitalWrite", "digitalRead", "analogWrite", "analogRead", "delay", "delayMicroseconds", "millis", "Serial", "begin", "println", "print", "attach", "write", "pulseIn", "tone", "noTone") -> {
                    pushStyle(SpanStyle(color = SyntaxKeyword, fontWeight = FontWeight.SemiBold))
                    append(token)
                    pop()
                }
                token in setOf("HIGH", "LOW", "OUTPUT", "INPUT", "INPUT_PULLUP", "true", "false", "for", "while", "if", "else") -> {
                    pushStyle(SpanStyle(color = ArduinoAmber, fontWeight = FontWeight.Bold))
                    append(token)
                    pop()
                }
                token.startsWith("\"") && token.endsWith("\"") -> {
                    pushStyle(SpanStyle(color = SyntaxString))
                    append(token)
                    pop()
                }
                token.all { it.isDigit() } && token.isNotEmpty() -> {
                    pushStyle(SpanStyle(color = SyntaxNumber))
                    append(token)
                    pop()
                }
                else -> {
                    pushStyle(SpanStyle(color = Color(0xFFE2E8F0)))
                    append(token)
                    pop()
                }
            }
        }
    }
}

package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlockScope
import com.example.model.BlocklyBlock
import com.example.model.BlocklyDiagnostic
import com.example.model.DiagnosticSeverity
import com.example.ui.theme.*

@Composable
fun BlockCard(
    block: BlocklyBlock,
    diagnostics: List<BlocklyDiagnostic>,
    isFocused: Boolean,
    onUpdateField: (String, String) -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    val blockErrors = remember(diagnostics, block.id) {
        diagnostics.filter { it.blockId == block.id && it.severity == DiagnosticSeverity.ERROR }
    }

    val blockColor = block.category.color
    val hasIssue = blockErrors.isNotEmpty() || !block.isSupportedOnArduino

    // Scratch Puzzle Block Container
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("block_card_${block.id}")
            .then(
                if (isFocused || hasIssue) {
                    Modifier.border(
                        width = 2.5.dp,
                        color = if (hasIssue) ScratchRed else Color.White,
                        shape = RoundedCornerShape(12.dp)
                    )
                } else Modifier
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = blockColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Scratch Puzzle Top Notch Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Puzzle tab icon + Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Scratch Category Dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = block.title,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                }

                // Scope badge & Action Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.Black.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (block.scope == BlockScope.SETUP) "setup" else "forever",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(onClick = onMoveUp, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move Up", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onMoveDown, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move Down", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }

            // Incompatible Block Warning Banner (Scratch style notice)
            if (!block.isSupportedOnArduino) {
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = ScratchRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "Incompatible with Arduino: ${block.type}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = ScratchRed
                            )
                            Text(
                                text = block.incompatibilityReason ?: "Cannot compile to Arduino C++.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            }

            // Diagnostics errors banner
            if (blockErrors.isNotEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        for (err in blockErrors) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = ScratchRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = err.message,
                                    fontSize = 12.sp,
                                    color = ScratchRed,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // Scratch Pill Input Fields (White Rounded Capsules)
            Surface(
                color = Color.White.copy(alpha = 0.18f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    RenderScratchBlockFields(block = block, onUpdateField = onUpdateField)

                    // Render nested C-block contents (for if/loops)
                    if (block.allowsChildren || block.childBlocks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Column {
                                Text(
                                    text = "inside loop / condition:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                for (child in block.childBlocks) {
                                    Text(
                                        text = "• ${child.title} [${child.fields.entries.joinToString { "${it.key}=${it.value}" }}]",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RenderScratchBlockFields(
    block: BlocklyBlock,
    onUpdateField: (String, String) -> Unit
) {
    when (block.type) {
        "arduino_digital_write" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("set pin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillDropdown(
                    label = "Pin",
                    selected = block.fields["PIN"] ?: "13",
                    options = (0..13).map { it.toString() },
                    onSelect = { onUpdateField("PIN", it) },
                    modifier = Modifier.width(90.dp)
                )
                Text("to", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                val state = block.fields["STATE"] ?: "HIGH"
                ScratchPillDropdown(
                    label = "State",
                    selected = state,
                    options = listOf("HIGH", "LOW"),
                    onSelect = { onUpdateField("STATE", it) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        "arduino_analog_write" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("set PWM pin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillDropdown(
                    label = "Pin",
                    selected = block.fields["PIN"] ?: "9",
                    options = listOf("3", "5", "6", "9", "10", "11"),
                    onSelect = { onUpdateField("PIN", it) },
                    modifier = Modifier.width(90.dp)
                )
                Text("to", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillInput(
                    value = block.fields["VALUE"] ?: "128",
                    onValueChange = { onUpdateField("VALUE", it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "0-255"
                )
            }
        }

        "arduino_delay", "arduino_delay_microseconds" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("wait", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillInput(
                    value = block.fields["TIME"] ?: "1000",
                    onValueChange = { onUpdateField("TIME", it) },
                    modifier = Modifier.width(110.dp),
                    placeholder = "1000"
                )
                Text(
                    text = if (block.type == "arduino_delay") "milliseconds" else "microseconds",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }

        "serial_println", "serial_print" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("say to Serial", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillInput(
                    value = block.fields["MESSAGE"] ?: "Hello Arduino",
                    onValueChange = { onUpdateField("MESSAGE", it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "Message text..."
                )
            }
        }

        "servo_write" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("turn servo pin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillDropdown(
                    label = "Pin",
                    selected = block.fields["PIN"] ?: "9",
                    options = (2..13).map { it.toString() },
                    onSelect = { onUpdateField("PIN", it) },
                    modifier = Modifier.width(85.dp)
                )
                Text("to", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillInput(
                    value = block.fields["ANGLE"] ?: "90",
                    onValueChange = { onUpdateField("ANGLE", it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "0-180°"
                )
                Text("degrees", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        "tone_buzzer" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("play tone pin", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillDropdown(
                    label = "Pin",
                    selected = block.fields["PIN"] ?: "11",
                    options = (2..13).map { it.toString() },
                    onSelect = { onUpdateField("PIN", it) },
                    modifier = Modifier.width(85.dp)
                )
                Text("freq", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillInput(
                    value = block.fields["FREQ"] ?: "440",
                    onValueChange = { onUpdateField("FREQ", it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "Hz"
                )
            }
        }

        "controls_if", "controls_ifelse" -> {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("if <", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ScratchPillInput(
                    value = block.fields["VAL1"] ?: "digitalRead(2)",
                    onValueChange = { onUpdateField("VAL1", it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "val1"
                )
                ScratchPillDropdown(
                    label = "op",
                    selected = block.fields["OP"] ?: "==",
                    options = listOf("==", "!=", "<", ">", "<=", ">="),
                    onSelect = { onUpdateField("OP", it) },
                    modifier = Modifier.width(75.dp)
                )
                ScratchPillInput(
                    value = block.fields["VAL2"] ?: "HIGH",
                    onValueChange = { onUpdateField("VAL2", it) },
                    modifier = Modifier.weight(1f),
                    placeholder = "val2"
                )
                Text("> then", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for ((key, value) in block.fields) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$key:",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.width(80.dp)
                        )
                        ScratchPillInput(
                            value = value,
                            onValueChange = { onUpdateField(key, it) },
                            modifier = Modifier.weight(1f),
                            placeholder = key
                        )
                    }
                }
            }
        }
    }
}

// Scratch White Capsule Pill Input
@Composable
fun ScratchPillInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    Surface(
        modifier = modifier
            .height(36.dp)
            .clip(RoundedCornerShape(50)),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            androidx.compose.foundation.text.BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2E384D)
                ),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isEmpty() && placeholder.isNotEmpty()) {
                        Text(placeholder, fontSize = 12.sp, color = Color.Gray)
                    }
                    innerTextField()
                }
            )
        }
    }
}

// Scratch White Capsule Pill Dropdown
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScratchPillDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(50))
                .clickable { expanded = true },
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = selected,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E384D)
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = Color(0xFF576075),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt, fontWeight = FontWeight.SemiBold) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

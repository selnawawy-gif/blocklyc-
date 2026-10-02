package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BlocklyDiagnostic
import com.example.model.DiagnosticSeverity
import com.example.ui.theme.*

@Composable
fun DiagnosticsPanel(
    diagnostics: List<BlocklyDiagnostic>,
    onFocusBlock: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var severityFilter by remember { mutableStateOf<DiagnosticSeverity?>(null) }

    val errorCount = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warningCount = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }

    val filteredList = remember(diagnostics, severityFilter) {
        if (severityFilter == null) diagnostics else diagnostics.filter { it.severity == severityFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("diagnostics_panel")
    ) {
        // Summary Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    errorCount > 0 -> ErrorContainer
                    warningCount > 0 -> WarningContainer
                    else -> SuccessContainer
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when {
                        errorCount > 0 -> Icons.Default.Error
                        warningCount > 0 -> Icons.Default.Warning
                        else -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    tint = when {
                        errorCount > 0 -> ErrorRed
                        warningCount > 0 -> WarningOrange
                        else -> SuccessGreen
                    },
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = when {
                            errorCount > 0 -> "$errorCount Conversion Error(s) Detected"
                            warningCount > 0 -> "$warningCount Warning(s) Detected"
                            else -> "All Blocks Valid & Ready to Compile"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = when {
                            errorCount > 0 -> ErrorRed
                            warningCount > 0 -> WarningOrange
                            else -> SuccessGreen
                        }
                    )
                    Text(
                        text = when {
                            errorCount > 0 -> "Incompatible blocks or missing sockets prevent clean C++ generation."
                            warningCount > 0 -> "Code will generate, but check hardware pin conflicts or limits."
                            else -> "Blockly workspace matches Arduino hardware specification."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Filter Chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = severityFilter == null,
                onClick = { severityFilter = null },
                label = { Text("All (${diagnostics.size})") }
            )
            FilterChip(
                selected = severityFilter == DiagnosticSeverity.ERROR,
                onClick = { severityFilter = DiagnosticSeverity.ERROR },
                label = { Text("Errors ($errorCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ErrorRed,
                    selectedLabelColor = Color.White
                )
            )
            FilterChip(
                selected = severityFilter == DiagnosticSeverity.WARNING,
                onClick = { severityFilter = DiagnosticSeverity.WARNING },
                label = { Text("Warnings ($warningCount)") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = WarningOrange,
                    selectedLabelColor = Color.White
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Diagnostics List
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No diagnostics in this view",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredList, key = { it.id }) { diag ->
                    DiagnosticCard(
                        diagnostic = diag,
                        onFocusBlock = { diag.blockId?.let { id -> onFocusBlock(id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagnosticCard(
    diagnostic: BlocklyDiagnostic,
    onFocusBlock: () -> Unit
) {
    val isError = diagnostic.severity == DiagnosticSeverity.ERROR

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Severity badge + Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isError) ErrorRed else WarningOrange)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = diagnostic.severity.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = diagnostic.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (isError) ErrorRed else MaterialTheme.colorScheme.onSurface
                    )
                }

                // Section Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = diagnostic.section,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Explanation Message
            Text(
                text = diagnostic.message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Recommendation
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Recommendation: ${diagnostic.recommendation}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Location information & Action Button
            if (diagnostic.blockId != null || diagnostic.line != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = buildString {
                            if (diagnostic.blockId != null) append("Block ID: ${diagnostic.blockId}  ")
                            if (diagnostic.line != null) append("Line: ${diagnostic.line}  ")
                        },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.outline
                    )

                    if (diagnostic.blockId != null) {
                        OutlinedButton(
                            onClick = onFocusBlock,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Inspect Block", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

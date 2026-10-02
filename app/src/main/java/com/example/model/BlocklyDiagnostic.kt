package com.example.model

enum class DiagnosticSeverity {
    ERROR,   // Prevents successful compilation to Arduino C++
    WARNING, // Code can compile, but will likely malfunction on hardware
    INFO     // Optimization or informational hint
}

data class BlocklyDiagnostic(
    val id: String = java.util.UUID.randomUUID().toString(),
    val severity: DiagnosticSeverity,
    val title: String,
    val message: String,
    val recommendation: String,
    val blockId: String? = null,
    val blockType: String? = null,
    val section: String = "General",
    val line: Int? = null,
    val column: Int? = null
)

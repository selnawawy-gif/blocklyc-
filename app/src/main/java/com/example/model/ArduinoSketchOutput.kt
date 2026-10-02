package com.example.model

data class ArduinoSketchOutput(
    val cppCode: String,
    val diagnostics: List<BlocklyDiagnostic> = emptyList(),
    val usedPins: Set<String> = emptySet(),
    val activePeripherals: List<String> = emptyList()
) {
    val errorCount: Int get() = diagnostics.count { it.severity == DiagnosticSeverity.ERROR }
    val warningCount: Int get() = diagnostics.count { it.severity == DiagnosticSeverity.WARNING }
    val isSuccess: Boolean get() = errorCount == 0
}

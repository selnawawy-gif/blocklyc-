package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ArduinoBoardType
import com.example.ui.theme.ArduinoTeal

@Composable
fun ArduinoPinoutView(
    board: ArduinoBoardType,
    usedPins: Set<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("arduino_pinout_view"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${board.displayName} Pinout Inspector",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = "${usedPins.size} Active Pin(s)",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = ArduinoTeal,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas Schematic / Board Layout
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(Color(0xFF006468), RoundedCornerShape(8.dp))
            ) {
                val w = size.width
                val h = size.height

                // Board PCB
                drawRoundRect(
                    color = Color(0xFF00878A),
                    size = Size(w - 16f, h - 16f),
                    topLeft = Offset(8f, 8f),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // USB B Connector & Power Barrel Jack
                drawRoundRect(
                    color = Color(0xFF94A3B8),
                    size = Size(28.dp.toPx(), 20.dp.toPx()),
                    topLeft = Offset(12f, 12f),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // ATmega328P DIP IC
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    size = Size(70.dp.toPx(), 18.dp.toPx()),
                    topLeft = Offset(w / 2 - 35.dp.toPx(), h / 2 - 9.dp.toPx()),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // Top Header Pins (Digital 0-13)
                val digPins = 14
                val digStep = (w - 70f) / digPins
                for (i in 0 until digPins) {
                    val pinName = i.toString()
                    val isUsed = usedPins.contains(pinName)
                    val pinX = 40f + (digPins - 1 - i) * digStep
                    drawCircle(
                        color = if (isUsed) Color(0xFFFFAB00) else Color(0xFFCBD5E1),
                        radius = if (isUsed) 5.dp.toPx() else 3.dp.toPx(),
                        center = Offset(pinX, 18f)
                    )
                }

                // Bottom Header Pins (Analog A0-A5 + Power)
                val analogPins = listOf("A5", "A4", "A3", "A2", "A1", "A0")
                val aStep = (w / 2.2f) / analogPins.size
                for ((index, pinName) in analogPins.withIndex()) {
                    val isUsed = usedPins.contains(pinName)
                    val pinX = (w - 30f) - (index * aStep)
                    drawCircle(
                        color = if (isUsed) Color(0xFF00FF66) else Color(0xFFCBD5E1),
                        radius = if (isUsed) 5.dp.toPx() else 3.dp.toPx(),
                        center = Offset(pinX, h - 18f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFFFAB00), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Digital/PWM Used", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFF00FF66), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Analog Used", fontSize = 11.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(Color(0xFFCBD5E1), RoundedCornerShape(2.dp)))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Available Pins", fontSize = 11.sp)
                }
            }
        }
    }
}

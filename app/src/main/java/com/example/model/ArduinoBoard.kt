package com.example.model

enum class ArduinoBoardType(
    val displayName: String,
    val mcu: String,
    val digitalPins: List<Int>,
    val analogPins: List<String>,
    val pwmPins: Set<Int>,
    val builtInLedPin: Int,
    val hardwareSerialPins: Set<Int>
) {
    UNO(
        displayName = "Arduino Uno (ATmega328P)",
        mcu = "ATmega328P",
        digitalPins = (0..13).toList(),
        analogPins = listOf("A0", "A1", "A2", "A3", "A4", "A5"),
        pwmPins = setOf(3, 5, 6, 9, 10, 11),
        builtInLedPin = 13,
        hardwareSerialPins = setOf(0, 1)
    ),
    NANO(
        displayName = "Arduino Nano (ATmega328P)",
        mcu = "ATmega328P",
        digitalPins = (0..13).toList(),
        analogPins = listOf("A0", "A1", "A2", "A3", "A4", "A5", "A6", "A7"),
        pwmPins = setOf(3, 5, 6, 9, 10, 11),
        builtInLedPin = 13,
        hardwareSerialPins = setOf(0, 1)
    ),
    MEGA(
        displayName = "Arduino Mega 2560",
        mcu = "ATmega2560",
        digitalPins = (0..53).toList(),
        analogPins = (0..15).map { "A$it" },
        pwmPins = (2..13).toSet() + setOf(44, 45, 46),
        builtInLedPin = 13,
        hardwareSerialPins = setOf(0, 1, 14, 15, 16, 17, 18, 19)
    ),
    ESP32(
        displayName = "ESP32 Dev Module",
        mcu = "ESP32-WROOM-32",
        digitalPins = listOf(2, 4, 5, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 25, 26, 27, 32, 33),
        analogPins = listOf("32", "33", "34", "35", "36", "39"),
        pwmPins = listOf(2, 4, 5, 12, 13, 14, 15, 16, 17, 18, 19, 21, 22, 23, 25, 26, 27).toSet(),
        builtInLedPin = 2,
        hardwareSerialPins = setOf(1, 3)
    );

    fun isValidDigitalPin(pin: Int): Boolean = digitalPins.contains(pin)

    fun isValidPwmPin(pin: Int): Boolean = pwmPins.contains(pin)

    fun isValidAnalogPin(pinStr: String): Boolean = analogPins.contains(pinStr)
}

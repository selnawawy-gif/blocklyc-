package com.example.model

enum class CircuitToolType(
    val displayName: String,
    val description: String,
    val category: String,
    val defaultPin: String
) {
    BUILTIN_LED("Arduino Built-in LED", "Onboard SMD LED connected to digital pin 13", "Outputs", "13"),
    LED_RED("Red LED", "5mm standard Red LED with resistor", "Outputs", "12"),
    LED_GREEN("Green LED", "5mm standard Green LED with resistor", "Outputs", "8"),
    LED_YELLOW("Yellow LED", "5mm standard Yellow LED with resistor", "Outputs", "7"),
    LED_BLUE("Blue LED (PWM)", "5mm Blue LED for brightness fading", "Outputs", "6"),
    RGB_NEOPIXEL("WS2812B RGB NeoPixel", "Addressable 24-bit full RGB smart LED", "Outputs", "5"),
    TRAFFIC_LIGHT("Traffic Light Module", "3-in-1 Red, Yellow, Green LED signals", "Outputs", "10,9,8"),
    SERVO_MOTOR("SG90 Micro Servo", "Precision 0° to 180° rotary actuator", "Actuators", "9"),
    DC_MOTOR("DC Motor & Fan", "PWM variable speed motor with rotating propeller", "Actuators", "3"),
    PIEZO_BUZZER("Active/Passive Buzzer", "Sound tone and frequency generator", "Outputs", "11"),
    RELAY_MODULE("5V 10A Relay Module", "Electromechanical switch for high current loads", "Actuators", "4"),
    PUSH_BUTTON("Tactile Push Button", "Momentary switch with pull-up resistor", "Inputs", "2"),
    SLIDE_SWITCH("Slide SPST Switch", "Two-position ON/OFF latching toggle switch", "Inputs", "4"),
    POTENTIOMETER("Rotary Potentiometer (10kΩ)", "Adjustable analog voltage divider (0-1023)", "Inputs", "A0"),
    LDR_SENSOR("LDR Photoresistor", "Light-dependent sensor with ambient lux slider", "Inputs", "A1"),
    ULTRASONIC_HC_SR04("HC-SR04 Ultrasonic", "Sonar distance measurement sensor (2-400 cm)", "Sensors", "11,12"),
    PIR_MOTION("PIR Motion Sensor", "Pyroelectric infrared motion detection sensor", "Sensors", "2"),
    DHT11_SENSOR("DHT11 Temp & Humidity", "Digital temperature and relative humidity sensor", "Sensors", "7"),
    LCD_1602_I2C("16x2 Character LCD (I2C)", "LiquidCrystal alphanumeric display with backlight", "Displays", "A4,A5"),
    SEVEN_SEGMENT("7-Segment LED Display", "Single digit numeric decimal indicator", "Displays", "A0"),
    OSCILLOSCOPE("Digital Oscilloscope Probe", "Real-time waveform logic analyzer", "Instruments", "13")
}

data class SimulatedCircuitState(
    // Pin voltage states: 0 to 255 (0 = LOW, 255 = HIGH, intermediate = PWM)
    val digitalPinOutputs: Map<Int, Int> = mapOf(13 to 0),
    val pinModes: Map<Int, String> = mapOf(13 to "OUTPUT"),
    
    // Interactive inputs manipulated by the user
    val buttonPressed: Boolean = false,
    val slideSwitchOn: Boolean = false,
    val potentiometerValue: Int = 512, // 0 - 1023
    val ldrLightLevel: Int = 600,       // 0 - 1023
    val ultrasonicDistanceCm: Float = 25.0f, // 2 - 400 cm
    val pirMotionDetected: Boolean = false,
    val ambientTemperatureC: Float = 24.5f,
    val ambientHumidityPercent: Float = 55.0f,
    
    // Actuators & Outputs dynamic states
    val servoAngleDeg: Float = 90.0f,
    val dcMotorRpm: Float = 0.0f,
    val buzzerFrequencyHz: Int = 0,
    val buzzerIsActive: Boolean = false,
    val relayClosed: Boolean = false,
    val rgbColor: Long = 0xFF00FF00, // Hex ARGB
    val lcdLine1: String = "Arduino Ready",
    val lcdLine2: String = "Blockly Active",
    
    // Serial Terminal output buffer
    val serialLog: List<String> = listOf("--- Serial Monitor Initialized @ 9600 baud ---"),
    val serialBaudRate: Int = 9600,
    
    // Oscilloscope probe history (pin 13 or probe pin)
    val oscilloscopeSignalHistory: List<Float> = emptyList(),
    val isRunning: Boolean = false,
    val cycleCount: Long = 0,
    val executionSpeedMs: Long = 200 // Cycle delay
)

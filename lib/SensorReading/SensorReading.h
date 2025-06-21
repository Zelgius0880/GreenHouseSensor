//
// Created by Florian on 28-05-25.
//

#ifndef SENSORREADING_H
#define SENSORREADING_H
// Define the structure for sensor data
#define RECORD_SIZE_BYTES                12

struct SensorReading {
    float temperature;
    float humidity;
    uint32_t timestamp; // 'long' is typically 32-bit on Arduino

    // Default constructor (optional, but good practice)
    SensorReading() : temperature(0.0f), humidity(0.0f), timestamp(0L) {}
    SensorReading(float temp, float hum, uint32_t ts) : temperature(temp), humidity(hum), timestamp(ts) {}
};
#endif //SENSORREADING_H

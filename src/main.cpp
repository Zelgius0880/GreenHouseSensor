#include <Wire.h>
#include <Arduino.h>
#include <SHTSensor.h>
#include <DS3132Clock.h>
#include <FramStorage.h>
#include <SensorReading.h>
#include <BleSensorServer.h>

SHTSensor sht;
DS3231Clock rtc = DS3231Clock();
FramStorage fram;
BleSensorServer bleServer("Greenhouse Sensor", &fram, &sht, &rtc); // Customize device name if desired

void saveRecordIfNeeded(const SensorReading &reading);

[[noreturn]] void error() {
    while (true) {
        rgbLedWrite(BUILTIN_LED, 0, 255, 0);
        delay(1000);
        rgbLedWrite(BUILTIN_LED, 0, 0, 0);
        delay(1000);
    }
}

void setup() {
    pinMode(BUILTIN_LED, OUTPUT);
    rgbLedWrite(BUILTIN_LED, 255, 255, 255);

    Wire.begin(21, 22);
    Serial.begin(9600);
    Serial.println("Serial Initialized.");
    delay(1000); // let serial console settle


    if (fram.begin(DEFAULT_FRAM_I2C_ADDRESS, 32 * 1024)) {
        Serial.println("FRAM Initialized.");
    } else {
        Serial.println("FRAM Initialization Failed!");
        error();
    }

    rtc.begin();

    if (rtc.getCurrentDateTime().Unix64Time() == 0)
        rtc.setTime(RtcDateTime(2025, 5, 21, 16, 32, 15));

    if (sht.init()) {
        Serial.print("init(): success\n");
    } else {
        Serial.print("init(): failed\n");
        error();
    }
    sht.setAccuracy(SHTSensor::SHT_ACCURACY_MEDIUM); // only supported by SHT3x

    /*fram.writeUInt16(LAST_RECORD_ADDRESS, RECORD_START_ADDRESS + RECORD_SIZE_BYTES);
    fram.writeUInt16(FIRST_RECORD_ADDRESS, RECORD_START_ADDRESS);
    fram.writeUInt32(LAST_RECORD_TIMESTAMP_ADDRESS, 0);*/

    bleServer.begin();
    rgbLedWrite(BUILTIN_LED, 16, 0, 0);
}

void loop() {

    const RtcDateTime dt = rtc.getCurrentDateTime();
    if (sht.readSample()) {
        const auto humidity = sht.getHumidity();
        const auto temperature = sht.getTemperature();
        saveRecordIfNeeded(SensorReading{temperature, humidity, dt.Unix32Time()});
    } else {
        Serial.print("Error in readSample()\n");
    }
    delay(1000);
}

void saveRecordIfNeeded(const SensorReading &reading) {
    const auto lastRecordTimestamp = fram.readUInt32(LAST_RECORD_TIMESTAMP_ADDRESS);
    if (reading.timestamp - lastRecordTimestamp > RECORD_INTERVAL_SECONDS) {
        const auto firstAddress = fram.readUInt16(FIRST_RECORD_ADDRESS);

        const auto lastAddress = fram.readUInt16(LAST_RECORD_ADDRESS);
        const uint16_t address = NEXT_ADDRESS(lastAddress);
        Serial.print("NEXT_ADDRESS: ");
        Serial.println(address, HEX);
        if (address == firstAddress) {
            fram.writeUInt16(FIRST_RECORD_ADDRESS, NEXT_ADDRESS(firstAddress));
        }

        fram.writeSensorReading(address, reading);
        fram.writeUInt32(LAST_RECORD_TIMESTAMP_ADDRESS, reading.timestamp);
        fram.writeUInt16(LAST_RECORD_ADDRESS, address);
    }
}

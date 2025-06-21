//
// Created by Florian on 28-05-25.
//

#ifndef BLESERVER_H
#define BLESERVER_H

#include <Arduino.h> // For Serial prints
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h> // For CCCD descriptor for notifications
#include <FramStorage.h> // For CCCD descriptor for notifications
#include <SHTSensor.h>
#include <DS3132Clock.h>
#include <utility>


#include "SensorReading.h"


// Default UUIDs from your example
#define RECORD_SERVICE_UUID                "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define RECORD_REQUEST_CHARACTERISTIC_UUID      "00000001-1fb5-459e-8fcc-c5c9c331914b"
#define RECORD_DATA_CHARACTERISTIC_UUID         "00000002-1fb5-459e-8fcc-c5c9c331914b"
#define BLUETOOTH_RECORD_SIZE 14

struct BluetoothRecord {
    uint16_t offset;
    float temperature;
    float humidity;
    uint32_t timestamp; // 'long' is typically 32-bit on Arduino

    // Default constructor (optional, but good practice)
    BluetoothRecord() : offset(0), temperature(0.0f), humidity(0.0f),  timestamp(0L) {}
    BluetoothRecord(const uint16_t offset, const float temp, const float hum, const uint32_t ts) : offset(offset), temperature(temp), humidity(hum),  timestamp(ts) {}
};

class DS3231Clock;

class BleSensorServer {
public:
    /**
     * @brief Constructor for the BLE Sensor Server.
     * @param deviceName The name of the BLE device to be advertised.
     * @param fram
     * @param sensor
     * @param rtc
     */
    explicit BleSensorServer(String  deviceName, FramStorage* fram, SHTSensor* sensor, DS3231Clock* rtc);

    /**
     * @brief Initializes the BLE server, service, characteristic, and starts advertising.
     */
    void begin();

    /**
     * @brief Allows checking if a client is connected.
     * @return True if one or more clients are connected, false otherwise.
     */
    bool isClientConnected();

private:
    String _deviceName;
    BLEServer* _pServer;
    BLEService* _pService;
    BLECharacteristic* _requestCharacteristic;
    BLECharacteristic* _dataCharacteristic;
    uint32_t _connectedClients;
    FramStorage* _fram;
    SHTSensor* _sht;
    DS3231Clock* _rtc;

    void sendRecords(uint16_t offset) const;
    void updateCurrentRecord() const;
    void serializeBluetoothRecord( BluetoothRecord *record, uint8_t *buffer) const ;
    // Callback class for server events (connect/disconnect)
    class ServerCallbacks : public BLEServerCallbacks {
        BleSensorServer* _owner;
    public:
        explicit ServerCallbacks(BleSensorServer* owner) : _owner(owner) {}
        void onConnect(BLEServer* pServer) override;
        void onDisconnect(BLEServer* pServer) override;
    };

    class RecordRequestCallbacks final : public BLECharacteristicCallbacks {
        BleSensorServer* _owner;
    public:
        explicit RecordRequestCallbacks(BleSensorServer* owner) : _owner(owner) {}
        void onWrite(BLECharacteristic *pCharacteristic) override;
    };
};


#endif //BLESERVER_H

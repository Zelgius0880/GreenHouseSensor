#include "BleSensorServer.h"

// --- ServerCallbacks Implementation ---
void BleSensorServer::ServerCallbacks::onConnect(BLEServer *pServer) {
    _owner->_connectedClients++;
    Serial.print("BLE Client Connected. Total clients: ");
    Serial.println(_owner->_connectedClients);
    // Optionally stop advertising if you only want one connection,
    // or manage advertising based on the number of connections.
    // For simplicity, ESP32 default behavior allows multiple connections
    // and advertising might continue or stop based on stack configuration.
    rgbLedWrite(BUILTIN_LED, 0, 0, 16);
}

void BleSensorServer::ServerCallbacks::onDisconnect(BLEServer *pServer) {
    if (_owner->_connectedClients > 0) {
        _owner->_connectedClients--;
    }
    Serial.print("BLE Client Disconnected. Total clients: ");
    Serial.println(_owner->_connectedClients);
    // It's common to restart advertising to allow new connections.
    // The ESP32 BLE stack might handle this automatically if configured,
    // but explicitly starting it ensures it's discoverable again.
    pServer->startAdvertising(); // Restart advertising
    Serial.println("Advertising restarted.");
    rgbLedWrite(BUILTIN_LED, 16, 0, 0);
}

// --- RecordRequestCallbacks Implementation ---
void BleSensorServer::RecordRequestCallbacks::onWrite(BLECharacteristic *pCharacteristic) {
    Serial.println("Client write");
    uint16_t offset = 0;
    memcpy(&offset, pCharacteristic->getData(), sizeof(uint16_t));

    if (offset == 0xFFFF) {
        Serial.println("Sending a new measure");
        _owner->updateCurrentRecord();
    } else {
        Serial.print("Sending record at offset ");
        Serial.println(offset);
        _owner->sendRecords(offset);
    }
}

// --- BleSensorServer Implementation ---
BleSensorServer::BleSensorServer(String deviceName, FramStorage *framStorage, SHTSensor *sensor, DS3231Clock *rtc)
    : _deviceName(std::move(deviceName)),
      _pServer(nullptr),
      _pService(nullptr),
      _requestCharacteristic(nullptr),
      _dataCharacteristic(nullptr),
      _connectedClients(0),
      _fram(framStorage),
      _sht(sensor),
      _rtc(rtc) {
}

void BleSensorServer::begin() {
    Serial.println("Initializing BLE Sensor Server...");
    Serial.print("Device Name: ");
    Serial.println(_deviceName.c_str());

    BLEDevice::init(_deviceName);
    _pServer = BLEDevice::createServer();
    _pServer->setCallbacks(new ServerCallbacks(this)); // Attach callbacks

    _pService = _pServer->createService(RECORD_SERVICE_UUID);

    // REQUEST NOTIFIER
    _requestCharacteristic = _pService->createCharacteristic(
        RECORD_REQUEST_CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_WRITE
    );
    _requestCharacteristic->setCallbacks(new RecordRequestCallbacks(this));

    // DATA LISTING
    _dataCharacteristic = _pService->createCharacteristic(
        RECORD_DATA_CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ
    );

    _pService->start();

    // Configure and start advertising
    BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(RECORD_SERVICE_UUID);
    pAdvertising->setScanResponse(true); // Necessary for some scanning apps to see service UUIDs

    // These calls were in your example; they relate to connection parameters.
    // Note: The exact methods `setMinPreferred`/`setMaxPreferred` might vary slightly
    // or be part of specific BLE stack versions. Common methods are `setMinInterval`/`setMaxInterval`.
    // Assuming these are correct for your environment as per the example:
    pAdvertising->setMinPreferred(0x06); // Helps with iPhone connection issues
    pAdvertising->setMinPreferred(0x12); // This will override the previous setMinPreferred.
    // You might intend setMinInterval and setMaxInterval,
    // or setMinPreferred and setMaxPreferred if available.
    // For now, matching the example's structure.

    BLEDevice::startAdvertising();
    Serial.println("BLE Sensor Server started. Advertising...");
    Serial.print("Service UUID: ");
    Serial.println(RECORD_SERVICE_UUID);
}

void BleSensorServer::sendRecords(const uint16_t offset) const {
    const uint16_t first = _fram->readUInt16(FIRST_RECORD_ADDRESS);
    const uint16_t last  = _fram->readUInt16(LAST_RECORD_ADDRESS);

    // Calculate how many full records are available
    uint16_t available;
    if (last >= first)
        available = (last - first) / RECORD_SIZE_BYTES;
    else
        available = (RECORD_END_ADDRESS - first + last - RECORD_START_ADDRESS) / RECORD_SIZE_BYTES;

    if (offset >= available) {
        _dataCharacteristic->setValue(nullptr, 0);
        return;
    }

    // Compute record address, handling wrap
    int32_t address = last - offset * RECORD_SIZE_BYTES;
    if (address < RECORD_START_ADDRESS)
        address += (RECORD_END_ADDRESS - RECORD_START_ADDRESS);

    // Read and send
    const auto reading = _fram->readSensorReading(address);
    BluetoothRecord record = {
        offset,
        reading.temperature,
        reading.humidity,
        reading.timestamp
    };

    uint8_t buffer[BLUETOOTH_RECORD_SIZE];
    serializeBluetoothRecord(&record, buffer);
    _dataCharacteristic->setValue(buffer, BLUETOOTH_RECORD_SIZE);
}


void BleSensorServer::updateCurrentRecord() const {
    BluetoothRecord reading = {
        0xFFFF,
        _sht->getTemperature(),
        _sht->getHumidity(),
        _rtc->getCurrentDateTime().Unix32Time(),
    };
    uint8_t buffer[BLUETOOTH_RECORD_SIZE];
    serializeBluetoothRecord(&reading, buffer);
    _dataCharacteristic->setValue(buffer, BLUETOOTH_RECORD_SIZE);
}


bool BleSensorServer::isClientConnected() {
    return _connectedClients > 0;
}

void BleSensorServer::serializeBluetoothRecord(BluetoothRecord *record, uint8_t *buffer) const {
    size_t i = 0;

    // Serialize offset (uint16_t)
    buffer[i++] = record->offset & 0xFF;
    buffer[i++] = (record->offset >> 8) & 0xFF;

    // Serialize temperature (float)
    memcpy(&buffer[i], &record->temperature, sizeof(float));
    i += sizeof(float);

    // Serialize humidity (float)
    memcpy(&buffer[i], &record->humidity, sizeof(float));
    i += sizeof(float);

    // Serialize timestamp (uint32_t)
    buffer[i++] = record->timestamp & 0xFF;
    buffer[i++] = (record->timestamp >> 8) & 0xFF;
    buffer[i++] = (record->timestamp >> 16) & 0xFF;
    buffer[i++] = (record->timestamp >> 24) & 0xFF;
}

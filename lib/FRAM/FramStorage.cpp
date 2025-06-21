//
// Created by Florian on 23-05-25.
//

#include "FramStorage.h"

FramStorage::FramStorage() : _initialized(false), _framSizeBytes(0) {
    // _fram object is default constructed
}

bool FramStorage::begin(uint8_t addr, uint32_t framSizeBytes, TwoWire *theWire) {
    _framSizeBytes = framSizeBytes;
    _initialized = _fram.begin(addr, theWire);
    // As an additional check, you might want to see if getDeviceID() returns a non-zero value
    // if (_initialized && getDeviceID() == 0) {
    //     _initialized = false; // Consider it a failure if device ID is 0 after successful begin
    // }
    return _initialized;
}

bool FramStorage::isInitialized() const {
    return _initialized;
}

uint32_t FramStorage::getFramSize() const {
    return _framSizeBytes;
}


bool FramStorage::_checkBounds(uint16_t address, size_t count) const {
    if (!_initialized) {
        return false;
    }
    if (_framSizeBytes == 0) { // If size is 0, bounds checking is effectively disabled by this class
        return true;
    }
    if (address >= _framSizeBytes) { // Start address out of bounds
        return false;
    }
    if (count == 0) { // Accessing zero bytes is fine if start address is okay
        return true;
    }
    // Check if (address + count) would exceed framSizeBytes, preventing overflow during sum
    if ((uint32_t)address + count > _framSizeBytes) {
        return false;
    }
    return true;
}

// --- Read Methods ---
uint8_t FramStorage::readByte(uint16_t framAddress) {
    if (!_checkBounds(framAddress, sizeof(uint8_t))) {
        return 0; // Default error value
    }
    return _fram.read(framAddress);
}

int16_t FramStorage::readInt16(uint16_t framAddress) {
    return readGeneric<int16_t>(framAddress, 0);
}

uint16_t FramStorage::readUInt16(uint16_t framAddress) {
    return readGeneric<uint16_t>(framAddress, 0);
}

int32_t FramStorage::readInt32(uint16_t framAddress) {
    return readGeneric<int32_t>(framAddress, 0);
}

uint32_t FramStorage::readUInt32(uint16_t framAddress) {
    return readGeneric<uint32_t>(framAddress, 0);
}

float FramStorage::readFloat(uint16_t framAddress) {
    return readGeneric<float>(framAddress, NAN); // Return NAN on error for float
}

double FramStorage::readDouble(uint16_t framAddress) {
    // sizeof(double) can be 4 (like float on AVR) or 8 (ESP32, SAMD).
    // readGeneric handles this based on the actual size.
    return readGeneric<double>(framAddress, NAN); // Return NAN on error for double
}

uint16_t FramStorage::readBytes(uint16_t framAddress, uint8_t* buffer, uint16_t length) {
    if (buffer == nullptr || length == 0 || !_initialized) {
        return 0;
    }

    uint16_t bytesToRead = length;
    if (_framSizeBytes > 0) { // If bounds checking is enabled
        if (framAddress >= _framSizeBytes) {
            return 0; // Start address out of bounds
        }
        // Calculate how many bytes can actually be read without going out of bounds
        if ((uint32_t)framAddress + length > _framSizeBytes) {
            bytesToRead = _framSizeBytes - framAddress;
        }
    }
    
    if (bytesToRead == 0 && length > 0) return 0; // Calculated no bytes to read within bounds

    for (uint16_t i = 0; i < bytesToRead; ++i) {
        buffer[i] = _fram.read(framAddress + i);
    }
    return bytesToRead;
}

String FramStorage::readString(uint16_t framAddress, uint16_t maxLength) {
    if (maxLength == 0 || !_initialized) {
        return String();
    }

    String result = "";
    uint16_t currentMaxLength = maxLength;

    if (_framSizeBytes > 0) { // Adjust maxLength based on FRAM size
        if (framAddress >= _framSizeBytes) {
            return String(); // Start address out of bounds
        }
        if ((uint32_t)framAddress + maxLength > _framSizeBytes) {
            currentMaxLength = _framSizeBytes - framAddress;
        }
    }
    
    if (currentMaxLength == 0 && maxLength > 0) return String();


    // Pre-allocate a reasonable amount of memory for the String
    result.reserve(currentMaxLength > 32 ? 32 : currentMaxLength);

    for (uint16_t i = 0; i < currentMaxLength; ++i) {
        uint8_t c = _fram.read(framAddress + i);
        if (c == '\0') { // Null terminator found
            break;
        }
        result += (char)c;
    }
    return result;
}

SensorReading FramStorage::readSensorReading(uint16_t framAddress) {
    // The readGeneric template method can handle reading the entire struct.
    // It will return a default-constructed SensorReading (all members 0)
    // if _checkBounds fails or if not initialized.
    return readGeneric<SensorReading>(framAddress);
}

// --- Write Methods ---
bool FramStorage::writeByte(uint16_t framAddress, uint8_t value) {
    if (!_checkBounds(framAddress, sizeof(uint8_t))) {
        return false;
    }
    _fram.write(framAddress, value);
    return true; // Adafruit_FRAM_I2C::write returns void, assume success if bounds check passed
}

bool FramStorage::writeInt16(uint16_t framAddress, int16_t value) {
    return writeGeneric<int16_t>(framAddress, value);
}

bool FramStorage::writeUInt16(uint16_t framAddress, uint16_t value) {
    return writeGeneric<uint16_t>(framAddress, value);
}

bool FramStorage::writeInt32(uint16_t framAddress, int32_t value) {
    return writeGeneric<int32_t>(framAddress, value);
}

bool FramStorage::writeUInt32(uint16_t framAddress, uint32_t value) {
    return writeGeneric<uint32_t>(framAddress, value);
}

bool FramStorage::writeFloat(uint16_t framAddress, float value) {
    return writeGeneric<float>(framAddress, value);
}

bool FramStorage::writeDouble(uint16_t framAddress, double value) {
    return writeGeneric<double>(framAddress, value);
}

bool FramStorage::writeBytes(uint16_t framAddress, const uint8_t* buffer, uint16_t length) {
    if (buffer == nullptr) return false;
    if (length == 0) return true; // Nothing to write, considered success
    if (!_checkBounds(framAddress, length)) {
        return false;
    }

    for (uint16_t i = 0; i < length; ++i) {
        _fram.write(framAddress + i, buffer[i]);
    }
    return true;
}

bool FramStorage::writeString(uint16_t framAddress, const char* str) {
    if (str == nullptr || !_initialized) { // Check _initialized here as _checkBounds relies on it
        return false;
    }
    uint16_t len = strlen(str);
    // Check bounds for the string content + null terminator
    if (!_checkBounds(framAddress, len + 1)) {
        return false;
    }

    for (uint16_t i = 0; i < len; ++i) {
        _fram.write(framAddress + i, (uint8_t)str[i]);
    }
    _fram.write(framAddress + len, '\0'); // Write null terminator
    return true;
}

bool FramStorage::writeString(uint16_t framAddress, const String& str) {
    if (!_initialized) return false;
    uint16_t len = str.length();
    // Check bounds for the string content + null terminator
    if (!_checkBounds(framAddress, len + 1)) {
        return false;
    }

    for (uint16_t i = 0; i < len; ++i) {
         _fram.write(framAddress + i, (uint8_t)str.charAt(i));
    }
    _fram.write(framAddress + len, '\0'); // Write null terminator
    return true;
}

bool FramStorage::writeSensorReading(uint16_t framAddress, const SensorReading& data) {
    // The writeGeneric template method can handle writing the entire struct.
    return writeGeneric<SensorReading>(framAddress, data);
}

// --- Private Helper Methods ---
template <typename T>
bool FramStorage::writeGeneric(uint16_t framAddress, const T& value) {
    if (!_checkBounds(framAddress, sizeof(T))) {
        return false;
    }
    const uint8_t* p = reinterpret_cast<const uint8_t*>(&value);
    for (size_t i = 0; i < sizeof(T); ++i) {
        _fram.write(framAddress + i, p[i]);
    }
    return true;
}

template <typename T>
T FramStorage::readGeneric(uint16_t framAddress, T defaultValue) {
    if (!_checkBounds(framAddress, sizeof(T))) {
        return defaultValue;
    }
    T value;
    uint8_t* p = reinterpret_cast<uint8_t*>(&value);
    for (size_t i = 0; i < sizeof(T); ++i) {
        p[i] = _fram.read(framAddress + i);
    }
    return value;
}
//
// Created by Florian on 23-05-25.
//

#ifndef FRAM_STORAGE_H
#define FRAM_STORAGE_H

#include <Wire.h>
#include <Adafruit_FRAM_I2C.h> // The HAL for FRAM interaction
#include <Arduino.h>           // For String, NAN, etc.
#include <SensorReading.h>

// Default I2C address for many FRAM chips (e.g., MB85RC series)
#define DEFAULT_FRAM_I2C_ADDRESS 0x50

#define RECORD_INTERVAL_SECONDS  (20*60) // 20 minutes

#define LAST_RECORD_TIMESTAMP_ADDRESS   0x00
#define FIRST_RECORD_ADDRESS            0x04
#define LAST_RECORD_ADDRESS             0x06
#define RECORD_START_ADDRESS            0x08
#define RECORD_END_ADDRESS              0x7CEC


#define NEXT_ADDRESS(address) (((uint16_t)(address) + RECORD_SIZE_BYTES >= RECORD_END_ADDRESS) ? (RECORD_START_ADDRESS) : ((address) + RECORD_SIZE_BYTES))
class FramStorage {
public:
    FramStorage();

    /**
     * @brief Initializes the FRAM module.
     * @param addr I2C address of the FRAM chip.
     * @param framSizeBytes Total size of the FRAM chip in bytes.
     *                      If 0 (default), no explicit bounds checking is performed by this class
     *                      beyond what the underlying address type (uint16_t) allows.
     * @param theWire Pointer to the TwoWire interface to use (e.g., &Wire, &Wire1).
     * @return True if initialization was successful (chip detected), false otherwise.
     */
    bool begin(uint8_t addr = DEFAULT_FRAM_I2C_ADDRESS, uint32_t framSizeBytes = 0, TwoWire *theWire = &Wire);

    /**
     * @brief Checks if the FRAM was successfully initialized.
     * @return True if initialized, false otherwise.
     */
    [[nodiscard]] bool isInitialized() const;

    /**
     * @brief Gets the configured size of the FRAM chip.
     * @return The size of the FRAM in bytes as configured in begin(). Returns 0 if not set.
     */
    [[nodiscard]] uint32_t getFramSize() const;

    // --- Read Methods ---
    // If not initialized or address is out of bounds (and size is set),
    // these methods typically return 0, NAN, or an empty String.

    uint8_t  readByte  (uint16_t framAddress);
    int16_t  readInt16 (uint16_t framAddress);
    uint16_t readUInt16(uint16_t framAddress);
    int32_t  readInt32 (uint16_t framAddress);
    uint32_t readUInt32(uint16_t framAddress);
    float    readFloat (uint16_t framAddress);
    double   readDouble(uint16_t framAddress); // Note: sizeof(double) varies by platform

    /**
     * @brief Reads a block of bytes from FRAM.
     * @param framAddress Starting address in FRAM.
     * @param buffer Pointer to the buffer to store read bytes.
     * @param length Number of bytes to read.
     * @return Number of bytes actually read. Can be less than 'length' if
     *         the end of FRAM (if size configured) is reached or if not initialized.
     */
    uint16_t readBytes(uint16_t framAddress, uint8_t* buffer, uint16_t length);

    /**
     * @brief Reads a null-terminated string from FRAM.
     * @param framAddress Starting address in FRAM.
     * @param maxLength Maximum number of characters to read (excluding null terminator).
     *                  Reading stops at null terminator or after maxLength characters.
     * @return The String read from FRAM. Empty if error or not found.
     */
    String readString(uint16_t framAddress, uint16_t maxLength);

    /**
     * @brief Reads a SensorReading structure from FRAM.
     * @param framAddress Starting address in FRAM where the structure is stored.
     * @return SensorReading object. Members will be default-initialized (e.g., 0) on error or if not initialized.
     */
    SensorReading readSensorReading(uint16_t framAddress);


    // --- Write Methods ---
    // All write methods return true on success, false on failure (e.g., not initialized, address out of bounds).

    bool writeByte  (uint16_t framAddress, uint8_t value);
    bool writeInt16 (uint16_t framAddress, int16_t value);
    bool writeUInt16(uint16_t framAddress, uint16_t value);
    bool writeInt32 (uint16_t framAddress, int32_t value);
    bool writeUInt32(uint16_t framAddress, uint32_t value);
    bool writeFloat (uint16_t framAddress, float value);
    bool writeDouble(uint16_t framAddress, double value);

    /**
     * @brief Writes a block of bytes to FRAM.
     * @param framAddress Starting address in FRAM.
     * @param buffer Pointer to the buffer containing bytes to write.
     * @param length Number of bytes to write.
     * @return True if all bytes were successfully passed to the write function, false otherwise
     *         (e.g., not initialized, out of bounds).
     */
    bool writeBytes(uint16_t framAddress, const uint8_t* buffer, uint16_t length);

    /**
     * @brief Writes a null-terminated C-string to FRAM.
     * @param framAddress Starting address in FRAM.
     * @param str The C-string to write. A null terminator will also be written.
     * @return True on success, false otherwise.
     */
    bool writeString(uint16_t framAddress, const char* str);

    /**
     * @brief Writes an Arduino String object to FRAM.
     * @param framAddress Starting address in FRAM.
     * @param str The String object to write. A null terminator will also be written.
     * @return True on success, false otherwise.
     */
    bool writeString(uint16_t framAddress, const String& str);

    /**
     * @brief Writes a SensorReading structure to FRAM.
     * @param framAddress Starting address in FRAM to store the structure.
     * @param data The SensorReading object to write.
     * @return True on success, false otherwise (e.g., not initialized, out of bounds).
     */
    bool writeSensorReading(uint16_t framAddress, const SensorReading& data);

private:
    Adafruit_FRAM_I2C _fram;    // Instance of the Adafruit FRAM HAL
    bool _initialized;
    uint32_t _framSizeBytes; // For optional bounds checking

    /**
     * @brief Internal helper to check if an access is within configured bounds.
     * @param address Starting address of the access.
     * @param count Number of bytes to access.
     * @return True if access is within bounds or if bounds checking is disabled, false otherwise.
     */
    [[nodiscard]] bool _checkBounds(uint16_t address, size_t count) const;

    /**
     * @brief Generic template helper for writing any data type.
     */
    template <typename T>
    bool writeGeneric(uint16_t framAddress, const T& value);

    /**
     * @brief Generic template helper for reading any data type.
     */
    template <typename T>
    T readGeneric(uint16_t framAddress, T defaultValue = T());
};

#endif // FRAM_STORAGE_H
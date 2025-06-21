
#ifndef DS3231CLOCK_H
#define DS3231CLOCK_H


#include <Wire.h>
#include <RtcDS3231.h>

class DS3231Clock {
public:
    // Constructor
    DS3231Clock();

    // Initializes the RTC module and sets initial time if needed
    void begin();

    // Gets the current date and time from the RTC
    RtcDateTime getCurrentDateTime();

    // Sets the RTC's date and time
    void setTime(const RtcDateTime& dt);

    // Gets the temperature from the RTC's sensor
    float getTemperature(); // Returns temperature in Celsius, or NAN on error

    // Checks if the RTC's date and time are considered valid
    bool isDateTimeValid();

private:
    RtcDS3231<TwoWire> _rtc; // The RTC library object
    uint8_t _lastErrorCode;  // Stores the last error code from I2C communication

    // Helper function to check and print I2C errors
    // Returns true if an error occurred, false otherwise
    bool wasError(const char *topic);

    // Helper function to print RtcDateTime objects to Serial
    void printDateTime(const RtcDateTime &dt);
};

#endif // DS3231CLOCK_H

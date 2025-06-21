#include "DS3132Clock.h"

// Constructor: Initializes the RTC object and last error code
DS3231Clock::DS3231Clock() : _rtc(Wire), _lastErrorCode(Rtc_Wire_Error_None) {
    // The _rtc member is initialized using the member initializer list with Wire.
    // _lastErrorCode is initialized to no error.
}

void DS3231Clock::begin() {
    // It's good practice to ensure Serial is started before printing.
    // This might be done in the main sketch's setup().
    // If not, uncommenting the next two lines can be helpful for debugging,
    // but a library class shouldn't typically manage Serial.begin() itself.
    // if (!Serial) {
    //   Serial.begin(115200); // Or your preferred baud rate
    //   while(!Serial); // Wait for serial port to connect (for some boards)
    // }

    Serial.print("DS3231Clock: Compiled on ");
    Serial.print(__DATE__);
    Serial.print(" at ");
    Serial.println(__TIME__);

    _rtc.Begin(); // Initialize the RTC communication
    if (wasError("RTC Begin")) {
        Serial.println("DS3231Clock: Critical error during RTC.Begin(). Halting further RTC setup.");
        return; // Early exit if RTC.Begin() fails
    }


#if defined(WIRE_HAS_TIMEOUT)
    Wire.setWireTimeout(3000 /* us */, true /* reset_on_timeout */);
    Serial.println("DS3231Clock: Wire timeout set.");
#endif

    RtcDateTime compiledTime(__DATE__, __TIME__);
    Serial.print("DS3231Clock: Compile time: ");
    printDateTime(compiledTime);
    Serial.println();

    if (!_rtc.IsDateTimeValid()) {
        if (!wasError("IsDateTimeValid (initial check)")) { // Check for I2C error first
            Serial.println("DS3231Clock: RTC lost confidence in the DateTime or was not set!");
            Serial.println("DS3231Clock: Setting RTC to compile time.");
            _rtc.SetDateTime(compiledTime);
            if (wasError("SetDateTime (after invalid)")) {
                 Serial.println("DS3231Clock: Failed to set RTC time after invalid state.");
            }
        } else {
            Serial.println("DS3231Clock: Error communicating with RTC to check if DateTime is valid.");
        }
    }

    if (!_rtc.GetIsRunning()) {
        if (!wasError("GetIsRunning")) { // Check for I2C error first
            Serial.println("DS3231Clock: RTC was not actively running, starting now.");
            _rtc.SetIsRunning(true);
            if (wasError("SetIsRunning")) {
                Serial.println("DS3231Clock: Failed to start the RTC.");
            }
        } else {
             Serial.println("DS3231Clock: Error communicating with RTC to check if it's running.");
        }
    }

    RtcDateTime currentTime = _rtc.GetDateTime();
    if (!wasError("GetDateTime (sync check)")) { // Check for I2C error first
        if (currentTime < compiledTime) {
            Serial.println("DS3231Clock: RTC time is older than compile time. Updating RTC time.");
            _rtc.SetDateTime(compiledTime);
            if (wasError("SetDateTime (sync update)")) {
                Serial.println("DS3231Clock: Failed to update RTC time to compile time.");
            }
        } else if (currentTime > compiledTime) {
            Serial.println("DS3231Clock: RTC time is newer than compile time (expected).");
        } else { // currentTime == compiledTime
            Serial.println("DS3231Clock: RTC time matches compile time.");
        }
    } else {
        Serial.println("DS3231Clock: Error getting current time for sync check.");
    }

    // Configure RTC pins to a known state
    _rtc.Enable32kHzPin(false);
    if (wasError("Enable32kHzPin(false)")) {
        Serial.println("DS3231Clock: Error disabling 32kHz pin.");
    } else {
        Serial.println("DS3231Clock: 32kHz pin output disabled.");
    }

    _rtc.SetSquareWavePin(DS3231SquareWavePin_ModeNone);
    if (wasError("SetSquareWavePin(ModeNone)")) {
        Serial.println("DS3231Clock: Error setting square wave pin to None.");
    } else {
        Serial.println("DS3231Clock: Square wave pin output disabled.");
    }
    Serial.println("DS3231Clock: begin() complete.");
}

RtcDateTime DS3231Clock::getCurrentDateTime() {
    // First, check if the time is marked as valid by the RTC chip itself
    if (!_rtc.IsDateTimeValid()) {
        // This specific check for IsDateTimeValid doesn't involve an I2C read that LastError() would catch
        // if the OSF flag is set. It's more of a status flag.
        // However, if there was a previous I2C error, wasError might report it.
        // For clarity, we can call wasError to see if there's a lingering I2C issue.
        if (!wasError("IsDateTimeValid (pre-get)")) { // Check for I2C error before proceeding
             Serial.println("DS3231Clock: RTC lost confidence in the DateTime (OSF bit set).");
             // Depending on policy, you might want to return an invalid RtcDateTime or attempt a fix.
             // For now, we'll proceed to try and read it anyway, as GetDateTime will clear OSF if successful.
        } else {
            Serial.println("DS3231Clock: Communication error before checking IsDateTimeValid.");
            return RtcDateTime(0); // Return an obviously invalid time
        }
    }

    RtcDateTime now = _rtc.GetDateTime();
    if (wasError("GetDateTime (current)")) { // This checks for errors during the GetDateTime I2C transaction
        Serial.println("DS3231Clock: Error reading current DateTime from RTC.");
        return RtcDateTime(0); // Return an invalid/epoch time on error
    }

    // Optionally print the time when fetched - good for debugging
    // Serial.print("DS3231Clock: Current RTC DateTime: ");
    // printDateTime(now);
    // Serial.println();
    return now;
}

void DS3231Clock::setTime(const RtcDateTime& dt) {
    _rtc.SetDateTime(dt);
    if (wasError("SetDateTime")) {
        Serial.println("DS3231Clock: Error setting RTC DateTime.");
    } else {
        Serial.print("DS3231Clock: DateTime set to: ");
        printDateTime(dt);
        Serial.println();
    }
}

float DS3231Clock::getTemperature() {
    RtcTemperature temp = _rtc.GetTemperature();
    if (wasError("GetTemperature")) {
        Serial.println("DS3231Clock: Error reading temperature from RTC.");
        return NAN; // Not-A-Number to indicate error
    }
    // Serial.print("DS3231Clock: Temperature: ");
    // Serial.print(temp.AsFloatDegC());
    // Serial.println(" C");
    return temp.AsFloatDegC();
}

bool DS3231Clock::isDateTimeValid() {
    bool isValid = _rtc.IsDateTimeValid();
    if (wasError("IsDateTimeValid (check)")) { // Check for I2C communication error
        Serial.println("DS3231Clock: Error communicating with RTC for IsDateTimeValid check.");
        return false; // If communication failed, we can't trust the validity
    }
    if (!isValid) {
        // Serial.println("DS3231Clock: RTC reports DateTime is not valid (OSF bit set).");
    }
    return isValid;
}

// Private helper function to check and print I2C errors
bool DS3231Clock::wasError(const char *topic) {
    _lastErrorCode = _rtc.LastError(); // Update the stored last error code
    if (_lastErrorCode != Rtc_Wire_Error_None) {
        Serial.print("DS3231Clock: [");
        Serial.print(topic);
        Serial.print("] I2C Error (");
        Serial.print(_lastErrorCode);
        Serial.print("): ");
        switch (_lastErrorCode) {
            case Rtc_Wire_Error_TxBufferOverflow:
                Serial.println("Transmit buffer overflow");
                break;
            case Rtc_Wire_Error_NoAddressableDevice:
                Serial.println("No device responded at address");
                break;
            case Rtc_Wire_Error_UnsupportedRequest:
                Serial.println("Device doesn't support request");
                break;
            case Rtc_Wire_Error_Unspecific:
                Serial.println("Unspecified error");
                break;
            case Rtc_Wire_Error_CommunicationTimeout:
                Serial.println("Communication timeout");
                break;
            // Rtc_Wire_Error_None is handled by the if condition
            default:
                Serial.println("Unknown I2C error");
                break;
        }
        return true; // Error occurred
    }
    return false; // No error
}

// Private helper function to print RtcDateTime objects
void DS3231Clock::printDateTime(const RtcDateTime &dt) {
    char buffer[25]; // "MM/DD/YYYY HH:MM:SS" + null
    snprintf_P(buffer, sizeof(buffer),
               PSTR("%02u/%02u/%04u %02u:%02u:%02u"),
               dt.Month(), dt.Day(), dt.Year(),
               dt.Hour(), dt.Minute(), dt.Second());
    Serial.print(buffer);
}
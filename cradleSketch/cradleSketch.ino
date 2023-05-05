#include <ServoOsc.h>

// Signals
byte const INFERENCE_HAPPY = 'h';               // Signals received
byte const INFERENCE_NEUTRAL = 'n';             // --
byte const INFERENCE_DISTRESS = 'd';            // --  
byte const CLASSIFIER_MALFUNCTION = '-';        // --

String const PLAY_HAPPY_MUSIC = "1\n";          // Signals sent
String const PLAY_NEUTRAL_MUSIC = "2\n";        // --
String const PLAY_DISTRESS_MUSIC = "3\n";       // --
String const PAUSE_MUSIC = "p\n";               // --
String const RESUME_MUSIC = "r\n";              // --
String const CAMERA_ON = "e\n";                 // --
String const CAMERA_OFF = "d\n";                // --
String const INFER_EMOTION = "i\n";             // --

// Hardware
int const BAUD_RATE  = 9600;
int const SENSOR_PIN = 9;
int const SERVO_PIN  = 11;
int const LED_PIN    = LED_BUILTIN;
ServoOsc  cradleRockerServo;

// Status and Timers
bool isSensingMovement = false;                             // true when system polling sensor to see if HIGH for longer than awake decision threshold 
bool isSensorInCooldown = false;                            // true when sensor reads LOW during isSensingMovement period
bool isBabyAwake = false;                                   // true when sensor reads HIGH for longer than awake decision threshold
bool isAwaitingInference = false;                           // true when system requests inference via serial and is waiting for result
bool isCameraOn = false;                                    // true when system turns on camera right before requesting inference

unsigned long currentTimeMillis;                            // updated at the start of every loop() iteration
unsigned long movementStartMillis = INFINITY;               // started when movement is first detected from a "cold" state
unsigned long sensorCooldownStartMillis = INFINITY;         // started if sensor spontaneosly reads LOW when system isSensingMovement   
unsigned long responseStartMillis = 0;                      // started when cradle and lullaby settings are set according to received inference result

bool checkBabyAwake();
void setRockerPatternAndLullaby( byte inferredEmotion );

void setup() {
    pinMode( SENSOR_PIN, INPUT );
    pinMode( LED_PIN, OUTPUT );

    cradleRockerServo.setAmplitude(10);
    cradleRockerServo.setOffset(-45);
    cradleRockerServo.setPeriod(2500);
    cradleRockerServo.attach( SERVO_PIN );

    Serial.begin( BAUD_RATE );
    while (!Serial) {
        ; // wait for serial port to connect. Needed for native USB
    }

    digitalWrite( LED_PIN, HIGH );
    delay(1000);
    digitalWrite( LED_PIN, LOW );
}

void loop() {

    unsigned long const DELAY_BETWEEN_INFERENCE_CALLS_MILLIS = 10000;
    unsigned long const MAX_RESPONSE_LENGTH_MILLIS = 20000;
    
    currentTimeMillis = millis();
    isBabyAwake = checkBabyAwake();

    if ( isBabyAwake ) {

        if ( isAwaitingInference ) {

            if ( Serial.available() ) {
                byte inferredEmotion = Serial.read();
                setRockerPatternAndLullaby( inferredEmotion );
                responseStartMillis = millis();
                isAwaitingInference = false;
            }

        } else {

            if ( currentTimeMillis - responseStartMillis > DELAY_BETWEEN_INFERENCE_CALLS_MILLIS ) {
                
                if ( ! isCameraOn ) {               // Turn on camera if not already on
                    Serial.print( CAMERA_ON );
                    isCameraOn = true;
                    delay(50);
                }
                Serial.print( INFER_EMOTION );
                isAwaitingInference = true;
            }
        }

    } else {

        if ( isCameraOn ) {                         // Turn off camera if baby asleep
            Serial.print( CAMERA_OFF );
            isCameraOn = false;
        }
    }
    
    cradleRockerServo.update(); delay(20);
}

bool checkBabyAwake() {

    unsigned long const SENSOR_COOLDOWN_THRESHOLD_MILLIS = 2000;
    unsigned long const AWAKE_DECISION_THRESHOLD_MILLIS  = 8000;

    int sensor_reading = digitalRead(SENSOR_PIN);
    digitalWrite( LED_PIN, sensor_reading );

    if ( isSensingMovement ) {

        if ( sensor_reading == HIGH ) {

            isSensorInCooldown = false;
            if ( currentTimeMillis - movementStartMillis > AWAKE_DECISION_THRESHOLD_MILLIS ) {
                return true;
            }

        } else {

            if ( isSensorInCooldown ) {
                unsigned long timeElapsedCoolingDown = currentTimeMillis - sensorCooldownStartMillis;
                if ( timeElapsedCoolingDown > SENSOR_COOLDOWN_THRESHOLD_MILLIS ) {
                    isSensingMovement = false;
                }
            } else {
                sensorCooldownStartMillis = millis();
                isSensorInCooldown = true;
            }
        }

    } else {

        if ( sensor_reading == HIGH ) {
            isSensingMovement = true;
            movementStartMillis = millis();
        }

    }
    return false;
}

void setRockerPatternAndLullaby( byte inferredEmotion ) {

    int const HAPPY_AMPLITUDE    = 45, HAPPY_PERIOD   = 5000;
    int const NEUTRAL_AMPLITUDE  = 20, NEUTRAL_PERIOD = 5000;
    int const DISTRESS_AMPLITUDE = 45, DISTRES_PERIOD = 2500;

    switch (inferredEmotion) {

        case INFERENCE_HAPPY:
            cradleRockerServo.setAmplitude( HAPPY_AMPLITUDE );
            cradleRockerServo.setPeriod( HAPPY_PERIOD );
            Serial.print( PLAY_HAPPY_MUSIC );
            break;

        case INFERENCE_NEUTRAL:
            cradleRockerServo.setAmplitude( NEUTRAL_AMPLITUDE );
            cradleRockerServo.setPeriod( NEUTRAL_PERIOD );
            Serial.print( PLAY_NEUTRAL_MUSIC );
            break;

        case INFERENCE_DISTRESS:
            cradleRockerServo.setAmplitude( DISTRESS_AMPLITUDE );
            cradleRockerServo.setPeriod( DISTRES_PERIOD );
            Serial.print( PLAY_DISTRESS_MUSIC );
            break;

        default:
            break;

    }
}
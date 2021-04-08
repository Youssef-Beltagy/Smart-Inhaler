/*
    Written by Youssef Beltagy.
    
    Based on the sample code that is written by Neil Kolban,
    ported to Arduino ESP32 by Evandro Copercini, and updated by chegewara.
    
    Check this link for documentation and directions:
    https://github.com/nkolban/ESP32_BLE_Arduino
    
    Here is a video explanation of the notification feature. Though the code currently uses the read feature.
    Video: https://www.youtube.com/watch?v=oCMOYS71NIU

    I also recommend reading the header files of the BLE libraries.
    
   This program creates a BLE server that provides temperature, humidity, and two dummy characterists in a GATT server service.
   One of the two dummy characterisitcs returns values in the range ['A','Z'] and loops through them.
   The other dummy characteristic does the same but with values in the range of ['0','9']
   The service advertises itself with UUID of: 25380284-e1b6-489a-bbcf-97d8f7470aa4
   The service has a temperature characteristic UUID of: c3856cfa-4af6-4d0d-a9a0-5ed875d937cc
   The service has a humidity characteristic UUID of:e36d8858-cac3-4b03-9356-98b40fdd122e
   The service has a digit characteristic UUID of: 03192130-212a-48c9-b058-ee4dace59d26
   The service has a character characteristic UUID of: 71eec950-3841-4984-83fa-0cfd8b9c901f
   These UUIDs were randomly generated using https://www.uuidgenerator.net/.

   The program works as follows:
   1. Creates a BLE Server
   2. Creates a BLE Service
   3. Creates four BLE Characteristics (temperature, humidity, and two dummy characteristics) on the Service
   4. Start the service.
   5. Start advertising.

   The sensor readings are updated at the time of request using a CharacteristicCallBackHandler.
*/

// Libraries
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h> //TODO: delete after you confirm notification is unnecessary.
#include "DHT.h"

// Define the UUIDs
#define SERVICE_UUID "25380284-e1b6-489a-bbcf-97d8f7470aa4"
#define TEMP_CHARACTERISTIC_UUID "c3856cfa-4af6-4d0d-a9a0-5ed875d937cc"
#define HUMIDITY_CHARACTERISTIC_UUID "e36d8858-cac3-4b03-9356-98b40fdd122e"
#define DIGIT_CHARACTERISTIC_UUID "03192130-212a-48c9-b058-ee4dace59d26"
#define ALPHABET_CHARACTERISTIC_UUID "71eec950-3841-4984-83fa-0cfd8b9c901f"

// This preprocessor allows the compiler to compile debugging print statements.
// Comment the preprocessor line before releasing.
//#define SMART_PIN_DEBUG


// Global variables

// Bluetooth server global variables.
BLEServer *serverPtr = nullptr;
BLECharacteristic *TempCharacteristicPtr = nullptr;
BLECharacteristic *HumidityCharacteristicPtr = nullptr;
BLECharacteristic *DigitCharacteristicPtr = nullptr;
BLECharacteristic *AlphabetCharacteristicPtr = nullptr;

//TODO: delete after you confirm notification is unnecessary.
bool deviceConnected = false;

// The temperature and humidity sensor I used is DHT11.
// You can get if from Adafruit here: https://www.adafruit.com/product/386
// I recommend getting it in a kit or from another supplier to save money.
// I connected the sensor the pin 5 of the esp32.
DHT dht(5, DHT11);


//Helper functions

// Converts a float to a string (represented as null terminated array of characters at arr) and returns the size of the string.
uint8_t doubleToCharArray(double num, uint8_t* arr, signed char width = 3, unsigned char precision = 2){
  dtostrf(num, width, precision, (char*) arr);

  for(uint8_t i = 0; true; i++){
    if(arr[i] == '\0') return i;
  }
}


//Classes
//TODO: refactor after you confirm notification is unnecessary.
//This class is used to handle callbacks when the server connects or disconnects with another device.
class ServerCallbacks : public BLEServerCallbacks
{
  void onConnect(BLEServer *serverPtr)
  {
    #ifdef SMART_PIN_DEBUG
    Serial.println("Established Connection");
    #endif
    deviceConnected = true;
  };

  void onDisconnect(BLEServer *serverPtr)
  {
    #ifdef SMART_PIN_DEBUG
    Serial.println("Connection Lost");
    #endif

    deviceConnected = false;

    delay(500);                    // give the bluetooth stack the chance to get things ready
    serverPtr->startAdvertising(); // restart advertising

    #ifdef SMART_PIN_DEBUG
    Serial.println("Advertising");
    #endif
  }
};

// This class is used to process read requests.
class CharacteristicCallBackHandler : public BLECharacteristicCallbacks
{
  // A pointer to a function that fills a pointer to a byte array with data and returns the size.
  uint8_t (*function)(uint8_t*);

  // A pointer to a byte array to store values and send them through the Bluetooth connection.
  uint8_t* buf = nullptr;


  #ifdef SMART_PIN_DEBUG
  char* label = nullptr;
  #endif


  // On read, get the data and get the size of the data. Then update the value of the characteristic.
  void onRead(BLECharacteristic *ptr)
  {
    uint8_t size = (*function)(buf);
    ptr->setValue(buf, size);

    #ifdef SMART_PIN_DEBUG
    Serial.print(label);
    Serial.print("0x");
    for(int i = 0; i < size; i++)
      Serial.print(buf[i],HEX);
    Serial.println();
    #endif
  }

public:
  #ifdef SMART_PIN_DEBUG
  CharacteristicCallBackHandler(uint8_t (*function)(uint8_t*), uint8_t* buffer, char* label):
  function(function),
  bur(buffer),
  label(label){};
  #endif

  CharacteristicCallBackHandler(uint8_t (*function)(uint8_t*), uint8_t* buffer):
  function(function),
  buf(buffer){};
};

void setup()
{
  #ifdef  SMART_PIN_DEBUG
  Serial.begin(115200);
  #endif

  // Initialize the temperature sensor.
  dht.begin();

  // Create the BLE Device
  BLEDevice::init("Chicken-BLE");

  // Create the BLE Server
  serverPtr = BLEDevice::createServer();
  serverPtr->setCallbacks(new ServerCallbacks()); //TODO: delete after you confirm notification is unnecessary.

  // Create the BLE Service
  BLEService *pService = serverPtr->createService(SERVICE_UUID);

  // Create a BLE Characteristic for temperature
  TempCharacteristicPtr = pService->createCharacteristic(
      TEMP_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ);

  TempCharacteristicPtr->setCallbacks(new CharacteristicCallBackHandler(
        [](uint8_t * ptr)->uint8_t{
          return doubleToCharArray(dht.readTemperature(), ptr);
        },
        new uint8_t [10]
        // with the default parameters to doubleToCharArray, you need at least 6 bytes.
        // Maybe even seven if '-' for negative numbers is not counted in the width.
      )
    );

  // Create a BLE Characteristic for humidity
  HumidityCharacteristicPtr = pService->createCharacteristic(
      HUMIDITY_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ);

  
  HumidityCharacteristicPtr->setCallbacks(new CharacteristicCallBackHandler(
        [](uint8_t * ptr)->uint8_t{
          return doubleToCharArray(dht.readHumidity(), ptr);
        },
        new uint8_t [10]
      )
    );

  // Create a BLE Characteristic for digits
  DigitCharacteristicPtr = pService->createCharacteristic(
      DIGIT_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ);

  DigitCharacteristicPtr->setCallbacks(new CharacteristicCallBackHandler(
        [](uint8_t * ptr)->uint8_t{
          
          //Loop through '0'-'9' and back again to '0'

          if(*ptr < 48 || *ptr > 57) *ptr = 48;
          else
            *ptr = (*ptr - 48 + 1)%10 + 48;
          return 1;
        },
        new uint8_t [1]
      )
    );

  // Create a BLE Characteristic for Alphabet
  AlphabetCharacteristicPtr = pService->createCharacteristic(
      ALPHABET_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ);

  AlphabetCharacteristicPtr->setCallbacks(new CharacteristicCallBackHandler(
        [](uint8_t * ptr)->uint8_t{

          //Loop through 'A'-'Z' and back again to 'A'
          
          if(*ptr < 65 || *ptr > 90) *ptr = 65;
          else
            *ptr = (*ptr - 65 + 1)%26 + 65;
          return 1;
        },
        new uint8_t [1]
      )
    );

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(false);
  pAdvertising->setMinPreferred(0x0); // set value to 0x00 to not advertise this parameter
  BLEDevice::startAdvertising();

  #ifdef SMART_PIN_DEBUG
  Serial.println("Awaiting a client connection...");
  #endif
}

void loop()
{
  //spin wait.
}
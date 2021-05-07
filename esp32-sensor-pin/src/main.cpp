/*    
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
   The service has a WearableData characteristic UUID of: c3856cfa-4af6-4d0d-a9a0-5ed875d937cc
   These UUIDs were randomly generated using https://www.uuidgenerator.net/.

   The program works as follows:
   1. Creates a BLE Server
   2. Creates a BLE Service
   3. Creates wearable data BLE Characteristic on the Service
   4. Start the service.
   5. Start advertising.

   The sensor readings are updated at the time of request using a WearableDataCharacteristicCallBackHandler.
*/

// Libraries
#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include "DHT.h"

// Define the UUIDs
#define SERVICE_UUID "25380284-e1b6-489a-bbcf-97d8f7470aa4"
#define WEARABLE_DATA_CHARACTERISTIC_UUID "c3856cfa-4af6-4d0d-a9a0-5ed875d937cc"

// This preprocessor allows the compiler to compile debugging print statements.
// Comment the preprocessor line before releasing.
#define WEARABLE_SENSOR_DEBUG

// Global variables

// Bluetooth server global variables.
BLEServer *serverPtr = nullptr;
BLECharacteristic *wearableDataCharacteristicPtr = nullptr;

bool deviceConnected = false;
bool advertising = false;
uint32_t lastNotificationTime;
uint32_t minNotificationDelay = 20000; // 20 seconds

// The temperature and humidity sensor I used is DHT11.
// You can get it from Adafruit here: https://www.adafruit.com/product/386
// I recommend getting it in a kit or from another supplier to save money.
// I connected the sensor to pin 5 of the esp32.
DHT dht(5, DHT11);


//WearableData contract struct.
//This struct represents a single WearableData entry. It is ten bytes.
//Sizeof returns 12 (probably because esp32 is a 32-bit MCU so its memory is allocated in 4-byte blocks)
//In little Endian, you will find the temperature in the first four bytes. The humidity is the following four bytes.
//then a byte for the character (UTF-8) and another for the digit.
struct WearableData{
  float temperature;// 4 bytes - little endian
  float humidity;   // 4 bytes - little endian
  char  character;   // 1 byte
  char  digit;      // 1 byte
};

//TODO: consider making a characteristic Class.
WearableData* wearableDataPtr;

// Updates the values of the WearableData objected pointed to by wearableDataPtr
void updateWearableData(WearableData* wearableDataPtr){
  wearableDataPtr->temperature = dht.readTemperature(); // float in little Endian.
  wearableDataPtr->humidity = dht.readHumidity();

  //Set Digit
  //Loop through '0'-'9' and back again to '0'
  if(wearableDataPtr->digit < '0' || wearableDataPtr->digit > '9') wearableDataPtr->digit = '0';
  else
    wearableDataPtr->digit = (wearableDataPtr->digit - '0' + 1)%10 + '0';

  //Set character
  //Loop through 'A'-'Z' and back again to 'A'
  if(wearableDataPtr->character < 'A' || wearableDataPtr->character > 'Z') wearableDataPtr->character = 'A';
  else
    wearableDataPtr->character = (wearableDataPtr->character - 'A' + 1)%26 + 'A';

  #ifdef WEARABLE_SENSOR_DEBUG
  Serial.println("==============================================");
  Serial.println("\tWearableData");
  Serial.print("\t\tTemperature:");
  Serial.println(wearableDataPtr->temperature);
  Serial.print("\t\tHumidity:");
  Serial.println(wearableDataPtr->humidity);
  Serial.print("\t\tCharacter:");
  Serial.println(wearableDataPtr->character);
  Serial.print("\t\tDigit:");
  Serial.println(wearableDataPtr->digit);
  Serial.print("\tPacket: ");
  Serial.print("0x");
  for(int i = 0; i < sizeof(WearableData); i++){
    Serial.print(((uint8_t*) wearableDataPtr)[i],HEX);
    if(i + 1 != sizeof(WearableData)) Serial.print("-");
  }
  Serial.println();
  Serial.println("==============================================");
  #endif
}

//Classes
//This class is used to handle callbacks when the server connects or disconnects with another device.
class ServerCallbacks : public BLEServerCallbacks
{
  void onConnect(BLEServer *serverPtr)
  {
    #ifdef WEARABLE_SENSOR_DEBUG
    Serial.println("Established Connection");
    #endif

    deviceConnected = true;
    advertising = false;
  };

  void onDisconnect(BLEServer *serverPtr)
  {
    #ifdef WEARABLE_SENSOR_DEBUG
    Serial.println("Connection Lost");
    #endif

    deviceConnected = false;

    #ifdef WEARABLE_SENSOR_DEBUG
    Serial.println("Advertising");
    #endif
  }
};

// This class is used to process read requests.
class WearableDataCharacteristicCallBackHandler : public BLECharacteristicCallbacks
{
public:

  // On read, update the Wearable data.
  void onRead(BLECharacteristic *ptr)
  {
    updateWearableData(wearableDataPtr);
    ptr->setValue((uint8_t*) wearableDataPtr, sizeof(WearableData));
  }
  
};

void setup()
{
  #ifdef  WEARABLE_SENSOR_DEBUG
  Serial.begin(115200);
  #endif

  lastNotificationTime = millis();

  // Initialize the temperature sensor.
  dht.begin();

  // Initialize the WearableData
  wearableDataPtr = new WearableData();

  // Create the BLE Device
  BLEDevice::init("Wearable Sensor"); // device name
  BLEDevice::setEncryptionLevel(ESP_BLE_SEC_ENCRYPT); // To demand bonding


  // Create the BLE Server
  serverPtr = BLEDevice::createServer();

  serverPtr->setCallbacks(new ServerCallbacks());

  // Create the BLE Service
  BLEService *pService = serverPtr->createService(SERVICE_UUID);

  // Create a BLE Characteristic for temperature
  wearableDataCharacteristicPtr = pService->createCharacteristic(
      WEARABLE_DATA_CHARACTERISTIC_UUID,
      BLECharacteristic::PROPERTY_READ    |
      BLECharacteristic::PROPERTY_WRITE   |
      BLECharacteristic::PROPERTY_NOTIFY  |
      BLECharacteristic::PROPERTY_INDICATE  );

  wearableDataCharacteristicPtr->setCallbacks(new WearableDataCharacteristicCallBackHandler());

  wearableDataCharacteristicPtr->addDescriptor(new BLE2902()); // needed for notifications and indications

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();

  BLEAdvertisementData advertisementData = BLEAdvertisementData();
  
  advertisementData.setCompleteServices(BLEUUID(SERVICE_UUID)); // add the service ID to the advertisement data.
  advertisementData.setFlags(ESP_BLE_ADV_FLAG_GEN_DISC | ESP_BLE_ADV_FLAG_DMT_CONTROLLER_SPT | ESP_BLE_ADV_FLAG_DMT_HOST_SPT); // changing the flags didn't solve the autoconnect problem.
  pAdvertising->setAdvertisementData(advertisementData);

  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true); // the SERVICE_UUID only appears if this is true
  pAdvertising->setMinPreferred(0x0); // set value to 0x00 to not advertise this parameter.
  
  
  advertising = true;
  deviceConnected = false;
  BLEDevice::startAdvertising();

  #ifdef WEARABLE_SENSOR_DEBUG
  Serial.println("Awaiting a client connection...");
  #endif
}

void loop()
{
  if(deviceConnected && millis() - lastNotificationTime > minNotificationDelay){

    // Notify the last updated values.
    #ifdef  WEARABLE_SENSOR_DEBUG
    Serial.println("Attempt to send Notification");
    #endif
    updateWearableData(wearableDataPtr);
    wearableDataCharacteristicPtr->setValue((uint8_t*) wearableDataPtr, sizeof(WearableData));
    wearableDataCharacteristicPtr->notify();
    lastNotificationTime = millis();
  }

  if(!deviceConnected && !advertising){
    delay(500);
    BLEDevice::startAdvertising();
    advertising = true;
  }

}
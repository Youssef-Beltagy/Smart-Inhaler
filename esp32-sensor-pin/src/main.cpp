/*
    Written by Youssef Beltagy, the Chicken Lord
    
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
uint32_t lastNotificationTime;
uint32_t minNotificationDelay = 20000; // 20 seconds

// The temperature and humidity sensor I used is DHT11.
// You can get if from Adafruit here: https://www.adafruit.com/product/386
// I recommend getting it in a kit or from another supplier to save money.
// I connected the sensor the pin 5 of the esp32.
DHT dht(5, DHT11);


//WearableData contract struct.
//This struct represents a single WearableData entry. It is ten bytes big.
// Weirdly, Sizeof returns 12
struct WearableData{
  float temperature;// 4 bytes - little endian
  float humidity;   // 4 bytes - little endian
  char  character;   // 1 byte
  char  digit;      // 1 byte
};

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
  };

  void onDisconnect(BLEServer *serverPtr)
  {
    #ifdef WEARABLE_SENSOR_DEBUG
    Serial.println("Connection Lost");
    #endif

    deviceConnected = false;

    delay(500);                    // give the bluetooth stack a chance to get things ready
    serverPtr->startAdvertising(); // restart advertising

    #ifdef WEARABLE_SENSOR_DEBUG
    Serial.println("Advertising");
    #endif
  }
};

// This class is used to process read requests.
class WearableDataCharacteristicCallBackHandler : public BLECharacteristicCallbacks
{
public:

  // On read, get the data and get the size of the data. Then update the value of the characteristic.
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
  BLEDevice::init("Wearable Sensor");

  //-----------------FIXME
  //BLEDevice::setEncryptionLevel(ESP_BLE_SEC_ENCRYPT);
  //BLEDevice::setSecurityCallbacks();

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
      BLECharacteristic::PROPERTY_NOTIFY  );

  wearableDataCharacteristicPtr->setCallbacks(new WearableDataCharacteristicCallBackHandler());

  wearableDataCharacteristicPtr->addDescriptor(new BLE2902());

  // Start the service
  pService->start();

  // Start advertising
  BLEAdvertising *pAdvertising = BLEDevice::getAdvertising();

  // ---------------------------------- TODO: possibly add advertisement data
  // BLEAdvertisementData advertisementData;
  // // set Advertisement data.

  // pAdvertising->setAdvertisementData(advertisementData);

  BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
  
  oAdvertisementData.setCompleteServices(BLEUUID(SERVICE_UUID));
  
  pAdvertising->setAdvertisementData(oAdvertisementData);

  // TODO: Advertise Service UUID addServiceUUID
  // FIXME: follow best practices for adverising packets: https://reelyactive.github.io/diy/best-practices-ble-identifiers/
  // TODO: Make custom BLE characteristic
  // TODO: What do the flags mean.
  // TODO: What is the scan response?
  // ------------------------------------------

  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->setScanResponse(true); // the SERVICE_UUID only appears if this is true
  pAdvertising->setMinPreferred(0x0); // set value to 0x00 to not advertise this parameter. TODO: Consider changing this parameter as necessary.
  
  
  // Serial.print("connect: ");
  // Serial.println(serverPtr->connect(BLEAddress("D0:04:01:14:AE:22")));
  
  BLEDevice::startAdvertising();

  //TODO: Add advertising data
  //TODO: Possibly add characteristic descriptions

  #ifdef WEARABLE_SENSOR_DEBUG
  Serial.println("Awaiting a client connection...");
  #endif
}

void loop()
{
  if(deviceConnected && millis() - lastNotificationTime > minNotificationDelay){
    
    //TODO: update the data before notifying
    //TODO: test indications

    // Notify the last updated values.
    updateWearableData(wearableDataPtr);
    wearableDataCharacteristicPtr->setValue((uint8_t*) wearableDataPtr, sizeof(WearableData));
    wearableDataCharacteristicPtr->notify();
    lastNotificationTime = millis();
    #ifdef  WEARABLE_SENSOR_DEBUG
    Serial.println("Attempted to send Notification");
    #endif
  }
}


/*


/*
    Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleServer.cpp
    Ported to Arduino ESP32 by Evandro Copercini
*/

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>

// See the following for generating UUIDs:
// https://www.uuidgenerator.net/

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

#define LOOP_DELAY 100


float blePairTimeOut = 0;

class MySecurity : public BLESecurityCallbacks {
  
  bool onConfirmPIN(uint32_t pin){
    Serial.println("onConfirmPIN");
    if (blePairTimeOut > 0){
      return true;  
    }
    else{
      return false;
    }
  }
  
  uint32_t onPassKeyRequest(){
    Serial.println("onPassKeyRequest");
        ESP_LOGI(LOG_TAG, "PassKeyRequest");
    return 133700;
  }

  void onPassKeyNotify(uint32_t pass_key){
    Serial.println("onPassKeyNotify");
        ESP_LOGI(LOG_TAG, "On passkey Notify number:%d", pass_key);
  }

  bool onSecurityRequest(){
    Serial.println("onSecurityRequest");
      ESP_LOGI(LOG_TAG, "On Security Request");
    return true;
  }

  void onAuthenticationComplete(esp_ble_auth_cmpl_t cmpl){
    Serial.println("onAuthenticationComplete");
    ESP_LOGI(LOG_TAG, "Starting BLE work!");
    if(cmpl.success){
      Serial.println("onAuthenticationComplete -> success");
      uint16_t length;
      esp_ble_gap_get_whitelist_size(&length);
      ESP_LOGD(LOG_TAG, "size: %d", length);
    }
    else{
      Serial.println("onAuthenticationComplete -> fail");
    }
  }
};

void setup() {
    

    
  Serial.begin(115200);
  Serial.println("Starting BLE work!");

  BLEDevice::init("ESP32 TW");
  BLEDevice::setEncryptionLevel(ESP_BLE_SEC_ENCRYPT);
  /*
   * Required in authentication process to provide displaying and/or input passkey or yes/no butttons confirmation
   */
  BLEDevice::setSecurityCallbacks(new MySecurity());
  BLEServer *pServer = BLEDevice::createServer();
  
  //public services
  BLEService *pService = pServer->createService(SERVICE_UUID);
  BLECharacteristic *pCharacteristic = pService->createCharacteristic(
                                         CHARACTERISTIC_UUID,
                                         BLECharacteristic::PROPERTY_READ |
                                         BLECharacteristic::PROPERTY_WRITE
                                       );

  pCharacteristic->setValue("Hello World says Neil");
  pCharacteristic->setCallbacks("
  pService->start();
  BLEAdvertising *pAdvertising = pServer->getAdvertising();
  pAdvertising->start();



  
  BLESecurity *pSecurity = new BLESecurity();
  pSecurity->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND); //ESP_LE_AUTH_REQ_SC_ONLY
  pSecurity->setCapability(ESP_IO_CAP_KBDISP);
  pSecurity->setInitEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
  Serial.println("Characteristic defined! Now you can read it in your phone!");
}

void loop() {
  // put your main code here, to run repeatedly:
    int val = hallRead();
  // print the results to the serial monitor:
  //Serial.print("sensor = ");
  
  if (val > 100 ) {
    // turn LED on:
    Serial.println("BLE ACTIVE");
    blePairTimeOut = 10;
    
  } else {
    // turn LED off:
  }
  
  delay(LOOP_DELAY);
  if (blePairTimeOut > 0){
    blePairTimeOut = blePairTimeOut - LOOP_DELAY/1000;
  }
}



*/
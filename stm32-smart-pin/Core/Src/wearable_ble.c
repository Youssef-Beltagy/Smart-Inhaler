#include "wearable_ble.h"
#include "common_blesvc.h"
#include "app_ble.h"
#include <stdio.h>
#include "dht_11.h"
#include "PM_Sensor.h"
#include <math.h>


#define SERVICE_UUID 						"25380284e1b6489abbcf97d8f7470aa4"
#define WEARABLE_DATA_CHARACTERISTIC_UUID 	"c3856cfa4af64d0da9a05ed875d937cc"

#define RETRY_TIMES 2

typedef struct{
	float temperature;				// 4 bytes - little endian
	float humidity;   				// 4 bytes - little endian
	int32_t  particle_0_5_count; 	// 4 bytes - little endian
	char character;   				// 1 byte
	char  digit;      				// 1 byte
} wearable_data_t;

typedef struct{
  uint16_t	service_handler;				        /**< Service handle */
  uint16_t	data_characteristic_handler;	  /**< Characteristic handle */
} Wearable_Context_t;

PLACE_IN_SECTION("BLE_DRIVER_CONTEXT") static Wearable_Context_t wearable_context;


static wearable_data_t getWearableData(){

	uint8_t ret_code = 0;

	wearable_data_t data;

	{ // Get dht 11 sensor data
		//DHT 11 Data
		DHT_11_Data dht11_data;
		for(uint8_t i = 0; i < RETRY_TIMES ; i++){ // Linear retry logic
			ret_code = get_dht11_data (&dht11_data);
			if(ret_code) break; // success ; no need to retry
			HAL_Delay(5 + 5*i);
		}

		if(ret_code){ // success
			data.temperature = dht11_data.Temperature;
			data.humidity = dht11_data.Humidity;
		}else{
			data.temperature = NAN;
			data.humidity = NAN;
		}

	}

	{
		// PM2.5 sensor data
		struct PMS_AQI_data pm_data;
		for(uint8_t i = 0; i < RETRY_TIMES ; i++){ // Linear retry logic
			ret_code = read_pm_sensor_data(&pm_data);
			if(ret_code) break; // success ; no need to retry
			HAL_Delay(5 + 5*i);
		}

		if(ret_code){ // success
			data.particle_0_5_count = pm_data.particles_05um;
		}else{
			data.particle_0_5_count = 0xFFFFFFFF;
		}
	}

	// Character Dummy Data
	static char chr = 'A';
	data.character = chr;
	chr = (chr - 'A' + 1)%26 + 'A';

	static char dig = '0';
	data.digit = dig;
	dig = (dig - '0' + 1)%10 + '0';


	return data;
}

/**
 * @brief  Event handler
 * @param  Event: Address of the buffer holding the Event
 * @retval Ack: Return whether the Event has been managed or not
 */
static SVCCTL_EvtAckStatus_t Wearable_BLE_Event_Handler(void *Event)
{
  SVCCTL_EvtAckStatus_t return_value = SVCCTL_EvtNotAck;

  hci_event_pckt *event_pckt = (hci_event_pckt *)(((hci_uart_pckt*)Event)->data);

  switch(event_pckt->evt)
  {
    case HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE:
    {
    	  evt_blecore_aci *blecore_evt = (evt_blecore_aci*)event_pckt->data;
      switch(blecore_evt->ecode)
      {
        case ACI_GATT_READ_PERMIT_REQ_VSEVT_CODE:
        {
        	aci_gatt_read_permit_req_event_rp0* read_permit_req = (aci_gatt_read_permit_req_event_rp0*)blecore_evt->data;
			if(read_permit_req->Attribute_Handle == (wearable_context.data_characteristic_handler + 1))
			{
				//https://community.st.com/s/question/0D53W000003xw7LSAQ/basic-ble-reading-for-stm32wb

				static wearable_data_t data;

				data = getWearableData();

				//todo: I want to confirm whether this is synchronous or asynchronous. Could be a problem with two asynch calls right after each other.
				aci_gatt_update_char_value(wearable_context.service_handler,
						wearable_context.data_characteristic_handler,
															0, /* charValOffset */
															sizeof(wearable_data_t), /* charValueLen */
															(uint8_t*) (&data));
				aci_gatt_allow_read(read_permit_req->Connection_Handle);

				 return_value = SVCCTL_EvtNotAck;
			}
			break;
        }

        break;

        default:
        break;
      }
    }
    break; /* HCI_HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE_SPECIFIC */

    default:
      break;
  }

  return(return_value);
}/* end SVCCTL_EvtAckStatus_t */

uint8_t isWearableConnected(void){
	return APP_BLE_Get_Server_Connection_Status() == APP_BLE_CONNECTED_SERVER;
}

void Wearable_On_Connect(void){

}

void Wearable_On_Disconnect(void){

}


static uint8_t charToInt(char c){

	if( c >= '0' && c <= '9') return c - '0';
	if( c >= 'a' && c <= 'f') return c - 'a' + 10;
	if( c >= 'A' && c <= 'F') return c - 'A' + 10;

	return 0xff; //error;
}

static void Char_Array_To_128UUID(char * charArrayPtr, uint8_t* uuidPtr){

	uint8_t maxSize = 16;
    for (uint8_t count = 0; count < maxSize; count++) {

    	uuidPtr[maxSize - 1 - count] = charToInt(*charArrayPtr) << 4;
    	charArrayPtr ++;

    	uuidPtr[maxSize - 1 - count] += charToInt(*charArrayPtr);
    	charArrayPtr++;

    }

}

uint8_t Get_Wearable_Service_UUID(uint8_t* uuidPtr){
	Char_Array_To_128UUID(SERVICE_UUID, uuidPtr);
	return 16;
}

/**
 * @brief  Service initialization
 * @param  None
 * @retval None
 */
void Wearable_Sensor_Init(void)
{

  /**
   *	Register the event handler to the BLE controller
   */
  SVCCTL_RegisterSvcHandler(Wearable_BLE_Event_Handler);

    /**
     *  Wearable Data Service
     *
     * Max_Attribute_Records = 2*no_of_char + 1
     * service_max_attribute_record = 1 for service
     *                                2 for data characteristic
     *                                3 because I don't know what this is.
     *
     */
  	Char_UUID_t  uuid128;
  	Char_Array_To_128UUID(SERVICE_UUID , (uint8_t*)&uuid128);
    aci_gatt_add_service(UUID_TYPE_128,
                      (Service_UUID_t *) &uuid128,
                      PRIMARY_SERVICE,
                      6,
                      &(wearable_context.service_handler));

    /**
     *  Add LED Characteristic
     */
  	Char_Array_To_128UUID( WEARABLE_DATA_CHARACTERISTIC_UUID , (uint8_t*)&uuid128);
    aci_gatt_add_char(wearable_context.service_handler,
                      UUID_TYPE_128, &uuid128,
                      sizeof(wearable_data_t),
                      CHAR_PROP_READ,
					  ATTR_PERMISSION_AUTHEN_READ,
					  GATT_NOTIFY_READ_REQ_AND_WAIT_FOR_APPL_RESP, /* gattEvtMask */
                      10, /* encryKeySize */
                      1, /* isVariable */
                      &(wearable_context.data_characteristic_handler));

}

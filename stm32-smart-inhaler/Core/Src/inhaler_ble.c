#include "inhaler_ble.h"
#include "fram.h"
#include "common_blesvc.h"
#include "app_ble.h"
#include "pvd.h"
#include <stdio.h>
#include "stm32wbxx_hal.h"
#include "stm32_seq.h"
#include "inhaler_utilities.h"
#include "rtc.h"

#define SERVICE_UUID			 	"e814c25d7107459eb25d23fec96d49da"
#define TIME_CHARACTERISTIC_UUID 	"015529f7554c4138a71e40a2dfede10a"
#define IUE_CHARACTERISTIC_UUID 	"d7dc7c5048ce45a49c3e243a5bb75608"

#define LED_TICK                	(100*1000/CFG_TS_TICK_VAL) /**< 200ms */
#define LED_NUM_BLINKS				5
#define ADV_TIMOUT        			(10*1000*1000/CFG_TS_TICK_VAL) /**< 10s */

/**
 * Represents the state of the inhaler
 */
typedef struct{
  uint16_t	service_handler;				        /**< Service handle */
  uint16_t	timer_characteristic_handler;	  /**< Characteristic handle */
  uint16_t	iue_characteristic_handler;
  uint8_t	inhaler_connected;
  uint8_t	iue_indications_enabled;
  uint8_t	advertisement_timer_handler;
  uint8_t	blink_led_timer_handler;
}Inhaler_Context_t;

PLACE_IN_SECTION("BLE_DRIVER_CONTEXT") static Inhaler_Context_t inhaler_context;

/**
 * Chooses which LED to blink and initializes it.
 */
uint32_t pick_and_initialize_led(void){

	GPIO_InitTypeDef GPIO_InitStruct = {0};

	uint8_t battery_status = get_battery_status();

	if(battery_status == BATTERY_DEAD){

		/*Configure GPIO pin : PB12 -- RED LED */
		GPIO_InitStruct.Pin = GPIO_PIN_12;


	}else if(battery_status == BATTERY_ALMOST_DEAD){

		/*Configure GPIO pin : PB14 -- Yellow LED */
		GPIO_InitStruct.Pin = GPIO_PIN_14;

	}else{ 	// BATTERY_FULL

		/*Configure GPIO pin : PB13 -- Green LED */
		GPIO_InitStruct.Pin = GPIO_PIN_13;
	}


	GPIO_InitStruct.Mode = GPIO_MODE_OUTPUT_PP;
	GPIO_InitStruct.Pull = GPIO_NOPULL;
	GPIO_InitStruct.Speed = GPIO_SPEED_FREQ_LOW;
	HAL_GPIO_Init(GPIOB, &GPIO_InitStruct);

	return GPIO_InitStruct.Pin;

}

/**
 * A counter of how many times the LED was toggled.
 */
static uint8_t led_counter = 0;

/**
 * Blinks one of the LEDs depending on the battery status.
 */
void blink_led(void){

	static uint32_t pin = 0;

	if(led_counter == 0){
		HW_TS_Start(inhaler_context.blink_led_timer_handler, (uint32_t)LED_TICK);
		// The timeserver automatically stops and restarts the timer if it is already running

		if(pin != 0){
			HAL_GPIO_WritePin(GPIOB, pin, GPIO_PIN_RESET);
		}

		pin = pick_and_initialize_led();

	}

	led_counter++;

	HAL_GPIO_WritePin(GPIOB, pin, led_counter % 2);

	if(led_counter == LED_NUM_BLINKS * 2){
		//turn off LED
		HW_TS_Stop(inhaler_context.blink_led_timer_handler);
		led_counter = 0;
	}

}

/**
 * Starts advertising the device.
 */
void adv_start(void){

	if(!inhaler_context.inhaler_connected){
		Adv_Request(APP_BLE_FAST_ADV);
		HW_TS_Start(inhaler_context.advertisement_timer_handler, (uint32_t)ADV_TIMOUT);
		// The time server automatically stops and restarts a timer if it was already running.
	}

	led_counter = 0; //reset the LED blinks
	blink_led();

}

/**
 * Cancels advertising.
 */
void adv_cancel(){

	// After experimenting, it seems okay to call aci_gap_set_non_discoverable after a connection was made.
	if (!inhaler_context.inhaler_connected)
	{

		tBleStatus result = 0x00;

		result = aci_gap_set_non_discoverable();

		if (result == BLE_STATUS_SUCCESS)
		{
		  APP_DBG_MSG("  \r\n\r");
		  APP_DBG_MSG("** STOP ADVERTISING **  \r\n\r");
		}
		else
		{
		  APP_DBG_MSG("** STOP ADVERTISING **  Failed \r\n\r");
		}

	}

}

/**
 * Schedules a task to blink the LED
 */
void blink_led_req(){
	UTIL_SEQ_SetTask( 1<<CFG_TASK_BLINK_LED, CFG_SCH_PRIO_0);
}

/**
 * Schedules a task to cancel advertisement.
 */
void adv_cancel_req(void){
	UTIL_SEQ_SetTask( 1<<CFG_TASK_ADV_CANCEL_ID, CFG_SCH_PRIO_0);
}

void record_iue(void){

	IUE_t iue = {0};

	iue.timestamp = get_timestamp(TRUE);

	wakeup_fram();

	push(iue);

	hibernate_fram();

	UTIL_SEQ_SetTask(1 << CFG_TASK_SEND_IUE, CFG_SCH_PRIO_0);

}

void send_iue(void){

	if(!inhaler_context.inhaler_connected || !inhaler_context.iue_indications_enabled) return;

	wakeup_fram();

	IUE_t iue = peek();

	uint32_t stack_size = get_stack_size();

	hibernate_fram();

	if(stack_size == 0){

		// If there is nothing to send, terminate BLE connection to save power.
		if(inhaler_context.inhaler_connected) disconnect();

		return;
	}

	aci_gatt_update_char_value(inhaler_context.service_handler,
								inhaler_context.iue_characteristic_handler,
								0, /* charValOffset */
								sizeof(IUE_t), /* charValueLen */
								(uint8_t*) (&iue));


// FIXME: This function can be optimized to avoid waking the FRAM unnecessarily.
}

void pop_iue(){

	wakeup_fram();

	pop();

	hibernate_fram();

	UTIL_SEQ_SetTask(1 << CFG_TASK_SEND_IUE, CFG_SCH_PRIO_0);

}

void HAL_GPIO_EXTI_Callback(uint16_t GPIO_Pin){

	if(GPIO_Pin == GPIO_PIN_12){
		//SW1 -- Inhaler button
		UTIL_SEQ_SetTask(1 << CFG_TASK_RECORD_IUE, CFG_SCH_PRIO_0);
		UTIL_SEQ_SetTask( 1<<CFG_TASK_ADV_START, CFG_SCH_PRIO_0);
	}else if (GPIO_Pin == GPIO_PIN_0){
		//SW2 -- Multi-purpose button
		UTIL_SEQ_SetTask(1 << CFG_TASK_SEND_IUE, CFG_SCH_PRIO_0);
		UTIL_SEQ_SetTask( 1<<CFG_TASK_ADV_START, CFG_SCH_PRIO_0);
	}
}

static SVCCTL_EvtAckStatus_t inhaler_handler(evt_blecore_aci * blecore_evt){

	SVCCTL_EvtAckStatus_t return_value = SVCCTL_EvtNotAck;

	switch(blecore_evt->ecode)
	{
		case ACI_GATT_READ_PERMIT_REQ_VSEVT_CODE:
		{
			aci_gatt_read_permit_req_event_rp0* read_permit_req = (aci_gatt_read_permit_req_event_rp0*)blecore_evt->data;
			if(read_permit_req->Attribute_Handle == (inhaler_context.timer_characteristic_handler + 1))
			{
				//https://community.st.com/s/question/0D53W000003xw7LSAQ/basic-ble-reading-for-stm32wb

				static int64_t cur_timestamp = 0;

				cur_timestamp = get_timestamp(FALSE);// send the data in little endian.

				aci_gatt_update_char_value(inhaler_context.service_handler,
						inhaler_context.timer_characteristic_handler,
															0, /* charValOffset */
															sizeof(cur_timestamp), /* charValueLen */
															(uint8_t*) (&cur_timestamp));

				aci_gatt_allow_read(read_permit_req->Connection_Handle);

				 return_value = SVCCTL_EvtNotAck;
			}

		}
		break;

		case ACI_GATT_WRITE_PERMIT_REQ_VSEVT_CODE:
		{
			return_value = SVCCTL_EvtAckFlowEnable;

			uint8_t result = FALSE;

			aci_gatt_write_permit_req_event_rp0* write_request_req = (aci_gatt_write_permit_req_event_rp0*) blecore_evt->data;;
			if(write_request_req->Attribute_Handle == (inhaler_context.timer_characteristic_handler + 1)
					&& write_request_req->Data_Length == sizeof(int64_t))
			{	// Write to the clock characteristic.
				//Assume a 64 bit timestamp was sent in big endian
				uint8_t* arr = write_request_req->Data;
				int64_t timestamp = 0;

				for(int i = 0; i < sizeof(int64_t); i++){ // receive the data in little endian
					timestamp <<= 8;
					timestamp += arr[sizeof(int64_t) - i - 1];
				}

				 result = rtc_set_timestamp(timestamp);

			}

			if(result){

				/* received a correct value for HRM control point char */
				  result = aci_gatt_write_resp(write_request_req->Connection_Handle,
									  write_request_req->Attribute_Handle,
									  0x00, /* write_status = 0 (no error))*/
									  0x00, /* err_code TODO: specify later or find a a standard*/
									  write_request_req->Data_Length,
									  (uint8_t *)(write_request_req->Data));

			}else{
				// Failed to write event.
				/* received value of HRM control point char is incorrect */
				  aci_gatt_write_resp(write_request_req->Connection_Handle,
						  	  	  	  write_request_req->Attribute_Handle,
									  0x01, /* write_status = 1 (error))*/
									  0x01, /* err_code TODO: specify later or find a standard*/
									  write_request_req->Data_Length,
									  (uint8_t *)(write_request_req->Data));
			}

		}
		break;

		case ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE:
		{
			aci_gatt_attribute_modified_event_rp0* attribute_modified = (aci_gatt_attribute_modified_event_rp0*)blecore_evt->data;

			// Is this the IUE characteristic?
			if(attribute_modified->Attr_Handle == (inhaler_context.iue_characteristic_handler + 2))
			{
				return_value = SVCCTL_EvtAckFlowEnable;

				if(attribute_modified->Attr_Data[0] & COMSVC_Indication)
				{
					// Indications have been enabled.
					inhaler_context.iue_indications_enabled = TRUE;

					UTIL_SEQ_SetTask(1 << CFG_TASK_SEND_IUE, CFG_SCH_PRIO_0);

				}
				else
				{
					// Indications have been disabled.
					inhaler_context.iue_indications_enabled = FALSE;
				}
			}

		}
		break;

		case ACI_GATT_SERVER_CONFIRMATION_VSEVT_CODE:
		{

			//TODO: Is there a way to confirm this server confirmation came from an the iue characteristic indication?
			// I tried using aci_gatt_server_confirmation_even_rp0, but it didn't have the information I needed to confirm what caused this
			// server confirmation.
			//aci_gatt_server_confirmation_event_rp0* server_confirmation = (aci_gatt_server_confirmation_event_rp0*)blecore_evt->data;

			//FIXME: Assumes this server confirmation is for an indication and pop the last recorded IUE.
			// Notice that if an indication was sent, an iue was recorded, and then a server response received,
			// the wrong iue will be popped and the same iue will be sent twice.
			// This is a non-trivial issue to handle (I have limited time) and it is unlikely to happen seeing
			// that IUEs are recorded when a user pushes a button. BLE communication is fast enough that the
			// server response and everything will be happen before the user presses twice.
			UTIL_SEQ_SetTask(1 << CFG_TASK_POP_IUE, CFG_SCH_PRIO_0);
		}
		break;

		default:
		break;
	}


	return return_value;

}

/**
 * @brief  Event handler
 * @param  Event: Address of the buffer holding the Event
 * @retval Ack: Return whether the Event has been managed or not
 */
static SVCCTL_EvtAckStatus_t gatt_event_handler(void *Event)
{
  SVCCTL_EvtAckStatus_t return_value = SVCCTL_EvtNotAck;

  hci_event_pckt *event_pckt = (hci_event_pckt *)(((hci_uart_pckt*)Event)->data);

  switch(event_pckt->evt)
  {
    case HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE:
    {
	  evt_blecore_aci *blecore_evt = (evt_blecore_aci*)event_pckt->data;
	  return_value = inhaler_handler(blecore_evt);

    }
    break; /* HCI_HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE_SPECIFIC */

    default:
      break;
  }

  return(return_value);
}/* end SVCCTL_EvtAckStatus_t */

/**
 * Called when the inhaler establishes a BLE connection.
 */
void Inhaler_On_Connect(void){
	inhaler_context.inhaler_connected = TRUE;
	HW_TS_Stop(inhaler_context.advertisement_timer_handler);
}

/**
 * Called when the inhaler loses its BLE connection.
 */
void Inhaler_On_Disconnect(void){
	inhaler_context.inhaler_connected = FALSE;
	inhaler_context.iue_indications_enabled = FALSE; // Also disable indications.
}

/**
 * Converts a string into its BCD and reverses it.
 */
static void Char_Array_To_128UUID(char * charArrayPtr, uint8_t* uuidPtr){

	uint8_t maxSize = 16;
    for (uint8_t count = 0; count < maxSize; count++) {

    	uuidPtr[maxSize - 1 - count] = charToInt(*charArrayPtr) << 4;
    	charArrayPtr ++;

    	uuidPtr[maxSize - 1 - count] += charToInt(*charArrayPtr);
    	charArrayPtr++;

    }

}

/**
 * Populated the inhaler service UUID in the provided pointer.
 */
uint8_t Get_Inhaler_Service_UUID(uint8_t* uuidPtr){
	Char_Array_To_128UUID(SERVICE_UUID, uuidPtr);
	return 16;
}

/**
 * @brief  Initializes the inhaler GATT server, the PVD, the fram, and
 * the timers and tasks necessary for the inhaler to function.
 * @param  None
 * @retval None
 */
void Inhaler_Init(void)
{

	init_pvd();

	hibernate_fram();

	/**
	 * Register tasks in the sequencer
	 */
	UTIL_SEQ_RegTask( 1<<CFG_TASK_RECORD_IUE, UTIL_SEQ_RFU, record_iue);
	UTIL_SEQ_RegTask( 1<<CFG_TASK_SEND_IUE, UTIL_SEQ_RFU, send_iue);
	UTIL_SEQ_RegTask( 1<<CFG_TASK_POP_IUE, UTIL_SEQ_RFU, pop_iue);
	UTIL_SEQ_RegTask( 1<<CFG_TASK_ADV_CANCEL_ID, UTIL_SEQ_RFU, adv_cancel);
	UTIL_SEQ_RegTask( 1<<CFG_TASK_BLINK_LED, UTIL_SEQ_RFU, blink_led);
	UTIL_SEQ_RegTask( 1<<CFG_TASK_ADV_START, UTIL_SEQ_RFU, adv_start);


	inhaler_context.inhaler_connected = FALSE;
	inhaler_context.iue_indications_enabled = FALSE;

	/**
	* Create timer to handle the Advertising Stop
	*/
	HW_TS_Create(CFG_TIM_PROC_ID_ISR, &(inhaler_context.advertisement_timer_handler), hw_ts_SingleShot, adv_cancel_req);

	/**
	* Create timer to handle the blinking the LED Switch
	*/
	HW_TS_Create(CFG_TIM_PROC_ID_ISR, &(inhaler_context.blink_led_timer_handler), hw_ts_Repeated, blink_led_req);

	/**
	*	Register the event handler to the BLE controller
	*/
	SVCCTL_RegisterSvcHandler(gatt_event_handler);

    /**
     *  Inhaler Service
     *
     * Max_Attribute_Records = 2*no_of_char + 1
     * service_max_attribute_record = 1 for service
     *                                2 for iue characteristic
     *                                2 for clk/timer characteristic
     *                                3 because I don't know what this is.
     *
     */
  	Char_UUID_t  uuid128;
  	Char_Array_To_128UUID(SERVICE_UUID , (uint8_t*)&uuid128);
    aci_gatt_add_service(UUID_TYPE_128,
                      (Service_UUID_t *) &uuid128,
                      PRIMARY_SERVICE,
                      8,
                      &(inhaler_context.service_handler));

    /** TODO: this characteristic can be updated and used later to set the inhaler calendar through the phone.
     * Timer characteristic
     */
  	Char_Array_To_128UUID( TIME_CHARACTERISTIC_UUID , (uint8_t*)&uuid128);
    aci_gatt_add_char(inhaler_context.service_handler,
                      UUID_TYPE_128, &uuid128,
                      sizeof(int64_t),
                      CHAR_PROP_READ | CHAR_PROP_WRITE,
					  ATTR_PERMISSION_AUTHEN_READ | ATTR_PERMISSION_AUTHEN_WRITE,
					  GATT_NOTIFY_READ_REQ_AND_WAIT_FOR_APPL_RESP|GATT_NOTIFY_WRITE_REQ_AND_WAIT_FOR_APPL_RESP, /* gattEvtMask */
                      10, /* encryKeySize */
                      1, /* isVariable */
                      &(inhaler_context.timer_characteristic_handler));

    /**
     * The iue characteristic to send iues to the phone.
     */
  	Char_Array_To_128UUID( IUE_CHARACTERISTIC_UUID , (uint8_t*)&uuid128);
  	aci_gatt_add_char(inhaler_context.service_handler,
  	                      UUID_TYPE_128, &uuid128,
  	                      sizeof(IUE_t),
						  CHAR_PROP_INDICATE,
  						  ATTR_PERMISSION_AUTHEN_READ | ATTR_PERMISSION_AUTHEN_WRITE, // Authentication will also be required to enable the indications
						  GATT_DONT_NOTIFY_EVENTS, /* gattEvtMask */
  	                      10, /* encryKeySize */
  	                      1, /* isVariable */
  	                      &(inhaler_context.iue_characteristic_handler));

}

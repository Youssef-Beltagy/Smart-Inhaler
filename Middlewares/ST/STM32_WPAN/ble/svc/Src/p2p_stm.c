#include "common_blesvc.h"
#include <stdio.h>

// todo: too bloated: move to somewhere else

// todo: rename
/* Private typedef -----------------------------------------------------------*/
typedef struct{
  uint16_t	PeerToPeerSvcHdle;				        /**< Service handle */
  uint16_t	P2PWriteClientToServerCharHdle;	  /**< Characteristic handle */
  uint16_t	P2PNotifyServerToClientCharHdle;	/**< Characteristic handle */
#if(BLE_CFG_OTA_REBOOT_CHAR != 0)
  uint16_t  RebootReqCharHdle;                /**< Characteristic handle */
#endif
}PeerToPeerContext_t;

/* Private defines -----------------------------------------------------------*/
// Define the UUIDs
#define SERVICE_UUID "25380284e1b6489abbcf97d8f7470aa4"
#define WEARABLE_DATA_CHARACTERISTIC_UUID "c3856cfa4af64d0da9a05ed875d937cc"

/* Private macros ------------------------------------------------------------*/

/* Private variables ---------------------------------------------------------*/
/**
 * START of Section BLE_DRIVER_CONTEXT
 */
PLACE_IN_SECTION("BLE_DRIVER_CONTEXT") static PeerToPeerContext_t aPeerToPeerContext;

/**
 * END of Section BLE_DRIVER_CONTEXT
 */
/* Private function prototypes -----------------------------------------------*/
static SVCCTL_EvtAckStatus_t PeerToPeer_Event_Handler(void *pckt);

/**
 * @brief  Event handler
 * @param  Event: Address of the buffer holding the Event
 * @retval Ack: Return whether the Event has been managed or not
 */
static SVCCTL_EvtAckStatus_t PeerToPeer_Event_Handler(void *Event)
{
  SVCCTL_EvtAckStatus_t return_value;
  hci_event_pckt *event_pckt;
  evt_blecore_aci *blecore_evt;
  aci_gatt_attribute_modified_event_rp0    * attribute_modified;
  P2PS_STM_App_Notification_evt_t Notification;

  return_value = SVCCTL_EvtNotAck;
  event_pckt = (hci_event_pckt *)(((hci_uart_pckt*)Event)->data);

  switch(event_pckt->evt)
  {
    case HCI_VENDOR_SPECIFIC_DEBUG_EVT_CODE:
    {
      blecore_evt = (evt_blecore_aci*)event_pckt->data;
      switch(blecore_evt->ecode)
      {
        case ACI_GATT_ATTRIBUTE_MODIFIED_VSEVT_CODE:
       {
          attribute_modified = (aci_gatt_attribute_modified_event_rp0*)blecore_evt->data;
            if(attribute_modified->Attr_Handle == (aPeerToPeerContext.P2PNotifyServerToClientCharHdle + 2))
            {
              /**
               * Descriptor handle
               */
              return_value = SVCCTL_EvtAckFlowEnable;
              /**
               * Notify to application
               */
              if(attribute_modified->Attr_Data[0] & COMSVC_Notification)
              {
                Notification.P2P_Evt_Opcode = P2PS_STM__NOTIFY_ENABLED_EVT;
                P2PS_STM_App_Notification(&Notification);
              }
              else
              {
                Notification.P2P_Evt_Opcode = P2PS_STM_NOTIFY_DISABLED_EVT;
                P2PS_STM_App_Notification(&Notification);
              }
            }
            
            else if(attribute_modified->Attr_Handle == (aPeerToPeerContext.P2PWriteClientToServerCharHdle + 1))
            {
              BLE_DBG_P2P_STM_MSG("-- GATT : LED CONFIGURATION RECEIVED\n");
              Notification.P2P_Evt_Opcode = P2PS_STM_WRITE_EVT;
              Notification.DataTransfered.Length=attribute_modified->Attr_Data_Length;
              Notification.DataTransfered.pPayload=attribute_modified->Attr_Data;
              P2PS_STM_App_Notification(&Notification);  
            }

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


/* Public functions ----------------------------------------------------------*/

static void charArrayTo128UUID(char * charArrayPtr, uint8_t* uuidPtr){

    for (size_t count = 0; count < 16; count++) {
        sscanf(charArrayPtr, "%2hhx", &uuidPtr[count]);
        charArrayPtr += 2;
    }

}

/**
 * @brief  Service initialization
 * @param  None
 * @retval None
 */
void P2PS_STM_Init(void)
{
 

  /**
   *	Register the event handler to the BLE controller
   */
  SVCCTL_RegisterSvcHandler(PeerToPeer_Event_Handler);
  
    /**
     *  Peer To Peer Service
     *
     * Max_Attribute_Records = 2*no_of_char + 1
     * service_max_attribute_record = 1 for Peer To Peer service +
     *                                2 for P2P Write characteristic +
     *                                2 for P2P Notify characteristic +
     *                                1 for client char configuration descriptor +
     *                                
     */

  	Char_UUID_t  uuid128;

  	charArrayTo128UUID(SERVICE_UUID , (uint8_t*)&uuid128);
    aci_gatt_add_service(UUID_TYPE_128,
                      (Service_UUID_t *) &uuid128,
                      PRIMARY_SERVICE,
                      6,
                      &(aPeerToPeerContext.PeerToPeerSvcHdle));

    /**
     *  Add LED Characteristic
     */
  	charArrayTo128UUID( WEARABLE_DATA_CHARACTERISTIC_UUID , (uint8_t*)&uuid128);
    aci_gatt_add_char(aPeerToPeerContext.PeerToPeerSvcHdle,
                      UUID_TYPE_128, &uuid128,
                      2,                                   
                      CHAR_PROP_READ,
                      ATTR_PERMISSION_NONE,
                      GATT_NOTIFY_ATTRIBUTE_WRITE, /* gattEvtMask */
                      10, /* encryKeySize */
                      1, /* isVariable */
                      &(aPeerToPeerContext.P2PWriteClientToServerCharHdle));

}

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/

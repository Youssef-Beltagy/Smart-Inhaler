/* USER CODE BEGIN Header */
/**
 ******************************************************************************
 * @file    p2p_server_app.c
 * @author  MCD Application Team
 * @brief   peer to peer Server Application
 ******************************************************************************
 * @attention
 *
 * <h2><center>&copy; Copyright (c) 2019 STMicroelectronics.
 * All rights reserved.</center></h2>
 *
 * This software component is licensed by ST under Ultimate Liberty license
 * SLA0044, the "License"; You may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 *                             www.st.com/SLA0044
 *
 ******************************************************************************
 */
/* USER CODE END Header */

/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "app_common.h"
#include "dbg_trace.h"
#include "ble.h"
#include "p2p_server_app.h"
#include "stm32_seq.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <time.h>
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */
extern RTC_HandleTypeDef hrtc;

typedef struct {
  uint8_t               Notification_Status; /* used to check if P2P Server is enabled to Notify */
  uint16_t              ConnectionHandle;
  uint8_t  				Update_timer_Id;
} P2P_Server_App_Context_t;

/* USER CODE END PTD */

/* Private defines ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
/* USER CODE END PD */

/* Private macros -------------------------------------------------------------*/
/* USER CODE BEGIN PM */
/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/
/* USER CODE BEGIN PV */
RTC_TimeTypeDef currentTime;
RTC_DateTypeDef currentDate;
time_t timestamp;
struct tm currTime;
extern __IO uint8_t  timestamp_flag;

/**
 * START of Section BLE_APP_CONTEXT
 */

PLACE_IN_SECTION("BLE_APP_CONTEXT") static P2P_Server_App_Context_t P2P_Server_App_Context;

/**
 * END of Section BLE_APP_CONTEXT
 */
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
/* USER CODE BEGIN PFP */
static void P2PS_Send_Notification(void);
/* USER CODE END PFP */

/* Functions Definition ------------------------------------------------------*/
void P2PS_STM_App_Notification(P2PS_STM_App_Notification_evt_t *pNotification) {
	/* USER CODE BEGIN P2PS_STM_App_Notification_1 */
	/* USER CODE END P2PS_STM_App_Notification_1 */
	switch(pNotification->P2P_Evt_Opcode) {
		/* USER CODE BEGIN P2PS_STM_App_Notification_P2P_Evt_Opcode */
		/* USER CODE END P2PS_STM_App_Notification_P2P_Evt_Opcode */

    	case P2PS_STM__NOTIFY_ENABLED_EVT:
    		/* USER CODE BEGIN P2PS_STM__NOTIFY_ENABLED_EVT */
    		P2P_Server_App_Context.Notification_Status = 1;
    		/* USER CODE END P2PS_STM__NOTIFY_ENABLED_EVT */
    	break;

    	case P2PS_STM_NOTIFY_DISABLED_EVT:
    		/* USER CODE BEGIN P2PS_STM_NOTIFY_DISABLED_EVT */
    		P2P_Server_App_Context.Notification_Status = 0;
    		/* USER CODE END P2PS_STM_NOTIFY_DISABLED_EVT */
    	break;

    	case P2PS_STM_WRITE_EVT:
			/* USER CODE BEGIN P2PS_STM_WRITE_EVT */
			/* USER CODE END P2PS_STM_WRITE_EVT */
    	break;

    	default:
			/* USER CODE BEGIN P2PS_STM_App_Notification_default */
			/* USER CODE END P2PS_STM_App_Notification_default */
    	break;
	}
	/* USER CODE BEGIN P2PS_STM_App_Notification_2 */
	/* USER CODE END P2PS_STM_App_Notification_2 */
	return;
}

void P2PS_APP_Notification(P2PS_APP_ConnHandle_Not_evt_t *pNotification) {
	/* USER CODE BEGIN P2PS_APP_Notification_1 */
	/* USER CODE END P2PS_APP_Notification_1 */
	switch(pNotification->P2P_Evt_Opcode) {
		/* USER CODE BEGIN P2PS_APP_Notification_P2P_Evt_Opcode */
		/* USER CODE END P2PS_APP_Notification_P2P_Evt_Opcode */
		case PEER_CONN_HANDLE_EVT :
			/* USER CODE BEGIN PEER_CONN_HANDLE_EVT */
			/* USER CODE END PEER_CONN_HANDLE_EVT */
		break;

		case PEER_DISCON_HANDLE_EVT :
			/* USER CODE BEGIN PEER_DISCON_HANDLE_EVT */
			/* USER CODE END PEER_DISCON_HANDLE_EVT */
		break;

		default:
			/* USER CODE BEGIN P2PS_APP_Notification_default */
			/* USER CODE END P2PS_APP_Notification_default */
			break;
	}
	/* USER CODE BEGIN P2PS_APP_Notification_2 */
	/* USER CODE END P2PS_APP_Notification_2 */
	return;
}

void P2PS_APP_Init(void) {
	/* USER CODE BEGIN P2PS_APP_Init */
	UTIL_SEQ_RegTask( 1<< CFG_MY_TASK_NOTIFY_TIME, UTIL_SEQ_RFU, P2PS_Send_Notification );
	P2P_Server_App_Context.Notification_Status=0;
	/* USER CODE END P2PS_APP_Init */
	return;
}

/* USER CODE BEGIN FD */

/* USER CODE END FD */

/*************************************************************
 *
 * LOCAL FUNCTIONS
 *
 *************************************************************/
/* USER CODE BEGIN FD_LOCAL_FUNCTIONS*/

void P2PS_Send_Notification(void) {

	HAL_RTC_GetTime(&hrtc, &currentTime, RTC_FORMAT_BIN);
	HAL_RTC_GetDate(&hrtc, &currentDate, RTC_FORMAT_BIN);

	currTime.tm_year = currentDate.Year + 100;  // In fact: 2000 + 18 - 1900
	currTime.tm_mday = currentDate.Date;
	currTime.tm_mon  = currentDate.Month - 1;

	currTime.tm_hour = currentTime.Hours;
	currTime.tm_min  = currentTime.Minutes;
	currTime.tm_sec  = currentTime.Seconds;

	timestamp = mktime(&currTime);


	uint8_t value[4];

	value[0] = (uint8_t)(timestamp >> 24);
	value[1] = (uint8_t)(timestamp >> 16);
	value[2] = (uint8_t)(timestamp >> 8);
	value[3] = (uint8_t)(timestamp);


	if(P2P_Server_App_Context.Notification_Status && timestamp_flag){
		P2PS_STM_App_Update_Char(P2P_NOTIFY_CHAR_UUID, (uint8_t *)&value);

	} else {
		APP_DBG_MSG("-- P2P APPLICATION SERVER : CAN'T INFORM CLIENT -  NOTIFICATION DISABLED\n ");
	}

	return;
}

/* USER CODE END FD_LOCAL_FUNCTIONS*/

/************************ (C) COPYRIGHT STMicroelectronics *****END OF FILE****/

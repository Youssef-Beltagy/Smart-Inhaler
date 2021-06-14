/*
 * rtc.c
 *
 *  Created on: Jun 14, 2021
 *      Author: youss
 */

#include "rtc.h"


struct tm currTime;
RTC_TimeTypeDef currentTime;
RTC_DateTypeDef currentDate;
uint32_t theTimestamp;


//TODO: Organize this later:
// RTC is initialized in main
// Timeserver is initialized in app_entry and uses the rtc
// Prescaler is defined in app_conf.h
// Is the RTC powered by the LSE? This is necessary for it to function correctly in standby mode
// Todo: 32-bit timestamp is not ideal.

extern RTC_HandleTypeDef hrtc; //TODO: Consider passing this as a parameter rather than using extern.


/**
  * @brief  This function creates a timestamp using the RTC
  * @param  None
  * @retval 32 bit integer (decimal) timestamp in epoch time
  */
uint32_t get_timestamp( void )
{
	HAL_RTC_GetTime(&hrtc, &currentTime, RTC_FORMAT_BIN);
	HAL_RTC_GetDate(&hrtc, &currentDate, RTC_FORMAT_BIN);

	currTime.tm_year = currentDate.Year + 100;  // In fact: 2000 + 18 - 1900
	currTime.tm_mday = currentDate.Date;
	currTime.tm_mon  = currentDate.Month - 1;

	currTime.tm_hour = currentTime.Hours;
	currTime.tm_min  = currentTime.Minutes;
	currTime.tm_sec  = currentTime.Seconds;

	theTimestamp = (uint32_t) mktime(&currTime);
	return (theTimestamp);
}



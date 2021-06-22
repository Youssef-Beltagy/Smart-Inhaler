/*
 * rtc.h
 *
 *  Created on: Jun 14, 2021
 *      Author: youss
 */

#ifndef CORE_INC_RTC_H_
#define CORE_INC_RTC_H_

#include <time.h>
#include "stm32wbxx_hal.h"

/**
 * Gets the current time from the RTC as an epoch timestamp.
 */
int64_t get_timestamp(uint8_t in_millis);

/**
 * Sets the rtc using a timestamp
 * Returns TRUE on success; False on Failure
 */
uint8_t rtc_set_timestamp(time_t timestamp);

/**
 * Sets the rtc using date and time objects
 * Returns TRUE on success; False on Failure
 */
uint8_t rtc_set_datetime(RTC_DateTypeDef date, RTC_TimeTypeDef time);

/**
 * Populates sDate and sTime using the timestamp.
 *
 * The format is Binary and not BCD.
 */
void timestamp_to_rtc_datetime(RTC_DateTypeDef * date_ptr, RTC_TimeTypeDef * time_ptr, time_t timestamp);

/**
 * Convert RTC date and time to a utc seconds timestamp.
 *
 * The format of the input must be binary and not BCD.
 */
time_t rtc_datetime_to_timestamp(RTC_DateTypeDef date, RTC_TimeTypeDef time, int8_t hr_offset);

/**
* This was an attempt to automatically load the compile time into the rtc.
*
* However, as it turns out, DATE and TIME don't represent UNIX time in this environment.
*
* But simply  the system time (computer time).
*
* As such, this function alone is insufficient.
*/
void get_compile_time(RTC_DateTypeDef * sDate, RTC_TimeTypeDef * sTime, int8_t hr_offset);

#endif /* CORE_INC_RTC_H_ */

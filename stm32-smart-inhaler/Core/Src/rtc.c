#include "rtc.h"
#include "inhaler_utilities.h"
#include "main.h"

//TODO: Organize this later:
// RTC is initialized in main
// Timeserver is initialized in app_entry and uses the rtc
// Prescaler is defined in app_conf.h

extern RTC_HandleTypeDef hrtc;

/**
 * Sets the rtc using a timestamp
 * Returns TRUE on success; False on Failure
 */
uint8_t rtc_set_timestamp(time_t timestamp){
	RTC_DateTypeDef date;
	RTC_TimeTypeDef time;

	timestamp_to_rtc_datetime(&date, &time, timestamp);

	return rtc_set_datetime(date, time);

}

/**
 * Sets the rtc using date and time objects
 * Returns TRUE on success; False on Failure
 */
uint8_t rtc_set_datetime(RTC_DateTypeDef date, RTC_TimeTypeDef time){
	uint8_t result = TRUE;
	result = result && (HAL_OK ==  HAL_RTC_SetTime(&hrtc, &time, RTC_FORMAT_BIN));
	result = result && (HAL_OK == HAL_RTC_SetDate(&hrtc, &date, RTC_FORMAT_BIN));
	return result;
}

/**
 * Populates sDate and sTime using the timestamp.
 *
 * The format is Binary and not BCD.
 */
void timestamp_to_rtc_datetime(RTC_DateTypeDef * date_ptr, RTC_TimeTypeDef * time_ptr, time_t timestamp){

    struct tm ts = {0};

	ts = *(gmtime(&timestamp));

	ts.tm_year -= 100; // convert from date year from the 1900 to years from 2000
	ts.tm_mon += 1; // convert from 0-indexing for the months to 1-indexing

	date_ptr->Year = ts.tm_year;
	date_ptr->Month = ts.tm_mon;
	date_ptr->Date = ts.tm_mday;
	date_ptr->WeekDay = ts.tm_wday;

	time_ptr->Hours = ts.tm_hour;
	time_ptr->Minutes = ts.tm_min;
	time_ptr->Seconds = ts.tm_sec;

}

/**
 * Convert RTC date and time to a utc timestamp.
 *
 * The format of the input must be binary and not BCD.
 */
time_t rtc_datetime_to_timestamp(RTC_DateTypeDef date, RTC_TimeTypeDef time, int8_t hr_offset){

	struct tm datetime = {0};

	datetime.tm_year = date.Year + 100;  // In fact: + 2000 - 1900 -- convert from date year from 2000 to date year from 1900
	datetime.tm_mday = date.Date;
	datetime.tm_mon  = date.Month - 1; // convert from 1-indexing to 0-indexing

	datetime.tm_hour = time.Hours + hr_offset;
	datetime.tm_min  = time.Minutes;
	datetime.tm_sec  = time.Seconds;

	datetime.tm_isdst = 0;

	time_t curTime = mktime(&datetime); // get the timestamp.

	return curTime;

}

static void parse_datetime_macros(RTC_DateTypeDef * date_ptr, RTC_TimeTypeDef * time_ptr){
	char * date = __DATE__;
		switch(date[0]){
		case 'J':
			if(date[1] == 'a'){// Jan
				date_ptr->Month = 1;
			}else if (date[2] == 'n'){ // Jun
				date_ptr->Month = 6;
			}else{
				date_ptr->Month = 7; // Jul
			}
			break;
		case 'F':
			date_ptr->Month = 2; // Feb
			break;
		case 'M':
			if(date[2] == 'r'){ // Mar
				date_ptr->Month = 3;
			}else{
				date_ptr->Month = 5; // May
			}
			break;
		case 'A':
			if(date[2] == 'g'){ // Aug
				date_ptr->Month = 8;
			}else{
				date_ptr->Month = 4; // Apr
			}
			break;
		case 'S':
			date_ptr->Month = 9; // Sep
			break;
		case 'O':
			date_ptr->Month = 10; //Oct
			break;
		case 'N':
			date_ptr->Month = 11; // Nov
			break;
		case 'D':
			date_ptr->Month = 12; //Dec
			break;
		default:
		Error_Handler();
		}

		int i = 4;
		date_ptr->Date = charToInt(date[i]);
		i++;
		if(date[i] != ' '){
			date_ptr->Date *= 10;
			date_ptr->Date += charToInt(date[i]);
			i++;
		}
		i += 2;
		date_ptr->Year = charToInt(date[i]) * 100;
		i++;
		date_ptr->Year += charToInt(date[i]) * 10;
		i++;
		date_ptr->Year += charToInt(date[i]);
		i++;


		// Time
		char * ctime = __TIME__;
		i = 0;
		time_ptr->Hours = charToInt(ctime[i]) * 10;
		i++;
		time_ptr->Hours += charToInt(ctime[i]);
		i++;

		i++; // colon

		time_ptr->Minutes = charToInt(ctime[i]) * 10;
		i++;
		time_ptr->Minutes += charToInt(ctime[i]);
		i++;

		i++; // colon

		time_ptr->Seconds = charToInt(ctime[i]) * 10;
		i++;
		time_ptr->Seconds += charToInt(ctime[i]);
		i++;
}

/**
* This was an attempt to automatically load the compile time into the rtc.
*
* However, as it turns out, DATE and TIME don't represent UNIX time in this environment.
*
* But simply  the system time (computer time). As such, you need to manually specifiy the offset from UTC.
*/
void get_compile_time(RTC_DateTypeDef * date_ptr, RTC_TimeTypeDef * time_ptr, int8_t hr_offset){

	parse_datetime_macros(date_ptr, time_ptr);

	time_t curTime = rtc_datetime_to_timestamp(*date_ptr, *time_ptr, hr_offset); // convert to timestamp.

	timestamp_to_rtc_datetime(date_ptr, time_ptr, curTime); //convert timestamp back to RTC date time objects

}



/**
 * Gets the current time from the RTC as an epoch timestamp.
 *
 * returns the timestamp in seconds if in_millis is FALSE
 *
 * if in_millis is true, returns the timestamp in milliseconds
 */
int64_t get_timestamp( uint8_t in_millis )
{

	//TODO: Confirm the sleep modes don't ruin the shadow registers.
	// that when this function is called when the MCU wakes from sleep, it returns the time
	// right now rather than when it went to sleep.

	// If sleep mode does mess with the shadow registers, by-pass the shadow registers, read the time two times
	// and only return it if it didn't change.

	RTC_TimeTypeDef time;
	RTC_DateTypeDef date;

	HAL_RTC_GetTime(&hrtc, &time, RTC_FORMAT_BIN);
	HAL_RTC_GetDate(&hrtc, &date, RTC_FORMAT_BIN);

	int64_t theTimestamp = rtc_datetime_to_timestamp(date, time, 0);

	if(in_millis){
		// convert to epoch milliseconds
		theTimestamp *= 1000;
		theTimestamp += (time.SecondFraction - time.SubSeconds) * 1000 / (time.SecondFraction + 1);
	}

	return (theTimestamp);
}



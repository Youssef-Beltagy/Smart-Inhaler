/*
 * DHT.h
 *
 *  Created on: Jun 28, 2020
 *      Author: Controllerstech.com
 *
 *      Modified on May 30, 2021 for CSS 427
 */

#ifndef DHT_11_H_
#define DHT_11_H_

#include "stdbool.h"


typedef struct
{
	float Temperature;
	float Humidity;
}DHT_11_Data;

/**
 * This function is called within DHT_GetData.  Ideally, we would only
 * call this once to initialize the sensor, but for some reason,
 * unless it is called right before the sensor is read, the sensor does not respond.
 */
void DHT_Initialize();

/**
 * To get the DHT11 data, you only need to call this function.
 */
void DHT_GetData (DHT_11_Data *DHT_Data);

#endif /* INC_DHT_H_ */

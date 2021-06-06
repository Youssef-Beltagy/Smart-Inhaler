/*
 * data_interface.h
 *
 *  Created on: Jun 6, 2021
 */

#ifndef APPLICATION_USER_DATA_DATA_INTERFACE_H_
#define APPLICATION_USER_DATA_DATA_INTERFACE_H_


typedef struct{
	float temperature;// 4 bytes - little endian
	float humidity;   // 4 bytes - little endian
	char  character;  // todo: update to PM2.5
	char  digit;      // 1 byte
} wearable_data_t;


#endif /* APPLICATION_USER_DATA_DATA_INTERFACE_H_ */

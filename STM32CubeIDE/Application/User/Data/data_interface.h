/*
 * data_interface.h
 *
 *  Created on: Jun 6, 2021
 *      Author: youss
 */

#ifndef APPLICATION_USER_DATA_DATA_INTERFACE_H_
#define APPLICATION_USER_DATA_DATA_INTERFACE_H_


struct wearable_data{
	float temperature;// 4 bytes - little endian
	float humidity;   // 4 bytes - little endian
	char  character;  // todo: update to PM2.5
	char  digit;      // 1 byte
};


#endif /* APPLICATION_USER_DATA_DATA_INTERFACE_H_ */

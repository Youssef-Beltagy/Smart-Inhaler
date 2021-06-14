/*
 * pvd.h
 *
 *  Configurations for the PVD. Please see pvd.c
 */

#ifndef CORE_INC_PVD_H_
#define CORE_INC_PVD_H_


#include "stm32wbxx_hal.h"
#include "utilities_conf.h"


#define BATTERY_FULL  			0
#define BATTERY_ALMOST_DEAD  	1
#define BATTERY_DEAD  			2

#define BATTERY_UPPER_THRESHOLD PWR_PVDLEVEL_4  //Measured around 2.69 V
#define BATTERY_LOWER_THRESHOLD PWR_PVDLEVEL_3  //Measured around 2.570
#define PVD_EDGE_MODE 			PWR_PVD_MODE_IT_RISING 	//Strangely, RISING is for detecting when VDD is below the threshold

#define PVD_INTERRUPT_PRIORITY	15 // The lowest interrupt priority

void init_pvd();

uint8_t get_battery_status();

#endif /* CORE_INC_PVD_H_ */

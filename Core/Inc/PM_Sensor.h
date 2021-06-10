/*
 * PM_Sensor.h
 *
 *
 * Note: This is for the PMSA003I sensor by Plantower
 *
 *
 *  Created on: Jun 9, 2021
 *      Author:
 */

#ifndef SRC_PM_SENSOR_H_
#define SRC_PM_SENSOR_H_

#include "stdint.h"
#include "stdbool.h"
#include "string.h"
#include "stdio.h"

#include "stm32wbxx_hal.h"

#define PMS_ADDR 0x12 << 1 // Use 8-bit address
#define PMS_REG 0x00
#define PMS_BUF_LENGTH 32  // number of bytes in the standard PMS packet sent by the Plantower sensor

/**! Structure holding Plantower's standard packet **/
// IMPORTANT: DO NOT CHANGE - these variables must stay the same to match the packet
typedef struct PMS_AQI_data {
  uint16_t framelen;       // How long this data chunk is
  uint16_t pm10_standard,  // Standard PM1.0
      pm25_standard,       // Standard PM2.5
      pm100_standard;      // Standard PM10.0
  uint16_t pm10_env,       // Environmental PM1.0
      pm25_env,            // Environmental PM2.5
      pm100_env;           // Environmental PM10.0
  uint16_t particles_03um, // 0.3um Particle Count
      particles_05um,      // 0.5um Particle Count
      particles_10um,      // 1.0um Particle Count
      particles_25um,      // 2.5um Particle Count
      particles_50um,      // 5.0um Particle Count
      particles_100um;     // 10.0um Particle Count
  uint16_t unused;         // Unused
  uint16_t checksum;       // Packet checksum
} PMS_AQI_data;

/**
 * Reads PM sensor and populates input PM data struct
 * - returns false if some kind of error happened, true if everything went fine
 */
bool read_PM_Sensor(struct PMS_AQI_data *PM_data);

/**
 * print PMS data to UART1
 */
void print_PMSAQIdata(struct PMS_AQI_data PM_data);

#endif /* SRC_PM_SENSOR_H_ */

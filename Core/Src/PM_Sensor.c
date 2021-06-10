/*
 * PM_Sensor.c
 *
 *  Created on: Jun 9, 2021
 *      Author: panther
 */

#include "PM_Sensor.h"

static PMS_AQI_data default_PMS = {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};

// must have this defined in main.c
extern I2C_HandleTypeDef hi2c1;

// for printing/debugging
extern UART_HandleTypeDef huart1;

/**
 * IMPORTANT: Assumes an input buffer length of 32!
 *
 * Returns true if checksum makes sense, false if not
 */
bool parse_PMSAQIdata(uint8_t *buffer, struct PMS_AQI_data *PM_data) {
	uint16_t sum = 0;

	// get checksum ready
	for (uint8_t i = 0; i < PMS_BUF_LENGTH - 2; i++) {
		sum += buffer[i];
	}

	// Re-arrange bytes to fit exactly in our PMSAQIdata struct
	// - The data comes in endian'd, this solves it so it works on all platforms
	uint16_t buffer_u16[PMS_BUF_LENGTH / 2];
	for (uint8_t i = 0; i < PMS_BUF_LENGTH / 2; i++) {
		buffer_u16[i] = buffer[2 + i * 2 + 1];
		buffer_u16[i] += (buffer[2 + i * 2] << 8);
	}

	// pack it into the data struct
	*PM_data = default_PMS; // initialize to default
	memcpy((void *)&*PM_data, (void *)buffer_u16, PMS_BUF_LENGTH - 2);

	// does the checksum make sense?
	if (sum != PM_data->checksum) {
		return false;
	}

	return true;
}

/**
 * Reads PM sensor and populates input PM data struct
 */
bool read_PM_Sensor(struct PMS_AQI_data *PM_data) {
	HAL_StatusTypeDef ret;
	uint8_t buf[PMS_BUF_LENGTH];

	// Tell PM sensor that we want to read
	buf[0] = PMS_REG;
	ret = HAL_I2C_Master_Transmit(&hi2c1, PMS_ADDR, buf, 1, HAL_MAX_DELAY);

	if ( ret == HAL_OK ) {

		// Read PMS_BUF_LENGTH bytes (PMS_BUF_LENGTH = 32 is the standard Plantower packet size) from PM sensor
		ret = HAL_I2C_Master_Receive(&hi2c1, PMS_ADDR, buf, PMS_BUF_LENGTH, HAL_MAX_DELAY);
		if ( ret == HAL_OK ) {

			if (parse_PMSAQIdata(buf, PM_data)) { // populate data struct from buffer
				return true;
			}
		}
	}
	return false;
}

/**
 * Print the PMS data struct to huart1
 * - (must define huart1 in main)
 */
void print_PMSAQIdata(struct PMS_AQI_data data) {
	uint8_t uart_buf[550];
	int uart_buf_len = 0;

	uart_buf_len = sprintf((char *)uart_buf, "\r\n************* PMS Sensor data **********\r\n");

	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Concentration Units (standard)\r\n");
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "---------------------------------------\r\n");
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "PM 1.0: %d", data.pm10_standard);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "\t\tPM 2.5: %d", data.pm25_standard);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "\t\tPM 10: %d\r\n", data.pm100_standard);

	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Concentration Units (environmental)\r\n");
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "---------------------------------------\r\n");
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "PM 1.0: %d", data.pm10_env);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "\t\tPM 2.5: %d", data.pm25_env);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "\t\tPM 10: %d\r\n", data.pm100_env);

	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "---------------------------------------\r\n");
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Particles > 0.3 um / 0.1L air: %d\r\n", data.particles_03um);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Particles > 0.5 um / 0.1L air: %d\r\n", data.particles_05um);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Particles > 1.0 um / 0.1L air: %d\r\n", data.particles_10um);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Particles > 2.5 um / 0.1L air: %d\r\n", data.particles_25um);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Particles > 5.0 um / 0.1L air: %d\r\n", data.particles_50um);
	uart_buf_len += sprintf((char *)(uart_buf + uart_buf_len), "Particles > 10 um / 0.1L air: %d\r\n", data.particles_100um);

	HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
}

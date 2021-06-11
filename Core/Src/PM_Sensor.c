/*
 * PM_Sensor.c
 *
 *  Created on: Jun 9, 2021
 *      Author: panther
 */

#include "PM_Sensor.h"

// must have this defined in main.c
extern I2C_HandleTypeDef hi2c1;

// for printing/debugging
extern UART_HandleTypeDef huart1;

/**
 * IMPORTANT: Assumes an input buffer length of 32!
 *
 * Returns true if checksum makes sense, false if not
 */
uint8_t static parse_pm_sensor_data(uint8_t *input_buffer_ptr, struct PMS_AQI_data *output_pm_data_ptr) {

	if(input_buffer_ptr[0] != 0x42 || input_buffer_ptr[1] != 0x4d) return 0;

	// Re-arrange bytes to fit exactly in our PMSAQIdata struct
	// - The data comes in big endian, this solves it so it works on all platforms
	uint8_t* bufOffset2 = input_buffer_ptr + 2;
	for (uint8_t i = 0; i < sizeof(PMS_AQI_data) / 2; i++) {
		((uint16_t*)output_pm_data_ptr)[i]  = bufOffset2[i * 2] << 8;
		((uint16_t*)output_pm_data_ptr)[i] += bufOffset2[i * 2 + 1];
	}

	uint16_t sum = 0;
	// get checksum ready
	for (uint8_t i = 0; i < PMS_BUF_LENGTH - 2; i++) {
		sum += input_buffer_ptr[i];
	}

	// does the checksum make sense?
	if (sum != output_pm_data_ptr->checksum) return 0;


	return 1;
}

/**
 * Reads PM sensor and populates input PM data struct
 */
uint8_t read_pm_sensor_data(struct PMS_AQI_data *pm_data_output_ptr) {

	HAL_StatusTypeDef ret;
	uint8_t buf[PMS_BUF_LENGTH];

	// Tell PM sensor that we want to read
	buf[0] = PMS_REG;
	ret = HAL_I2C_Master_Transmit(&hi2c1, PMS_ADDR, buf, 1, 1);

	if(ret != HAL_OK) return 0;

	// Read PMS_BUF_LENGTH bytes (PMS_BUF_LENGTH = 32 is the standard Plantower packet size) from PM sensor
	ret = HAL_I2C_Master_Receive(&hi2c1, PMS_ADDR, buf, PMS_BUF_LENGTH, 1);

	if ( ret != HAL_OK ) return 0;

	return parse_pm_sensor_data(buf, pm_data_output_ptr);

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

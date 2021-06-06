/*
 * utilities.h
 *
 *  Created on: May 30, 2021
 *      Author:
 */

#ifndef SRC_NON_VOLATILE_STORAGE_UTILITIES_H_
#define SRC_NON_VOLATILE_STORAGE_UTILITIES_H_

#include <stdint.h>
#include <stdio.h>
#include "stm32wbxx_hal.h"
#include <time.h>

// handles to variables in main
extern UART_HandleTypeDef huart1;

void print_byte_array_as_hex_to_console(uint8_t *buffer_to_print, uint16_t buffer_to_print_size);

void hex_number_to_byte_array(uint32_t number_to_convert, uint8_t *array_buf, uint8_t length_of_byte_array);

uint32_t byte_array_to_hex(uint8_t *array_buf, uint8_t length_of_byte_array);

void print_formatted_time(time_t time);


#endif /* SRC_NON_VOLATILE_STORAGE_UTILITIES_H_ */

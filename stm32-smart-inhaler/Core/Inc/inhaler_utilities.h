/*
 * inhaler_utilities.h
 *
 *  Created on: Jun 16, 2021
 *      Author: youss
 */

#ifndef CORE_INC_INHALER_UTILITIES_H_
#define CORE_INC_INHALER_UTILITIES_H_

#include "stm32wbxx_hal.h"
#include "app_common.h"
#include <time.h>
#include <stdio.h>
#include <string.h>

#ifndef FALSE
#define FALSE 0
#endif

#ifndef TRUE
#define TRUE !FALSE
#endif

typedef struct{
	int64_t timestamp;      // 8 bytes
} IUE_t;

/**
 * Converts char to its BCD
 */
uint8_t charToInt(char c);

/**
 * Prints an array of bytes give a pointer to the array and its size.
 */
void print_bytes(uint8_t *buffer_to_print,uint16_t buffer_to_print_size);

/**
 * Converts a uint32_t to a Big Endian byte array. Will truncate the LSB if
 * size is smaller than 4.
 *
 * the "be" in the function is for "Big Endian"
 */
void to_be_bytes(uint32_t number_to_convert, uint8_t *array_buf, uint8_t length_of_byte_array);

/**
 * Converts a Big Endian byte array to a uint32_t.
 *
 * The "be" in the function name is for big endian.
 *
 * If size is smaller than 4, the LSB (higher indices) will be ignored.
 *
 * Assumes a size from 0 to 4, inclusive.
 */
uint32_t be_byte_array_to_uint32(uint8_t *array_buf, uint8_t length_of_byte_array);

/**
 * Prints a time stamp.
 */
void print_formatted_time(time_t time);

#endif /* CORE_INC_INHALER_UTILITIES_H_ */

/*
 * utilities.c
 *
 *  Created on: May 29, 2021
 *      Author:
 *
 *      Some general helper functions, like printing a byte array
 */
#include "utilities.h"

void print_byte_array_as_hex_to_console(uint8_t *buffer_to_print,uint16_t buffer_to_print_size) {
	uint8_t uart_buf[120];
	uint16_t out_buf_len = 0;
	int i;
	for (i = 0; i < buffer_to_print_size; i++) {
		out_buf_len += sprintf(uart_buf + out_buf_len, " 0x%X ",
				(unsigned int) buffer_to_print[i]);
	}
	out_buf_len += sprintf(uart_buf + out_buf_len, "\r\n");
	HAL_UART_Transmit(&huart1, uart_buf, out_buf_len, 100);
}

/**
 * Note: this will truncate your number to the length of the byte array parameter
 */
void hex_number_to_byte_array(uint32_t number_to_convert, uint8_t *array_buf,
		uint8_t length_of_byte_array) {
	for (size_t i = 0; i < length_of_byte_array; i++) {
		size_t shift = 8 * (length_of_byte_array - 1 - i);
		array_buf[i] = (number_to_convert >> shift) & 0xff;
	}
}

// Important: max length of byte array is 4! If it is over that, this function returns -1
uint32_t byte_array_to_hex(uint8_t *array_buf, uint8_t length_of_byte_array) {
	// check if length is valid
	if (length_of_byte_array > 4) {
		return -1;
	}

	uint32_t return_hex = 0;
	for (size_t i = 0; i < length_of_byte_array; i++) {
		return_hex *= 256; // move the base up by one byte
		return_hex += array_buf[i];
	}
	return return_hex;
}

void print_formatted_time(time_t time) {
	char print_time[50];
	uint8_t uart_buf[120];
	strftime(print_time, sizeof(print_time), "%Y-%m-%d %H:%M:%S",
			localtime(&time));

	uint16_t out_buf_len = 0;
	out_buf_len += sprintf(uart_buf + out_buf_len,
			"\r\n YY-MM-DD hours:minutes:seconds = %s", print_time);
	HAL_UART_Transmit(&huart1, uart_buf, out_buf_len, 100);
}

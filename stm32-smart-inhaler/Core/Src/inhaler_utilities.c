#include "inhaler_utilities.h"

extern UART_HandleTypeDef huart1;

/**
 * Prints an array of bytes give a pointer to the array and its size.
 */
void print_bytes(uint8_t *buf,uint16_t size) {
	uint8_t uart_buf[5 * size + 5];// Five characters for every byte
													//, one character for a terminating null
													//, and another 4 just to be safe
	uint16_t out_buf_len = 0;
	for (uint8_t i = 0; i < size; i++) {
		out_buf_len += sprintf((char*)(uart_buf + out_buf_len), "0x%X ",
				(unsigned int) buf[i]);
	}

	out_buf_len += sprintf((char*)(uart_buf + out_buf_len), "\r\n");
	HAL_UART_Transmit(&huart1, uart_buf, out_buf_len, 100);
}

/**
 * Converts a uint32_t to a Big Endian byte array. Will truncate the LSB if
 * size is smaller than 4.
 *
 * the "be" in the function is for "Big Endian"
 */
void to_be_bytes(uint32_t number_to_convert, uint8_t *buf, uint8_t size) {
	for (uint8_t i = 0; i < size; i++) {
		size_t shift = 8 * (size - 1 - i);
		buf[i] = (number_to_convert >> shift) & 0xff;
	}
}

/**
 * Converts a Big Endian byte array to a uint32_t.
 *
 * The "be" in the function name is for big endian.
 *
 * If size is smaller than 4, the LSB (higher indices) will be ignored.
 *
 * Assumes a size from 0 to 4, inclusive.
 */
uint32_t be_byte_array_to_uint32(uint8_t *buf, uint8_t size) {

	uint32_t return_hex = 0;
	for (size_t i = 0; i < size; i++) {
		return_hex <<= 8; // move the base up by one byte
		return_hex += buf[i];
	}

	return return_hex;
}

/**
 * Prints a time stamp.
 */
void print_formatted_time(time_t time) {
	char print_time[50];
	uint8_t uart_buf[120];
	strftime(print_time, sizeof(print_time), "%Y-%m-%d %H:%M:%S",
			localtime(&time));

	uint16_t out_buf_len = sprintf((char *)uart_buf,
			"\r\n YY-MM-DD hours:minutes:seconds = %s", print_time);
	HAL_UART_Transmit(&huart1, uart_buf, out_buf_len, 100);
}


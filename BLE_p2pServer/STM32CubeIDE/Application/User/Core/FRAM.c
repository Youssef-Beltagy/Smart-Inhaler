/*
 * SPI1_DataStorage.c
 *
 *  Created on: May 29, 2021
 *      Author:
 */
#include "FRAM.h"

// handles to variables in main
extern SPI_HandleTypeDef hspi1;
extern UART_HandleTypeDef huart1;

// SPI1 Memory helper variables and functions ------------------------------------------------------------------------------

// *** Memory (currently CY15X104Q FRAM) instructions begin ***

// - write enable control -
static const uint8_t FRAM_WREN = 0x06; // set write enable latch

// - register access -
static const uint8_t FRAM_RDSR = 0x05; // read status register

// - memory write -
static const uint8_t FRAM_WRITE = 0x02; // write memory data

// - memory read -
static const uint8_t FRAM_READ = 0x03; // read data memory

// - low power mode commands - (do we need these)
static const uint8_t FRAM_DPD = 0xBA; // enter deep power-down
static const uint8_t FRAM_HBN = 0xB9; // enter hibernate mode

// *** FRAM instructions end ***


#define CS_PIN_NUM GPIO_PIN_4
#define CS_PORT GPIOA

void write_FRAM(uint32_t address,  uint8_t *byte_buffer_to_write, uint32_t size_of_write) {
	// convert hex address into address array buffer
	uint8_t address_buf[ADDRESS_LENGTH_IN_BYTES];
	hex_number_to_byte_array(address, address_buf, ADDRESS_LENGTH_IN_BYTES);

	// enable write enable latch (allow write operations)
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_WREN, 1, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);

	// write to memory
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_WRITE, 1, 100);
	HAL_SPI_Transmit(&hspi1, address_buf, ADDRESS_LENGTH_IN_BYTES, 100);
	HAL_SPI_Transmit(&hspi1, byte_buffer_to_write, size_of_write, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);
}

void read_FRAM(uint32_t address, uint32_t size_of_read, uint8_t *spi_buf) {
	// convert hex address into address array buffer
	uint8_t address_buf[ADDRESS_LENGTH_IN_BYTES];
	hex_number_to_byte_array(address, address_buf, ADDRESS_LENGTH_IN_BYTES);

	// read into spi_buf from SPI1 storage
	// note: doesn't check if the spi_buf has enough room to store the read
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_READ, 1, 100);
	HAL_SPI_Transmit(&hspi1, address_buf, ADDRESS_LENGTH_IN_BYTES, 100);
	HAL_SPI_Receive(&hspi1, spi_buf, size_of_read, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);
}

void print_FRAM_until_address(uint32_t until_address) {
	uint8_t spi_buf[100];
	uint8_t uart_buf[120];

	uint32_t index = 0;
	uint32_t number_of_bytes_per_line = 4;
	for (index; index < until_address; index+= number_of_bytes_per_line) {

		// print address
		uint16_t uart_buf_len = 0;
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> (at address) ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		uint8_t print_addr_array[ADDRESS_LENGTH_IN_BYTES];
		hex_number_to_byte_array(index, print_addr_array, ADDRESS_LENGTH_IN_BYTES);
		print_byte_array_as_hex_to_console(print_addr_array, ADDRESS_LENGTH_IN_BYTES);

		read_FRAM(index, number_of_bytes_per_line, spi_buf);

		// print byte array of memory
		print_byte_array_as_hex_to_console(spi_buf, number_of_bytes_per_line);
	}

}
uint32_t read_stack_size_FRAM() {
	uint8_t spi_buf[50];

	// Read from memory
	read_FRAM(STACK_SIZE_ADDRESS, STACK_SIZE_VARIABLE_LENGTH_BYTES, spi_buf);

	return byte_array_to_hex(spi_buf, STACK_SIZE_VARIABLE_LENGTH_BYTES);
}

void print_stack_size_FRAM() {
	uint8_t uart_buf[120];
	uint32_t read_size = read_stack_size_FRAM();

	int uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size (read from memory is) = %d", read_size);
	HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
}

void write_stack_size_FRAM(uint32_t new_stack_size) {

	// write input stack size to a byte array
	uint8_t new_stack_size_buf[STACK_SIZE_VARIABLE_LENGTH_BYTES];
	hex_number_to_byte_array(new_stack_size, new_stack_size_buf, STACK_SIZE_VARIABLE_LENGTH_BYTES);
	write_FRAM(STACK_SIZE_ADDRESS, new_stack_size_buf, STACK_SIZE_VARIABLE_LENGTH_BYTES);
}

void print_status_register() {
	uint8_t spi_buf[50];
	// read status register
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_RDSR, 1, 100);
	HAL_SPI_Receive(&hspi1, spi_buf, 1, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);


	// print to console for debugging
	print_byte_array_as_hex_to_console(spi_buf, 1);
}

// -------------------------------------------------------------------------------------------------------------------------------

// Stack variables

// 0xFFFFFFFF is an impossible stack size for the CY15B104QN (only has 512K locations), so if it is that value, we know
// that stack_size isn't initialized from a reset/hasn't been read in through FRAM
static uint32_t stack_size = UNINITIALIZED_STACK_SIZE;

/**
 * returns number of entries currently stored in the storage stack
 * - Note - if the stack size is uninitialized from RESET, this reads it in from memory
 */
uint32_t get_num_entries_stored() {
	if (stack_size == UNINITIALIZED_STACK_SIZE) {
		stack_size = read_stack_size_FRAM();
	}
	return stack_size;
}

/**
 *
 */
bool push(struct IUE IUEToAdd) {

	// do we have room within our max address to add this IUE?
	uint32_t new_top_address = get_num_entries_stored(hspi1) * sizeof(struct IUE) + STARTING_ADDRESS;
	if (new_top_address > MAX_ADDRESS) {
		return false;
	}

	// we have room; send it to the SPI data storage
	write_FRAM(new_top_address, (uint8_t*)&IUEToAdd, sizeof(struct IUE));

	// update stack
	stack_size++;
	write_stack_size_FRAM(stack_size);
	return true;
}

/**
 *
 */
struct IUE pop() {
	struct IUE topIUE = peek();

	// update stack size
	stack_size--;
	write_stack_size_FRAM(stack_size);
	return topIUE;
}



/**
 * assumes sending and receiving is the same endian (why wouldn't it be?)
 */
struct IUE peek() {
	if (get_num_entries_stored() == 0) {
		struct IUE empty_IUE;
		empty_IUE.timestamp = -1;// it does not let me return NULL here
		return empty_IUE;
	}
	// initialize top address to read from
	uint32_t stack_top_address = (get_num_entries_stored(hspi1)-1) * sizeof(struct IUE) + STARTING_ADDRESS;

	// read from memory at top address
	struct IUE top_IUE;
	read_FRAM(stack_top_address, sizeof(struct IUE), (uint8_t *)&top_IUE);
	return top_IUE;
}





/*
 * FRAM.c
 *
 *  Created on: May 29, 2021
 *      Author:
 */
#include "FRAM.h"


#define ADDRESS_LENGTH_IN_BYTES 3      // for CY15B104QN
#define FRAM_DATA_SIZE_IN_BYTES 1      // for CY15B104QN
#define MAX_ADDRESS 0x07FFFF           // for CY15B104QN
#define STARTING_ADDRESS 0x000004      // for CY15B104QN

#define STACK_SIZE_ADDRESS 0x000000    // The superblock to store the stack size.
#define STACK_SIZE_VARIABLE_LENGTH_BYTES 4
#define UNINITIALIZED_STACK_SIZE 0xFFFFFFFF // for a stack size that is stale (hasn't been read in from memory since reset)

#define CS_PIN_NUM GPIO_PIN_4
#define CS_PORT GPIOA

//Accesses the variables in main.c
extern SPI_HandleTypeDef hspi1;
extern UART_HandleTypeDef huart1;
//TODO: remove uses of huart1 from here if you have time. Even if it is only for testing.
//TODO: This is not good abstraction of concerns.

//TODO: handler SPI and HAL status codes more appropriately.

// *** Memory (currently CY15X104Q FRAM) instructions begin ***
static const uint8_t FRAM_WREN = 0x06; // set write enable latch
static const uint8_t FRAM_WRDI = 0x04; // reset write enable latch
static const uint8_t FRAM_RDSR = 0x05; // read status register
static const uint8_t FRAM_WRITE = 0x02; // write memory data
static const uint8_t FRAM_READ = 0x03; // read data memory
static const uint8_t FRAM_DPD = 0xBA; // enter deep power-down
static const uint8_t FRAM_HBN = 0xB9; // enter hibernate mode

// *** FRAM instructions end ***
void write_FRAM(uint32_t address,  uint8_t *byte_buffer_to_write, uint32_t size_of_write) {
	// convert hex address into address array buffer
	uint8_t address_buf[ADDRESS_LENGTH_IN_BYTES];
	to_be_bytes(address, address_buf, ADDRESS_LENGTH_IN_BYTES);

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
	to_be_bytes(address, address_buf, ADDRESS_LENGTH_IN_BYTES);

	// read into spi_buf from SPI1 storage
	// note: doesn't check if the spi_buf has enough room to store the read
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_READ, 1, 100);
	HAL_SPI_Transmit(&hspi1, address_buf, ADDRESS_LENGTH_IN_BYTES, 100);
	HAL_SPI_Receive(&hspi1, spi_buf, size_of_read, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);
}

void print_FRAM_until_address(uint32_t until_address) {

	char * uart_buf = "\r\n--> (at address) "; // FIXME: remove this wasteful
	uint32_t number_of_bytes_per_line = 4;

	uint8_t spi_buf[number_of_bytes_per_line];
	for (uint32_t index = 0; index < until_address; index+= number_of_bytes_per_line) {

		HAL_UART_Transmit(&huart1, uart_buf, strlen(uart_buf), 100);
		uint8_t print_addr_array[ADDRESS_LENGTH_IN_BYTES];
		to_be_bytes(index, print_addr_array, ADDRESS_LENGTH_IN_BYTES);
		print_bytes(print_addr_array, ADDRESS_LENGTH_IN_BYTES);

		read_FRAM(index, number_of_bytes_per_line, spi_buf);

		// print byte array of memory
		print_bytes(spi_buf, number_of_bytes_per_line);
	}

}

uint32_t read_stack_size_FRAM() {
	uint8_t spi_buf[STACK_SIZE_VARIABLE_LENGTH_BYTES];

	// Read from memory
	read_FRAM(STACK_SIZE_ADDRESS, STACK_SIZE_VARIABLE_LENGTH_BYTES, spi_buf);

	return be_byte_array_to_uint32(spi_buf, STACK_SIZE_VARIABLE_LENGTH_BYTES); // FIXME: This is not a good idea
}

void print_stack_size_FRAM() {
	uint8_t uart_buf[120];
	uint32_t read_size = read_stack_size_FRAM();

	int uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size (read from memory is) = %ud", read_size);
	HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
}

void write_stack_size_FRAM(uint32_t new_stack_size) {

	// write input stack size to a byte array
	uint8_t new_stack_size_buf[STACK_SIZE_VARIABLE_LENGTH_BYTES];
	to_be_bytes(new_stack_size, new_stack_size_buf, STACK_SIZE_VARIABLE_LENGTH_BYTES);
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
	print_bytes(spi_buf, 1);
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


void test_fram(){

	uint8_t uart_buf[120];
		int uart_buf_len;
		uint8_t spi_buf[120];


	uart_buf_len = sprintf((char *)uart_buf, "\r\n******************************* SPI Storage & Utility Tests ******************************");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		// test variables
		uint32_t until_address = 0x000020;

		uint8_t test_addr_buff[ADDRESS_LENGTH_IN_BYTES];
		uint8_t test_addr = 0x56;

		uint8_t test_buf[] = {0xA1, 0xB2, 0xC3, 0xD4};
		uint8_t test_buf_length = 4;

		// test utilities.c function print_byte_array_as_hex_to_console()
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Testing function print_byte_array_as_hex_to_console()");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);


		print_bytes(test_buf, test_buf_length);

		// test  byte_array_to_hex()
		uint32_t test_array_to_hex;
		test_array_to_hex = be_byte_array_to_uint32(test_buf, test_buf_length);

		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Testing function byte_array_to_hex() conversion of -> ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_bytes(test_buf, test_buf_length);
		uart_buf_len = sprintf((char *)uart_buf, "\r\n = %X in hex", (unsigned int)test_array_to_hex);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);


		uart_buf_len = sprintf((char *)uart_buf, "\r\n---------------------------- Testing FRAM Utility Functions ------------------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Dumping memory until address : %X, non-inclusive", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);

		uart_buf_len = sprintf((char *)uart_buf, "\r\n------------------------------------------------------------------------------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		// print out status register
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Status Register is ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_status_register();


		// test write
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> DO NOT USE - DEBUG ONLY! - Test writing : ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		uint8_t test_write_buf[4] = {0x44, 0x44, 0x44, 0x44};
		uint8_t test_write_buff_length = 4;
		print_bytes(test_write_buf, test_write_buff_length);


		to_be_bytes(test_addr, test_addr_buff, ADDRESS_LENGTH_IN_BYTES); // test write to the very beginning of memory - for debug only!

		uart_buf_len = sprintf((char *)uart_buf, "\r\n, writing to address : ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_bytes(test_addr_buff, ADDRESS_LENGTH_IN_BYTES);

		write_FRAM(test_addr, test_write_buf, test_write_buff_length);

		// print out status register
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Status Register is ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_status_register();

		uart_buf_len = sprintf((char *)uart_buf, "\r\n------------------------------------------------------------------------------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		// test reading
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Test reading ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		uart_buf_len = sprintf((char *)uart_buf, "\r\n, reading from address : ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_bytes(test_addr_buff, ADDRESS_LENGTH_IN_BYTES);

		uint8_t test_read_size = 4;
		read_FRAM(test_addr, test_read_size, spi_buf);
		print_bytes(spi_buf, test_read_size);


		uart_buf_len = sprintf((char *)uart_buf, "\r\n-------------------- Testing SPI1_DataStorage / stack functionality ----------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		uint32_t num_entries_in_stack = get_num_entries_stored();
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size using get_num_entries_stored() = %d", (unsigned int)num_entries_in_stack);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_stack_size_FRAM(); // check stack size read from memory against get_num_entries()

		uint8_t num_pushes = 6;
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Testing push() for %d entries", num_pushes);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		int i = 0;
		for (i; i < num_pushes; i++) {
			struct IUE push_IUE;
			push_IUE.timestamp = i*10000 + 11; // set some placeholder time stamps
			push(push_IUE);

			uart_buf_len = sprintf((char *)uart_buf, "\r\n\n	- pushed ");
			HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
			print_formatted_time(push_IUE.timestamp);
		}

		num_entries_in_stack = get_num_entries_stored();
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size using get_num_entries_stored() = %d", (unsigned int)num_entries_in_stack);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_stack_size_FRAM();

		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n\n--> Dumping memory until address : %X, non-inclusive", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);


		uart_buf_len = sprintf((char *)uart_buf, "\r\n\n--> Testing pop() until stack is empty");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);


		while (get_num_entries_stored() > 0) {
			struct IUE popped_IUE = pop(); // this tests peek too, because pop() uses peek()

			uart_buf_len = sprintf((char *)uart_buf, "\r\n\n	- popped ");
			HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
			print_formatted_time(popped_IUE.timestamp);
		}

		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n\n--> Dumping memory until address : %X, non-inclusive", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);


		num_entries_in_stack = get_num_entries_stored();
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size using get_num_entries_stored() = %d", (unsigned int)num_entries_in_stack);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_stack_size_FRAM();


}




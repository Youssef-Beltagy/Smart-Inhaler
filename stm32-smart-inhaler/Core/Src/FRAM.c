#include "fram.h"

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

//TODO: handle SPI and HAL status codes more appropriately. For example, return an error code on failure.
//TODO: this needs to be carefully thought through.

// *** Memory (currently CY15X104Q FRAM) instructions begin ***
static const uint8_t FRAM_WREN = 0x06; // set write enable latch
//static const uint8_t FRAM_WRDI = 0x04; // reset write enable latch
static const uint8_t FRAM_RDSR = 0x05; // read status register
static const uint8_t FRAM_WRITE = 0x02; // write memory data
static const uint8_t FRAM_READ = 0x03; // read data memory
//static const uint8_t FRAM_DPD = 0xBA; // enter deep power-down
static const uint8_t FRAM_HBN = 0xB9; // enter hibernate mode

// *** FRAM instructions end ***
static void write_FRAM(uint32_t address,  uint8_t *input_buf, uint32_t write_size) {
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
	HAL_SPI_Transmit(&hspi1, input_buf, write_size, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);
}

static void read_FRAM(uint32_t address, uint32_t read_size, uint8_t *out_buf) {
	// convert hex address into address array buffer
	uint8_t address_buf[ADDRESS_LENGTH_IN_BYTES];
	to_be_bytes(address, address_buf, ADDRESS_LENGTH_IN_BYTES);

	// read into out_buf from SPI1 storage
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_READ, 1, 100);
	HAL_SPI_Transmit(&hspi1, address_buf, ADDRESS_LENGTH_IN_BYTES, 100);
	HAL_SPI_Receive(&hspi1, out_buf, read_size, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);
}

void print_FRAM_until_address(uint32_t until_address) {

	char * uart_buf = "(at address): ";

	uint32_t number_of_bytes_per_line = 4;
	uint8_t spi_buf[number_of_bytes_per_line];
	uint8_t print_addr_array[ADDRESS_LENGTH_IN_BYTES];

	for (uint32_t index = 0; index < until_address; index+= number_of_bytes_per_line) {

		HAL_UART_Transmit(&huart1, (uint8_t*) uart_buf, (uint16_t) strlen(uart_buf), 100);
		to_be_bytes(index, print_addr_array, ADDRESS_LENGTH_IN_BYTES);
		print_bytes(print_addr_array, ADDRESS_LENGTH_IN_BYTES);

		read_FRAM(index, number_of_bytes_per_line, spi_buf);

		// print byte array of memory
		print_bytes(spi_buf, number_of_bytes_per_line);
	}

}

uint32_t read_stack_size_FRAM() {
	uint32_t stack_size = 0;

	// Read from memory
	read_FRAM(STACK_SIZE_ADDRESS, STACK_SIZE_VARIABLE_LENGTH_BYTES, (uint8_t*) &stack_size);

	return stack_size;
}

void write_stack_size_FRAM(uint32_t new_stack_size) {

	// write input stack size to a byte array
	write_FRAM(STACK_SIZE_ADDRESS, (uint8_t*)&new_stack_size, STACK_SIZE_VARIABLE_LENGTH_BYTES);
}

void print_stack_size_FRAM() {
	uint32_t stack_size = read_stack_size_FRAM();

	uint8_t uart_buf[60];
	int uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size (read from memory is) = %ld", stack_size);
	HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
}

void print_status_register() {
	uint8_t spi_buf;
	// read status register
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_RDSR, 1, 100);
	HAL_SPI_Receive(&hspi1, &spi_buf, 1, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);

	print_bytes(&spi_buf, 1);
}

// 0xFFFFFFFF is an impossible stack size for the CY15B104QN (only has 512K locations), so if it is that value, we know
// that stack_size isn't initialized.
static uint32_t stack_size = UNINITIALIZED_STACK_SIZE;

/**
 * returns number of entries currently stored in the storage stack
 * - Note - if the stack size is uninitialized from a power cycle, this reads it in from FRAM.
 * If the stack size is initialized, it is read from memory.
 *
 * Be aware the stack_size is only unintialized if the MCU loses power and restarts.
 * The stack size will remain initialized across resets.
 */
uint32_t get_stack_size() {

	/*
	 * This function must be called before any direct accesses to the stack_size.
	 */

	if (stack_size == UNINITIALIZED_STACK_SIZE) {
		stack_size = read_stack_size_FRAM();
	}

	return stack_size;
}


uint8_t push(IUE_t IUEToAdd) {

	// do we have room within our max address to add this IUE?
	uint32_t new_top_address = get_stack_size() * sizeof(IUE_t) + STARTING_ADDRESS;
	if ((new_top_address + sizeof(IUE_t) - 1) > MAX_ADDRESS) {
		return FALSE;
	}

	// we have room; send it to the SPI data storage
	write_FRAM(new_top_address, (uint8_t*)&IUEToAdd, sizeof(IUE_t));

	// update stack
	stack_size++;
	write_stack_size_FRAM(stack_size);
	return TRUE;
}


IUE_t pop() {
	IUE_t topIUE = peek();

	// update stack size
	stack_size--;
	write_stack_size_FRAM(stack_size);
	return topIUE;
}


IUE_t peek() {
	if (get_stack_size() == 0) {
		IUE_t empty_IUE = {0};
		return empty_IUE;
	}

	// initialize top address to read from
	uint32_t stack_top_address = (get_stack_size()-1) * sizeof(IUE_t) + STARTING_ADDRESS;

	// read from memory at top address
	IUE_t top_IUE;
	read_FRAM(stack_top_address, sizeof(IUE_t), (uint8_t *)&top_IUE);
	return top_IUE;
}

/**
 * Clears the fram stack. Clears the fram if clean == TRUE. Otherwise, just trims the data.
 *
 */
uint8_t clear(uint8_t clean){

	/**
	 * Always returns true for now. In the future, write_FRAM should return a status code.
	 *
	 * clear would return the logical AND of all those status codes.
	 */

	write_stack_size_FRAM(0);

	if(!clean) return TRUE;

	uint32_t clean_size = MAX_ADDRESS + 1;

	uint8_t clean_block[16] = {0};// clean 16 bytes at a time.

	for(uint32_t cleaned = 0; cleaned < clean_size; cleaned += sizeof(clean_block)){
		write_FRAM(STACK_SIZE_ADDRESS + cleaned, clean_block, 16);
	}

	return TRUE;

}

/**
 * Puts the FRAM in hibernate mode to save energy.
 */
uint8_t hibernate_fram(){

	uint8_t return_code;
	// Send the hibernate op-code
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	return_code = HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_HBN, 1, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);

	return return_code;
}

/**
 * Wakes the fram from the hibernate mode.
 */
uint8_t wakeup_fram(){

	uint8_t return_code = TRUE;

	//Send dummy data twice to wake up the FRAM.
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	return_code = return_code && HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_RDSR, 1, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);

	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_RESET);
	return_code = return_code && HAL_SPI_Transmit(&hspi1, (uint8_t *)&FRAM_RDSR, 1, 100);
	HAL_GPIO_WritePin(CS_PORT, CS_PIN_NUM, GPIO_PIN_SET);

	//Actually, one time should be enough (this was tested), but this is just to be safe.

	return return_code;

}


void test_fram(){

	clear(TRUE);

	uint8_t uart_buf[120];
	int uart_buf_len;
	uint8_t spi_buf[120];


	uart_buf_len = sprintf((char *)uart_buf, "\r\n******************************* SPI Storage & Utility Tests ******************************");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		// test variables
		uint32_t until_address = 0x000020;

		uint8_t test_addr_buff[ADDRESS_LENGTH_IN_BYTES];
		uint8_t test_addr = 0x04;

		uint8_t test_buf[] = {0xA1, 0xB2, 0xC3, 0xD4};
		uint8_t test_buf_length = 4;

		// test utilities.c function print_byte_array_as_hex_to_console()
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Testing function print_bytes()");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);


		print_bytes(test_buf, test_buf_length);

		// test  byte_array_to_hex()
		uint32_t test_array_to_hex;
		test_array_to_hex = be_byte_array_to_uint32(test_buf, test_buf_length);

		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Testing function be_byte_array_to_uint32() conversion of -> ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_bytes(test_buf, test_buf_length);
		uart_buf_len = sprintf((char *)uart_buf, "\r\n = %X in hex", (unsigned int)test_array_to_hex);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);


		uart_buf_len = sprintf((char *)uart_buf, "\r\n---------------------------- Testing FRAM Utility Functions ------------------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Dumping memory until address : %X", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);

		uart_buf_len = sprintf((char *)uart_buf, "\r\n------------------------------------------------------------------------------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		hibernate_fram();
		HAL_Delay(100);
		wakeup_fram();

		// print out status register
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Status Register is ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_status_register();


		// test write
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> DO NOT USE - DEBUG ONLY! - Test writing : ");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		uint8_t test_write_buf[4] = {0x11, 0x22, 0x33, 0x44};
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


		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n------------------------------------------------------------------------------------------");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Dumping memory until address : %X", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);

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

		uint32_t num_entries_in_stack = get_stack_size();
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size using get_num_entries_stored() = %d", (unsigned int)num_entries_in_stack);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_stack_size_FRAM(); // check stack size read from memory against get_num_entries()

		uint8_t num_pushes = 6;
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Testing push() for %d entries", num_pushes);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		for (int i = 0; i < num_pushes; i++) {
			IUE_t push_IUE;
			push_IUE.timestamp = i*10000 + 11; // set some placeholder time stamps
			push(push_IUE);

			uart_buf_len = sprintf((char *)uart_buf, "\r\n\n	- pushed ");
			HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
			print_formatted_time(push_IUE.timestamp);
		}

		num_entries_in_stack = get_stack_size();
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size using get_num_entries_stored() = %d", (unsigned int)num_entries_in_stack);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_stack_size_FRAM();

		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n\n--> Dumping memory until address : %X, non-inclusive", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);


		uart_buf_len = sprintf((char *)uart_buf, "\r\n\n--> Testing pop() until stack is empty");
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);


		while (get_stack_size() > 0) {
			IUE_t popped_IUE = pop(); // this tests peek too, because pop() uses peek()

			uart_buf_len = sprintf((char *)uart_buf, "\r\n\n	- popped ");
			HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
			print_formatted_time(popped_IUE.timestamp);
		}

		// dump memory
		uart_buf_len = sprintf((char *)uart_buf, "\r\n\n--> Dumping memory until address : %X, non-inclusive", (unsigned int)until_address);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);
		print_FRAM_until_address(until_address);


		num_entries_in_stack = get_stack_size();
		uart_buf_len = sprintf((char *)uart_buf, "\r\n--> Stack size using get_num_entries_stored() = %d", (unsigned int)num_entries_in_stack);
		HAL_UART_Transmit(&huart1, uart_buf,uart_buf_len, 100);

		print_stack_size_FRAM();

		clear(TRUE);


}




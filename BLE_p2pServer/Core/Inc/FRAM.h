/*
 * TimestampStorage.h
 *
 *  Created on: May 28, 2021
 *      Author:
 */

#ifndef SRC_NON_VOLATILE_STORAGE_FRAM_H_
#define SRC_NON_VOLATILE_STORAGE_FRAM_H_


#include <stdint.h>
#include <stdbool.h>
#include <string.h>
#include "stm32wbxx_hal.h"

#include "utilities.h"

#define ADDRESS_LENGTH_IN_BYTES 3      // for CY15B104QN
#define FRAM_DATA_SIZE_IN_BYTES 1      // for CY15B104QN
#define MAX_ADDRESS 0x07FFFF           // for CY15B104QN
#define STARTING_ADDRESS 0x000004      // for CY15B104QN

#define STACK_SIZE_ADDRESS 0x000000    // for CY15B104QN
#define STACK_SIZE_VARIABLE_LENGTH_BYTES 4
#define UNINITIALIZED_STACK_SIZE 0xFFFFFFFF // for a stack size that is stale (hasn't been read in from memory since reset)
/**
 * Interface for interaction with SPI non-volatile data storage
 * - uses a stack internally
 */

struct IUE {
	uint32_t timestamp;
};




/**
 * returns number of entries currently stored in the storage stack
 */
uint32_t get_num_entries_stored();

/**
 *
 */
bool push(struct IUE IUEToAdd);

/**
 *
 */
struct IUE pop();

/**
 *
 */
struct IUE peek();


// ***** FRAM debug / utility functions *****

void print_status_register();

uint32_t read_stack_size_FRAM();

void print_stack_size_FRAM();

void print_FRAM_until_address(uint32_t until_address);

void read_FRAM(uint32_t address, uint32_t size_of_read, uint8_t *spi_buf);


#endif /* SRC_NON_VOLATILE_STORAGE_FRAM_H_ */

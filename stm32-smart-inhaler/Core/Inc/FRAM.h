/**
 * Interface for interaction with SPI1 non-volatile data storage
 * - uses a stack internally
 */

#ifndef _FRAM_H_
#define _FRAM_H_

#include <stdio.h>
#include "stm32wbxx_hal.h"
#include <time.h>
#include <string.h>
#include "inhaler_utilities.h"

//TODO: Add clear FRAM and an init function.

/**
 * FRAM tests
 */
void test_fram();

/**
 * returns number of entries currently stored in the storage stack
 */
uint32_t get_num_entries_stored();

/**
 * Pushes an IUE to the stack
 */
uint8_t push(IUE_t IUEToAdd);

/**
 * Pops and returns the IUE at the top of the stack.
 */
IUE_t pop();

/**
 * Returns the IUE at the top of the stack.
 */
IUE_t peek();

#endif /* _FRAM_H_ */

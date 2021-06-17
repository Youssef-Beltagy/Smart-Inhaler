/**
 * Interface for interaction with SPI1 non-volatile data storage
 * - uses a stack internally
 */

#ifndef _FRAM_H_
#define _FRAM_H_

#include "inhaler_utilities.h"

/**
 * FRAM tests
 */
void test_fram();

/**
 * returns number of entries currently stored in the storage stack
 */
uint32_t get_stack_size();

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

/**
 * Clears the fram stack. Clears the fram if clean == TRUE. Otherwise, just trims the data.
 *
 */
uint8_t clear(uint8_t clean);

/**
 * Initializes the fram.
 */
uint8_t init_fram();

/**
 * Puts the FRAM in hibernate mode to save energy.
 */
uint8_t hibernate_fram();

/**
 * Wakes the fram from the hibernate mode.
 */
uint8_t wakeup_fram();

#endif /* _FRAM_H_ */

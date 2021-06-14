/**
 * pvd.c
 *
 * Initializes the programmable voltage detector (pvd) and handles its logic.
 *
 * The pvd is used to monitor the status of the battery
 *
 */

#include "pvd.h"

/**
 * Represents the status of the battery.
 */
static volatile uint8_t battery_flag;

/**
  * @brief  Configures the PVD resources.
  * @param  None
  * @retval None
  */
void init_pvd(void) {

	battery_flag = BATTERY_FULL;

	PWR_PVDTypeDef sConfigPVD;
	//Configure the PVD Level
	sConfigPVD.PVDLevel = BATTERY_UPPER_THRESHOLD;
	sConfigPVD.Mode = PVD_EDGE_MODE;

	HAL_PWR_ConfigPVD(&sConfigPVD);
	/* Enable the PVD Output */
	HAL_PWR_EnablePVD();

	/* Configure the NVIC for PVD*/
	HAL_NVIC_SetPriority(PVD_PVM_IRQn, PVD_INTERRUPT_PRIORITY, PVD_INTERRUPT_PRIORITY);
	HAL_NVIC_EnableIRQ(PVD_PVM_IRQn);


}


/**
  * @brief  PWR PVD interrupt callback
  * @param  none
  * @retval none
  */
void HAL_PWR_PVDCallback(void) {
	if (battery_flag == BATTERY_FULL) { // First interrupt

		battery_flag = BATTERY_ALMOST_DEAD;

		HAL_PWR_DisablePVD();

		PWR_PVDTypeDef sConfigPVD;
		sConfigPVD.PVDLevel = BATTERY_LOWER_THRESHOLD;
		sConfigPVD.Mode = PVD_EDGE_MODE;
		HAL_PWR_ConfigPVD(&sConfigPVD);

		// Strangely, it is necessary to disable and reenable the PVD.
		HAL_PWR_EnablePVD();

	}else{ // Second and subsequent interrupts (though it should not be possible to get more than two). 

		if(battery_flag == BATTERY_ALMOST_DEAD){
			battery_flag = BATTERY_DEAD;
		}

		HAL_NVIC_DisableIRQ(PVD_PVM_IRQn);
		HAL_PWR_DisablePVD();

	}
}

/**
  * @brief  get battery level
  * @retval returns the value of the PVD flag
  */
uint8_t get_battery_status() {
	uint8_t copy;

	UTILS_ENTER_CRITICAL_SECTION( );
	copy = battery_flag;
	UTILS_EXIT_CRITICAL_SECTION( );

	return copy;
}

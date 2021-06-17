
#ifndef __WEARABLE_BLE_H
#define __WEARABLE_BLE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>

#ifndef FALSE
#define FALSE 0
#define TRUE 1 // TODO: centralize all the TRUE/FALSE definitions
#endif

void Wearable_Sensor_Init(void);

void Wearable_On_Connect(void);

void Wearable_On_Disconnect(void);

uint8_t Get_Wearable_Service_UUID(uint8_t* uuidPtr);

#ifdef __cplusplus
}
#endif

#endif /*__WEARABLE_BLE_H */


################################################################################
# Automatically-generated file. Do not edit!
# Toolchain: GNU Tools for STM32 (9-2020-q2-update)
################################################################################

# Add inputs and outputs from these tool invocations to the build variables 
C_SRCS += \
C:/STM32Cube_FW_WB_V1.11.0/Projects/P-NUCLEO-WB55.Nucleo/Drivers/BSP/P-NUCLEO-WB55.Nucleo/stm32wbxx_nucleo.c 

OBJS += \
./Drivers/BSP/P-NUCLEO-WB55.Nucleo/stm32wbxx_nucleo.o 

C_DEPS += \
./Drivers/BSP/P-NUCLEO-WB55.Nucleo/stm32wbxx_nucleo.d 


# Each subdirectory must supply rules for building sources it contributes
Drivers/BSP/P-NUCLEO-WB55.Nucleo/stm32wbxx_nucleo.o: C:/STM32Cube_FW_WB_V1.11.0/Projects/P-NUCLEO-WB55.Nucleo/Drivers/BSP/P-NUCLEO-WB55.Nucleo/stm32wbxx_nucleo.c Drivers/BSP/P-NUCLEO-WB55.Nucleo/subdir.mk
	arm-none-eabi-gcc "$<" -mcpu=cortex-m4 -std=gnu11 -g3 -DDEBUG -DUSE_STM32WBXX_NUCLEO -DUSE_HAL_DRIVER -DSTM32WB55xx -c -I../Core/Inc -I../STM32_WPAN/App -I../../../../../../../../Drivers/STM32WBxx_HAL_Driver/Inc -I../../../../../../../../Drivers/STM32WBxx_HAL_Driver/Inc/Legacy -I../../../../../../../../Utilities/lpm/tiny_lpm -I../../../../../../../../Middlewares/ST/STM32_WPAN -I../../../../../../../../Middlewares/ST/STM32_WPAN/interface/patterns/ble_thread -I../../../../../../../../Middlewares/ST/STM32_WPAN/interface/patterns/ble_thread/tl -I../../../../../../../../Middlewares/ST/STM32_WPAN/interface/patterns/ble_thread/shci -I../../../../../../../../Middlewares/ST/STM32_WPAN/utilities -I../../../../../../../../Middlewares/ST/STM32_WPAN/ble/core -I../../../../../../../../Middlewares/ST/STM32_WPAN/ble/core/auto -I../../../../../../../../Middlewares/ST/STM32_WPAN/ble/core/template -I../../../../../../../../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../../../../../../../../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../../../../../../../../Drivers/CMSIS/Device/ST/STM32WBxx/Include -I../../../../../../../../Utilities/sequencer -I../../../../../../../../Middlewares/ST/STM32_WPAN/ble -I../../../../../../../../Drivers/CMSIS/Include -I../../../../../../Drivers/BSP/P-NUCLEO-WB55.Nucleo -I../../../../../../../../../Drivers/STM32WBxx_HAL_Driver/Inc -I../../../../../../../../../Drivers/STM32WBxx_HAL_Driver/Inc/Legacy -I../../../../../../../../../Utilities/lpm/tiny_lpm -I../../../../../../../../../Middlewares/ST/STM32_WPAN -I../../../../../../../../../Middlewares/ST/STM32_WPAN/interface/patterns/ble_thread -I../../../../../../../../../Middlewares/ST/STM32_WPAN/interface/patterns/ble_thread/tl -I../../../../../../../../../Middlewares/ST/STM32_WPAN/interface/patterns/ble_thread/shci -I../../../../../../../../../Middlewares/ST/STM32_WPAN/utilities -I../../../../../../../../../Middlewares/ST/STM32_WPAN/ble/core -I../../../../../../../../../Middlewares/ST/STM32_WPAN/ble/core/auto -I../../../../../../../../../Middlewares/ST/STM32_WPAN/ble/core/template -I../../../../../../../../../Middlewares/ST/STM32_WPAN/ble/svc/Inc -I../../../../../../../../../Middlewares/ST/STM32_WPAN/ble/svc/Src -I../../../../../../../../../Drivers/CMSIS/Device/ST/STM32WBxx/Include -I../../../../../../../../../Utilities/sequencer -I../../../../../../../../../Middlewares/ST/STM32_WPAN/ble -I../../../../../../../../../Drivers/CMSIS/Include -I../../../../../../../Drivers/BSP/P-NUCLEO-WB55.Nucleo -O0 -ffunction-sections -fdata-sections -Wall -fstack-usage -MMD -MP -MF"Drivers/BSP/P-NUCLEO-WB55.Nucleo/stm32wbxx_nucleo.d" -MT"$@" --specs=nano.specs -mfpu=fpv4-sp-d16 -mfloat-abi=hard -mthumb -o "$@"

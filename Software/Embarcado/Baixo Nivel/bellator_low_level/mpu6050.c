/*
 * mpu6050.c
 *
 *  Created on: Mar 21, 2013
 *      Author: telmo
 *
 *  This code was based on Jeff Rowberg code for arduino https://github.com/jrowberg/i2cdevlib/tree/master/Arduino/MPU6050
 */
#include "lpc2103.h"
#include "i2c.h"
#include "logger.h"
#include "mpu6050.h"

/** Power on and prepare for general usage.
* This will activate the device and take it out of sleep mode (which must be done
* after start-up). This function also sets both the accelerometer and the gyroscope
* to their most sensitive settings, namely +/- 2g and +/- 250 degrees/sec, and sets
* the clock source to use the X Gyro for reference, which is slightly better than
* the default internal clock source.
*/
void mpu_init(void) {
	log_string_mpu(">> mpu_init\n");

    // configure clock source
	mpu_set_clock_source(MPU6050_CLOCK_PLL_XGYRO);
	// disable temperature sensor
	mpu_set_temperature_sensor_enabled(0);
	// set scale to
	// acc ±2g 16384 LSB/g
	// gyro ± 250 °/s 131 LSB/°/s
    mpu_set_full_scale_gyro_range(MPU6050_GYRO_FS_250);
    mpu_set_full_scale_accel_range(MPU6050_ACCEL_FS_2);

    // divide gyro output rate by 7+1, if DLPF>0 set back to 0 to keep output rate 1kHz
    mpu_set_gyro_rate(7);
    // set digital low pass filter cut off frequency (disabled)
    mpu_set_DLPF_mode(0);

    // FIFO WAS NOT USED
    // enable FIFO
    mpu_set_6axis_FIFO_enabled(1);

    // DATAREADY INTERRUPT WAS NOT USED
    // configure interruption
    //mpu_set_interrupt_mode(0); // active high
    //mpu_set_interrupt_drive(0); // push/pull
    //mpu_set_interrupt_latch(0); // 50us pulse on interrupt
    //mpu_set_FIFO_overflow_interrupt(1); // generate interrupt on FIFO overflow
    //mpu_set_data_ready_interrupt(1); // data ready interrupt

    // clear interrupts
	//char source;
	//mpu_clear_interrupt(&source);
    // reset FIFO
    mpu_reset_FIFO();
    // enable fifo
    mpu_set_FIFO_enabled(1);

    // stop sleeping
    mpu_set_sleep_enable(0);

	log_string_mpu("<< mpu_init\n");
}

/** Set clock source setting.
* An internal 8MHz oscillator, gyroscope based clock, or external sources can
* be selected as the MPU-60X0 clock source. When the internal 8 MHz oscillator
* or an external source is chosen as the clock source, the MPU-60X0 can operate
* in low power modes with the gyroscopes disabled.
*
* Upon power up, the MPU-60X0 clock source defaults to the internal oscillator.
* However, it is highly recommended that the device be configured to use one of
* the gyroscopes (or an external clock source) as the clock reference for
* improved stability. The clock source can be selected according to the following table:
*
* <pre>
* CLK_SEL | Clock Source
* --------+--------------------------------------
* 0 | Internal oscillator
* 1 | PLL with X Gyro reference
* 2 | PLL with Y Gyro reference
* 3 | PLL with Z Gyro reference
* 4 | PLL with external 32.768kHz reference
* 5 | PLL with external 19.2MHz reference
* 6 | Reserved
* 7 | Stops the clock and keeps the timing generator in reset
* </pre>
*
* @param source New clock source setting
* @see getClockSource()
* @see MPU6050_RA_PWR_MGMT_1
* @see MPU6050_PWR1_CLKSEL_BIT
* @see MPU6050_PWR1_CLKSEL_LENGTH
*/
void mpu_set_clock_source(char source) {
    i2c_write_bits(MPU6050_RA_PWR_MGMT_1, MPU6050_PWR1_CLKSEL_BIT, MPU6050_PWR1_CLKSEL_LENGTH, source);
}

/** Set full-scale gyroscope range.
* @param range New full-scale gyroscope range value
* @see getFullScaleRange()
* @see MPU6050_GYRO_FS_250
* @see MPU6050_RA_GYRO_CONFIG
* @see MPU6050_GCONFIG_FS_SEL_BIT
* @see MPU6050_GCONFIG_FS_SEL_LENGTH
*/
void mpu_set_full_scale_gyro_range(char range) {
	i2c_write_bits(MPU6050_RA_GYRO_CONFIG, MPU6050_GCONFIG_FS_SEL_BIT, MPU6050_GCONFIG_FS_SEL_LENGTH, range);
}

/** Set full-scale accelerometer range.
* @param range New full-scale accelerometer range setting
* @see getFullScaleAccelRange()
*/
void mpu_set_full_scale_accel_range(char range) {
	i2c_write_bits(MPU6050_RA_ACCEL_CONFIG, MPU6050_ACONFIG_AFS_SEL_BIT, MPU6050_ACONFIG_AFS_SEL_LENGTH, range);
}

/** Set sleep mode status.
* @param enabled New sleep mode enabled status
* @see getSleepEnabled()
* @see MPU6050_RA_PWR_MGMT_1
* @see MPU6050_PWR1_SLEEP_BIT
*/
void mpu_set_sleep_enable(int enable) {
	i2c_write_bits(MPU6050_RA_PWR_MGMT_1, MPU6050_PWR1_SLEEP_BIT, 1, enable);
}

/**
 * Set temperature sensor enabled status.
 * Note: this register stores the *disabled* value, but for consistency with the
 * rest of the code, the function is named and used with standard true/false
 * values to indicate whether the sensor is enabled or disabled, respectively.
 * This bit resets the FIFO buffer when set to 1 while FIFO_EN equals 0. This
 * bit automatically clears to 0 after the reset has been triggered.
 * @param enabled 1 enabled; 0 disabled
 */
void mpu_set_temperature_sensor_enabled(char enabled) {
	i2c_write_bits(MPU6050_RA_PWR_MGMT_1, MPU6050_PWR1_TEMP_DIS_BIT, 1, 1-enabled);
}

/** Set gyroscope output rate divider.
* The sensor register output, FIFO output, DMP sampling, Motion detection, Zero
* Motion detection, and Free Fall detection are all based on the Sample Rate.
* The Sample Rate is generated by dividing the gyroscope output rate by
* SMPLRT_DIV:
*
* Sample Rate = Gyroscope Output Rate / (1 + SMPLRT_DIV)
*
* where Gyroscope Output Rate = 8kHz when the DLPF is disabled (DLPF_CFG = 0 or
* 7), and 1kHz when the DLPF is enabled (see Register 26).
*
* Note: The accelerometer output rate is 1kHz. This means that for a Sample
* Rate greater than 1kHz, the same accelerometer sample may be output to the
* FIFO, DMP, and sensor registers more than once.
*
* For a diagram of the gyroscope and accelerometer signal paths, see Section 8
* of the MPU-6000/MPU-6050 Product Specification document.
*
* @param rate New sample rate divider
* @see getRate()
* @see MPU6050_RA_SMPLRT_DIV
*/
void mpu_set_gyro_rate(char rate) {
    i2c_write_byte(MPU6050_RA_SMPLRT_DIV, rate);
}

/** Get digital low-pass filter configuration.
* The DLPF_CFG parameter sets the digital low pass filter configuration. It
* also determines the internal sampling rate used by the device as shown in
* the table below.
*
* Note: The accelerometer output rate is 1kHz. This means that for a Sample
* Rate greater than 1kHz, the same accelerometer sample may be output to the
* FIFO, DMP, and sensor registers more than once.
*
* <pre>
* | ACCELEROMETER | GYROSCOPE
* DLPF_CFG | Bandwidth | Delay | Bandwidth | Delay | Sample Rate
* ---------+-----------+--------+-----------+--------+-------------
* 0 | 260Hz | 0ms | 256Hz | 0.98ms | 8kHz
* 1 | 184Hz | 2.0ms | 188Hz | 1.9ms | 1kHz
* 2 | 94Hz | 3.0ms | 98Hz | 2.8ms | 1kHz
* 3 | 44Hz | 4.9ms | 42Hz | 4.8ms | 1kHz
* 4 | 21Hz | 8.5ms | 20Hz | 8.3ms | 1kHz
* 5 | 10Hz | 13.8ms | 10Hz | 13.4ms | 1kHz
* 6 | 5Hz | 19.0ms | 5Hz | 18.6ms | 1kHz
* 7 | -- Reserved -- | -- Reserved -- | Reserved
* </pre>
*
* @return DLFP configuration
* @see MPU6050_RA_CONFIG
* @see MPU6050_CFG_DLPF_CFG_BIT
* @see MPU6050_CFG_DLPF_CFG_LENGTH
*/
void mpu_set_DLPF_mode(char mode) {
	i2c_write_bits(MPU6050_RA_CONFIG, MPU6050_CFG_DLPF_CFG_BIT, MPU6050_CFG_DLPF_CFG_LENGTH, mode);
}

/**
 * Configure FIFO to store data from accelerometer temp and gyro
 * @param enabled 0 disabled; 1 enabled
 */
void mpu_set_6axis_FIFO_enabled(char enabled) {
	i2c_write_byte(MPU6050_RA_FIFO_EN, 0x78);
//	i2c_write_bits(MPU6050_RA_FIFO_EN, MPU6050_ACCEL_FIFO_EN_BIT, 1, enabled);
//	i2c_write_bits(MPU6050_RA_FIFO_EN, MPU6050_TEMP_FIFO_EN_BIT, 1, 0);
//	i2c_write_bits(MPU6050_RA_FIFO_EN, MPU6050_XG_FIFO_EN_BIT, 1, enabled);
//	i2c_write_bits(MPU6050_RA_FIFO_EN, MPU6050_YG_FIFO_EN_BIT, 1, enabled);
//	i2c_write_bits(MPU6050_RA_FIFO_EN, MPU6050_ZG_FIFO_EN_BIT, 1, enabled);
}

/**
 * Configure interruption mode
 * @param mode 0 active high; 1 active low
 */
void mpu_set_interrupt_mode(char mode) {
	i2c_write_bits(MPU6050_RA_INT_PIN_CFG, MPU6050_INTCFG_INT_LEVEL_BIT, 1, mode);
}

/**
 * Set interrupt drive mode
 * @param drive 0 push/pull; 1 open-drain
 */
void mpu_set_interrupt_drive(char drive) {
	i2c_write_bits(MPU6050_RA_INT_PIN_CFG, MPU6050_INTCFG_INT_OPEN_BIT, 1, drive);
}

/**
 * Set interrupt latch mode
 * @param latch 0 50us pulse; 1 high until interrupt is cleared
 */
void mpu_set_interrupt_latch(char latch) {
	i2c_write_bits(MPU6050_RA_INT_PIN_CFG, MPU6050_INTCFG_LATCH_INT_EN_BIT, 1, latch);
}

/**
 * Set FIFO Buffer Overflow interrupt enabled status.
 * @param enabled 1 enabled; 0 disabled
 */
void mpu_set_FIFO_overflow_interrupt(char enabled) {
	i2c_write_bits(MPU6050_RA_INT_ENABLE, MPU6050_INTERRUPT_FIFO_OFLOW_BIT, 1, enabled);
}

/**
 * Set Data Ready interrupt enabled status.
 * @param enabled 1 enabled; 0 disabled
 */
void mpu_set_data_ready_interrupt(char enabled) {
	i2c_write_bits(MPU6050_RA_INT_ENABLE, MPU6050_INTERRUPT_DATA_RDY_BIT, 1, enabled);
}

/** Get full set of interrupt status bits.
* These bits clear to 0 after the register has been read. Very useful
* for getting multiple INT statuses, since each single bit read clears
* all of them because it has to read the whole byte.
* @see MPU6050_RA_INT_STATUS
*/
int mpu_clear_interrupt(char* data) {
	return i2c_read_byte(MPU6050_RA_INT_STATUS, data);
}

/** Enable FIFO usage.
 * @param enabled 1 enabled; 0 disabled
 */
void mpu_set_FIFO_enabled(char enabled) {
	i2c_write_bits(MPU6050_RA_USER_CTRL, MPU6050_USERCTRL_FIFO_EN_BIT, 1, enabled);
}

/**
 * Reset the FIFO.
 * This bit resets the FIFO buffer when set to 1 while FIFO_EN equals 0. This
 * bit automatically clears to 0 after the reset has been triggered.
 * @param enabled 1 enabled; 0 disabled
 */
void mpu_reset_FIFO(void) {
	i2c_write_bits(MPU6050_RA_USER_CTRL, MPU6050_USERCTRL_FIFO_RESET_BIT, 1, 1);
}


/** Get current FIFO buffer size.
* This value indicates the number of bytes stored in the FIFO buffer. This
* number is in turn the number of bytes that can be read from the FIFO buffer
* and it is directly proportional to the number of samples available given the
* set of sensor data bound to be stored in the FIFO (register 35 and 36).
*/
void mpu_get_FIFO_size(int* size) {
	char count[2];
	i2c_read_bytes(MPU6050_RA_FIFO_COUNTH, 2, count);
	*size = (((int)count[0]) << 8) | count[1];
}

/** Get raw 6-axis motion sensor readings (accel/gyro).
* Retrieves all currently available motion sensor values.
* @param ax 16-bit signed integer container for accelerometer X-axis value
* @param ay 16-bit signed integer container for accelerometer Y-axis value
* @param az 16-bit signed integer container for accelerometer Z-axis value
* @param gx 16-bit signed integer container for gyroscope X-axis value
* @param gy 16-bit signed integer container for gyroscope Y-axis value
* @param gz 16-bit signed integer container for gyroscope Z-axis value
*
* buff:
* ax_h, ax_l, ay_h, ay_l, az_h, az_l, gx_h, gx_l, gy_h, gy_l, gz_h, gz_l,
*
* @see getAcceleration()
* @see getRotation()
* @see MPU6050_RA_ACCEL_XOUT_H
*/
void mpu_get_motion6(char* buff) {

	log_string_mpu(">> mpu_get_motion6\n");

	char c[14];
	i2c_read_bytes(MPU6050_RA_ACCEL_XOUT_H, 14, c);
	*buff = c[0];
	*(buff+1) = c[1];
	*(buff+2) = c[2];
	*(buff+3) = c[3];
	*(buff+4) = c[4];
	*(buff+5) = c[5];
	*(buff+6) = c[8];
	*(buff+7) = c[9];
	*(buff+8) = c[10];
	*(buff+9) = c[11];
	*(buff+10) = c[12];
	*(buff+11) = c[13];

	log_string_mpu("<< mpu_get_motion6\n");
}

/** Get byte from FIFO buffer.
* This register is used to read and write data from the FIFO buffer. Data is
* written to the FIFO in order of register number (from lowest to highest). If
* all the FIFO enable flags (see below) are enabled and all External Sensor
* Data registers (Registers 73 to 96) are associated with a Slave device, the
* contents of registers 59 through 96 will be written in order at the Sample
* Rate.
*
* The contents of the sensor data registers (Registers 59 to 96) are written
* into the FIFO buffer when their corresponding FIFO enable flags are set to 1
* in FIFO_EN (Register 35). An additional flag for the sensor data registers
* associated with I2C Slave 3 can be found in I2C_MST_CTRL (Register 36).
*
* If the FIFO buffer has overflowed, the status bit FIFO_OFLOW_INT is
* automatically set to 1. This bit is located in INT_STATUS (Register 58).
* When the FIFO buffer has overflowed, the oldest data will be lost and new
* data will be written to the FIFO.
*
* If the FIFO buffer is empty, reading this register will return the last byte
* that was previously read from the FIFO until new data is available. The user
* should check FIFO_COUNT to ensure that the FIFO buffer is not read when
* empty.
*
* buff:
* ax_h, ax_l, ay_h, ay_l, az_h, az_l, gx_h, gx_l, gy_h, gy_l, gz_h, gz_l,
*
*/
void mpu_get_FIFO_motion6(char* buff) {

	char c[12];
	i2c_read_bytes(MPU6050_RA_FIFO_R_W, 12, c);

	*buff = c[0];
	*(buff+1) = c[1];
	*(buff+2) = c[2];
	*(buff+3) = c[3];
	*(buff+4) = c[4];
	*(buff+5) = c[5];
	*(buff+6) = c[6];
	*(buff+7) = c[7];
	*(buff+8) = c[8];
	*(buff+9) = c[9];
	*(buff+10) = c[10];
	*(buff+11) = c[11];

//	i2c_read_byte(MPU6050_RA_FIFO_R_W, ax_h);
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, ax_l);
//
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, ay_h);
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, ay_l);
//
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, az_h);
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, az_l);
//
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, gx_h);
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, gx_l);
//
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, gy_h);
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, gy_l);
//
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, gz_h);
//	i2c_read_byte(MPU6050_RA_FIFO_R_W, gz_l);
}

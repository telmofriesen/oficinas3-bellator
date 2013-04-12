/*
 * imu.c
 *
 *  Created on: Mar 21, 2013
 *      Author: telmo
 */
#include "lpc2103.h"
#include "imu.h"
#include "logger.h"

void __attribute__ ((interrupt("IRQ"))) i2c_read_bytes_isr(void);
void __attribute__ ((interrupt("IRQ"))) i2c_write_byte_isr(void);

static inline int i2c_read_byte(char reg_addr, char* data);
static inline int i2c_read_bytes(char reg_addr, char length, char* data);

static inline int i2c_write_bits(char reg_addr, char bit, char length, char data);
static inline int i2c_write_byte(char reg_addr, char data);

static inline void mpu_set_clock_source(char source);
static inline void mpu_set_full_scale_gyro_range(char range);
static inline void mpu_set_full_scale_accel_range(char range);
static inline void mpu_set_sleep_enable(int enable);
static inline void mpu_set_temperature_sensor_enabled(char enabled);
static inline void mpu_set_gyro_rate(char rate);
static inline void mpu_set_DLPF_mode(char mode);

volatile int busy = 0;
volatile int temp = 0;

static int buff_size = 0;
static int buff_pos = 0;
static char* c_buff = 0;
static char ra_buff = 0;
static char buff[14] = { 0, };


/**
 * I2C 1
 *
 * I2Cbitfrequency = PCLK / ( I2C1SCLH + I2C1SCLL )
 * 0 < I2Cbitfrequency < 400kHz
 * I2C1SCLL >= 4, I2C1SCLH >=4
 * pclk=15MHz or pclk=14.7456MHz, depending on previous configuration
 *
 * MPU Address: b1101000W
 * W bit 0 => write on the mpu registers
 * I2C Addr + Reg Addr + Data
 * I2C Addr + Reg Addr + Data + Data for Addr+1, etc
 * W bit 1 => read
 */
void i2c_init(void){

	log_string_i2c(">> i2c_init\n");

	// Set the pin function
	PINSEL1 |= 0x1 << 2; // SCL1
	PINSEL1 |= 0x1 << 4; // SDA1

	I2C1CONCLR = 0x6C; // clear all flags
	I2C1CONSET |= 0x1 << 6; // enable i2c1
	I2C1SCLH = 19; // Set the bit rate:
	I2C1SCLL = 19; // 394.7kHz for pclk=15MHz | 388.0kHz for pclk=14.7456MHz

	// Enable the interrupts
	VICVectCntl0 = 0x33; //Vectored Interrupt slot enabled with source #19 (I2C1)
	VICIntEnable |= 0x1 << 19; //source #19 enabled as FIQ or IRQ

	// Enable i2c as FIQ
	//VICIntSelect |= 0x1 << 19;// I2C1 as FIQ
	//VICIntEnable |= 0x1 << 19; //source #19 enabled as FIQ or IRQ

	log_string_i2c("<< i2c_init\n");
}

//void i2c_isr(void) {
//	(*current_isr)();
//}

static inline int i2c_read_byte(char reg_addr, char* data) {
	log_string_i2c(">> read_byte\n");

	int i = i2c_read_bytes(reg_addr,1,data);

	log_string_i2c("<< read_byte\n");
	return i;
}

static inline int i2c_read_bytes(char reg_addr, char length, char* data) {
	log_string_i2c(">> read_bytes\n");

	buff_size = length;
	buff_pos = 0;
	ra_buff = reg_addr;
	c_buff = data;

	busy = 1;

	//Setting the interrupt handler location for write byte
	VICVectAddr0 = (unsigned int) &i2c_read_bytes_isr;
	// Send Start bit
	I2C1CONSET = 0x20; // Transmit start condition

	log_string_i2c("waiting\n");

	while (busy); // busy wait for read process

	// nao apagar, gambi master
	log_string("<< read_bytes\n");
	return 1;
}

/**
 * i2c interrupt handler
 */
void i2c_read_bytes_isr(void) {
	log_string_i2c(">> read_bytes_isr\n");
	temp = I2C1STAT;

	switch (temp) {
	case TW_START: // A START condition has been transmitted.
		log_string_i2c("TW_START\n");
		I2C1DAT = (MPU6050_ADDRESS_AD0_LOW << 0x1) | I2C_WRITE; // Slave address + Write
		I2C1CONCLR = 0x28; // Clear SI and STA flag
		break;
	case TW_REP_START: // A repeated START	condition has been transmitted.
		log_string_i2c("TW_REP_START\n");
		I2C1DAT = (MPU6050_ADDRESS_AD0_LOW << 0x1) | I2C_READ; // Slave address + Read
		I2C1CONCLR = 0x28; // Clear SI and STA flag
		break;
	case TW_MT_SLA_ACK: // SLA+W has been transmitted; ACK has been received.
		log_string_i2c("TW_MT_SLA_ACK\n");
		I2C1DAT = ra_buff; // Register address to be written
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_SLA_NACK: // SLA+W has been transmitted; NOT ACK has been received.
		log_string_i2c("TW_MT_SLA_NACK\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_DATA_ACK: // Data byte in I2DAT has been transmitted; ACK has been received.
		log_string_i2c("TW_MT_DATA_ACK\n");
		I2C1CONSET = 0x20; // Transmit start condition
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_DATA_NACK: // Data byte in I2DAT has been transmitted; NOT ACK has been received.
		log_string_i2c("TW_MT_DATA_NACK\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_ARB_LOST: // Arbitration lost in SLA+R/W or Data bytes.
		log_string_i2c("TW_MT_ARB_LOST\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;

	case TW_MR_SLA_ACK: // SLA+R has been transmitted; ACK has been received.
		log_string_i2c("TW_MR_SLA_ACK\n");
		if(buff_size > 1)
			I2C1CONSET = 0x04; // Transmit ACK on data receives
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MR_SLA_NACK: // SLA+R has been transmitted; NOT ACK or has been received.
		log_string_i2c("TW_MR_SLA_NACK\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MR_DATA_ACK: // Data byte has been received; ACK has been returned.
		log_string_i2c("TW_MR_DATA_ACK\n");
		log_string_i2c("pos: ");
		log_int_i2c(buff_pos);
		log_string_i2c("\n");
		if ((buff_pos + 2) < buff_size) {
			c_buff[buff_pos++] = I2C1DAT;
			I2C1CONCLR = 0x08; // Clear SI
		}
		else {
			c_buff[buff_pos++] = I2C1DAT;
			I2C1CONCLR = 0x0C; // Transmit NACK on next data receive, Clear SI
		}
		break;
	case TW_MR_DATA_NACK: // Data byte has been received; NOT ACK has been returned.
		log_string_i2c("TW_MR_DATA_NACK\n");
		log_string_i2c("pos: ");
		log_int_i2c(buff_pos);
		log_string_i2c("\n");
		if (buff_pos < buff_size) {
			c_buff[buff_pos++] = I2C1DAT;
		}
		I2C1CONSET = 0x10; // Transmit stop condition
		I2C1CONCLR = 0x08; // Clear SI
		busy = 0; // data ready to be returned
		break;

	default:
		log_string_warning("[i2c] default: ");
		log_int_warning(temp);
		log_string_warning("\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;
	}

	VICVectAddr = 0;
}

static inline int i2c_write_bits(char reg_addr, char bit, char length, char data){
	log_string_i2c(">> write_bits\n");
	//      010 value to write
	// 76543210 bit numbers
	//    xxx   args: bitStart=4, length=3
	// 00011100 mask byte
	// 10101111 original value (sample)
	// 10100011 original & ~mask
	// 10101011 masked | value

	char c = 0;
	i2c_read_bytes(reg_addr,1,&c);
	char mask = ((1 << length) - 1) << (bit - length + 1);
	data <<= (bit - length + 1); // shift data into correct position
	data &= mask; // zero all non-important bits in data
	c &= ~(mask); // zero all important bits in existing byte
	c |= data; // combine data with existing byte

	int i = i2c_write_byte(reg_addr, c);

	log_string_i2c("<< write_bits\n");

	return i;
}

static inline int i2c_write_byte(char reg_addr, char data) {
	log_string_i2c(">> write_byte\n");

	buff_size = 1;
	buff_pos = 0;
	ra_buff = reg_addr;
	c_buff = &data;

	busy = 1;

	//Setting the interrupt handler location for write byte
	VICVectAddr0 = (unsigned int) &i2c_write_byte_isr;

	// Send Start bit
	I2C1CONSET = 0x20; // Transmit start condition

	while (busy); // busy wait for read process

	// nao apagar, gambi master
	log_string("<< write_byte\n");
	return 1;
}

/**
 * i2c interrupt handler
 */
void i2c_write_byte_isr(void) {
	log_string_i2c(">> i2c_write_byte_isr\n");
	temp = I2C1STAT;

	switch (temp) {
	case TW_START: // A START condition has been transmitted.
		log_string_i2c("TW_START\n");
		I2C1DAT = (MPU6050_ADDRESS_AD0_LOW << 0x1) | I2C_WRITE; // Slave address + Write
		I2C1CONCLR = 0x28; // Clear SI and STA flag
		break;
	case TW_REP_START: // A repeated START	condition has been transmitted.
		log_string_i2c("TW_REP_START\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_SLA_ACK: // SLA+W has been transmitted; ACK has been received.
		log_string_i2c("TW_MT_SLA_ACK\n");
		I2C1DAT = ra_buff; // Register address to be written
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_SLA_NACK: // SLA+W has been transmitted; NOT ACK has been received.
		log_string_i2c("TW_MT_SLA_NACK\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_DATA_ACK: // Data byte in I2DAT has been transmitted; ACK has been received.
		log_string_i2c("TW_MT_DATA_ACK\n");
		if (buff_pos < buff_size) {
			I2C1DAT = c_buff[buff_pos++]; // Send data
			I2C1CONCLR = 0x08; // Clear SI
		} else {
			I2C1CONSET |= 0x01 << 4; // Transmit stop condition
			I2C1CONCLR = 0x08; // Clear SI
			busy = 0; // done
		}
		break;
	case TW_MT_DATA_NACK: // Data byte in I2DAT has been transmitted; NOT ACK has been received.
		log_string_i2c("TW_MT_DATA_NACK\n");
		I2C1CONSET |= 0x01 << 4; // Transmit stop condition
		I2C1CONCLR = 0x08; // Clear SI
		busy = 0; // done
		break;
	case TW_MT_ARB_LOST: // Arbitration lost in SLA+R/W or Data bytes.
		log_string_i2c("TW_MT_ARB_LOST\n");
		I2C1CONCLR = 0x08; // Clear SI
		break;

	default:
		log_string_warning("[i2c] default: ");
		log_int_warning(temp);
		log_string_warning("\n");
		I2C1CONCLR = 0x08; // Clear SI
		log_string_i2c("default\n");
		break;
	}

	VICVectAddr = 0;
}


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
    //mpu_set_6axis_FIFO_enabled(1);

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
    //mpu_reset_FIFO();
    // enable fifo
    //mpu_set_FIFO_enabled(1);

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
static inline void mpu_set_clock_source(char source) {
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
static inline void mpu_set_full_scale_gyro_range(char range) {
	i2c_write_bits(MPU6050_RA_GYRO_CONFIG, MPU6050_GCONFIG_FS_SEL_BIT, MPU6050_GCONFIG_FS_SEL_LENGTH, range);
}

/** Set full-scale accelerometer range.
* @param range New full-scale accelerometer range setting
* @see getFullScaleAccelRange()
*/
static inline void mpu_set_full_scale_accel_range(char range) {
	i2c_write_bits(MPU6050_RA_ACCEL_CONFIG, MPU6050_ACONFIG_AFS_SEL_BIT, MPU6050_ACONFIG_AFS_SEL_LENGTH, range);
}

/** Set sleep mode status.
* @param enabled New sleep mode enabled status
* @see getSleepEnabled()
* @see MPU6050_RA_PWR_MGMT_1
* @see MPU6050_PWR1_SLEEP_BIT
*/
static inline void mpu_set_sleep_enable(int enable) {
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
static inline void mpu_set_temperature_sensor_enabled(char enabled) {
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
static inline void mpu_set_gyro_rate(char rate) {
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
static inline void mpu_set_DLPF_mode(char mode) {
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
void mpu_get_motion6(char* data) {

	log_string_mpu(">> mpu_get_motion6\n");

	i2c_read_bytes(MPU6050_RA_ACCEL_XOUT_H, 14, buff);

	*data = buff[0];
	*(data+1) = buff[1];
	*(data+2) = buff[2];
	*(data+3) = buff[3];
	*(data+4) = buff[4];
	*(data+5) = buff[5];
	*(data+6) = buff[8];
	*(data+7) = buff[9];
	*(data+8) = buff[10];
	*(data+9) = buff[11];
	*(data+10) = buff[12];
	*(data+11) = buff[13];

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
//void mpu_get_FIFO_motion6(char* data) {
//
//	i2c_read_bytes(MPU6050_RA_FIFO_R_W, 12, buff);
//
//	*data = buff[0];
//	*(data+1) = buff[1];
//	*(data+2) = buff[2];
//	*(data+3) = buff[3];
//	*(data+4) = buff[4];
//	*(data+5) = buff[5];
//	*(data+6) = buff[6];
//	*(data+7) = buff[7];
//	*(data+8) = buff[8];
//	*(data+9) = buff[9];
//	*(data+10) = buff[10];
//	*(data+11) = buff[11];
//}

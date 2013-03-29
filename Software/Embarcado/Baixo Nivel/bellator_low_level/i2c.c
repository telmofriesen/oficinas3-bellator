/*
 * i2c.c
 *
 *  Created on: Mar 21, 2013
 *      Author: telmo
 */
#include "lpc2103.h"
#include "i2c.h"
#include "logger.h"
#include "mpu6050.h"


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

	log_string_i2c("<< i2c_init\n");
}

int i2c_read_byte(char reg_addr, char* data) {
	log_string_i2c("read_byte\n");

	int i = i2c_read_bytes(reg_addr,1,data);

	log_string_i2c("read_byte..returning\n");
	return i;
}

int i2c_read_bytes(char reg_addr, char length, char* data) {
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

	log_string_i2c("<< read_bytes\n");
	return 1;
}

/**
 * i2c interrupt handler
 */
void i2c_read_bytes_isr(void) {
	log_string_i2c(">> read_bytes_isr\n");
	int temp = 0;
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
		break;
	case TW_MT_DATA_ACK: // Data byte in I2DAT has been transmitted; ACK has been received.
		log_string_i2c("TW_MT_DATA_ACK\n");
		I2C1CONSET = 0x20; // Transmit start condition
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_DATA_NACK: // Data byte in I2DAT has been transmitted; NOT ACK has been received.
		log_string_i2c("TW_MT_DATA_NACK\n");
		break;
	case TW_MT_ARB_LOST: // Arbitration lost in SLA+R/W or Data bytes.
		log_string_i2c("TW_MT_ARB_LOST\n");
		break;

	case TW_MR_SLA_ACK: // SLA+R has been transmitted; ACK has been received.
		log_string_i2c("TW_MR_SLA_ACK\n");
		I2C1CONSET = 0x04; // Transmit ACK on data receives
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MR_SLA_NACK: // SLA+R has been transmitted; NOT ACK or has been received.
		log_string_i2c("TW_MR_SLA_NACK\n");
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
		log_string_i2c("[i2c] default\n");
		break;
	}

	VICVectAddr = 0;
}

int i2c_write_bits(char reg_addr, char bit, char length, char data){
	//      010 value to write
	// 76543210 bit numbers
	//    xxx   args: bitStart=4, length=3
	// 00011100 mask byte
	// 10101111 original value (sample)
	// 10100011 original & ~mask
	// 10101011 masked | value

	char c;
	i2c_read_byte(reg_addr, &c);
	char mask = ((1 << length) - 1) << (bit - length + 1);
	data <<= (bit - length + 1); // shift data into correct position
	data &= mask; // zero all non-important bits in data
	c &= ~(mask); // zero all important bits in existing byte
	c |= data; // combine data with existing byte

	return i2c_write_byte(reg_addr, c);
}

int i2c_write_byte(char reg_addr, char data) {
	log_string_i2c("write_byte\n");

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

	log_string_i2c("write_byte..returning\n");
	return 1;
}

/**
 * i2c interrupt handler
 */
void i2c_write_byte_isr(void) {
	log_string_i2c(">> i2c_write_byte_isr\n");
	int temp = 0;
	temp = I2C1STAT;

	switch (temp) {
	case TW_START: // A START condition has been transmitted.
		log_string_i2c("TW_START\n");
		I2C1DAT = (MPU6050_ADDRESS_AD0_LOW << 0x1) | I2C_WRITE; // Slave address + Write
		I2C1CONCLR = 0x28; // Clear SI and STA flag
		break;
	case TW_REP_START: // A repeated START	condition has been transmitted.
		log_string_i2c("TW_REP_START\n");
		break;
	case TW_MT_SLA_ACK: // SLA+W has been transmitted; ACK has been received.
		log_string_i2c("TW_MT_SLA_ACK\n");
		I2C1DAT = ra_buff; // Register address to be written
		I2C1CONCLR = 0x08; // Clear SI
		break;
	case TW_MT_SLA_NACK: // SLA+W has been transmitted; NOT ACK has been received.
		log_string_i2c("TW_MT_SLA_NACK\n");
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
		break;

	default:
		log_string_i2c("default\n");
		break;
	}

	VICVectAddr = 0;
}

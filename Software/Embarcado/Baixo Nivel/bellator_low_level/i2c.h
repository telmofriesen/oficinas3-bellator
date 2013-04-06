/*
 * i2c.h
 *
 *  Created on: Mar 21, 2013
 *      Author: telmo
 */

#ifndef I2C_H_
#define I2C_H_

/* Master */

// A START condition has been transmitted.
#define TW_START                0x08
// A repeated START	condition has been transmitted.
#define TW_REP_START            0x10

/* Master Transmitter */

// SLA+W has been transmitted; ACK has been received.
#define TW_MT_SLA_ACK           0x18
// SLA+W has been transmitted; NOT ACK has been received.
#define TW_MT_SLA_NACK          0x20
// Data byte in I2DAT has been transmitted; ACK has been received.
#define TW_MT_DATA_ACK          0x28
// Data byte in I2DAT has been transmitted; NOT ACK has been received.
#define TW_MT_DATA_NACK         0x30
// Arbitration lost in SLA+R/W or Data bytes.
#define TW_MT_ARB_LOST          0x38

/* Master Receiver */
#define TW_MR_ARB_LOST          0x38
#define TW_MR_SLA_ACK           0x40
#define TW_MR_SLA_NACK          0x48
#define TW_MR_DATA_ACK          0x50
#define TW_MR_DATA_NACK         0x58

#define TW_OK                   0
#define TW_ERROR                1

#define I2C_WRITE 				0x00
#define I2C_READ 				0x01

void __attribute__ ((interrupt("IRQ"))) i2c_read_bytes_isr(void);
void __attribute__ ((interrupt("IRQ"))) i2c_write_byte_isr(void);

void i2c_init(void);
//void i2c_isr(void);

int i2c_read_byte(char reg_addr, char* data);
int i2c_read_bytes(char reg_addr, char length, char* data);
//void i2c_read_bytes_isr(void);

int i2c_write_bits(char reg_addr, char bit, char length, char data);
int i2c_write_byte(char reg_addr, char data);
//void i2c_write_byte_isr(void);

static volatile int busy = 0;

static int buff_size = 0;
static int buff_pos = 0;
static char* c_buff = 0;
static char ra_buff = 0;

#endif /* I2C_H_ */

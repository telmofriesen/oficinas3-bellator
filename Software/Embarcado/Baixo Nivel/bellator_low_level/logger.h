/*
 * logger.h
 *
 *  Created on: Mar 3, 2009
 *      Author: telmo
 */

#ifndef LOGGER_H_
#define LOGGER_H_

//#if defined(ERROR) || defined(WARNING) || defined(DEBUG) || defined(DEBUG_I2C) || defined(DEBUG_MPU)
//#	define logger_init(str) logger_init(str)
//#else
//#	define logger_init(str)
//#endif

#ifdef ERROR
#   define log_char_error(str) log_char(str)
#   define log_int_error(str) log_int(str)
#   define log_short_error(str) log_short(str)
#   define log_string_error(str) log_string(str)
#   define log_byte_error(str) log_byte(str)
#   define log2bytes_error(str) log2bytes(str)
#   define log4bytes_error(str) log4bytes(str)
#else
#   define log_char_error(str)
#   define log_int_error(str)
#   define log_short_error(str)
#   define log_string_error(str)
#   define log_byte_error(str)
#   define log2bytes_error(str)
#   define log4bytes_error(str)
#endif

#ifdef WARNING
#   define log_char_warning(str) log_char(str)
#   define log_int_warning(str) log_int(str)
#   define log_short_warning(str) log_short(str)
#   define log_string_warning(str) log_string(str)
#   define log_byte_warning(str) log_byte(str)
#   define log2bytes_warning(str) log2bytes(str)
#   define log4bytes_warning(str) log4bytes(str)
#else
#   define log_char_warning(str)
#   define log_int_warning(str)
#   define log_short_warning(str)
#   define log_string_warning(str)
#   define log_byte_warning(str)
#   define log2bytes_warning(str)
#   define log4bytes_warning(str)
#endif

#ifdef DEBUG
#   define log_char_debug(str) log_char(str)
#   define log_int_debug(str) log_int(str)
#   define log_short_debug(str) log_short(str)
#   define log_string_debug(str) log_string(str)
#   define log_byte_debug(str) log_byte(str)
#   define log2bytes_debug(str) log2bytes(str)
#   define log4bytes_debug(str) log4bytes(str)
#else
#   define log_char_debug(str)
#   define log_int_debug(str)
#   define log_short_debug(str)
#   define log_string_debug(str)
#   define log_byte_debug(str)
#   define log2bytes_debug(str)
#   define log4bytes_debug(str)
#endif

#ifdef DEBUG_I2C
#   define log_char_i2c(str) log_char(str)
#   define log_int_i2c(str) log_int(str)
#   define log_short_i2c(str) log_short(str)
#   define log_string_i2c(str) log_string(str)
#   define log_byte_i2c(str) log_byte(str)
#   define log2bytes_i2c(str) log2bytes(str)
#   define log4bytes_i2c(str) log4bytes(str)
#else
#   define log_char_i2c(str)
#   define log_int_i2c(str)
#   define log_short_i2c(str)
#   define log_string_i2c(str)
#   define log_byte_i2c(str)
#   define log2bytes_i2c(str)
#   define log4bytes_i2c(str)
#endif

#ifdef DEBUG_MPU
#   define log_char_mpu(str) log_char(str)
#   define log_int_mpu(str) log_int(str)
#   define log_short_mpu(str) log_short(str)
#   define log_string_mpu(str) log_string(str)
#   define log_byte_mpu(str) log_byte(str)
#   define log2bytes_mpu(str) log2bytes(str)
#   define log4bytes_mpu(str) log4bytes(str)
#else
#   define log_char_mpu(str)
#   define log_int_mpu(str)
#   define log_short_mpu(str)
#   define log_string_mpu(str)
#   define log_byte_mpu(str)
#   define log2bytes_mpu(str)
#   define log4bytes_mpu(str)
#endif

const char ascii[] = "0123456789ABCDEF";

void logger_init(void);
static void log_char(char c);
void log_int(int num);
void log_short(short num);
void log_string(const char *c);
void log_byte(char c);
void log2bytes(short c);
void log4bytes(int c);

#endif /* LOGGER_H_ */

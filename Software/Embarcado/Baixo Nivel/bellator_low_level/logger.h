/*
 * logger.h
 *
 *  Created on: Mar 3, 2009
 *      Author: telmo
 */

#ifndef LOGGER_H_
#define LOGGER_H_

const char ascii[] = "0123456789ABCDEF";

void logger_init(void);
static void ua_outchar(char c);
void log_int(int num);
void log_short(short num);
void log_string(const char *c);
void log2bytes(short c);
void log4bytes(int c);

#endif /* LOGGER_H_ */

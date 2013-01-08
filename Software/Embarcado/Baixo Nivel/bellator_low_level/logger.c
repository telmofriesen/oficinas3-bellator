/*
 * logger.c
 *
 *  Created on: Jul 14, 2009
 *      Author: telmo
 */
#include "lpc2103.h"
#include "logger.h"

/**
 * Start logger using UART0
 * 8N1 (8 data, Non parity, 1 stop)
 * 115200 bps
 *
 * UARTn_baudrate = PCLK / ( 16 * ( 256 * UnDLM + UnDLL) * ( 1 + DivAddVal/MulVal))
 * ou UARTn_baudrate = PCLK / ( 16 * ( 256 * UnDLM + UnDLL))
 * 115131 = 15MHz / ( 16 * ( 256*0 + 6) * ( 1 + 5/14) )
 * 115200 = 14.7456MHz / ( 16 * ( 256*0 + 8) )
 *
 */
void logger_init(void){ // using UART0
	PINSEL0 |= 0x05; // Set the pins function
	U0FCR    = 0x07; // FIFOControlRegister, Tx, Rx FIFO Reset and FIFO enable
	U0LCR 	 = 0x83; // DivisorLatchAccessBit = 1,  UART 8N1, allow access to divider-latches

#ifdef CRYSTAL12MHz
	U0DLL	 = 0x06; // DivisorLatchLow bit
	U0DLM	 = 0x00; // DivisorLatchHigh bit
	U0FDR	|= 0x05; // DivAddVal
	U0FDR	|= 0x0E << 4; // MulVal = 14
#endif
#ifdef CRYSTAL14745600Hz
	U0DLL	 = 0x08; // DivisorLatchLow bit
	U0DLM	 = 0x00; // DivisorLatchHigh bit
#endif

	U0LCR	 = 0x03; // DivisorLatchAccessBit = 0,  UART 8N1, forbid access to divider-latches
}

static void ua_outchar(char c){
	U0THR = c;     // TransmitHoldingRegister , DivisorLatchAccessBit must be 0 to transmit
	while(!(U0LSR & 0x40));
}

void log_int(int num){
	if(num & 0x80000000){// se for negativo
		ua_outchar('-');
		num = ~num;
		num += 0x1;
	}else
		ua_outchar(' ');
	ua_outchar(ascii[num >> 28]);
	ua_outchar(ascii[num >> 24 & 0x0000000f]);
	ua_outchar(ascii[num >> 20 & 0x0000000f]);
	ua_outchar(ascii[num >> 16 & 0x0000000f]);
	ua_outchar(ascii[num >> 12 & 0x0000000f]);
	ua_outchar(ascii[num >> 8 & 0x0000000f]);
	ua_outchar(ascii[num >> 4 & 0x0000000f]);
	ua_outchar(ascii[num & 0x0000000f]);
}

void log_short(short num){
	if(num & 0x8000){// se for negativo
		ua_outchar('-');
		num = ~num;
		num++;
	}else
		ua_outchar(' ');
	ua_outchar(ascii[num >> 12]);
	ua_outchar(ascii[num >> 8 & 0x000f]);
	ua_outchar(ascii[num >> 4 & 0x000f]);
	ua_outchar(ascii[num & 0x000f]);
}

void log_string(const char *s){
	while(*s){
		if(*s == '\n')
			ua_outchar('\r'); // \n + \r = new line
		ua_outchar(*s);
		s++;
	}
}

void log2bytes(short c){
	if(c & 0x8000){// se for negativo
		ua_outchar('-');
		c = ~c;
		c++;
	}else
		ua_outchar(' ');
	ua_outchar(ascii[c >> 12]);
	ua_outchar(ascii[c >> 8 & 0x000f]);
	ua_outchar(ascii[c >> 4 & 0x000f]);
	ua_outchar(ascii[c & 0x000f]);
}

void log4bytes(int c){
	if(c & 0x80000000){// se for negativo
		ua_outchar('-');
		c = ~c;
		c += 0x1;
	}else
		ua_outchar(' ');
	ua_outchar(ascii[c >> 28]);
	ua_outchar(ascii[c >> 24 & 0x0000000f]);
	ua_outchar(ascii[c >> 20 & 0x0000000f]);
	ua_outchar(ascii[c >> 16 & 0x0000000f]);
	ua_outchar(ascii[c >> 12 & 0x0000000f]);
	ua_outchar(ascii[c >> 8 & 0x0000000f]);
	ua_outchar(ascii[c >> 4 & 0x0000000f]);
	ua_outchar(ascii[c & 0x0000000f]);
}

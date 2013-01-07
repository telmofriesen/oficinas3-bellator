/**
 *
 *
 *
 */
/* global defines */
#define CRYSTAL12MHz
//#define CRYSTAL14745600Hz
#define CMD_BUFF_SIZE 8 // has to be a power of two
#define DEBUG

/* includes */
#include "lpc2103.h"
#include "logger.h"
#include "logger.c"
#include "irq.h"
#include "irq.c"
#include "protocol.h"

/* interruptions */
void __attribute__ ((interrupt("IRQ"))) protocol_in(void);
//void __attribute__ ((interrupt("FIQ"))) update_position(void);
void __attribute__ ((interrupt("IRQ"))) error(void);


/* init functions */
inline void PLL_Init(void);
inline void MAM_Init(void);
inline void APB_Init(void);

inline void pulses_in_init(void);
inline void i2c_init(void);
inline void adc_init(void);
inline void pwm_out_init(void);
static inline void protocol_init(void);


/* getters */
int get_ir_sensor_data(unsigned short i);
int get_encoder_count(unsigned short i);

/* auxiliary functions */
static void protocol_out_cmd(void);
static void protocol_out_char(char c);

/* data structs defenitions */
struct cmd_buff {
  unsigned int i;
  char buff [CMD_BUFF_SIZE];      // Circular Buffer
};

/* global variables */
static struct cmd_buff cmd_out = { 0, };
static struct cmd_buff cmd_in = { 0, };

/**
 * Entry point
 */
int main(void){

	PLL_Init(); // Turn on PLL clock
	MAM_Init(); // Turn on MAM pre-fetcher
	APB_Init(); // Turn on the peripheral devices clock divider

#ifdef DEBUG
	logger_init();
	log_string("iniciando...\n");
#endif

	enableIRQ(); // Enable interruptions

	pulses_in_init(); // start counting pulses from the encoder	| Timer 0 counter mode, no interruption
	i2c_init(); // start the communication with the IMU			| Int prirority 1
	adc_init(); // start reading the IR sensor signals			| Burst mode, no interruption
	pwm_out_init(); // start pwm for the H bridges				| Int prirority 2
	protocol_init(); // start the communication protocol		| Int prirority 3

	//enableFIQ();
	//positioning_init();

	VICDefVectAddr = (unsigned int) &error;

	while(1){
	}
	return 0;
}

/**
 * Sets the processor clock
 *
 * Fosc = External oscilator =	12MHz | 14.7456MHz
 * CCLK = PLL Clock output =	60MHz | 58.9824MHz  (desired system clock)
 * MSEL = Clock multiplier =	4	  | 3			(MSEL = CCLK/Fosc -1)
 * PSEL = Clock divider = 		1	  | 1			(P | 156MHz < Fcco < 320MHz, Fcco = CCLK * 2 * P)
 * 													(P=1 -> PSEL=00, P=2 -> PSEL=01, P=4 -> PSEL=10, P=8 -> PSEL=11)
 */
inline void PLL_Init(void){

#ifdef CRYSTAL12MHz
	PLLCFG=0x24;                // 12MHz crystal      -> 60MHz
#endif
#ifdef CRYSTAL14745600Hz
	PLLCFG=0x23;                // 14.7456MHz crystal -> 58.9824MHz
#endif

	PLLCON=0x1;                 //PLLE = 1, PLLEnable
	PLLFEED=0xAA;               // Validation sequence
	PLLFEED=0x55;               // Validation sequence
	while(!(PLLSTAT & 0x400)); // Wait PLL to lock
	PLLCON=0x3;                 // PLLC = 1, PLLConnect, Assert the PLL to be the cclk
	PLLFEED=0xAA;               // Validation sequence
	PLLFEED=0x55;               // Validation sequence
}

/**
 * Starts the Memory Acceleration Module
 * System clock
 * < 20MHz         -> MAMTIM = 1 CCLK
 * 20MHz to 40MHz  -> MAMTIM = 2 CCLK
 * 40MHz to 60MHz  -> MAMTIM = 3 CCLK
 * > 60MHz         -> MAMTIM = 4 CCLK
 */
inline void MAM_Init(void){

#ifdef CRYSTAL12MHz
	MAMTIM = 4; // 4 clock fetches
#endif
#ifdef CRYSTAL14745600Hz
	MAMTIM = 3; // 3 clock fetches
#endif
	MAMCR = 2;	// MAM functions fully enabled
}

/**
 * Configure the peripheral devices clock divider
 */
inline void APB_Init(void){
	// peripheral clock = PCLK = CCLK/4
	//APBDIV |= 0x02;
	APBDIV &= ~0x03;
}

/**
 * Timer 2, capture pins as event counter
 */
inline void pulses_in_init(void){


}

/**
 * I2C 1
 */
inline void i2c_init(void){

}

/**
 * ADC0 Configured in BURST mode
 */
inline void adc_init(void){

	// Set the pin function
	PINSEL1 |= 0x3 << 12; // AD0.0
	PINSEL1 |= 0x3 << 14; // AD0.1
	PINSEL1 |= 0x3 << 16; // AD0.2
	PINSEL0 |= 0x3 << 20; // AD0.3
	PINSEL0 |= 0x3 << 22; // AD0.4

	// ADC setup
	ADCR |= 0x01 << 16; // Start A/D Conversion in burst mode
	ADCR |= 0x03 << 8; // 3,75MHz for adc if pclk=15MHz; 3,6864MHz if pclk=14.7456MHz
	ADCR |= 0x1F; // Read AD0.0 - AD0.4
	ADCR |= 0x01 << 21; // The ADC is operational
}

/**
 * Timer 1
 */
inline void pwm_out_init(void){

}

/**
 * Set up the protocol using UART1 to communicate with TS-7260
 *
 * 8N1 (8 data, Non parity, 1 stop)
 * 115200 bps
 *
 * UARTn_baudrate = PCLK / ( 16 * ( 256 * UnDLM + UnDLL) * ( 1 + DivAddVal/MulVal))
 * ou UARTn_baudrate = PCLK / ( 16 * ( 256 * UnDLM + UnDLL))
 * 115131 = 15MHz / ( 16 * ( 256*0 + 6) * ( 1 + 5/14) )
 * 115200 = 14.7456MHz / ( 16 * ( 256*0 + 8) )
 *
 * Set UART1 interrupt to the third slot in the vectored interrupts.
 */
static inline void protocol_init(void){
	volatile char dummy;

	PINSEL0 |= 0x05 << 16; // Set the pins function
	U1FCR    = 0x07; // FIFOControlRegister, Tx, Rx FIFO Reset and FIFO enable, Rx trigger = 1
	U1LCR 	 = 0x83; // DivisorLatchAccessBit = 1,  UART 8N1, allow access to divider-latches

#ifdef CRYSTAL12MHz
	U1DLL	 = 0x06; // DivisorLatchLow bit
	U1DLM	 = 0x00; // DivisorLatchHigh bit
	U1FDR	|= 0x05; // DivAddVal
	U1FDR	|= 0x0E << 4; // MulVal = 14
#endif
#ifdef CRYSTAL14745600Hz
	U1DLL	 = 0x08; // DivisorLatchLow bit
	U1DLM	 = 0x00; // DivisorLatchHigh bit
#endif

	U1LCR	 = 0x03; // DivisorLatchAccessBit = 0,  UART 8N1, forbid access to divider-latches

	VICVectAddr4 = (unsigned int) &protocol_in; //Setting the interrupt handler location to the first vectored interruption slot
	VICVectCntl4 = 0x27; //Vectored Interrupt slot 2 enabled with source #7 (UART1)
	VICIntEnable |= 0x00000080; //source #7 enabled as FIQ or IRQ

	cmd_out.i = 0;
	cmd_in.i = 0;

	dummy = U1IIR;   // Read IrqID - Required to Get Interrupts Started
	U1IER = 1;       // Enable UART1 RX (and THRE Interrupts)
}

/**
 *
 */
void protocol_in(void){
	volatile char dummy;
	volatile char iir;

	// TODO: disable interruptions

	// Repeat while there is at least one interrupt source.
	while (((iir = U1IIR) & 0x01) == 0) {
		switch (iir & 0x0E) {
		case 0x06: // Receive Line Status
			dummy = U1LSR; // Just clear the interrupt source
			break;

		case 0x04: // Receive Data Available
		case 0x0C: // Character Time-Out
			cmd_in.buff[cmd_in.i] = U1RBR;

			if (cmd_in.buff[cmd_in.i] == 'e'/*END_CMD*/) {
				if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'y'/*SYNC*/) {
					// Send IR info
					for (int i = OPTICAL_SENSOR_0; i <= OPTICAL_SENSOR_4; i++) {
						cmd_out.buff[0] = i;
						cmd_out.buff[1] = get_ir_sensor_data(i);
						cmd_out.buff[2] = END_CMD;
						cmd_out.buff[3] = '\n';
						cmd_out.i = 4;
						protocol_out_cmd();
					}
					// Send Encoders info
					for (int i = ENCODER_L; i <= ENCODER_R; i++) {
						int count = get_encoder_count(i);
						cmd_out.buff[0] = i;
						cmd_out.buff[1] = (count >> 0x8) & 0xFF;
						cmd_out.buff[2] = count & 0xFF;
						cmd_out.buff[3] = END_CMD;
						cmd_out.buff[4] = '\n';
						cmd_out.i = 5;
						protocol_out_cmd();
					}
				} else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 's'/*STOP*/) {
					//protocol_out_cmd("STOP\n");
				} else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'l'/*LEFT_WHEEL*/) {
					//out_cmd("LEFT_WHEEL\n");
				} else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'r'/*RIGHT_WHEEL*/) {
					//protocol_out_cmd("RIGHT_WHEEL\n");
				}
			}
			cmd_in.i = (cmd_in.i + 1) & (CMD_BUFF_SIZE-1);
			break;

		case 0x02: // THRE Interrupt, transmit interrupt
			U1THR = dummy; // Just clear the interrupt source
			break;

		case 0x00: // Modem Interrupt
			dummy = U1MSR; // Just clear the interrupt source
			break;

		default:
			break;
		}
	}

	// TODO: enable interruptions

	VICVectAddr = 0;
}

/**
 *
 */
void error(void){
	log_string("irq error");
}

/**
 * Return the value read from the i'th sensor
 */
int get_ir_sensor_data(unsigned short i) {

	int val = 0;
	switch (i) {
	case OPTICAL_SENSOR_0:
		while(ADDR0 & ((0x1 << 31) == 0));
		val = (ADDR0 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case OPTICAL_SENSOR_1:
		while(ADDR1 & ((0x1 << 31) == 0));
		val = (ADDR1 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case OPTICAL_SENSOR_2:
		while(ADDR2 & ((0x1 << 31) == 0));
		val = (ADDR2 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case OPTICAL_SENSOR_3:
		while(ADDR3 & ((0x1 << 31) == 0));
		val = (ADDR3 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case OPTICAL_SENSOR_4:
		while(ADDR4 & ((0x1 << 31) == 0));
		val = (ADDR4 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	}

	return val;
}



/**
 * Return the value read from the i'th sensor
 */
int get_encoder_count(unsigned short i) {
	int val = 0;
	return val;
}

/**
 *
 */
static void protocol_out_cmd(){
	for (unsigned short i = 0; i < cmd_out.i; i++)
		protocol_out_char(cmd_out.buff[i]);
}

/**
 *
 */
static void protocol_out_char(char c){
	U1THR = c;     // TransmitHoldingRegister , DivisorLatchAccessBit must be 0 to transmit
	while(!(U1LSR & 0x40));
}

//void out_str(const char *s){
//	while(*s){
//		if(*s == '\n')
//			out_char('\r'); // \n + \r = new line
//		protocol_out_char(*s);
//		s++;
//	}
//}



//inline void positioning_init(void){
//    //ADC
//	PINSEL1 |= 0x0000F000; // Set the pin function
//
//	//FIQ
//	VICIntSelect |= 0x08000000;//Timer 3 as FIQ
//	VICIntEnable |= 0x08000000;//source #27 enabled as FIQ or IRQ
//
//	//TIMER
//	T3MR0 = 10000;//10000;//5ms -> 200Hz
//	T3MCR = 0x03;//reset and interrupt on match0
//	T3PC = 0;//Prescale = 0
//	T3PR = 0x1C;//Prescale increments i 30 cclk cycles
//	T3TC = 0;//Reset T3
//	T3TCR = 1;//enable T3
//	//T3TC = T2TC;
//}

//static inline int ADC0_Read(void){
//	int i;
//	ADCR |= 0x01200601; // Start A/D Conversion Enabled, No Burst, 4,28MHz se pclk=30MHz
//	while(!(ADSTAT & 0x00000001)); // Wait for end of A/D Conversion
//	i = ADDR0; // Read A/D Data Register
//	ADCR &= 0xFEDFF9FE; // Stop A/D Conversion
//	return (i >> 6); // bit 6:15 is 10 bit AD value
//}
//
//static inline int ADC1_Read(void){
//	int i;
//	ADCR |= 0x01200602; // Start A/D Conversion Enabled, No Burst, 4,28MHz se pclk=30MHz
//	while(!(ADSTAT & 0x00000002)); // Wait for end of A/D Conversion
//	i = ADDR1; // Read A/D Data Register
//	ADCR &= 0xFEDFF9FD; // Stop A/D Conversion
//	return (i >> 6); // bit 6:15 is 10 bit AD value
//}

//int count1 = 0;
//void update_position(void){
//	if(calcount_g>0){//calibration
//		calx += ADC0_Read();
//		posx += ((velx*10) << 10) - 1;//only to let it in the same situation as i normal circumstances
//		caly += ADC1_Read();
//		posy += ((vely*10) << 10) - 1;//only to let it i the same situation as in normal circumstances
//		if(calcount_g == 1){
//			//calx >>= 1;
//			//caly >>= 1;
//			posx = posy = 0;
//			log_string("foi");
//		}
//		calcount_g--;
//	}else{
//		velx = (ADC0_Read() << 10) - calx;
//		posx += (velx*10);// fixed point with 5 hexa
//		vely = (ADC1_Read() << 10) - caly;
//		posy += (vely*10);// fixed point with 5 hexa
//		/*if(count1 == 35){
//			log_string("x: ");
//			log4bytes(posx);
//			log_string("\ty: ");
//			log4bytes(posy);
//			log_string("\n");
//			count1=0;
//		}else
//			count1++;*/
//	}
//	T3IR |= 0x01;//reset interruption
//	VICVectAddr = 0;
//}
//
//void pwm_in_handler(void){
//	const unsigned short ir = T1IR;
//	//capture 1.2 and 1.3
//	if(ir & 0x40){//CAP1.2 pwm0
//		if(T1CCR & 0x40){//rising edge
//			tmp0 = T1CR2;
//			T1CCR &= ~0x40;//interrupt disabled for the rising edge
//			T1CCR |= 0x80;//interrupt enabled for the falling edge
//		}else{//falling
//			unsigned short tmp = T1CR2 - tmp0;
//			if(tmp > 900 && tmp < 2200){
//				if(calcountx>0){
//					calpx += tmp;
//					if(calcountx ==1){
//						calpx <<= 3;
//					}
//					calcountx--;
//				}else{
//					int tmp2 = tmp;
//					tmp2 <<= 11;
//					tmp2 -= calpx;
//					vposx += tmp2;//fixed point 5 hexa
//					//pwm1 measured
//					//pwm0in = tmp - (calpx >> 11);
//				}
//			}
//			T1CCR &= ~0x80;
//			T1CCR |= 0x40;
//		}
//		T1IR |= 0x40; //reset interruption
//	}else{//CAP1.3 pwm1
//		if(T1CCR & 0x200){//rising edge
//			tmp1 = T1CR3;
//			T1CCR &= ~0x200;
//			T1CCR |= 0x400;
//		}else{//falling
//			unsigned short tmp = T1CR3 - tmp1;
//			if(tmp > 900 && tmp < 2200){
//				if(calcounty>0){
//					calpy += tmp;
//					if(calcounty==1){
//						calpy <<= 3;
//					}
//					calcounty--;
//				}else{
//					int tmp2 = tmp;
//					tmp2 <<= 11;
//					tmp2 -= calpy;
//					vposy += tmp2;
//					//pwm1 measured
//					//pwm1in = tmp - (calpy >> 11);
//				}
//			}
//			T1CCR &= ~0x400;
//			T1CCR |= 0x200;
//		}
//		T1IR |= 0x80; //reset interruption
//	}
//	VICVectAddr = 0;
//}
//
//void pid(void){
	//pwm0out = 499;
	//pwm1out = 499;
	//if(calcount_g == 0){
	/*	diffx = posx - vposx;//fixed point 5 hexa
		diffy = posy - vposy;

		int tmpx = diffx >> 20;//4 hexa places
		int tmpy = diffy >> 20;

		log4bytes(tmpx);
		if(tmpx>500){
			tmpx = 500;
		}else if(tmpx<-500){
			tmpx = -500;
		}
		if(tmpy>500){
			tmpy = 500;
		}else if(tmpy<-500){
			tmpy = -500;
		}


		//log4bytes(posx);
		//log_string("P");

		if(pwm0out == 999)
			pwm0out = 0;
		else
			pwm0out++;

		if(pwm1out == 999)
			pwm1out = 0;
		else
			pwm1out++;
	*/
//		T2MR0 = 18999 - tmpx;//pwm0out;//set pwm0 output
//		T2MR1 = 18999 - tmpy;//pwm1out;//set pwm1 output
	//}

	//T2MR0 = 18000 + pwm0in;//pwm0out;//set pwm0 output
	//T2MR1 = 18000 + pwm1in;//pwm1out;//set pwm1 output
//
//	log_string("p");
//	T2IR |= 0x04;
//	VICVectAddr = 0;
//}

/*inline void pwm_out_init(void){
	PINSEL0 |= 0x00028000; //Output pins set for MAT2.0 and MAT2.1

	VICVectAddr0 = (unsigned int) &pid; //Setting the interrupt handler location
	//to the first vectored interruption slot
	VICVectCntl0 = 0x3A; //Vectored Interrupt slot 0 enabled with source #26 (TIMER2)
	VICIntEnable |= 0x04000000; //source #26 enabled as FIQ or IRQ

	T2MR0 = 18500;//20000-1100 -> pwm0
	T2MR1 = 18500;//20000-1900 -> pwm1
	T2MR2 = 15000;// PID
	T2MR3 = 20000;//20ms -> period

	T2MCR |= 0x0440;//reset on match3 and interrupt on match2
	T2PWMCON |= 0x03; //set match0 and match1 as pwm;

	T2PC = 0;//Prescale = 0
	T2PR = 0x1C;//Prescale increments i 30 cclk cycles
	T2TC = 0;//Reset T2
	T2TCR = 1;//enable T2
}

inline void pwm_in_init(void){
	PINSEL1 |= 0x000000028;//set pin capture function CAP1.2 - p0.17, CAP1.3 p0.18

	VICVectAddr1 = (unsigned int) &pwm_in_handler; //Setting the interrupt handler location
	//to the second vectored interruption slot
	VICVectCntl1 = 0x25; //Vectored Interrupt slot 1 enabled with source #5 (TIMER1)
	VICIntEnable |= 0x20; //source #5 enabled as FIQ or IRQ

	T1CCR |= 0x0B40;//interrupt for Capture 2,3 rising edges

	T1PC = 0; //Prescale = 0;
	T1PR = 0x1C; //Prescale increments i 30 cclk cycles
	T1TC = 0; // reset T1
	T1TCR = 1; //enable T1
}*/



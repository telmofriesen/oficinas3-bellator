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
void __attribute__ ((interrupt("IRQ"))) encoder_pulse_in(void);
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

/* getters and setters */
int get_ir_sensor_data(unsigned short i);
int get_encoder_count(unsigned short i);
void set_wheel_pwm(unsigned short wheel, unsigned short val);

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
static int encoder_count[2] = { 0, 0};

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

	pulses_in_init(); // start counting pulses from the encoder	| Timer 2, Int prirority 0
	i2c_init(); // start the communication with the IMU			| Int prirority 1
	adc_init(); // start reading the IR sensor signals			| Burst mode, no interruption
	pwm_out_init(); // start pwm for the H bridges				| Timer 0 and Timer 1 operating in PWM mode
	protocol_init(); // start the communication protocol		| Int prirority 3

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
 * Timer 2, capture pins generating interruptions for event counter
 * The timer has no function, except for the interrupt generation.
 */
inline void pulses_in_init(void){

	// Set the pin function
	PINSEL1 |= 0x2 << 22; // CAP2.0
	PINSEL1 |= 0x2 << 24; // CAP2.1

	// Timer Setup
	T2CCR |= 0x5 << 0; // capture and interrupt on CAP2.0 rising edge
	T2CCR |= 0x5 << 3; // capture and interrupt on CAP2.1 rising edge
	T2TCR = 1; //enable T2

	VICVectAddr0 = (unsigned int) &encoder_pulse_in; //Setting the interrupt handler location
	VICVectCntl0 = 0x3A; //Vectored Interrupt slot enabled with source #26 (TIMER2)
	VICIntEnable |= 0x1 << 26; //source #26 enabled as FIQ or IRQ
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
 * Timer 0,1, 200Hz, at least 76 levels to comply with the old version
 * Timer 0 -> left wheel
 * Timer 1 -> right wheel
 */
inline void pwm_out_init(void){

	// Set the pin function
	PINSEL1 |= 0x2 << 0;  // MAT0.2
	PINSEL0 |= 0x2 << 10; // MAT0.1
	PINSEL0 |= 0x2 << 24; // MAT1.0
	PINSEL0 |= 0x2 << 26; // MAT1.1

#ifdef CRYSTAL12MHz
	T0PR = 294; // 255 levels for T2TC in 5ms
	T1PR = 294;
#endif
#ifdef CRYSTAL14745600Hz
	T0PR = 289; // 255 levels for T2TC in 5ms
	T1PR = 289;
#endif

	T0PC = 0; // Prescale = 0
	T1PC = 0;
	T0TC = 0; // Counter = 0
	T1TC = 0;

	T0MCR |= (0x1 << 10); // Reset the counter on MAT0.3
	T1MCR |= (0x1 << 10); // Reset the counter on MAT1.3
	T0MR3 = 255; // MAT0.3 every 255 counts (5ms)
	T1MR3 = 255; // MAT1.3 every 255 counts (5ms)

	T0PWMCON |= (0x1 << 2); // MAT0.2 configured as PWM output
	T0PWMCON |= (0x1 << 1); // MAT0.1 configured as PWM output
	T1PWMCON |= (0x1 << 0); // MAT1.0 configured as PWM output
	T1PWMCON |= (0x1 << 1); // MAT1.1 configured as PWM output

	T0MR2 = 0; // initially LOW
	T0MR1 = 84; // initially LOW
	T1MR0 = 168; // initially LOW
	T1MR1 = 255; // initially LOW

	T0TCR = 1; // enable T0
	T1TCR = 1; // enable T1
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

			if (cmd_in.buff[cmd_in.i] == /*'e'*/ END_CMD) {
				if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == /*'y'*/ SYNC) {
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
				} else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == /*'s'*/ STOP ) {
					// Stop PWMs
					set_wheel_pwm(LEFT_WHEEL,0);
					set_wheel_pwm(RIGHT_WHEEL,0);

				} else if (cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)] == /*'l'*/ LEFT_WHEEL
						|| cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)] == /*'r'*/ RIGHT_WHEEL) {

					/*if (cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)] == 'l') {
						if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'l') {
							set_wheel_pwm(LEFT_WHEEL, 0x00 );
						} else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'm') {
							set_wheel_pwm(LEFT_WHEEL, 0x0E);
						} else {
							set_wheel_pwm(LEFT_WHEEL, 0x7F);
						}
					}

					if (cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)] == 'r') {
						if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'l') {
							set_wheel_pwm(RIGHT_WHEEL, 0x00 );
						} else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == 'm') {
							set_wheel_pwm(RIGHT_WHEEL, 0x0E);
						} else {
							set_wheel_pwm(RIGHT_WHEEL, 0x7F);
						}
					}*/

					set_wheel_pwm(cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)],
							(unsigned short) (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)]));
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
 * Count the encoder pulses using CAP2.0-1 as interrupt sources
 */
void encoder_pulse_in(void) {

	const unsigned short ir = T2IR;

	if (ir & (0x1 << 4)) { //CAP2.0 left encoder
		encoder_count[0]++;
		T2IR |= 0x1 << 4; // reset CAP2.0
	}
	if (ir & (0x1 << 5)) { //CAP2.1 right encoder
		encoder_count[1]++;
		T2IR |= 0x1 << 5; // reset CAP2.1
	}

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
	int val;
	val = encoder_count[i - ENCODER_L];
	encoder_count[i - ENCODER_L] = 0;
	return val;
}

/**
 * Set the output pwm value
 */
void set_wheel_pwm(unsigned short wheel, unsigned short val) {

	if (wheel == LEFT_WHEEL) {
		if (val & PWM_DIR) { // Forward
			T0MR1 = 256;
			T0MR2 = 256 - val;
		} else { // Backwards
			T0MR2 = 256;
			T0MR1 = 256 - val;
		}
	} else if (wheel == RIGHT_WHEEL) {
		if (val & PWM_DIR) { // Forward
			T1MR1 = 256;
			T1MR0 = 256 - val;
		} else { // Backwards
			T1MR0 = 256;
			T1MR1 = 256 - val;
		}
	}
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

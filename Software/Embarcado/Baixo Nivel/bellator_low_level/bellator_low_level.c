/**
 *
 *
 *
 */
/* global defines */
#define CRYSTAL12MHz
//#define CRYSTAL14745600Hz
#define CMD_BUFF_SIZE 32 // has to be a power of two
#define DATA_BUFF_SIZE 256 // has to be a power of two

#define SAMPLE_RATE_1kHZ 0;
#define SAMPLE_RATE_500HZ 1;
#define SAMPLE_RATE_250HZ 2;
#define SAMPLE_RATE_125HZ 4;
#define SAMPLE_RATE_062HZ 8;
#define SAMPLE_RATE_012HZ 4096;

#define SAMPLE_RATE SAMPLE_RATE_125HZ

#define IENABLE \
		asm volatile ( "MRS		LR, SPSR" 		); /* copy SPSR_irq to LR */ \
		asm volatile ( "STMFD	SP!, {LR}"		); /* save SPSR_irq */ \
		asm volatile ( "MSR		CPSR_c, #0x1F"	); /* enable IRQ */ \
		asm volatile ( "STMFD	SP!, {LR}"		); /* save LR */ \

#define IDISABLE \
		asm volatile ( "LDMFD	SP!, {LR}" 		); /* restore LR */ \
		asm volatile ( "MSR		CPSR_c, #0x92"	); /* disable IRQ */ \
		asm volatile ( "LDMFD	SP!, {LR}"		); /* Restore SPSR_irq to LR */ \
		asm volatile ( "MSR		SPSR_cxsf, LR"	); /* copy LR to SPSR_irq */ \


/* includes */
#include "lpc2103.h"
#include "logger.h"
#include "irq.h"
#include "imu.h"
#include "protocol.h"

/* interruptions */
void __attribute__ ((interrupt("FIQ"))) pulse_in(void);
static void __attribute__ ((interrupt("IRQ"))) protocol_in(void);
static void __attribute__ ((interrupt("IRQ"))) sample(void);
static void __attribute__ ((interrupt("IRQ"))) error(void);

/* init functions */
static inline void PLL_Init(void);
static inline void MAM_Init(void);
static inline void APB_Init(void);

static inline void pulses_in_init(void);
static inline void imu_init(void);
static inline void adc_init(void);
static inline void pwm_out_init(void);
static inline void protocol_init(void);
static inline void sampler_init(void);

/* getters and setters */
static inline void get_ir_sensor_data(char * buff);
static inline void get_encoders_count(short * left_encoder, short * right_encoder);
static inline void set_wheel_pwm(unsigned short left_wheel, unsigned short right_wheel);

/* auxiliary functions */
static inline void protocol_out_cmd(void);
static inline void protocol_out_char(char c);

/* data structs defenitions */
struct cmd_buff {
  unsigned int i;
  char buff [CMD_BUFF_SIZE];      // Circular Buffer
};

struct sensors_data {
	short encoder_left, encoder_right;
	char ir_r, ir_mr, ir_m, ir_ml, ir_l;
	char ax_h, ax_l, ay_h, ay_l, az_h, az_l, gx_h, gx_l, gy_h, gy_l, gz_h, gz_l;
	unsigned short timestamp;
};
static struct sensors_data* in_data;
static struct sensors_data* out_data;

/* global variables */
static struct cmd_buff cmd_out = { 0, };
static struct cmd_buff cmd_in = { 0, };
volatile int encoder_count[2] = { 0, 0};
static int sent_encoder_count[2] = { 0, 0};
static int forward_r = 0, forward_l = 0;
unsigned volatile char send_data = 0;
static struct sensors_data sensors_data_buff[DATA_BUFF_SIZE]; // Circular Buffer
unsigned volatile short data_in_pos = 0; // last valid data in
unsigned volatile short data_out_pos = 0; // last data sent
static unsigned short timestamp = 0;
volatile unsigned short tmp;


/**
 * Entry point
 */
int main(void){

	PLL_Init(); // Turn on PLL clock
	MAM_Init(); // Turn on MAM pre-fetcher
	APB_Init(); // Turn on the peripheral devices clock divider

	logger_init(); // uart0
	log_string_debug("iniciando\n");

	enableIRQ(); // Enable interruptions
	enableFIQ();

	imu_init(); // start the IMU								| i2c1 FIQ
	pulses_in_init(); // start counting pulses from the encoder	| Timer 2, FIQ, eint0, FIQ
	adc_init(); // start reading the IR sensor signals			| Burst mode, no interruption
	pwm_out_init(); // start pwm for the H bridges				| Timer 0 and Timer 1 operating in PWM mode, no interruption
	protocol_init(); // start the communication protocol		| uart1, Priority 2
	sampler_init(); // start taking samples at 1kHz				| Timer 3, 1kH, Priority 1

	VICDefVectAddr = (unsigned int) &error;

	while (1) {
		if (send_data) {
			// while data available
			while(data_out_pos != data_in_pos) {

				log_string_debug("sending data");
				// send next data
				data_out_pos = ++data_out_pos % DATA_BUFF_SIZE;

				out_data = &(sensors_data_buff[data_out_pos]);

				// encoders
				cmd_out.buff[0] = (out_data->encoder_left >> 0x8) & 0xFF;
				cmd_out.buff[1] = out_data->encoder_left & 0xFF;
				cmd_out.buff[2] = (out_data->encoder_right >> 0x8) & 0xFF;
				cmd_out.buff[3] = out_data->encoder_right & 0xFF;

				// Infra Red
				cmd_out.buff[4] = out_data->ir_l;
				cmd_out.buff[5] = out_data->ir_ml;
				cmd_out.buff[6] = out_data->ir_m;
				cmd_out.buff[7] = out_data->ir_mr;
				cmd_out.buff[8] = out_data->ir_r;

				// IMU data
				cmd_out.buff[9] = out_data->ax_h;
				cmd_out.buff[10] = out_data->ax_l;
				cmd_out.buff[11] = out_data->ay_h;
				cmd_out.buff[12] = out_data->ay_l;
				cmd_out.buff[13] = out_data->az_h;
				cmd_out.buff[14] = out_data->az_l;
				cmd_out.buff[15] = out_data->gx_h;
				cmd_out.buff[16] = out_data->gx_l;
				cmd_out.buff[17] = out_data->gy_h;
				cmd_out.buff[18] = out_data->gy_l;
				cmd_out.buff[19] = out_data->gz_h;
				cmd_out.buff[20] = out_data->gz_l;

				// Timestamp
				cmd_out.buff[21] = (out_data->timestamp >> 8) & 0xFF;
				cmd_out.buff[22] = out_data->timestamp & 0xFF;

				// end cmd
				cmd_out.buff[23] = END_CMD;
				cmd_out.buff[24] = '\n';
				cmd_out.i = 25;

				protocol_out_cmd();
			}

			send_data = 0;
		}
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
static inline void PLL_Init(void){

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
static inline void MAM_Init(void){

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
 * for PCLK = CCLK/4
 */
static inline void APB_Init(void){
	// peripheral clock = PCLK = CCLK/4
	//APBDIV |= 0x02;
	APBDIV &= ~0x03;
}

/**
 * Timer 2, capture pins generating FIQs for event counter
 * The timer has no function, except for the interrupt generation.
 * and EINT0 also generating FIQs for event counter
 * CAP20 - Left encoder
 * CAP21 - Left encoder
 * CAP22 - Right encoder
 * EINT0 - Right encoder
 */
static inline void pulses_in_init(void){

	log_string_debug(">> pulses_in_init\n");

	// Set the pin function
	PINSEL1 |= 0x1 << 0;  // EINT0
	PINSEL1 |= 0x2 << 22; // CAP2.0
	PINSEL1 |= 0x2 << 24; // CAP2.1
	PINSEL1 |= 0x2 << 26; // CAP2.2

	// EINT setup
	EXTMODE |= 0x1 << 0; // EINT is edge sensitive
	EXTPOLAR |= 0x1 << 0; // EINT is rising edge sensitive
	EXTINT |= 0x1 << 0; // reset EINT0

	// Timer Setup
	T2CCR |= 0x5 << 0; // capture and interrupt on CAP2.0 rising edge
	T2CCR |= 0x5 << 3; // capture and interrupt on CAP2.1 rising edge
	T2CCR |= 0x5 << 6; // capture and interrupt on CAP2.2 rising edge
	T2TCR = 1; //enable T2

	// Enable the interrupts
	VICIntSelect |= 0x1 << 14;// EINT2 as FIQ
	VICIntEnable |= 0x1 << 14; //source #14 enabled as FIQ or IRQ

	VICIntSelect |= 0x1 << 26;// Timer 2 as FIQ
	VICIntEnable |= 0x1 << 26; // source #26 enabled as FIQ or IRQ

	log_string_debug("<< pulses_in_init\n");
}

/**
 * Start i2c communication
 * Configure MPU
 * Setup eint2 with priority 1 for data ready interrupt
 */
static inline void imu_init(void){

	log_string_debug(">> imu_init\n");

	// start the communication with the IMU
	i2c_init();

	// configure mpu and start taking samples
	mpu_init();

	// DATA READY INTERRUPT WAS NOT USED
	// Configure data ready interrupt
	// Set the pin function
	//PINSEL0 |= 0x1 << 30;  // EINT2

	// EINT setup
	//EXTMODE |= 0x1 << 2; // EINT2 is edge sensitive
	//EXTPOLAR |= 0x1 << 2; // EINT2 is rising edge sensitive
	//EXTINT |= 0x1 << 2; // reset EINT2

	//VICVectAddr1 = (unsigned int) &imu_data_ready; //Setting the interrupt handler location
	//VICVectCntl1 = 0x30; //Vectored Interrupt slot enabled with source #16 (EINT2)
	//VICIntEnable |= 0x1 << 16; //source #16 enabled as FIQ or IRQ

	log_string_debug("<< imu_init\n");
}

/**
 * ADC0 Configured in BURST mode
 */
static inline void adc_init(void){

	log_string_debug(">> adc_init\n");

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

	log_string_debug("<< adc_init\n");
}

/**
 * Timer 0,1, 200Hz, at least 76 levels to comply with the old version
 * Timer 0 -> left wheel
 * Timer 1 -> right wheel
 * PCLK = 15MHz or 14.7456MHz
 */
static inline void pwm_out_init(void){

	log_string_debug(">> pwm_out_init\n");

	// Set the pin function
	PINSEL0 |= 0x2 << 6;  // MAT0.0
	PINSEL0 |= 0x2 << 10; // MAT0.1
	PINSEL0 |= 0x2 << 24; // MAT1.0
	PINSEL0 |= 0x2 << 26; // MAT1.1

#ifdef CRYSTAL12MHz
	T0PR = 293; // 255 levels for T2TC in 5ms
	T1PR = 293; // TC increments every PR + 1 PCLKs
#endif
#ifdef CRYSTAL14745600Hz
	T0PR = 288; // 255 levels for T2TC in 5ms
	T1PR = 288;
#endif

	T0PC = 0; // Prescale = 0
	T1PC = 0;
	T0TC = 0; // Counter = 0
	T1TC = 0;

	T0MCR |= (0x1 << 10); // Reset the counter on MAT0.3
	T1MCR |= (0x1 << 10); // Reset the counter on MAT1.3
	T0MR3 = 255; // MAT0.3 every 255 counts (5ms)
	T1MR3 = 255; // MAT1.3 every 255 counts (5ms)

	T0PWMCON |= (0x1 << 0); // MAT0.0 configured as PWM output
	T0PWMCON |= (0x1 << 1); // MAT0.1 configured as PWM output
	T1PWMCON |= (0x1 << 0); // MAT1.0 configured as PWM output
	T1PWMCON |= (0x1 << 1); // MAT1.1 configured as PWM output

	T0MR0 = 256; // initially LOW
	T0MR1 = 256; // initially LOW
	T1MR0 = 256; // initially LOW
	T1MR1 = 256; // initially LOW

	T0TCR = 1; // enable T0
	T1TCR = 1; // enable T1

	log_string_debug("<< pwm_out_init\n");
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
 * Set UART1 interrupt to the second slot in the vectored interrupts.
 */
static inline void protocol_init(void){

	log_string_debug(">> protocol_init\n");

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

	VICVectAddr2 = (unsigned int) &protocol_in; //Setting the interrupt handler location to the 2th vectored interruption slot
	VICVectCntl2 = 0x27; //Vectored Interrupt slot 2 enabled with source #7 (UART1)
	VICIntEnable |= 0x00000080; //source #7 enabled as FIQ or IRQ

	cmd_out.i = 0;
	cmd_in.i = 0;

	tmp = U1IIR;   // Read IrqID - Required to Get Interrupts Started
	U1IER = 1;       // Enable UART1 RX (and THRE Interrupts)

	log_string_debug("<< protocol_init\n");
}

/**
 * PCLK = 15MHz or 14.7456MHz
 */
static inline void sampler_init(void){
	log_string_debug(">> sampler_init\n");

	// set pre scale for sample rate
	T3PR = SAMPLE_RATE; // Increment the timer every PCLK

	T3PC = 0;
	T3TC = 0; // Counter = 0

	T3MCR |= (0x1 << 0); // Interrupt on MAT3.0
	T3MCR |= (0x1 << 1); // Reset the counter on MAT3.0

#ifdef CRYSTAL12MHz
	T3MR0 = 15000; // MAT3.0 every 15000/(SAMPLE_RATE + 1) counts (1ms/(SAMPLE_RATE + 1))
#endif
#ifdef CRYSTAL14745600Hz
	T3MR0 = 14746; // MAT3.0 every 14746/(SAMPLE_RATE + 1) counts (1.000027127ms/(SAMPLE_RATE + 1))
#endif

	VICVectAddr1 = (unsigned int) &sample; //Setting the interrupt handler location
	VICVectCntl1 = 0x3B; //Vectored Interrupt slot enabled and with source #27 (TIMER3)
	VICIntEnable |= 0x1 << 27; //source #27 enabled as FIQ or IRQ

	T3TCR = 1; // enable T3

	log_string_debug("<< sampler_init\n");
}

/**
 * Communication Protocol state machine implementation;
 * This is triggered on uart1 interruption
 * This handles the following commands
 *
 */
static void protocol_in(void){

	log_string_debug(">> protocol_in\n");

	// Repeat while there is at least one interrupt source.
	while (((tmp = U1IIR) & 0x01) == 0) {
		switch (tmp & 0x0E) {
		case 0x06: // Receive Line Status
			tmp = U1LSR; // Just clear the interrupt source
			break;

		case 0x04: // Receive Data Available
		case 0x0C: // Character Time-Out
			cmd_in.buff[cmd_in.i] = U1RBR;

			if (cmd_in.buff[cmd_in.i] == END_CMD) {
				// ENGINES
				if (cmd_in.buff[(cmd_in.i-3) & (CMD_BUFF_SIZE-1)] == ENGINES) {
					log_string_debug("ENGINES\n");

					set_wheel_pwm((unsigned short) (cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)]),
								(unsigned short) (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)]));

					cmd_in.buff[(cmd_in.i-3) & (CMD_BUFF_SIZE-1)] = 0;
					cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)] = 0;
					cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] = 0;
					cmd_in.buff[(cmd_in.i) & (CMD_BUFF_SIZE-1)] = 0;
				}
				// SYNC
				else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == SYNC) {
					log_string_debug("SYNC\n");

					send_data = 1;

					cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] = 0;
					cmd_in.buff[(cmd_in.i) & (CMD_BUFF_SIZE-1)] = 0;
				}

				else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == TEST) {
					log_string_debug("TEST\n");

					short left, right;
					get_encoders_count(&left, &right);
					cmd_out.buff[0] = (left >> 0x8) & 0xFF; ;
					cmd_out.buff[1] = left & 0xFF;
					cmd_out.buff[2] = (right >> 0x8) & 0xFF; ;
					cmd_out.buff[3] = right & 0xFF;
					cmd_out.buff[4] = END_CMD;
					cmd_out.buff[5] = '\n';
					cmd_out.i = 6;

					protocol_out_cmd();

					cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] = 0;
					cmd_in.buff[(cmd_in.i) & (CMD_BUFF_SIZE-1)] = 0;
				}
			}

			cmd_in.i = (cmd_in.i + 1) & (CMD_BUFF_SIZE-1);
			break;

		case 0x02: // THRE Interrupt, transmit interrupt
			U1THR = tmp; // Just clear the interrupt source
			break;

		case 0x00: // Modem Interrupt
			tmp = U1MSR; // Just clear the interrupt source
			break;

		default:
			break;
		}
	}

	log_string_debug("<< protocol_in\n");

	VICVectAddr = 0;
}

/**
 * Hall the FIQ interrupts should be handled as fast as possible.
 *
 * Count the encoder pulses using CAP2.0-2 and EINT0 as interrupt sources
 *
 * Handle i2c requests
 */
void pulse_in(void) {

	log_string_debug("p\n");
	//log_string_debug(">> pulse_in\n");

	tmp = T2IR;

	// pulses in int
	if (tmp & (0x1 << 4)) { //CAP2.0 left encoder
		//log_string_debug("FIQ2\n");
		forward_l--;
		T2IR |= 0x1 << 4; // reset CAP2.0
	}
	if (tmp & (0x1 << 5)) { //CAP2.1 right encoder
		//log_string_debug("FIQ3\n");
		forward_r++;
		if (forward_r > 0)
			encoder_count[1]++;
		else
			encoder_count[1]--;
		T2IR |= 0x1 << 5; // reset CAP2.1
	}
	if (tmp & (0x1 << 6)) { //CAP2.2 right encoder
		//log_string_debug("FIQ4\n");
		forward_r--;
		T2IR |= 0x1 << 6; // reset CAP2.2
	}
	if (EXTINT & 0x1 << 0) { // EINT0 left encoder
		//log_string_debug("FIQ1\n");
		forward_l++;
		if (forward_l > 0)
			encoder_count[0]++;
		else
			encoder_count[0]--;
		EXTINT |= 0x1 << 0; // reset EINT0
	}

	//log_string_debug("<< pulse_in\n");

	VICVectAddr = 0;
}

/**
 * DATA READY INTERRUPT WAS NOT USED
 * Read IMU data when triggered by EINT2
 */
/*void imu_data_ready(void) {

	//log_string_debug(">> imu_data_ready\n");

	imu_data_available = 1;

	EXTINT |= 0x1 << 2; // reset EINT2

	//log_string_debug("<< imu_data_ready\n");

	VICVectAddr = 0;
}
*/

/**
 * Sample sensors every 1ms (1kHz)
 * data_in_pos -
 */
static void sample(void) {
	tmp = T3IR;
	if(tmp & 0x1) { // MAT3.0
		log_string_debug(">> sample\n");

		log_string_debug("IENABLE\n");
		// enable nested interrupts for i2c and FIQ
		T3IR |= 0x1 << 0; // clear MAT3.0 interrupt
		IENABLE

		// next position in buffer
		data_in_pos = (++data_in_pos) % DATA_BUFF_SIZE;

		// check for overflow
		if (data_in_pos == data_out_pos) {
			log_string_warning("LPC overflow\n");
			// the oldest data will be overwritten
			data_out_pos = (++data_out_pos) % DATA_BUFF_SIZE;
		}

		// read data and put on local circular buffer
		in_data = &(sensors_data_buff[data_in_pos]);

		// read encoder counts
		get_encoders_count(&(in_data->encoder_left), &(in_data->encoder_right));

		// read the last IMU data
		mpu_get_motion6(&(in_data->ax_h));

		// read IR data
		get_ir_sensor_data(&(in_data->ir_l));

		in_data->timestamp = timestamp++;

		log_string_debug("IDISABLE\n");
		IDISABLE

		log_string_debug("<< sample\n");

		T3IR |= 0x1 << 0; // clear MAT3.0 interrupt
	}

	VICVectAddr = 0;
}

/**
 *
 */
static void error(void){
	log_string_error("irq error");
}

/**
 * Return the value read from the i'th sensor
 * buff
 * ir_l, ir_ml, ir_m, ir_mr, ir_r
 *
 */
static inline void get_ir_sensor_data(char * buff) {

	unsigned short val;

	while(ADDR0 & ((0x1 << 31) == 0));
	val = (ADDR0 >> 6) & 0x3FF;
	val >>= 0x2; // they want a value from 1 to 255
	val += (val == 0);
	*buff = (char) val;

	while(ADDR1 & ((0x1 << 31) == 0));
	val = (ADDR1 >> 6) & 0x3FF;
	val >>= 0x2; // they want a value from 1 to 255
	val += (val == 0);
	*(buff+1) = (char) val;

	while(ADDR2 & ((0x1 << 31) == 0));
	val = (ADDR2 >> 6) & 0x3FF;
	val >>= 0x2; // they want a value from 1 to 255
	val += (val == 0);
	*(buff+2) = (char) val;

	while(ADDR3 & ((0x1 << 31) == 0));
	val = (ADDR3 >> 6) & 0x3FF;
	val >>= 0x2; // they want a value from 1 to 255
	val += (val == 0);
	*(buff+3) = (char) val;

	while(ADDR4 & ((0x1 << 31) == 0));
	val = (ADDR4 >> 6) & 0x3FF;
	val >>= 0x2; // they want a value from 1 to 255
	val += (val == 0);
	*(buff+4) = (char) val;
}



/**
 * Return the count value read from sensors
 */
static inline void get_encoders_count(short * left_encoder, short * right_encoder) {

	int val;
	val = encoder_count[ENCODER_L - ENCODER_L];
	*left_encoder = val - sent_encoder_count[ENCODER_L - ENCODER_L];
	sent_encoder_count[ENCODER_L - ENCODER_L] = val;

	val = encoder_count[ENCODER_R - ENCODER_L];
	*right_encoder = val - sent_encoder_count[ENCODER_R - ENCODER_L];
	sent_encoder_count[ENCODER_R - ENCODER_L] = val;
}

/**
 * Set the output pwm value
 */
static inline void set_wheel_pwm(unsigned short left_wheel, unsigned short right_wheel) {

//	if (left_wheel > 8)
//		left_wheel -= 8;
//	else
//		left_wheel = 0;

	if (right_wheel & PWM_DIR) { // Forward
		T0MR2 = 256;
		T0MR1 = 256 - (right_wheel & ~PWM_DIR)*2;
	} else { // Backwards
		T0MR1 = 256;
		T0MR2 = 256 - right_wheel*2;
	}

	if (left_wheel & PWM_DIR) { // Forward
		T1MR0 = 256;
		T1MR1 = 256 - (left_wheel & ~PWM_DIR)*2;
	} else { // Backwards
		T1MR1 = 256;
		T1MR0 = 256 - left_wheel*2;
	}
}

/**
 *
 */
static inline void protocol_out_cmd(){
	for (unsigned short i = 0; i < cmd_out.i; i++)
		protocol_out_char(cmd_out.buff[i]);
}

/**
 *
 */
static inline void protocol_out_char(char c){
	U1THR = c;     // TransmitHoldingRegister , DivisorLatchAccessBit must be 0 to transmit
	while(!(U1LSR & 0x40));
}

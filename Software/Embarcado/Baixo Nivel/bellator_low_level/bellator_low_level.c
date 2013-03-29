/**
 *
 *
 *
 */
/* global defines */
#define CRYSTAL12MHz
//#define CRYSTAL14745600Hz
#define CMD_BUFF_SIZE 32 // has to be a power of two
#define IMU_BUFF_SIZE 256 // has to be a power of two

//#define ERROR
//#define WARNING
//#define DEBUG
//#define DEBUG_I2C
//#define DEBUG_MPU

/* includes */
#include "lpc2103.h"
#include "logger.h"
#include "logger.c"
#include "irq.h"
#include "irq.c"
#include "i2c.h"
#include "i2c.c"
#include "protocol.h"
#include "mpu6050.h"
#include "mpu6050.c"

/* interruptions */
void __attribute__ ((interrupt("FIQ"))) encoder_pulse_in_isr(void);
void __attribute__ ((interrupt("IRQ"))) protocol_in(void);
void __attribute__ ((interrupt("IRQ"))) imu_data_ready(void);
void __attribute__ ((interrupt("IRQ"))) error(void);

/* init functions */
inline void PLL_Init(void);
inline void MAM_Init(void);
inline void APB_Init(void);

inline void pulses_in_init(void);
inline void imu_init(void);
inline void adc_init(void);
inline void pwm_out_init(void);
static inline void protocol_init(void);

/* getters and setters */
int get_ir_sensor_data(unsigned short i);
int get_encoder_count(unsigned short i);
void set_wheel_pwm(unsigned short left_wheel, unsigned short right_wheel);

/* auxiliary functions */
static void protocol_out_cmd(void);
static void protocol_out_char(char c);

/* data structs defenitions */
struct cmd_buff {
  unsigned int i;
  char buff [CMD_BUFF_SIZE];      // Circular Buffer
};

struct imu_data {
	char ax_h, ax_l, ay_h, ay_l, az_h, az_l, gx_h, gx_l, gy_h, gy_l, gz_h, gz_l;
	unsigned short timestamp;
};

/* global variables */
static struct cmd_buff cmd_out = { 0, };
static struct cmd_buff cmd_in = { 0, };
static unsigned int encoder_count[2] = { 0, 0};
static unsigned int sent_encoder_count[2] = { 0, 0};
static unsigned volatile char imu_data_available = 0;
static struct imu_data imu_data_buff[IMU_BUFF_SIZE]; // Circular Buffer
static unsigned short imu_data_in_pos = 0;
static unsigned short imu_data_out_pos = 0;
static unsigned short timestamp = 0;

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

	pulses_in_init(); // start counting pulses from the encoder	| Timer 2, FIQ, eint0, FIQ
	imu_init(); // start the IMU									| i2c1 Priority 0, eint2 Priority 1
	adc_init(); // start reading the IR sensor signals			| Burst mode, no interruption
	pwm_out_init(); // start pwm for the H bridges				| Timer 0 and Timer 1 operating in PWM mode, no interruption
	protocol_init(); // start the communication protocol			| uart1, Priority 2

	enableFIQ();

	VICDefVectAddr = (unsigned int) &error;

	//set_wheel_pwm(RIGHT_WHEEL,0x7F);
	//set_wheel_pwm(LEFT_WHEEL,0x7F);

	// imu_data_in_pos -> aponta para o ultimo dado valido
	// imu_data_out_pos -> aponta para o ultimo dado enviado
	while(1){
		if(imu_data_available) {
			char source;
			// find out where the interruption came from
			mpu_clear_interrupt(&source);

			log_string_debug("src:");
			log_byte_debug(source);
			log_string_debug("\n");

			if (source & (0x1 << MPU6050_INTERRUPT_DATA_RDY_BIT)) {
				log_string_debug("dataready: ");

				// find out how many entries are in the fifo
				int size;
				mpu_get_FIFO_size(&size);

				log_int_debug(size);
				log_string_debug(" bytes\n");

				// try to clear the fifo before an overflow occurs
				if (size > 840 || source & (0x1 << MPU6050_INTERRUPT_FIFO_OFLOW_BIT)) {
					log_string_warning("MPU overflow\n");
					mpu_set_FIFO_enabled(0);
					mpu_reset_FIFO();
					mpu_set_FIFO_enabled(1);
					mpu_get_FIFO_size(&size);
				}

				while (size >= 12) {
					size -= 12;

					// next position in buffer
					short imu_data_in_pos_tmp = (imu_data_in_pos + 1) % IMU_BUFF_SIZE;

					// check for overflow
					if (imu_data_in_pos_tmp == imu_data_out_pos) {
						log_string_warning("LPC overflow\n");
						// the oldest data will be overwritten
						imu_data_out_pos = ++imu_data_out_pos % IMU_BUFF_SIZE;
					}

					// read data and put on local circular buffer
					struct imu_data* data;
					data = &(imu_data_buff[imu_data_in_pos_tmp]);

					mpu_get_FIFO_motion6(&(data->ax_h), &(data->ax_l), &(data->ay_h), &(data->ay_l), &(data->az_h), &(data->az_l),
							&(data->gx_h), &(data->gx_l), &(data->gy_h), &(data->gy_l), &(data->gz_h), &(data->gz_l));
					data->timestamp = timestamp++;

//					protocol_out_char(0xFF);
//					protocol_out_char(0xFF);
//					protocol_out_char(data->ax_h);
//					protocol_out_char(data->ax_l);
//					protocol_out_char(data->ay_h);
//					protocol_out_char(data->ay_l);
//					protocol_out_char(data->az_h);
//					protocol_out_char(data->az_l);
//					protocol_out_char(data->gx_h);
//					protocol_out_char(data->gx_l);
//					protocol_out_char(data->gy_h);
//					protocol_out_char(data->gy_l);
//					protocol_out_char(data->gz_h);
//					protocol_out_char(data->gz_l);
//					protocol_out_char(0xFF);
//					protocol_out_char(0xFF);

					imu_data_in_pos = imu_data_in_pos_tmp;
				}
			}

			imu_data_available = 0;
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
 * Timer 2, capture pins generating FIQs for event counter
 * The timer has no function, except for the interrupt generation.
 * and EINT0 also generating FIQs for event counter
 * CAP20 - Left encoder
 * CAP21 - Left encoder
 * CAP22 - Right encoder
 * EINT0 - Right encoder
 */
inline void pulses_in_init(void){

	log_string_debug(">> pulses_in_init\n");

	// Set the pin function
	PINSEL1 |= 0x2 << 22; // CAP2.0
	PINSEL1 |= 0x2 << 24; // CAP2.1
	PINSEL1 |= 0x2 << 26; // CAP2.2
	PINSEL1 |= 0x1 << 0;  // EINT0

	// Timer Setup
	T2CCR |= 0x5 << 0; // capture and interrupt on CAP2.0 rising edge
	T2CCR |= 0x5 << 3; // capture and interrupt on CAP2.1 rising edge
	T2CCR |= 0x5 << 6; // capture and interrupt on CAP2.2 rising edge
	T2TCR = 1; //enable T2

	// EINT setup
	EXTMODE |= 0x1 << 0; // EINT is edge sensitive
	EXTPOLAR |= 0x1 << 0; // EINT is rising edge sensitive
	EXTINT |= 0x1 << 0; // reset EINT0

	// Enable the interrupts
	VICIntSelect |= 0x1 << 26;// Timer 2 as FIQ
	VICIntEnable |= 0x1 << 26; // source #26 enabled as FIQ or IRQ
	VICIntSelect |= 0x1 << 14;// EINT2 as FIQ
	VICIntEnable |= 0x1 << 14; //source #14 enabled as FIQ or IRQ

	log_string_debug("<< pulses_in_init\n");
}

/**
 * Start i2c communication
 * Configure MPU
 * Setup eint2 with priority 1 for data ready interrupt
 */
inline void imu_init(void){

	log_string_debug(">> imu_init\n");

	// start the communication with the IMU
	i2c_init();
	// configure mpu and start taking samples
	mpu_init();

	// Configure data ready interrupt
	// Set the pin function
	PINSEL0 |= 0x1 << 30;  // EINT2

	// EINT setup
	EXTMODE |= 0x1 << 2; // EINT2 is edge sensitive
	EXTPOLAR |= 0x1 << 2; // EINT2 is rising edge sensitive
	EXTINT |= 0x1 << 2; // reset EINT2

	VICVectAddr1 = (unsigned int) &imu_data_ready; //Setting the interrupt handler location
	VICVectCntl1 = 0x30; //Vectored Interrupt slot enabled with source #16 (EINT2)
	VICIntEnable |= 0x1 << 16; //source #16 enabled as FIQ or IRQ

	log_string_debug("<< imu_init\n");
}

/**
 * ADC0 Configured in BURST mode
 */
inline void adc_init(void){

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
 */
inline void pwm_out_init(void){

	log_string_debug(">> pwm_out_init\n");

	// Set the pin function
	PINSEL0 |= 0x2 << 6;  // MAT0.0
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

	VICVectAddr2 = (unsigned int) &protocol_in; //Setting the interrupt handler location to the 2th vectored interruption slot
	VICVectCntl2 = 0x27; //Vectored Interrupt slot 2 enabled with source #7 (UART1)
	VICIntEnable |= 0x00000080; //source #7 enabled as FIQ or IRQ

	cmd_out.i = 0;
	cmd_in.i = 0;

	dummy = U1IIR;   // Read IrqID - Required to Get Interrupts Started
	U1IER = 1;       // Enable UART1 RX (and THRE Interrupts)

	log_string_debug("<< protocol_init\n");
}

/**
 * Communication Protocol state machine implementation;
 * This is triggered on uart1 interruption
 * This handles the following commands
 *
 */
void protocol_in(void){

	log_string_debug(">> protocol_in\n");

	volatile char dummy;
	volatile char iir;

	// Repeat while there is at least one interrupt source.
	while (((iir = U1IIR) & 0x01) == 0) {
		switch (iir & 0x0E) {
		case 0x06: // Receive Line Status
			dummy = U1LSR; // Just clear the interrupt source
			break;

		case 0x04: // Receive Data Available
		case 0x0C: // Character Time-Out
			cmd_in.buff[cmd_in.i] = U1RBR;

			// State machine
			if (cmd_in.buff[cmd_in.i] == END_CMD) {
				// ENGINES
				if (cmd_in.buff[(cmd_in.i-2) & (CMD_BUFF_SIZE-1)] == ENGINES) {
					set_wheel_pwm((unsigned short) (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)]),
								(unsigned short) (cmd_in.buff[(cmd_in.i) & (CMD_BUFF_SIZE-1)]));
				}
				// SYNC
				else if (cmd_in.buff[(cmd_in.i-1) & (CMD_BUFF_SIZE-1)] == SYNC) {

					// Encoders
					int count = get_encoder_count(ENCODER_L);
					cmd_out.buff[0] = (count >> 0x8) & 0xFF;
					cmd_out.buff[1] = count & 0xFF;
					count = get_encoder_count(ENCODER_R);
					cmd_out.buff[2] = (count >> 0x8) & 0xFF;
					cmd_out.buff[3] = count & 0xFF;
					// Infra Red
					char val = get_ir_sensor_data(IR_L);
					cmd_out.buff[4] = val;
					val = get_ir_sensor_data(IR_ML);
					cmd_out.buff[5] = val;
					val = get_ir_sensor_data(IR_M);
					cmd_out.buff[6] = val;
					val = get_ir_sensor_data(IR_MR);
					cmd_out.buff[7] = val;
					val = get_ir_sensor_data(IR_R);
					cmd_out.buff[8] = val;
					// IMU
					// check if data ready, if yes send new data, if not send again last data sent
					if (imu_data_out_pos != imu_data_in_pos) {
						imu_data_out_pos = ++imu_data_out_pos % IMU_BUFF_SIZE;
					}
					struct imu_data* data;
					data = &(imu_data_buff[imu_data_out_pos]);

					cmd_out.buff[9] = data->ax_h;
					cmd_out.buff[10] = data->ax_l;
					cmd_out.buff[11] = data->ay_h;
					cmd_out.buff[12] = data->ay_l;
					cmd_out.buff[13] = data->az_h;
					cmd_out.buff[14] = data->az_l;
					cmd_out.buff[15] = data->gx_h;
					cmd_out.buff[16] = data->gx_l;
					cmd_out.buff[17] = data->gy_h;
					cmd_out.buff[18] = data->gy_l;
					cmd_out.buff[19] = data->gz_h;
					cmd_out.buff[20] = data->gz_l;
					cmd_out.buff[21] = (data->timestamp >> 8) & 0xFF;
					cmd_out.buff[22] = data->timestamp & 0xFF;

					// Done
					cmd_out.buff[23] = END_CMD;
					cmd_out.buff[24] = '\n';
					cmd_out.i = 25;
					protocol_out_cmd();
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

	log_string_debug("<< protocol_in\n");

	VICVectAddr = 0;
}

/**
 * Count the encoder pulses using CAP2.0-2 as interrupt sources
 */
void encoder_pulse_in_isr(void) {

	log_string_debug(">> encoder_pulse_in_isr\n");

	const unsigned short ir = T2IR;

	if (ir & (0x1 << 4)) { //CAP2.0 left encoder
		log_string_debug("FIQ1\n");
		encoder_count[0]++;
		T2IR |= 0x1 << 4; // reset CAP2.0
	}
	else if (ir & (0x1 << 5)) { //CAP2.1 left encoder
		log_string_debug("FIQ2\n");
		//detectar sentido
		T2IR |= 0x1 << 5; // reset CAP2.1
	}
	else if (ir & (0x1 << 6)) { //CAP2.2 right encoder
		log_string_debug("FIQ3\n");
		encoder_count[1]++;
		T2IR |= 0x1 << 6; // reset CAP2.1
	}
	else {
		log_string_debug("FIQ4\n");
		//detectar sentido
		EXTINT |= 0x1 << 0; // reset EINT0
	}

	log_string_debug("<< encoder_pulse_in_isr\n");

	VICVectAddr = 0;
}

/**
 * Read IMU data when triggered by EINT2
 */
void imu_data_ready(void) {

	//log_string_debug(">> imu_data_ready\n");

	imu_data_available = 1;

	EXTINT |= 0x1 << 2; // reset EINT2

	//log_string_debug("<< imu_data_ready\n");

	VICVectAddr = 0;
}

/**
 *
 */
void error(void){
	log_string_error("irq error");
}

/**
 * Return the value read from the i'th sensor
 */
int get_ir_sensor_data(unsigned short i) {

	int val = 0;
	switch (i) {
	case IR_L:
		while(ADDR0 & ((0x1 << 31) == 0));
		val = (ADDR0 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case IR_ML:
		while(ADDR1 & ((0x1 << 31) == 0));
		val = (ADDR1 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case IR_M:
		while(ADDR2 & ((0x1 << 31) == 0));
		val = (ADDR2 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case IR_MR:
		while(ADDR3 & ((0x1 << 31) == 0));
		val = (ADDR3 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	case IR_R:
		while(ADDR4 & ((0x1 << 31) == 0));
		val = (ADDR4 >> 6) & 0x3FF;
		val >>= 0x2; // they want a value from 1 to 255
		val += (val == 0);
		break;
	}

	return val;
}



/**
 * Return the count value read from the i'th sensor
 */
int get_encoder_count(unsigned short i) {
	unsigned int val, res;
	val = encoder_count[i - ENCODER_L];
	res = val - sent_encoder_count[i - ENCODER_L];
	sent_encoder_count[i - ENCODER_L] = val;
	return res;
}

/**
 * Set the output pwm value
 */
void set_wheel_pwm(unsigned short left_wheel, unsigned short right_wheel) {

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

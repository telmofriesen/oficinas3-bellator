/*
 * irq.h
 *
 *  Created on: Jul 15, 2009
 *      Author: telmo
 */

#ifndef IRQ_H_
#define IRQ_H_

#define IRQ_MASK 0x00000080
#define FIQ_MASK 0x00000040

unsigned enableIRQ(void);
unsigned disableIRQ(void);
unsigned enableFIQ(void);
unsigned disableFIQ(void);

#endif /* IRQ_H_ */

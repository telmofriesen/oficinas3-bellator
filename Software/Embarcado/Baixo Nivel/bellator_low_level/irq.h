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

static inline unsigned asm_get_cpsr(void);
static inline void asm_set_cpsr(unsigned val);
static inline unsigned enableIRQ(void);
static inline unsigned disableIRQ(void);
static inline unsigned restoreIRQ(unsigned oldCPSR);
static inline unsigned enableFIQ(void);
static inline unsigned disableFIQ(void);

#endif /* IRQ_H_ */

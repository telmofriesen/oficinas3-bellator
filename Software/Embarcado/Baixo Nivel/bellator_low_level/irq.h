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
inline unsigned enableIRQ(void);
inline unsigned disableIRQ(void);
inline unsigned restoreIRQ(unsigned oldCPSR);
inline unsigned enableFIQ(void);
inline unsigned disableFIQ(void);

#endif /* IRQ_H_ */

/*
 * irq.c
 *
 *  Created on: Jul 15, 2009
 *      Author: telmo
 */

#include "irq.h"

static inline unsigned asm_get_cpsr(void)
{
  unsigned long retval;
  asm volatile (" mrs  %0, cpsr" : "=r" (retval) : /* no inputs */  );
  return retval;
}

static inline void asm_set_cpsr(unsigned val)
{
  asm volatile (" msr  cpsr, %0" : /* no outputs */ : "r" (val)  );
}

inline unsigned enableIRQ(void)
{
  unsigned _cpsr;

  _cpsr = asm_get_cpsr();
  asm_set_cpsr(_cpsr & ~IRQ_MASK);
  return _cpsr;
}

inline unsigned disableIRQ(void)
{
  unsigned _cpsr;

  _cpsr = asm_get_cpsr();
  asm_set_cpsr(_cpsr | IRQ_MASK);
  return _cpsr;
}

inline unsigned restoreIRQ(unsigned oldCPSR)
{
  unsigned _cpsr;

  _cpsr = asm_get_cpsr();
  asm_set_cpsr((_cpsr & ~IRQ_MASK) | (oldCPSR & IRQ_MASK));
  return _cpsr;
}

inline unsigned enableFIQ(void)
{
  unsigned _cpsr;

  _cpsr = asm_get_cpsr();
  asm_set_cpsr(_cpsr & ~FIQ_MASK);
  return _cpsr;
}

inline unsigned disableFIQ(void)
{
  unsigned _cpsr;

  _cpsr = asm_get_cpsr();
  asm_set_cpsr(_cpsr | FIQ_MASK);
  return _cpsr;
}

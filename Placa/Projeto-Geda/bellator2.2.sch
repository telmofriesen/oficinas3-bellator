v 20110115 2
C 24900 40500 1 0 0 LPC2101_02_03_2.sym
{
T 33300 46200 5 10 1 1 0 6 1
refdes=LPC2101
T 29100 43700 5 10 0 0 0 0 1
device=LPC2101_02_03
T 29100 43900 5 10 0 0 0 0 1
footprint=LQFP48_7
T 24900 40500 5 10 0 0 0 0 1
net=3V3:17
T 24900 40500 5 10 0 0 0 0 1
net=3V3:40
T 24900 40500 5 10 0 0 0 0 1
net=GND:43
T 24200 43300 5 10 1 1 0 0 1
net=CAP20:8
T 24200 42900 5 10 1 1 0 0 1
net=CAP21:9
T 30700 39700 5 10 1 1 90 0 1
net=MAT01:23
T 29900 39700 5 10 1 1 90 0 1
net=MAT00:21
T 24200 42500 5 10 1 1 0 0 1
net=CAP22:10
T 30300 46500 5 10 1 1 90 0 1
net=EINT0:46
}
C 33500 43000 1 0 0 gnd-1.sym
C 22900 42000 1 270 0 crystal-1.sym
{
T 23300 41400 5 10 1 1 90 0 1
refdes=U14MH
T 23600 41800 5 10 0 0 270 0 1
symversion=0.1
T 22900 42000 5 10 0 0 0 0 1
footprint=XTAL_HC-49US_SMD
T 22800 41200 5 10 1 1 90 0 1
value=14.7456MHz
T 22900 42000 5 10 0 0 0 0 1
device=CRYSTAL
}
C 22200 42400 1 180 0 capacitor-1.sym
{
T 22000 41700 5 10 0 0 180 0 1
device=CAPACITOR
T 22000 41900 5 10 1 1 180 0 1
refdes=C1
T 22000 41500 5 10 0 0 180 0 1
symversion=0.1
T 22300 42500 5 10 1 1 180 0 1
value=33p
T 22200 42400 5 10 0 0 0 0 1
footprint=805
}
C 22200 41400 1 180 0 capacitor-1.sym
{
T 22000 40700 5 10 0 0 180 0 1
device=CAPACITOR
T 22000 40900 5 10 1 1 180 0 1
refdes=C2
T 22000 40500 5 10 0 0 180 0 1
symversion=0.1
T 22300 41500 5 10 1 1 180 0 1
value=33p
T 22200 41400 5 10 0 0 0 0 1
footprint=805
}
N 23000 41200 23000 41300 4
N 25000 41700 23400 41700 4
N 23400 41700 23400 41200 4
N 22200 41200 23400 41200 4
N 23000 42000 23000 42200 4
N 21200 41200 21200 42200 4
N 21200 42200 21300 42200 4
N 21300 41200 21200 41200 4
C 21100 40900 1 0 0 gnd-1.sym
C 23700 43700 1 90 0 resistor-1.sym
{
T 23300 44000 5 10 0 0 90 0 1
device=RESISTOR
T 23400 43900 5 10 1 1 90 0 1
refdes=R1
T 23900 43800 5 10 1 1 90 0 1
value=47K
T 23700 43700 5 10 0 0 0 0 1
footprint=805
}
N 23100 43700 25000 43700 4
C 28200 49300 1 90 0 resistor-1.sym
{
T 27800 49600 5 10 0 0 90 0 1
device=RESISTOR
T 27900 49500 5 10 1 1 90 0 1
refdes=R2
T 28400 49400 5 10 1 1 90 0 1
value=47K
T 28200 49300 5 10 0 0 0 0 1
footprint=805
}
C 37000 27900 1 270 1 connector3-1.sym
{
T 37900 29700 5 10 0 0 270 6 1
device=CONNECTOR_3
T 38100 27900 5 10 1 1 270 6 1
refdes=IRL
T 37000 27900 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-3x1.fp
}
C 35700 27900 1 270 1 connector3-1.sym
{
T 36600 29700 5 10 0 0 270 6 1
device=CONNECTOR_3
T 36800 27900 5 10 1 1 270 6 1
refdes=IRML
T 35700 27900 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-3x1.fp
}
C 34400 27900 1 270 1 connector3-1.sym
{
T 35300 29700 5 10 0 0 270 6 1
device=CONNECTOR_3
T 35500 27900 5 10 1 1 270 6 1
refdes=IRM
T 34400 27900 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-3x1.fp
}
C 33100 27900 1 270 1 connector3-1.sym
{
T 34000 29700 5 10 0 0 270 6 1
device=CONNECTOR_3
T 34200 27900 5 10 1 1 270 6 1
refdes=IRMR
T 33100 27900 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-3x1.fp
}
C 31800 27900 1 270 1 connector3-1.sym
{
T 32700 29700 5 10 0 0 270 6 1
device=CONNECTOR_3
T 32900 27900 5 10 1 1 270 6 1
refdes=IRR
T 31800 27900 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-3x1.fp
}
N 24000 29900 37800 29900 4
N 26700 29600 26700 29900 4
N 37800 29900 37800 29600 4
N 36500 29900 36500 29600 4
N 35200 29900 35200 29600 4
N 33900 29900 33900 29600 4
N 32600 29900 32600 29600 4
N 28400 29600 28400 29900 4
N 37500 29600 37500 30200 4
N 24000 30200 37500 30200 4
N 36200 30200 36200 29600 4
N 34900 30200 34900 29600 4
N 33600 30200 33600 29600 4
N 32300 30200 32300 29600 4
N 27800 30200 27800 29600 4
N 26100 30200 26100 29600 4
C 23700 30100 1 270 1 gnd-1.sym
T 25500 27500 9 10 1 0 0 0 1
Encoders (Pulses -0.5V to Vcc 5V)
T 31800 27500 9 10 1 0 0 0 1
IR Sensors (Analog signal typically 0.25V~2.85V, but may reach Vcc 5V + 0.3V)
N 33300 29600 33300 30400 4
N 35900 29600 35900 30400 4
N 32000 29600 32000 30400 4
N 25800 29600 25800 30400 4
N 37200 30400 37200 29600 4
C 31900 31300 1 270 0 resistor-1.sym
{
T 32300 31000 5 10 0 0 270 0 1
device=RESISTOR
T 32200 31100 5 10 1 1 270 0 1
refdes=R3
T 31700 31200 5 10 1 1 270 0 1
value=20
T 31900 31300 5 10 0 0 180 0 1
footprint=805
}
C 33200 31300 1 270 0 resistor-1.sym
{
T 33600 31000 5 10 0 0 270 0 1
device=RESISTOR
T 33500 31100 5 10 1 1 270 0 1
refdes=R4
T 33000 31200 5 10 1 1 270 0 1
value=20
T 33200 31300 5 10 0 0 180 0 1
footprint=805
}
C 34500 31300 1 270 0 resistor-1.sym
{
T 34900 31000 5 10 0 0 270 0 1
device=RESISTOR
T 34800 31100 5 10 1 1 270 0 1
refdes=R5
T 34300 31200 5 10 1 1 270 0 1
value=20
T 34500 31300 5 10 0 0 180 0 1
footprint=805
}
C 35800 31300 1 270 0 resistor-1.sym
{
T 36200 31000 5 10 0 0 270 0 1
device=RESISTOR
T 36100 31100 5 10 1 1 270 0 1
refdes=R6
T 35600 31200 5 10 1 1 270 0 1
value=20
T 35800 31300 5 10 0 0 180 0 1
footprint=805
}
C 37100 31300 1 270 0 resistor-1.sym
{
T 37500 31000 5 10 0 0 270 0 1
device=RESISTOR
T 37400 31100 5 10 1 1 270 0 1
refdes=R7
T 36900 31200 5 10 1 1 270 0 1
value=20
T 37100 31300 5 10 0 0 180 0 1
footprint=805
}
N 33300 31300 33300 31900 4
N 35900 31300 35900 31600 4
N 37200 31300 37200 31900 4
N 33600 41300 34500 41300 4
N 33600 41700 34800 41700 4
N 33600 42100 35100 42100 4
N 33600 42500 35400 42500 4
N 33600 42900 35700 42900 4
N 32000 32200 34500 32200 4
N 35400 31600 35900 31600 4
N 35700 31900 37200 31900 4
C 25600 31800 1 180 0 capacitor-1.sym
{
T 25400 31100 5 10 0 0 180 0 1
device=CAPACITOR
T 25400 31300 5 10 1 1 180 0 1
refdes=C3
T 25400 30900 5 10 0 0 180 0 1
symversion=0.1
T 25600 31800 5 10 1 1 90 0 1
value=150p
T 25600 31800 5 10 0 0 90 0 1
footprint=805
}
C 25600 32700 1 180 0 capacitor-1.sym
{
T 25400 32000 5 10 0 0 180 0 1
device=CAPACITOR
T 25400 32200 5 10 1 1 180 0 1
refdes=C4
T 25400 31800 5 10 0 0 180 0 1
symversion=0.1
T 25600 32700 5 10 1 1 90 0 1
value=150p
T 25600 32700 5 10 0 0 90 0 1
footprint=805
}
N 25600 31600 25800 31600 4
N 25600 32500 26800 32500 4
N 24700 31600 24600 31600 4
N 24600 30200 24600 32500 4
C 25900 30400 1 90 0 resistor-1.sym
{
T 25500 30700 5 10 0 0 90 0 1
device=RESISTOR
T 25600 30600 5 10 1 1 90 0 1
refdes=R8
T 26100 30500 5 10 1 1 90 0 1
value=332
T 25900 30400 5 10 0 0 90 0 1
footprint=805
}
C 26500 30400 1 90 0 resistor-1.sym
{
T 26100 30700 5 10 0 0 90 0 1
device=RESISTOR
T 26200 30600 5 10 1 1 90 0 1
refdes=R9
T 26700 30500 5 10 1 1 90 0 1
value=332
T 26500 30400 5 10 0 0 90 0 1
footprint=805
}
C 29300 33200 1 90 0 74HC7014.sym
{
T 25900 34700 5 10 1 1 90 6 1
refdes=UENC
T 25700 33600 5 10 0 0 90 0 1
device=74HC7014
T 25500 33600 5 10 0 0 90 0 1
footprint=SOIC-127P-600L1-14N
T 26800 35000 5 10 1 1 90 0 1
net=CAP20:4
T 27200 35000 5 10 1 1 90 0 1
net=CAP21:6
T 27600 35000 5 10 1 1 90 0 1
net=CAP22:8
T 26400 35000 5 10 1 1 90 0 1
net=EINT0:2
}
N 26800 32500 26800 33300 4
N 26400 33300 26400 32800 4
N 25800 32800 25800 31300 4
N 25800 32800 26400 32800 4
N 24600 32500 24700 32500 4
T 24300 30700 9 10 1 0 90 0 1
Low Pass filter (~20MHz)
T 31500 30500 9 10 1 0 90 0 1
Voltage limiter (3V3)
T 25400 33400 9 10 1 0 90 0 2
Schmitt trigger
(input -1.5V to 6V)
N 26400 34900 26400 35800 4
N 26800 34900 26800 35800 4
C 22100 45100 1 0 1 74HC244-1.sym
{
T 21800 48250 5 10 0 0 0 6 1
device=74HC244
T 20400 48100 5 10 1 1 0 0 1
refdes=UPWM
T 21800 48450 5 10 0 0 0 6 1
footprint=SOIC-127P-1030L1-20N
T 22400 45400 5 10 1 1 0 6 1
net=5V:20
T 20300 45400 5 10 1 1 0 6 1
net=GND:10
T 22100 47800 5 10 1 1 0 0 1
net=MAT00:2
T 22100 47500 5 10 1 1 0 0 1
net=MAT01:4
}
T 21900 48400 9 10 1 0 0 6 1
Buffer (Current driver)
C 18600 30000 1 0 0 led-2.sym
{
T 19400 30300 5 10 1 1 0 0 1
refdes=D1
T 18700 30600 5 10 0 0 0 0 1
device=LED
T 18600 30000 5 10 0 0 90 0 1
footprint=805
}
C 17300 30000 1 0 0 resistor-1.sym
{
T 17600 30400 5 10 0 0 0 0 1
device=RESISTOR
T 17500 30300 5 10 1 1 0 0 1
refdes=R10
T 17500 29800 5 10 1 1 0 0 1
value=20K
T 17300 30000 5 10 0 0 270 0 1
footprint=805
}
C 19800 30200 1 90 1 gnd-1.sym
N 23500 46600 27500 46600 4
C 17600 46700 1 0 0 connector4-1.sym
{
T 19400 47600 5 10 0 0 0 0 1
device=CONNECTOR_4
T 17600 48100 5 10 1 1 0 0 1
refdes=HBRIDGE
T 17600 46700 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-4x1.fp
}
N 20100 47800 19300 47800 4
N 19300 47500 20100 47500 4
N 20100 47200 19300 47200 4
N 19300 46900 20100 46900 4
N 22100 46900 28700 46900 4
N 30700 46400 30700 48900 4
N 31100 46400 31100 48900 4
N 27500 46400 27500 46600 4
N 28700 46400 28700 46900 4
N 22100 47200 23500 47200 4
T 17200 46500 9 10 1 0 90 0 1
H Bridge PWM output
C 21600 36500 1 0 0 DB9-1.sym
{
T 22600 39400 5 10 0 0 0 0 1
device=DB9
T 21800 39700 5 10 1 1 0 0 1
refdes=UART1
T 21600 36500 5 10 0 0 0 0 1
footprint=CON_DSUB_9F__AMP_747844
}
N 22800 38600 23400 38600 4
N 23400 38600 23400 38000 4
N 23100 37600 23100 38000 4
N 23100 38000 22800 38000 4
N 24600 37600 23100 37600 4
N 23400 38000 24600 38000 4
C 23800 39100 1 270 0 capacitor-2.sym
{
T 24500 38900 5 10 0 0 270 0 1
device=POLARIZED_CAPACITOR
T 24300 38900 5 10 1 1 270 0 1
refdes=C5
T 24700 38900 5 10 0 0 270 0 1
symversion=0.1
T 23600 38800 5 10 1 1 270 0 1
value=0.1u
T 23800 39100 5 10 0 0 270 0 1
footprint=805
}
N 24600 38200 24000 38200 4
N 24600 38400 24600 38200 4
C 24600 36500 1 0 0 max232-2.sym
{
T 26300 40000 5 10 1 1 0 6 1
refdes=MAX3232
T 25000 36300 5 10 1 1 0 0 1
net=3V3:16
T 24900 40150 5 10 0 0 0 0 1
device=MAX3232
T 24900 40350 5 10 0 0 0 0 1
footprint=SOIC-127P-600L1-16N
}
C 23800 40100 1 270 0 capacitor-2.sym
{
T 24500 39900 5 10 0 0 270 0 1
device=POLARIZED_CAPACITOR
T 24300 39900 5 10 1 1 270 0 1
refdes=C6
T 24700 39900 5 10 0 0 270 0 1
symversion=0.1
T 23600 39800 5 10 1 1 270 0 1
value=0.1u
T 23800 40100 5 10 0 0 270 0 1
footprint=805
}
N 24600 39100 24600 38800 4
N 24000 39100 24600 39100 4
N 24000 39200 24600 39200 4
N 24600 39600 24600 40100 4
N 24600 40100 24000 40100 4
N 27100 39100 26600 39100 4
N 26600 39100 26600 38800 4
C 27300 39200 1 90 0 capacitor-2.sym
{
T 26600 39400 5 10 0 0 90 0 1
device=POLARIZED_CAPACITOR
T 26800 39400 5 10 1 1 90 0 1
refdes=C7
T 26400 39400 5 10 0 0 90 0 1
symversion=0.1
T 27500 39500 5 10 1 1 90 0 1
value=0.1u
T 27300 39200 5 10 0 0 90 0 1
footprint=805
}
N 27100 39200 26600 39200 4
N 26600 39200 26600 39600 4
N 27500 38000 27500 40600 4
N 27100 38100 27100 38200 4
C 27300 38200 1 90 0 capacitor-2.sym
{
T 26800 38400 5 10 1 1 90 0 1
refdes=C8
T 27500 38500 5 10 1 1 90 0 1
value=0.1u
T 26600 38400 5 10 0 0 90 0 1
device=POLARIZED_CAPACITOR
T 26400 38400 5 10 0 0 90 0 1
symversion=0.1
T 27300 38200 5 10 0 0 90 0 1
footprint=805
}
N 26600 38000 27500 38000 4
C 27000 37800 1 0 0 gnd-1.sym
N 26600 37600 27900 37600 4
N 27900 37600 27900 40600 4
N 33600 43700 33900 43700 4
N 33900 36800 33900 43700 4
N 33600 44100 34200 44100 4
N 34200 37200 34200 44100 4
N 33900 36800 26600 36800 4
N 34200 37200 26600 37200 4
C 21600 32800 1 0 0 DB9-1.sym
{
T 22600 35700 5 10 0 0 0 0 1
device=DB9
T 21700 36000 5 10 1 1 0 0 1
refdes=UART2
T 21600 32800 5 10 0 0 0 0 1
footprint=CON_DSUB_9F__AMP_747844
}
N 24600 36800 23400 36800 4
N 22800 34300 23400 34300 4
N 23400 34300 23400 36800 4
N 23100 34900 23100 37200 4
N 23100 34900 22800 34900 4
N 23100 37200 24600 37200 4
C 24400 44700 1 0 0 3.3V-plus-1.sym
C 19200 36300 1 0 0 5V-plus-1.sym
N 18600 30100 18200 30100 4
N 16900 30100 17300 30100 4
N 24600 44700 24600 44500 4
N 24600 44500 25000 44500 4
C 23900 44700 1 0 0 generic-power.sym
{
T 24100 44950 5 10 1 1 0 3 1
net=1V8:1
}
N 25000 44100 24100 44100 4
N 24100 44100 24100 44700 4
C 23400 44700 1 0 0 3.3V-plus-1.sym
N 23600 44700 23600 44600 4
N 22100 43700 22000 43700 4
C 22100 43600 1 0 0 switch-pushbutton-nc-1.sym
{
T 21650 43400 5 10 0 0 0 0 1
device=SWITCH_PUSHBUTTON_NC
T 22300 43950 5 10 1 1 0 0 1
refdes=RESET
T 22100 43600 5 10 0 0 0 0 1
footprint=SW__Panasonic_EVQPA_Series
}
C 21900 43400 1 0 0 gnd-1.sym
C 27900 50400 1 0 0 3.3V-plus-1.sym
N 28100 50400 28100 50200 4
N 28100 48100 29500 48100 4
N 27500 49000 27500 49700 4
C 25800 49500 1 0 0 connector2-1.sym
{
T 26000 50500 5 10 0 0 0 0 1
device=CONNECTOR_2
T 25800 50300 5 10 1 1 0 0 1
refdes=ISP
T 25800 49500 5 10 0 0 0 0 1
footprint=CON_PIN_STRIP-2x1.fp
}
C 27600 50700 1 180 0 gnd-1.sym
T 25500 49300 9 10 1 0 90 0 1
ISP Mode jumper
N 27500 50400 27500 50000 4
C 22700 32800 1 0 0 gnd-1.sym
C 22700 36500 1 0 0 gnd-1.sym
C 28700 33000 1 0 0 gnd-1.sym
C 28600 34900 1 0 0 3.3V-plus-1.sym
C 17400 42100 1 0 0 gnd-1.sym
C 19600 41500 1 90 0 capacitor-1.sym
{
T 18900 41700 5 10 0 0 90 0 1
device=CAPACITOR
T 19100 41700 5 10 1 1 90 0 1
refdes=C9
T 18700 41700 5 10 0 0 90 0 1
symversion=0.1
T 19500 42100 5 10 1 1 0 0 1
value=10u
T 19600 41500 5 10 0 0 0 0 1
footprint=805
}
C 19300 41200 1 0 0 gnd-1.sym
N 17500 42000 16900 42000 4
N 16900 42000 16900 43200 4
C 17400 41500 1 0 0 LD1117xx.sym
{
T 19100 42900 5 10 1 1 0 6 1
refdes=U1V8
T 17800 43100 5 10 0 0 0 0 1
device=LD1117S18
T 17800 43300 5 10 0 0 0 0 1
footprint=SOT223
T 17100 42700 5 10 1 1 0 0 1
net=GND:1
T 19200 42900 5 10 1 1 0 0 1
net=1V8:2
}
N 19100 42400 19400 42400 4
C 17100 41100 1 90 0 capacitor-1.sym
{
T 16400 41300 5 10 0 0 90 0 1
device=CAPACITOR
T 16600 41300 5 10 1 1 90 0 1
refdes=C10
T 16200 41300 5 10 0 0 90 0 1
symversion=0.1
T 17000 41700 5 10 1 1 0 0 1
value=0.1u
T 17100 41100 5 10 0 0 0 0 1
footprint=805
}
C 16800 40800 1 0 0 gnd-1.sym
C 16700 43200 1 0 0 12V-plus-1.sym
T 16200 41400 9 10 1 0 90 0 1
1V8 Voltage regulator
C 17400 39100 1 0 0 gnd-1.sym
C 19600 38500 1 90 0 capacitor-1.sym
{
T 18900 38700 5 10 0 0 90 0 1
device=CAPACITOR
T 19100 38700 5 10 1 1 90 0 1
refdes=C11
T 18700 38700 5 10 0 0 90 0 1
symversion=0.1
T 19500 39100 5 10 1 1 0 0 1
value=10u
T 19600 38500 5 10 0 0 0 0 1
footprint=805
}
C 19300 38200 1 0 0 gnd-1.sym
N 17500 39000 16900 39000 4
N 16900 39000 16900 40200 4
C 17400 38500 1 0 0 LD1117xx.sym
{
T 19100 39900 5 10 1 1 0 6 1
refdes=U3V3
T 17800 40100 5 10 0 0 0 0 1
device=LD1117S33
T 17800 40300 5 10 0 0 0 0 1
footprint=SOT223
T 17100 39700 5 10 1 1 0 0 1
net=GND:1
T 19200 39900 5 10 1 1 0 0 1
net=3V3:2
}
N 19100 39400 19400 39400 4
C 16700 40200 1 0 0 12V-plus-1.sym
C 19200 39400 1 0 0 3.3V-plus-1.sym
T 16200 38400 9 10 1 0 90 0 1
3V3 Voltage regulator
C 17400 36000 1 0 0 gnd-1.sym
C 19600 35400 1 90 0 capacitor-1.sym
{
T 18900 35600 5 10 0 0 90 0 1
device=CAPACITOR
T 19100 35600 5 10 1 1 90 0 1
refdes=C13
T 18700 35600 5 10 0 0 90 0 1
symversion=0.1
T 19500 36000 5 10 1 1 0 0 1
value=10u
T 19600 35400 5 10 0 0 0 0 1
footprint=805
}
C 19300 35100 1 0 0 gnd-1.sym
N 17500 35900 16900 35900 4
N 16900 35900 16900 37100 4
C 17400 35400 1 0 0 LD1117xx.sym
{
T 19100 36800 5 10 1 1 0 6 1
refdes=U5V
T 17800 37000 5 10 0 0 0 0 1
device=LD1117S5
T 17800 37200 5 10 0 0 0 0 1
footprint=SOT223
T 17100 36600 5 10 1 1 0 0 1
net=GND:1
T 19200 36800 5 10 1 1 0 0 1
net=5V:2
}
N 19100 36300 19400 36300 4
C 16700 37100 1 0 0 12V-plus-1.sym
T 16200 35300 9 10 1 0 90 0 1
5V Voltage regulator
C 19200 42400 1 0 0 generic-power.sym
{
T 19400 42650 5 10 1 1 0 3 1
net=1V8:1
}
T 22200 43300 9 10 1 0 0 0 1
Reset Button
T 16200 29700 9 10 1 0 90 0 1
Power led
T 21300 36800 9 10 1 0 90 0 1
Serial Interface 0 - ISP / logs
T 21300 33000 9 10 1 0 90 0 1
Serial Interface 1 - TS-7260 Board
C 30200 51000 1 270 0 MPU-6050.sym
{
T 34600 48600 5 10 0 0 270 0 1
device=DIP24
T 32200 49500 5 10 1 1 270 0 1
refdes=MPU
T 30200 51000 5 10 0 0 0 0 1
footprint=MPU-6050
}
C 30200 48600 1 0 0 gnd-1.sym
C 30400 51300 1 180 0 gnd-1.sym
C 31700 48900 1 180 0 3.3V-plus-1.sym
N 31900 51000 31900 51500 4
N 31900 51500 29900 51500 4
N 29900 46400 29900 51500 4
C 31600 51300 1 180 0 gnd-1.sym
T 29700 49000 9 10 1 0 90 0 1
Gyro and Accelerometer (IMU)
T 31000 47400 9 10 1 0 90 0 1
I2C Bus
C 28900 46400 1 0 0 3.3V-plus-1.sym
C 21400 44800 1 0 0 gnd-1.sym
C 16900 29900 1 90 0 12V-plus-1.sym
T 37900 48300 9 10 1 0 90 0 1
Bellator 2.1
T 38200 52200 9 10 1 0 90 0 1
2.1
T 38500 52200 9 10 1 0 90 0 1
Telmo Friesen
T 38200 48300 9 10 1 0 90 0 1
bellator2.1.sch
T 38500 48300 9 10 1 0 90 0 1
1
T 38500 49800 9 10 1 0 90 0 1
1
C 38600 22200 1 90 0 title-A1.sym
C 24000 29700 1 90 0 5V-plus-1.sym
N 35700 42900 35700 31900 4
N 35400 31600 35400 42500 4
N 35100 42100 35100 31600 4
N 34600 31300 34600 31600 4
N 34600 31600 35100 31600 4
N 34800 41700 34800 31900 4
N 34500 32200 34500 41300 4
N 25000 43300 24200 43300 4
N 25000 42900 24200 42900 4
C 27200 40500 1 180 0 gnd-1.sym
N 27100 40200 27100 40100 4
N 30700 40600 30700 39600 4
N 23500 47200 23500 46600 4
N 22100 47800 22800 47800 4
N 22100 47500 22800 47500 4
N 29900 40600 29900 39500 4
C 26900 27900 1 90 0 connector4-1.sym
{
T 26000 29700 5 10 0 0 270 8 1
device=CONNECTOR_4
T 25500 27900 5 10 1 1 270 8 1
refdes=ENCL
T 26900 27900 5 10 0 0 0 6 1
footprint=CON_PIN_STRIP-4x1.fp
}
C 28600 27900 1 90 0 connector4-1.sym
{
T 27700 29700 5 10 0 0 270 8 1
device=CONNECTOR_4
T 27200 27900 5 10 1 1 270 8 1
refdes=ENCR
T 28600 27900 5 10 0 0 0 6 1
footprint=CON_PIN_STRIP-4x1.fp
}
N 26400 29600 26400 30400 4
N 26400 31300 26400 32500 4
C 27600 30400 1 90 0 resistor-1.sym
{
T 27200 30700 5 10 0 0 90 0 1
device=RESISTOR
T 27300 30600 5 10 1 1 90 0 1
refdes=R13
T 27800 30500 5 10 1 1 90 0 1
value=332
T 27600 30400 5 10 0 0 90 0 1
footprint=805
}
C 28200 30400 1 90 0 resistor-1.sym
{
T 27800 30700 5 10 0 0 90 0 1
device=RESISTOR
T 27900 30600 5 10 1 1 90 0 1
refdes=R14
T 28400 30500 5 10 1 1 90 0 1
value=332
T 28200 30400 5 10 0 0 90 0 1
footprint=805
}
N 28100 29600 28100 30400 4
N 27500 30400 27500 29600 4
C 29200 31800 1 180 0 capacitor-1.sym
{
T 29000 31100 5 10 0 0 180 0 1
device=CAPACITOR
T 29000 31300 5 10 1 1 180 0 1
refdes=C16
T 29000 30900 5 10 0 0 180 0 1
symversion=0.1
T 29200 31800 5 10 1 1 90 0 1
value=150p
T 29200 31800 5 10 0 0 90 0 1
footprint=805
}
C 29200 32700 1 180 0 capacitor-1.sym
{
T 29000 32000 5 10 0 0 180 0 1
device=CAPACITOR
T 29000 32200 5 10 1 1 180 0 1
refdes=C15
T 29000 31800 5 10 0 0 180 0 1
symversion=0.1
T 29200 32700 5 10 1 1 90 0 1
value=150p
T 29200 32700 5 10 0 0 90 0 1
footprint=805
}
N 28100 31300 28100 32800 4
N 28100 31600 28300 31600 4
N 27500 31300 27500 32500 4
N 27200 32500 28300 32500 4
N 27200 32500 27200 33300 4
N 27600 33300 27600 32800 4
N 27600 32800 28100 32800 4
N 29200 32500 29300 32500 4
N 29300 32500 29300 30200 4
N 29200 31600 29300 31600 4
N 27200 34900 27200 35800 4
N 27600 34900 27600 35800 4
N 30300 46400 30300 47300 4
N 25000 42500 24200 42500 4
N 25000 42100 23400 42100 4
N 22200 42200 23400 42200 4
N 23400 42200 23400 42100 4
N 29500 46400 29500 48100 4
N 28100 48100 28100 49300 4
N 27500 49000 28100 49000 4
C 16200 32100 1 0 0 6.3mmJack.sym
{
T 16200 33070 5 10 0 0 0 0 1
footprint=pwrjack-2.1-5.5-3pin.fp
T 16200 33000 5 10 1 1 0 0 1
refdes=PWR
}
C 18400 31900 1 0 0 diode-1.sym
{
T 18800 32500 5 10 0 0 0 0 1
device=DIODE
T 18700 32400 5 10 1 1 0 0 1
refdes=D2
T 18400 31900 5 10 0 0 0 0 1
footprint=DO-41.fp
}
C 17700 32500 1 0 0 gnd-1.sym
N 17800 32100 18400 32100 4
C 19800 32600 1 0 0 12V-plus-1.sym
N 19300 32100 20000 32100 4
N 20000 32100 20000 32600 4
N 32000 31300 32000 32200 4
N 34600 30400 34600 29600 4
C 33000 35400 1 0 0 DUAL-ZENER.sym
{
T 33400 36000 5 10 0 0 0 0 1
device=ZENER_DIODE
T 32600 36400 5 10 1 1 0 0 1
refdes=Z12
T 33000 35400 5 10 0 0 0 0 1
footprint=SOT363
T 32600 35400 5 10 1 1 0 0 1
value=4V3
}
N 33300 31900 34800 31900 4
C 33000 33900 1 0 0 DUAL-ZENER.sym
{
T 33400 34500 5 10 0 0 0 0 1
device=ZENER_DIODE
T 32600 34900 5 10 1 1 0 0 1
refdes=Z34
T 33000 33900 5 10 0 0 0 0 1
footprint=SOT363
T 32600 33900 5 10 1 1 0 0 1
value=4V3
}
C 33000 32400 1 0 0 DUAL-ZENER.sym
{
T 33400 33000 5 10 0 0 0 0 1
device=ZENER_DIODE
T 32600 33400 5 10 1 1 0 0 1
refdes=Z5
T 33000 32400 5 10 0 0 0 0 1
footprint=SOT363
T 32600 32400 5 10 1 1 0 0 1
value=4V3
}
N 33000 36300 32000 36300 4
N 32000 36300 32000 33100 4
N 33000 33300 32000 33300 4
N 33000 34200 32000 34200 4
N 33000 34800 32000 34800 4
N 33000 35700 32000 35700 4
C 31900 32800 1 0 0 gnd-1.sym
N 34200 36300 34500 36300 4
N 34200 35700 34800 35700 4
N 34200 34800 35100 34800 4
N 34200 34200 35400 34200 4
N 34200 33300 35700 33300 4
C 30100 38400 1 180 0 3.3V-plus-1.sym
C 29800 39500 1 270 0 resistor-1.sym
{
T 30200 39200 5 10 0 0 270 0 1
device=RESISTOR
T 30100 39300 5 10 1 1 270 0 1
refdes=R12
T 29600 39400 5 10 1 1 270 0 1
value=47K
T 29800 39500 5 10 0 0 180 0 1
footprint=805
}
N 29900 38400 29900 38600 4

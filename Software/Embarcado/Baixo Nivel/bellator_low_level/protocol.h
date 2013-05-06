/*************************
* Arquivo: protocolo.h
* Descricao: contem a definicao dos comandos que sao
* enviados/recebidos pelo robo
**************************/

#define ON					0x01
#define OFF					0x00

// Comando para requisição dos dados dos sensores
#define SYNC				0xA0
#define CLEAR_BUFF			0xA1

// Comandos para controlar os motores
#define ENGINES				0xB0
#define ENGINES_ACK			0xB1

// Comandos para envio dos dados dos sensores
#define SENSORS				0xC0
#define ENCODER_L			0xC1
#define ENCODER_R			0xC2
#define IR_L				0xC3
#define IR_ML				0xC4
#define IR_M				0xC5
#define IR_MR				0xC6
#define IR_R				0xC7

#define TEST				0xD0

// Mascara para obter o valor do pwm e o sentido
// o bit mais significativo representa o sentido,
// enquanto os restantes representam o valor de PWM
#define PWM_DIR				0x80  // Mascara que define o bit de sentido da PWM
#define PWM_MASK			0x7F  // Mascara que define o bit de valor   da PWM

// Resposta caso a mensagem recebida nao seja identificada
#define ERRO				0x45

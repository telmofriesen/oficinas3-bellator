package robo;

import comunicacao.ClientMessageProcessor;
import robo.Main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is a very simple example showing the most basic use of
 * {@link net.Network} and {@link net.Network_iface}. Feel free to use,
 * overwrite, or just ignore code as you like.
 *
 * As a default, a connection speed of 115200 baud is assumed. You can use a
 * different speed by giving it as an <b>int</b> as the first command line
 * argument or changing the default speed in the source code.
 *
 * @author Raphael Blatter (raphael@blatter.sg)
 */
public class SerialCommunicator implements SerialNetwork_iface {

    // set the speed of the serial port
    public static int speed = 115200;
    private static SerialNetwork network;
    private static boolean resend_active = false;
    private Main main;

    public SerialCommunicator(Main server) {
        this.main = server;
        network = new SerialNetwork(0, this, (byte) 0xFE);

        // initializing reader from command line
        int i, inp_num = 0;
        String input;
        BufferedReader in_stream = new BufferedReader(new InputStreamReader(
                System.in));

        // getting a list of the available serial ports
        Vector<String> ports = network.getPortList();

        // choosing the port to connect to
        System.out.println();
        if (ports.size() > 0) {
            System.out
                    .println("the following serial ports have been detected:");
        } else {
            System.out
                    .println("sorry, no serial ports were found on your computer\n");
            System.exit(0);
        }
        for (i = 0; i < ports.size(); ++i) {
            System.out.println("    " + Integer.toString(i + 1) + ":  "
                               + ports.elementAt(i));
        }
        boolean valid_answer = false;
        while (!valid_answer) {
            System.out
                    .println("enter the id (1,2,...) of the connection to connect to: ");
            try {
//                input = in_stream.readLine();
//                inp_num = Integer.parseInt(input);
                inp_num = 1;
                if ((inp_num < 1) || (inp_num >= ports.size() + 1))
                    System.out.println("your input is not valid");
                else
                    valid_answer = true;
            } catch (NumberFormatException ex) {
                System.out.println("please enter a correct number");
            }
//            catch (IOException e) {
//                System.out.println("there was an input error\n");
//                System.exit(1);
//            }
        }

        // connecting to the selected port
        if (network.connect(ports.elementAt(inp_num - 1), speed)) {
            System.out.println("Conectado na porta " + ports.elementAt(inp_num - 1));
        } else {
            System.out.println("sorry, there was an error connecting\n");
            System.exit(1);
        }

        // asking whether user wants to mirror traffic
//		System.out
//				.println("do you want this tool to send back all the received messages?");
//		valid_answer = false;
//		while (!valid_answer) {
//			System.out.println("'y' for yes or 'n' for no: ");
//			try {
//				input = in_stream.readLine();
//				if (input.equals("y")) {
//					resend_active = true;
//					valid_answer = true;
//				} else if (input.equals("n")) {
//					valid_answer = true;
//				} else if (input.equals("q")) {
//					System.out.println("example terminated\n");
//					System.exit(0);
//				}
//			} catch (IOException e) {
//				System.out.println("there was an input error\n");
//				System.exit(1);
//			}
//		}

        // reading in numbers (bytes) to be sent over the serial port
////		System.out.println("type 'q' to end the example");
//		while (true) {
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e1) {
//			}
//			System.out
//					.println("\nenter a number between 0 and 254 to be sent ('q' to exit): ");
//			try {
//				input = in_stream.readLine();
//				if (input.equals("q")) {
//					System.out.println("example terminated\n");
//					network.disconnect();
//					System.exit(0);
//				}
//				inp_num = Integer.parseInt(input);
//				if ((inp_num > 255) || (inp_num < 0)) {
//					System.out.println("the number you entered is not valid");
//				} else {
//					int temp[] = { inp_num };
//					System.out.println("sent " + inp_num + " over the serial port");
//				}
//			} catch (NumberFormatException ex) {
//				System.out.println("please enter a correct number");
//			} catch (IOException e) {
//				System.out.println("there was an input error");
//			}
//		}
    }

    /**
     * Implementing {@link net.Network_iface#networkDisconnected(int)}, which is
     * called when the connection has been closed. In this example, the program
     * is ended.
     *
     * @see net.Network_iface
     */
    public void networkDisconnected(int id) {
        System.exit(0);
    }

    /**
     * Envia uma mensagem via serial até a placa de baixo nível
     *
     * @param message
     */
    //TODO:
    //- Usar bytes ao invés de inteiros
    public void sendMessage(byte[] message) {
        network.writeSerial(message.length, message);

    }

    /**
     * Interpreta mensagens recebidas da porta serial (mensagens vindas da placa de baixo nível)
     *
     *
     * Implementing {@link net.Network_iface#parseInput(int, int, int[])} to
     * handle messages received over the serial port.
     *
     * Interpreta e executa os comandos recebidos da porta serial.
     *
     * @see net.Network_iface
     */
    //TODO:
    //- Usar bytes ao invés de inteiros
    //- Implementar a interpretação de mensagens das leituras dos sensores: recebe leituras e manda elas para a estação base
    public void parseInput(int id, int numBytes, byte[] message) {
//        try {

            System.out.printf("[SERIAL] received the following message: %X", message[0]);
            if(message[0] == ClientMessageProcessor.SENSORS){
                main.getSensorsSampler().novaLeituraSensores(message);
            }
            //        }
            //        String str = String.valueOf(message);
//            String str = new String(message, "ISO-8859-1"); //Converte o array de bytes para string, usando a codificação ISO-8859-1 para preservar cada bit.
//            if (main.getListener().getNumServerConnections() >= 1) {
//                ServerConnection con = main.getListener().getServerConnection(0);
//                con.sendMessage(str, false);
//            }
//        } catch (UnsupportedEncodingException ex) {
//            Logger.getLogger(SerialCommunicator.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    /**
     * Implementing {@link net.Network_iface#writeLog(int, String)}, which is
     * used to write information concerning the connection. In this example, all
     * the information is simply written out to command line.
     *
     * @see net.Network_iface
     */
    public void writeLog(int id, String text) {
//        System.out.println("   log:  |" + text + "|");
    }
}

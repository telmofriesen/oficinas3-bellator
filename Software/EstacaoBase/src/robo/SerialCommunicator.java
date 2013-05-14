package robo;

import comunicacao.ClientMessageProcessor;
import robo.Main;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import robo.gerenciamento.SensorsManager;

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
public class SerialCommunicator extends Thread implements SerialNetwork_iface {

    // set the speed of the serial port
    public static int speed = 115200;
    private static SerialNetwork network;
    private static boolean resend_active = false;
    private Main main;
    private ArrayList<SerialMessage> serialMessages = new ArrayList<SerialMessage>();
    private boolean run = true;

    public SerialCommunicator(Main server) {
        this.main = server;
        network = new SerialNetwork(0, this);

        // initializing reader from command line
        int i, inp_num = 0;
//        String input;
//        BufferedReader in_stream = new BufferedReader(new InputStreamReader(
//                System.in));

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
            System.out.println("enter the id (1,2,...) of the connection to connect to: ");
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

        start();

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

    @Override
    public void run() {
        SerialMessage message;
        int num_elementos = 0;
        synchronized (this) {
            run = true;
        }
        while (run) {
            synchronized (this) {
                num_elementos = serialMessages.size();
            }
            //Enquanto o vetor tiver elementos....
            while (num_elementos > 0) {
                synchronized (this) {
                    message = serialMessages.get(0);
                    //Interpreta a mensagem da posicao 0...

                    //Imprime mensagem de debug
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < message.numBytes; i++) {
                        sb.append(String.format("%X ", message.buffer[i]));
                    }
                    String str = sb.toString();
                    System.out.printf("[SERIAL] received (%d): %s\n", message.numBytes, str);
                    //--------------------------

                    //Calcula o checksum
                    int sum = 0;
                    for (int i = 0; i < message.numBytes - 2; i++) {
                        sum = (sum + ((short) (message.buffer[i]) & 0x00FF)) % 65536;
                    }
//        short sum_short = (short) sum;

                    int checksum = (int) SensorsManager.bytesToShort(message.buffer[message.numBytes - 2], message.buffer[message.numBytes - 1]) & 0x0000FFFF;
                    if (checksum != sum) {
                        System.out.printf("[SERIAL] erro de checksum. sum=%d, checksum=%d, mensagem:%s \n", sum, checksum, str);
                    } else {
                        if (message.buffer[0] == ClientMessageProcessor.SENSORS) {
                            System.out.println("[SERIAL] Sensors\n");
                            main.getSensorsSampler().novaLeituraSensores(message.buffer);
                        }
                    }
                    //Faz a fila andar....
                    serialMessages.remove(0);
                    num_elementos = serialMessages.size();
                }
            }
            synchronized (this) {
                while (serialMessages.isEmpty() && run) { //Enquanto a fila estiver vazia, espera até que hajam elementos.
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ServerMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
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
     * Envia uma mensagem via serial para a placa de baixo nível
     *
     * @param message
     */
    public void sendMessage(byte[] message) {
        System.out.print("[SERIAL] enviando mensagem: ");
        for (int i = 0; i < message.length; i++) {
            System.out.printf("%X ", message[i]);
        }
        network.writeSerial(message.length, message);
    }

    /**
     * Adiciona em uma fila as mensagens recebidas da porta serial (mensagens vindas da placa de baixo nível)
     *
     *
     * Implementing {@link net.Network_iface#parseInput(int, SerialMessage)} to
     * handle messages received over the serial port.
     *
     *
     * @see net.Network_iface
     */
    public synchronized void parseInput(int id, SerialMessage message) {
        serialMessages.add(message);
        this.notifyAll();
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

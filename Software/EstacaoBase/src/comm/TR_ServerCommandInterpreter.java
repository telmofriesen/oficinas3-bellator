package comm;

import controle.ControleSensores;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stefan
 */
//Interpretador de comandos
public class TR_ServerCommandInterpreter extends Thread {

    //Vetor que armazena a fila de comandos
    private ArrayList<String> commandsList = new ArrayList();
    //Referecia ao objeto connector
    private final TR_ServerConnection connection;
    private boolean run = true;
    private boolean terminated;
    
    public TR_ServerCommandInterpreter(TR_ServerConnection connection) {
        this.connection = connection;
        this.setName(this.getClass().getName());
    }
    
    @Override
    public void run() {
        String command;
        boolean run_status = this.run;
        while (run_status) {
            //Enquanto o vetor tiver elementos....
            while (commandsList.size() > 0) {
                synchronized (this) {
                    command = commandsList.get(0);
                }
                //Executa o comando da posicao 0...
                if (!runCommand(command)) {
                    System.out.printf("[TR_ServerCommandInterpreter] Comando invalido recebido: \"%s\"\n", command);
                }
                //Faz a fila andar....
                synchronized (this) {
                    commandsList.remove(0);
                }
            }
            synchronized (this) {
                while (commandsList.isEmpty() && run) { //Enquanto a fila estiver vazia, espera até que hajam elementos.
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TR_ServerCommandInterpreter.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                run_status = this.run;
            }
        }
    }

    /**
     * Adiciona o comado à lista de execução. Ele será posteriormente executado pela thread.
     *
     * @param message
     */
    public synchronized void processCommand(String message) {
        if (message != null && !message.isEmpty()) {
            commandsList.add(message);
            this.notifyAll();
        }
    }

    /**
     * Funcao que interpreta e executa efetivamente um comando.
     *
     * @param command O comando
     * @return true se o comando for executado com sucesso, false caso contrário.
     */
    public boolean runCommand(String command) {
        String[] split = command.split(" ");
        int handshakeStatus = connection.getHandshakeStatus();
        try {
            System.out.println("[TR_ServerCommandInterpreter] Comando recebido: " + command);
            if (split[0].equals("BELLATOR")) {
                if (split[1].equals("HANDSHAKE")) {
                    if (split[2].equals("REQUEST")) {
                        connection.handShakeRequestReceived();
                        connection.sendMessage("BELLATOR HANDSHAKE REPLY");
                        connection.handShakeReplySent();
                        return true;
                    } else if (split[2].equals("REPLY2")) {
                        connection.handShakeReply2Received();
                        return true;
                    }
                }
            } else {
                if (handshakeStatus != TR_ServerConnection.FULL_HANDSHAKE) {
                    System.out.printf("[TR_ServerCommandInterpreter] ERRO: recebido comando sem handshake prévio: \"%s\"\n", command);
                    return false;
                } else if (split[0].equals("KEEPALIVE")) { //KEEPALIVE
                    connection.keepAliveReceived();
                    return true;
                } else if (split[0].equals("ECHO")) { //ECHO
                    if (split[1].equals("REQUEST")) {
                        // Responde ao echo request
                        connection.sendMessageWithPriority("ECHO REPLY"); 
                        return true;
                    } else if (split[1].equals("REPLY")) {
                        connection.echoReplyReceived();
                        return true;
                    }
                } else if (split[0].equals("SENSORS")) { //SENSORS
                    if (split[1].equals("START")) {
                        connection.getServer().startSend_sensor_info();
                        connection.sendMessageWithPriority("SENSORS STATUS STARTED", true);
                        return true;
                    } else if (split[1].equals("STOP")) {
                        connection.getServer().stopSend_sensor_info();
                        connection.sendMessageWithPriority("SENSORS STATUS STOPPED", true);
                        return true;
                    } else if (split[1].equals("STATUS")) {
                        if (split[2].equals("REQUEST")) {
                            if (connection.getServer().isSend_sensor_info()) {
                                connection.sendMessageWithPriority("SENSORS STATUS STARTED", true);
                            } else {
                                connection.sendMessageWithPriority("SENSORS STATUS STOPPED", true);
                            }
                        }
                    } else if (split[1].equals("SAMPLE_RATE")) {
                        float sample_rate = Float.parseFloat(split[2]);
                        connection.getServer().setSample_rate(sample_rate);
                        return true;
                    }
                } else if (split[0].equals("ENGINES")) { //ENGINES
                    float dir = Float.parseFloat(split[1]);
                    float esq = Float.parseFloat(split[2]);
                    connection.getServer().setVelocidadeRodas(dir, esq);
                    return true;
                } else if (split[0].equals("DISCONNECT")) { //DISCONNECT
                    connection.closeConnection();
                    return true;
                }
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] ERRO: Comando com número inválido de parâmetros! \"%s\"\n", command);
        } catch (NumberFormatException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] ERRO: Comando com formato errado de parâmetros! \"%s\"\n", command);
        }
        return false;
    }
    
    public synchronized void terminate() {
        run = false;
        this.notifyAll();
    }
    
    public synchronized boolean isTerminated() {
        return terminated;
    }
}

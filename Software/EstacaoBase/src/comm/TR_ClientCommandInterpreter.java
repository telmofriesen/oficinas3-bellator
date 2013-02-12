package comm;

import controle.ControleSensores;
import controle.NumIRException;
import controle.TimestampException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interpretador de comandos.
 * Os comandos recebidos são inseridos em uma fila, de modo a serem posteriormente executados pela thread.
 * O objetivo é que o Receiver não fique bloqueado para esprar que cada comando seja executado.
 * OBS: Ver especificação do protocolo (Estação base <-> Robô) para maior esclarecimento.
 *
 * @author Stefan
 */
public class TR_ClientCommandInterpreter extends Thread {

    //Array que armazena a fila de comandos
    private ArrayList<String> commandsList = new ArrayList();
    //Referecia ao objeto connector do cliente
    private TR_ClientConnector connector;
    //Referecia ao objeto ControleSensores do cliente
    private ControleSensores controleSensores;
    //Indica se a thread deve rodar ou não
    private boolean run = true;

    public TR_ClientCommandInterpreter(TR_ClientConnector connector, ControleSensores controleSensores) {
        this.connector = connector;
        this.controleSensores = controleSensores;
    }

    @Override
    public void run() {
        String command;
        int num_elementos = 0;
        while (run) {
            synchronized (this) {
                num_elementos = commandsList.size();
            }
            //Enquanto o vetor tiver elementos....
            while (num_elementos > 0) {
                synchronized (this) {
                    command = commandsList.get(0);
                    //Executa o comando da posicao 0...
                    if (!runCommand(command)) {
                        System.out.printf("[TR_ClientCommandInterpreter] Comando invalido recebido: \"%s\"\n", command);
                    }
                    //Faz a fila andar....
                    commandsList.remove(0);
                    num_elementos = commandsList.size();
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
            }
        }
    }

    /**
     * Adiciona um comado à lista de execução. Ele será posteriormente executado pela thread.
     * OBS: Método thread-safe.
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
     * @return true caso haja sucesso na execução, false caso contrário.
     */
    public boolean runCommand(String command) {
        //Separa a string por espaços
        String[] split = command.split(" ");

        try {
            //System.out.println("Comando recebido: " + Command[0]);
            if (command.equals("BELLATOR HANDSHAKE REPLY")) { //BELLATOR HANDSHAKE REPLY
                connector.handShakeReplyReceived();
                return true;
            } else if (command.equals("SERVER FULL")) { //SERVER FULL 
                connector.serverFull();
                return true;
            } else if (split[0].equals("KEEPALIVE")) { //KEEPALIVE
                connector.keepAliveReceived();
                return true;
            } else if (split[0].equals("ECHO")) { //ECHO
                if (split[1].equals("REQUEST")) {
                    // Responde ao echo request
                    connector.sendMessageWithPriority("ECHO REPLY", true);
                    return true;
                } else if (split[1].equals("REPLY")) {
                    connector.echoReplyReceived();
                    return true;
                }
            } else if (split[0].equals("SENSORS")) { //SENSORS
                if (split[1].equals("SAMPLE")) {
                    float aceleracao = Float.parseFloat(split[2]);
                    float aceleracao_angular = Float.parseFloat(split[3]);
                    int num_IR_recebido = split.length - 5;
                    float[] dist_IR = new float[num_IR_recebido];
                    for (int i = 0, j = 4; j <= split.length - 2; i++, j++) {
                        dist_IR[i] = Float.parseFloat(split[j]);
                    }
                    long timestamp = Long.parseLong(split[split.length - 1]);
//                    System.out.printf("[TR_ServerCommandInterpreter] SENSORS SAMPLE %f %f ... %d\n", aceleracao, aceleracao_angular, timestamp);
                    controleSensores.novaLeituraSensores(aceleracao, aceleracao_angular, dist_IR, timestamp);
                    return true;
                } else if (split[1].equals("STATUS")) {
                    if (split[2].equals("STOPPED")) {
                        controleSensores.setSensorSampleStatus(ControleSensores.SAMPLE_STOPPED);
                        return true;
                    } else if (split[2].equals("STARTED")) {
                        controleSensores.setSensorSampleStatus(ControleSensores.SAMPLE_STARTED);
                        return true;
                    }
                }
            } else if (split[0].equals("DISCONNECT")) { //DISCONNECT
                connector.closeConnection();
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] ERRO: Comando com número inválido de parâmetros! \"%s\"\n", command);
            return false;
        } catch (NumberFormatException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] ERRO: Comando com formato errado de parâmetros! \"%s\"\n", command);
            return false;
        } catch (NumIRException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] %s \"%s\"\n", ex.getMessage(), command);
            return false;
        } catch (TimestampException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] %s \"%s\"\n", ex.getMessage(), command);
            return false;
        }
        return false;
    }

    /**
     * Requisita a finalização da Thread.
     * A finalização é feita amigavelmente, ou seja, aguarda-se que o loop principal finalize.
     */
    public synchronized void terminate() {
        run = false;
        this.notifyAll();
    }

    public void setConnector(TR_ClientConnector Connector) {
        this.connector = Connector;
    }
}
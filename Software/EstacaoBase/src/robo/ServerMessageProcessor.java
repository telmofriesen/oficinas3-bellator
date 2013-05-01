package robo;

import robo.gerenciamento.SensorsManager;
import robo.gerenciamento.WebcamManager;
import robo.gerenciamento.old.WebcamManagerNew;
import robo.gerenciamento.old.WebcamManagerNew2;
import robo.gerenciamento.old.WebcamSampler;
import java.awt.Dimension;
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
public class ServerMessageProcessor extends Thread {

    //Vetor que armazena a fila de comandos
    private ArrayList<String> commandsList = new ArrayList();
    //Referecia ao objeto connector
    private final ServerConnection connection;
    //Indica se a thread deve rodar ou não
    private boolean run = true;

    public ServerMessageProcessor(ServerConnection connection) {
        this.connection = connection;
        this.setName(this.getClass().getName());
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
                        System.out.printf("[TR_ServerCommandInterpreter] Comando invalido recebido: \"%s\"\n", command);
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
                        Logger.getLogger(ServerMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Adiciona o comado à lista de execução. Ele será posteriormente executado pela thread.
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
                        connection.sendMessageWithPriority("BELLATOR HANDSHAKE REPLY", true);
                        connection.handShakeReplySent();
                        return true;
                    } else if (split[2].equals("REPLY2")) {
                        connection.handShakeReply2Received();
                        return true;
                    }
                }
            } else {
                if (handshakeStatus != ServerConnection.HANDSHAKE_FULL) {
                    System.out.printf("[TR_ServerCommandInterpreter] ERRO: recebido comando sem handshake prévio: \"%s\"\n", command);
                    return false;
                } else if (split[0].equals("KEEPALIVE")) { //KEEPALIVE
                    connection.keepAliveReceived();
                    return true;
                } else if (split[0].equals("ECHO")) { //ECHO
                    if (split[1].equals("REQUEST")) {
                        // Responde ao echo request
                        connection.sendMessageWithPriority("ECHO REPLY", true);
                        return true;
                    } else if (split[1].equals("REPLY")) {
                        connection.echoReplyReceived();
                        return true;
                    }
                } else if (split[0].equals("SENSORS")) { //SENSORS
                    SensorsManager s = connection.getListener().getServer().getSensorsSampler();
                    if (split[1].equals("START")) {
                        s.startSampling();
                        connection.sendMessageWithPriority(s.getStatusMessage(), true);
                        return true;
                    } else if (split[1].equals("STOP")) {
                        s.stopSampling();
                        connection.sendMessageWithPriority(s.getStatusMessage(), true);
                        return true;
                    } else if (split[1].equals("STATUS")) {
                        if (split[2].equals("REQUEST")) {
                            connection.sendMessageWithPriority(s.getStatusMessage(), true);
                            return true;
                        }
                    } else if (split[1].equals("SAMPLE_RATE")) {
                        float sample_rate = Float.parseFloat(split[2]);
                        connection.getListener().getServer().getSensorsSampler().setSample_rate(sample_rate);
                        return true;
                    }
                } else if (split[0].equals("WEBCAM")) { //WEBCAM
                    final WebcamManagerNew2 w = connection.getListener().getServer().getWebcamManager();
                    if (split[1].equals("START")) {
                        new Thread() {
                            public void run() {
                                w.startSampling();
                            }
                        }.start();
//                        connection.sendMessageWithPriority(w.getStatusMessage(), true);
                        return true;
                    } else if (split[1].equals("STOP")) {
                        w.stopSampling();
//                        connection.sendMessageWithPriority(w.getStatusMessage(), true);
                        return true;
                    } else if (split[1].equals("STATUS")) {
                        if (split[2].equals("REQUEST")) {
                            connection.sendMessageWithPriority(w.getStatusMessage(), true);
                            return true;
                        }
                    } else if (split[1].equals("FRAMERATE")) {
                        float fps = Float.parseFloat(split[2]);
                        w.setFps(fps);
                        return true;
                    } else if (split[1].equals("BITRATE")) {
                        int bitrate = Integer.parseInt(split[2]);
                        w.setBitrate(bitrate);
                        return true;
                    } else if (split[1].equals("RESOLUTION")) {
                        String[] split_resolution = split[2].split("x");
                        int width = Integer.parseInt(split_resolution[0]);
                        int height = Integer.parseInt(split_resolution[1]);
                        w.setResolution(new Dimension(width, height));
                        return true;
                    }
                } else if (split[0].equals("ENGINES")) { //ENGINES
                    float left = Float.parseFloat(split[1]);
                    float right = Float.parseFloat(split[2]);
                    connection.getListener().getServer().getEnginesManager().setEnginesSpeed(left, right);

//                    connection.getListener().getServer().setVelocidadeRodas(dir, esq);
                    return true;
//                    System.out.printf("");
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

    /**
     * Requisita a finalização da Thread.
     * A finalização é feita amigavelmente, ou seja, aguarda-se que o loop principal finalize.
     */
    public synchronized void terminate() {
        run = false;
        this.notifyAll();
    }

    /**
     * Força a finalização da thread.
     */
    public void kill() {
        this.terminate();
    }
}

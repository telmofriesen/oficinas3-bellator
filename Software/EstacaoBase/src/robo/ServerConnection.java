/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robo;

import comunicacao.SenderMessage;
import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe que representa uma conexão do servidor. Contém os objetos necessários às conexões (Sender, Receiver, Interpreter).
 * IMPORTANTE: não esquecer de chamar startThreads() para iniciar as threads necessárias.
 *
 * @author stefan
 */
public class ServerConnection extends Thread {
    //==========================================
    //========= Constantes =====================
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Constantes">

    /**
     * HANDSHAKE: Sem atividade de handshake
     */
    public static final int HANDSHAKE_NO = 0;
    /**
     * HANDSHAKE REQUEST recebido
     */
    public static final int HANDSHAKE_REQUEST_RECEIVED = 1;
    /**
     * HANDSHAKE REPLY enviado
     */
    public static final int HANDSHAKE_REPLY_SENT = 2;
    /**
     * HANDSHAKE REPLY2 recebido (HANDSHAKE COMPLETO)
     */
    public static final int HANDSHAKE_FULL = 3;
    //</editor-fold>
    //==========================================
    //========= Variáveis ======================
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Variaveis">
    private boolean connected = true; //true até que disconnect() seja chamado
    private ServerReceiver receiver;
    private ServerSender sender;
    //Listener do servidor
    private ServerListener listener;
    //Interpretador de comandos
    private ServerMessageProcessor processor;
    //O socket da conexão aceita
    private Socket sock;
    //Estado atual de handshake
    private int handshakeStatus;
    //Horario (milissegundos) da ultima atividade de handshake.
    private long lastHandshakeActivity;
    //Tempo máximo (ms) para haver timeout de handshake.
    private int handshakeTimeout = 5000;
    //Horario em milissegundos da ultima requisicao de envio de pacote
    private long last_package_sent_time;
    //Horario em milissegundos do último recebimento de pacote
    private long last_package_received_time;
    //Intervalo máximo para envio de pacotes. Usado para enviar KEEPALIVE quando necessário
    private int package_send_interval_max = 2000;
    //Intervalos máximos para recebimento de pacotes:
    private int package_recv_interval_echo = 3000; //Envia um ECHO REQUEST
    private int package_recv_interval_warning = 5000; //Entra em estado de Warning
    private int package_recv_interval_timeout = 15000; //Timeout (desconecta do host)
    //Indica se o loop principal deve executar ou não.
    private boolean run;
    //</editor-fold>

    public ServerConnection(ServerListener listener, Socket sock) {
        this.listener = listener;
        this.sock = sock;
        this.receiver = new ServerReceiver(this);
        this.sender = new ServerSender(this);
        this.processor = new ServerMessageProcessor(this);
        lastHandshakeActivity = System.currentTimeMillis();
        handshakeStatus = HANDSHAKE_NO;
        this.setName(this.getClass().getName());
        run = true;
    }

    /**
     * Inicia as threads necessárias à conexão.
     */
    public void startThreads() {
        receiver.start();
        sender.start();
        processor.start();
        this.start();
    }

    @Override
    public void run() {
        long current_time = System.currentTimeMillis();
        last_package_sent_time = System.currentTimeMillis();
        last_package_received_time = current_time;
        while (true) {
            synchronized (this) {
                //Finaliza a execução do loop se for ordenado o término da thread (instrução dentro do bloco synchronized).
                if (!run) break;
                current_time = System.currentTimeMillis();
                if (handshakeStatus != HANDSHAKE_FULL) {
                    if (current_time - lastHandshakeActivity > handshakeTimeout) {//Atingido o tempo máximo sem haver handshake
                        System.out.println("[TR_ServerConnection] Atingido o tempo máximo sem haver handshake. Desconectando...");
                        this.disconnect(); //Desconecta do cliente e remove a conexão.  
                    }
                } else {
                    if (current_time - last_package_sent_time > package_send_interval_max) { //KEEPALIVE
                        //Se nenhum pacote tiver sido enviado no intervalo máximo de tempo...
                        //Envia um KEEPALIVE
                        sendKeepAlive();
                    }
                    long difftime = current_time - last_package_received_time;
                    if (difftime > package_recv_interval_timeout) { //TIMEOUT
                        //Se nenhum pacote tiver sido recebido no intervalo de tempo para timeout...
                        //Desconecta o socket.
                        System.out.printf("[TR_ServerConnection] Timeout\n");
                        //Para o robô por precaução
                        listener.getServer().getEnginesManager().setEnginesSpeed(0, 0);
                        closeConnection();
                    } else if (difftime > package_recv_interval_warning) { //WARNING
                        //Se nenhum pacote tiver sido recebido no intervalo de tempo para warning...
                        //Significa que a conexão está com falha de tempo de comunicação (apesar de o socket não estar fechado ainda)
                        System.out.printf("[TR_ServerConnection] Aviso: tempo excessivo sem receber mensagens: %d ms\n", current_time - last_package_received_time);
                        //Para o robô por precaução
                        listener.getServer().getEnginesManager().setEnginesSpeed(0, 0);
                        sendEchoRequest();
//                        setError(WARNING_COMMUNICATION_TIME, String.format("%d ms", current_time - last_package_received_time));
                    } else if (difftime > package_recv_interval_echo) { //ECHO
                        //Se nenhum pacote tiver sido recebido no intervalo de ECHO...
                        //Envia um "echo" para que o robo responda
                        sendEchoRequest();
                    }
                }
            }
            try {
                //"Dorme" por certo tempo para deixar outras threads executarem.
                sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //==========================================
    //========= Gerenciamento de conexão =======
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Gerenciamento de conexão">
    public synchronized boolean isConnected() {
        return connected;
    }

    /**
     * Desconecta corretamente do host (enviando mensagem de DISCONNECT e fechando a conexão).
     */
    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        sendMessageWithPriority("DISCONNECT", true);
        closeConnection();
    }

    /**
     * Fecha a conexão.
     */
    public void closeConnection() {
        synchronized (this) {
            if (connected == false) {
                return;
            }
            connected = false;
            run = false;
        }

        //Requisita finalização das threads
        sender.terminate();
        receiver.terminate();
        processor.terminate();
        int count = 0;
        //Espera o término das threads
        while (sender.isAlive() && receiver.isAlive() && processor.isAlive() && count < 5) {
            try {
                System.out.printf("[TR_ServerConnection] Aguardando sender, receiver e interpreter finalizarem.... (%d)\n", count);
                sleep(500);
                count++;
            } catch (InterruptedException ex) {
                Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Força o término das threads 
        sender.kill();
        receiver.kill();
        processor.kill();
        try {
            synchronized (this) {
                //Fecha o socket
                sock.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Remove esta conexão do listener.
//        listener.getServer().stopSend_sensor_info();
        listener.removeConnection(this);
        System.out.println("[TR_ServerConnection] Desconectado do host: " + sock);
    }

    public synchronized void handShakeRequestReceived() {
        if (handshakeStatus == HANDSHAKE_NO) {
            handshakeStatus = HANDSHAKE_REQUEST_RECEIVED;
            lastHandshakeActivity = System.currentTimeMillis();
        } else {
            System.out.println("[TR_ServerConnection] ERRO: HANDSHAKE REQUEST recebido erroneamente.");
            disconnect();
        }
    }

    public synchronized void handShakeReplySent() {
        if (handshakeStatus == HANDSHAKE_REQUEST_RECEIVED) {
            handshakeStatus = HANDSHAKE_REPLY_SENT;
            lastHandshakeActivity = System.currentTimeMillis();
        } else {
            System.out.println("[TR_ServerConnection] ERRO: HANDSHAKE REPLY enviado erroneamente.");
            disconnect();
        }
    }

    public synchronized void handShakeReply2Received() {
        if (handshakeStatus == HANDSHAKE_REPLY_SENT) {
            handshakeStatus = HANDSHAKE_FULL;
            lastHandshakeActivity = System.currentTimeMillis();
        } else {
            System.out.println("[TR_ServerConnection] ERRO: HANDSHAKE REPLY2 recebido erroneamente.");
            disconnect();
        }
    }

    public int getHandshakeStatus() {
        return handshakeStatus;
    }
    //</editor-fold>    
    //==========================================
    //===== Gerenciamento de mensagens =========
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Gerenciamento de mensagens">

    /**
     * Informa que um pacote foi recebido pelo Receiver.
     */
    public synchronized void messageReceived() {
        last_package_received_time = System.currentTimeMillis();
    }

    /**
     * Envia requisição de ECHO.
     */
    public void sendEchoRequest() {
        sendMessageWithPriority("ECHO REQUEST", true); //envia pacote echo para verificar conectividade.
    }

    /**
     * Informa que uma resposta de ECHO foi recebida.
     */
    public synchronized void echoReplyReceived() {
        last_package_received_time = System.currentTimeMillis();
    }

    /**
     * Envia um KEEPALIVE.
     */
    private void sendKeepAlive() {
        sendMessageWithPriority("KEEPALIVE", true);
    }

    /**
     * Informa que um KEEPALIVE foi recebido.
     */
    public synchronized void keepAliveReceived() {
        last_package_received_time = System.currentTimeMillis();
    }

    /**
     * Adiciona uma mensagem à fila prioritária de envio.
     *
     * @param message Mensagem a ser enviada.
     * @param flush_buffer Indica se um flush no buffer deve ser feito.
     */
    public synchronized void sendMessageWithPriority(String message, boolean flush_buffer) {
        sender.sendMessageWithPriority(message, flush_buffer);
        last_package_sent_time = System.currentTimeMillis();
    }

    /**
     * Adiciona uma mensagem à fila prioritária de envio.
     *
     * @param senderMessage Mensagem a ser enviada.
     */
    public synchronized void sendMessageWithPriority(SenderMessage senderMessage) {
        sender.sendMessageWithPriority(senderMessage);
        last_package_sent_time = System.currentTimeMillis();
    }

    /**
     * Adiciona uma mensagem à fila de envio.
     *
     * @param message Mensagem a ser enviada.
     * @param flush_buffer Indica se um flush no buffer deve ser feito.
     */
    public synchronized void sendMessage(String message, boolean flush_buffer) {
        sender.sendMessage(message, flush_buffer);
        last_package_sent_time = System.currentTimeMillis();
    }

    /**
     * Adiciona uma mensagem à fila de envio.
     *
     * @param senderMessage Mensagem a ser enviada.
     */
    public synchronized void sendMessage(SenderMessage senderMessage) {
        sender.sendMessage(senderMessage);
        last_package_sent_time = System.currentTimeMillis();
    }

    /**
     * Adiciona um comando à fila de execução.
     *
     * @param command O comando a ser executado.
     */
    public void processCommand(String command) {
        processor.processCommand(command);
    }
    //</editor-fold>
    //==========================================
    //========= Outros Getters e Setters =======
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Outros Getters e Setters">

    public ServerReceiver getReceiver() {
        return receiver;
    }

    public ServerSender getSender() {
        return sender;
    }

    public Socket getSock() {
        return sock;
    }

    public synchronized ServerMessageProcessor getInterpreter() {
        return processor;
    }

    public ServerListener getListener() {
        return listener;
    }
    //</editor-fold>
}

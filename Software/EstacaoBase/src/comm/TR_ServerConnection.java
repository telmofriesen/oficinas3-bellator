/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.io.IOException;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe que contém os objetos necessários às conexões.
 *
 * @author stefan
 */
public class TR_ServerConnection extends Thread {

    private boolean connected = true; //true até que disconnect() seja chamado
    private TR_ServerReceiver receiver;
    private TR_ServerSender sender;
    //Objeto linstener
    private Server server;
    private TR_ServerCommandInterpreter interpreter;
    //O socket da conexão aceita
    private Socket sock;
    private TR_ServerListener listener;
    private boolean run = true;
//    private boolean handshaked = false;
    public static final int NO_HANDSHAKE = 0, //Sem atividade de handshake
            REQUEST_RECEIVED_HANDSHAKE = 1, //REQUEST recebido 
            REPLY_SENT_HANDSHAKE = 2,//REPLY enviado
            FULL_HANDSHAKE = 3; //REPLY2 recebido
    private int handshakeStatus;
    private long lastHandshakeActivity;
    private int handshakeTimeout = 5000; //5 segundos (5000 ms) de limite para que o cliente envia um "BELLATOR HANDSHAKE REQUEST"
    private long last_package_sent_time; //Horario em milissegundos da ultima requisicao de envio de pacote
    private long last_package_received_time; //Horario em milissegundos do último recebimento de pacote
    private int package_send_interval_max = 2000;
    private int package_recv_interval_echo = 4000;
    private int package_recv_interval_warning = 8000;
    private int package_recv_interval_timeout = 15000;

    public TR_ServerConnection(Server server, TR_ServerListener listener, Socket sock) {
        this.server = server;
        this.listener = listener;
        this.sock = sock;
        this.receiver = new TR_ServerReceiver(this);
        this.sender = new TR_ServerSender(this);
        this.interpreter = new TR_ServerCommandInterpreter(this);
        lastHandshakeActivity = System.currentTimeMillis();
        handshakeStatus = NO_HANDSHAKE;
        this.setName(this.getClass().getName());
    }

    @Override
    public void run() {
        long current_time = System.currentTimeMillis();
        last_package_sent_time = System.currentTimeMillis();
        last_package_received_time = current_time;
        while (run) {
            synchronized (this) {
                current_time = System.currentTimeMillis();
                if (handshakeStatus != FULL_HANDSHAKE) {
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
                        closeConnection();
                    } else if (difftime > package_recv_interval_warning) { //WARNING
                        //Se nenhum pacote tiver sido recebido no intervalo de tempo para warning...
                        //Significa que a conexão está com falha de tempo de comunicação (apesar de o socket não estar fechado ainda)
                        System.out.printf("[TR_ServerConnection] Aviso: tempo excessivo sem receber mensagens: %d ms\n", current_time - last_package_received_time);
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
                sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(TR_ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void processCommand(String command) {
        interpreter.processCommand(command);
    }

    public void startThreads() {
        receiver.start();
        sender.start();
        interpreter.start();
        this.start();
    }

    public void sendEchoRequest() {
        sendMessageWithPriority("ECHO REQUEST"); //envia pacote echo para verificar conectividade.
    }

    public synchronized void echoReplyReceived() {
        last_package_received_time = System.currentTimeMillis();
    }

    private void sendKeepAlive() {
        sendMessageWithPriority("KEEPALIVE", true);
    }

    public synchronized void keepAliveReceived() {
        last_package_received_time = System.currentTimeMillis();
    }

    public synchronized void handShakeRequestReceived() {
        if (handshakeStatus == NO_HANDSHAKE) {
            handshakeStatus = REQUEST_RECEIVED_HANDSHAKE;
            lastHandshakeActivity = System.currentTimeMillis();
        } else {
            System.out.println("[TR_ServerConnection] ERRO: HANDSHAKE REQUEST recebido erroneamente.");
            disconnect();
        }
    }

    public synchronized void handShakeReplySent() {
        if (handshakeStatus == REQUEST_RECEIVED_HANDSHAKE) {
            handshakeStatus = REPLY_SENT_HANDSHAKE;
            lastHandshakeActivity = System.currentTimeMillis();
        } else {
            System.out.println("[TR_ServerConnection] ERRO: HANDSHAKE REPLY enviado erroneamente.");
            disconnect();
        }
    }

    public synchronized void handShakeReply2Received() {
        if (handshakeStatus == REPLY_SENT_HANDSHAKE) {
            handshakeStatus = FULL_HANDSHAKE;
            lastHandshakeActivity = System.currentTimeMillis();
        } else {
            System.out.println("[TR_ServerConnection] ERRO: HANDSHAKE REPLY2 recebido erroneamente.");
            disconnect();
        }
    }

    public int getHandshakeStatus() {
        return handshakeStatus;
    }

    public synchronized void sendMessageWithPriority(String message) {
        sender.sendMessageWithPriority(message);
        last_package_sent_time = System.currentTimeMillis();
    }

    public synchronized void sendMessageWithPriority(String message, boolean flush_buffer) {
        sender.sendMessageWithPriority(message, flush_buffer);
        last_package_sent_time = System.currentTimeMillis();
    }

    public synchronized void sendMessageWithPriority(SenderMessage senderMessage) {
        sender.sendMessageWithPriority(senderMessage);
        last_package_sent_time = System.currentTimeMillis();
    }

    public void sendMessage(String message) {
        sender.sendMessage(message);
        last_package_sent_time = System.currentTimeMillis();
    }

    public synchronized void sendMessage(String message, boolean flush_buffer) {
        sender.sendMessage(message, flush_buffer);
        last_package_sent_time = System.currentTimeMillis();
    }

    public synchronized void sendMessage(SenderMessage senderMessage) {
        sender.sendMessage(senderMessage);
        last_package_sent_time = System.currentTimeMillis();
    }

    public synchronized void packetReceived() {
        last_package_received_time = System.currentTimeMillis();
    }

    public TR_ServerReceiver getReceiver() {
        return receiver;
    }

    public TR_ServerSender getSender() {
        return sender;
    }

    public Socket getSock() {
        return sock;
    }

    public Server getServer() {
        return server;
    }

    public synchronized TR_ServerCommandInterpreter getInterpreter() {
        return interpreter;
    }

    public synchronized boolean isConnected() {
        return connected;
    }

    public synchronized void disconnect() {
        if (!connected) {
            return;
        }
        sendMessage("DISCONNECT");
        closeConnection();
    }

    public void closeConnection() {
        synchronized (this) {
            if (connected == false) {
                return;
            }
            connected = false;
            run = false;
        }

        sender.terminate();
        receiver.terminate();
        interpreter.terminate();
        while (sender.isAlive() && interpreter.isAlive()) {
            try {
//                System.out.println("Sender2 " + sender.isAlive() + " " + interpreter.isAlive());
                sleep(200); //Espera todas as threads finalizarem (menos o receiver, pois quando o socket for finalizado ele o será também, pois uma exceção será lançada internamente nele)
            } catch (InterruptedException ex) {
                Logger.getLogger(TR_ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        try {
            synchronized (this) {
                sock.close();
            }
        } catch (IOException ex) {
            Logger.getLogger(TR_ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }
        listener.removeConnection(this);
        server.stopSend_sensor_info();
        System.out.println("[TR_ServerConnection] Desconectado do host: " + sock);
    }
}

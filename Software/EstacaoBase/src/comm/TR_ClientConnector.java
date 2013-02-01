package comm;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Não esquecer de chamar o metodo start() para iniciar a Thread!
 *
 * @author Stefan
 */
public class TR_ClientConnector extends Thread {

    /**
     * Estados de conexão
     */
    public static final int DISCONNECTED = 0,
            CONNECTING = 1,
            CONNECTED_HANDSHAKE = 2,
            CONNECTED = 3;
    /**
     * Estados de erro
     */
    public static final int ERROR_NO = 0,
            ERROR_TIMEOUT = 1,
            WARNING_COMMUNICATION_TIME = 2,
            ERROR_UNKNOWN_HOST = 3,
            ERROR_IO = 4,
            ERROR_HANDSHAKE_TIMEOUT = 5,
            ERROR_HANDSHAKE = 6,
            ERROR_SERVER_FULL = 7;
    /**
     * Estados de handshake
     */
    public static final int HANDSHAKE_NO = 0, //Sem atividade de handshake 
            HANDSHAKE_REQUEST_SENT = 1, //Enviou REQUEST
            HANDSHAKE_HALF = 2, //Recebeu REPLY
            HANDSHAKE_FULL = 3; //Enviou REPLY2
    private int handshakeStatus;
    private long lastHandshakeActivity;
    private int handshakeTimeout = 5000; //5 segundos (5000 ms) de limite para que o cliente envia um "BELLATOR HANDSHAKE REQUEST"
    //Estado de conexão atual
    private int connectionStatus;
    //Estado de erro atual
    private int errorStatus;
    //Mensagem de erro atual
    private String errorMessage = "";
    //Socket da conexão atual
    private Socket sock;
    //Sender (thread que envia mensagens)
    TR_ClientReceiver receiver;
    //Receiver (thread que recebe mensagens)
    TR_ClientSender sender;
    //Interpretador de comandos (thread que interpreta comandos, ou seja, mensagens vindas do Receiver). 
    //Por padrão não há interpretador, ele deve ser adicionado externamente pelo método setInterpreter().
    public TR_ClientCommandInterpreter interpreter = null;
    //Host atual de conexão
    private String host;
    //Porta atual de conexão
    private int port;
    private long last_package_sent_time; //Horario em milissegundos da ultima requisicao de envio de pacote
    private long last_package_received_time; //Horario em milissegundos do último recebimento de pacote
    private int package_send_interval_max = 1000;
    private int package_recv_interval_echo = 1000;
    private int package_recv_interval_warning = 8000; //Especifica o tempo máximo entre cada pacote recebido
    private int package_recv_interval_timeout = 15000;
    //Listeners. "Escutam" mudanças de status nesta classe (recurso usado pela interface gráfica principalmente).
    private final CopyOnWriteArrayList<MyChangeListener> listeners;

    public TR_ClientConnector() {
//        this.interpreter = interpreter;
//        receiver.start();
//        sender.start();
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        setConnectionStatus(DISCONNECTED, false);
        setError(ERROR_NO, "");
        handshakeStatus = HANDSHAKE_NO;
    }

    public synchronized void disconnect() {
        if (isConnected()) sendMessageWithPriority("DISCONNECT");
        closeConnection();
    }

    public void closeConnection() {
        synchronized (this) {
            if (connectionStatus != CONNECTED && connectionStatus != CONNECTED_HANDSHAKE) {
                return;
            }
        }
        setConnectionStatus(DISCONNECTED, false);
        setError(ERROR_NO, "");

        sender.terminate();
        receiver.terminate();

        while (sender.isAlive()) {
            try {
                //Espera o sender finalizar.
                sleep(200);
            } catch (InterruptedException ex) {
                Logger.getLogger(TR_ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        try {
            sock.close();
        } catch (IOException ex) {
            setError(ERROR_IO, ex.getMessage());
            Logger.getLogger(TR_ClientConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        setConnectionStatus(DISCONNECTED, false);
        setError(ERROR_NO, "");
        System.out.println("[TR_ServerConnection] Desconectado do host");
    }

    /**
     * Agenda uma conexão a um host. A conexão será feita posteriormente pela
     * thread.
     *
     * @param host
     * @param port
     * @throws UnknownHostException
     * @throws IOException
     */
    public synchronized void connect(String host, int port) {
        sender = new TR_ClientSender(this);
        receiver = new TR_ClientReceiver(this);
        this.host = host;
        this.port = port;
        setConnectionStatus(CONNECTING, false);
        handshakeStatus = HANDSHAKE_NO;
        setError(ERROR_NO, "");
        this.notifyAll();
    }

    /**
     * Conecta efetivamente ao host.
     */
    public synchronized void _connect() {
        try {
            sock = new Socket(host, port);
            if (sock.isConnected()) {
//                setConnectionStatus(CONNECTED);
                setError(ERROR_NO, "");
                receiver.start();
                sender.start();
                long current_time = System.currentTimeMillis();
                //Envia o request de handshake
                sendMessageWithPriority("BELLATOR HANDSHAKE REQUEST");
                //Muda os status
                assert (handshakeStatus == HANDSHAKE_NO);
                handshakeStatus = HANDSHAKE_REQUEST_SENT;
                assert (connectionStatus == CONNECTING);
                setConnectionStatus(CONNECTED_HANDSHAKE);
                lastHandshakeActivity = current_time;
//                last_package_sent_time = System.currentTimeMillis();
                last_package_received_time = current_time;
            }
        } catch (UnknownHostException ex) {
            setError(ERROR_UNKNOWN_HOST, ex.getMessage(), false);
            setConnectionStatus(DISCONNECTED);
//            Logger.getLogger(TR_ClientConnector.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            setError(ERROR_IO, ex.getMessage(), false);
            setConnectionStatus(DISCONNECTED);
//            Logger.getLogger(TR_ClientConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    while (connectionStatus == DISCONNECTED) { //Se a conexão estiver fechada, espera até outra conexão ser feita.
                        fireChangeEvent();
                        this.wait();
                    }

                    if (connectionStatus == CONNECTING) {
                        _connect();
                    } else if (connectionStatus == CONNECTED_HANDSHAKE) {
                        long current_time = System.currentTimeMillis();
                        if (handshakeStatus != HANDSHAKE_FULL && current_time - lastHandshakeActivity > handshakeTimeout) {
                            setError(ERROR_HANDSHAKE_TIMEOUT, String.format("%d ms", current_time - lastHandshakeActivity));
                            this.disconnect();
                        }
                    } else if (connectionStatus == CONNECTED) {
                        long current_time = System.currentTimeMillis();
                        if (current_time - last_package_sent_time > package_send_interval_max) { //KEEPALIVE
                            //Se nenhum pacote tiver sido enviado no intervalo máximo de tempo...
                            //Envia um KEEPALIVE
                            sendKeepAlive();
                        }
                        long difftime = current_time - last_package_received_time;
                        if (difftime > package_recv_interval_timeout) { //TIMEOUT
                            //Se nenhum pacote tiver sido recebido no intervalo de tempo para timeout...
                            //Desconecta o socket.
                            setError(ERROR_TIMEOUT, String.format("%d ms", current_time - last_package_received_time));
                            closeConnection();
                        } else if (difftime > package_recv_interval_warning) { //WARNING
                            //Se nenhum pacote tiver sido recebido no intervalo de tempo para warning...
                            //Significa que a conexão está com falha de tempo de comunicação (apesar de o socket não estar fechado ainda)
                            setError(WARNING_COMMUNICATION_TIME, String.format("%d ms", current_time - last_package_received_time));
                            sendEchoRequest();
                        } else if (difftime > package_recv_interval_echo) { //ECHO
                            //Se nenhum pacote tiver sido recebido no intervalo de ECHO...
                            //Envia um "echo" para que o robo responda
                            sendEchoRequest();
                        }
                    } else {
                        setError(ERROR_NO, "");
                    }
                }
                sleep(500);
            } catch (InterruptedException ex) {
                errorMessage = ex.getMessage();
                Logger.getLogger(TR_ClientConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public synchronized void packetReceived() {
        last_package_received_time = System.currentTimeMillis();
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

    public synchronized void handShakeReplyReceived() {
        if (handshakeStatus == HANDSHAKE_REQUEST_SENT) {
            handshakeStatus = HANDSHAKE_HALF;
            lastHandshakeActivity = System.currentTimeMillis();
            //Envia o REPLY2
            sendMessageWithPriority("BELLATOR HANDSHAKE REPLY2");
            handShakeReply2Sent();
        } else {
            System.out.println("[TR_ClientConnector] ERRO: HANDSHAKE REPLY recebido erroneamente.");
            disconnect();
        }
    }

    public synchronized void handShakeReply2Sent() {
        if (handshakeStatus == HANDSHAKE_HALF) {
            handshakeStatus = HANDSHAKE_FULL;
            lastHandshakeActivity = System.currentTimeMillis();
            setError(ERROR_NO, "", false);
            setConnectionStatus(CONNECTED);
            sendMessageWithPriority("SENSORS STATUS REQUEST", true);
//            sendMessage("SENSORS START"); //TODO remover essa linha. Apenas para testes temporarios
        } else {
            System.out.println("[TR_ClientConnector] ERRO: HANDSHAKE REPLY2 enviado erroneamente.");
            disconnect();
        }
    }

    public synchronized void serverFull() {
        if (connectionStatus == CONNECTED_HANDSHAKE) {
            setError(ERROR_SERVER_FULL, "");
            disconnect();
        }
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

    public synchronized void sendMessage(String message) {
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

    public void processCommand(String message) {
        if (interpreter == null) {
            System.out.printf("[TR_ClientConnector] Não há interpretador de comandos registrado! Mensagem recebida: %s\n", message);
        } else {
            interpreter.processCommand(message);
        }
    }

    public synchronized void addMyChangeListener(MyChangeListener l) {
        this.listeners.add(l);
        fireChangeEvent();
    }

    public synchronized void removeMyChangeListener(MyChangeListener l) {
        this.listeners.remove(l);
    }

    // Event firing method.  Called internally by other class methods.
    //Notifica os listeners de que alguma modificacao foi feita.
    protected synchronized void fireChangeEvent() {
        MyChangeEvent evt = new MyChangeEvent(this);
        for (MyChangeListener l : listeners) {
            l.changeEventReceived(evt);
        }
    }

    public boolean isConnected() {
        return connectionStatus == CONNECTED;
    }

    /**
     * Muda o status da conexão e notifica os listeners sobre a mudança.
     *
     * @param connectionStatus O novo status.
     */
    private synchronized void setConnectionStatus(int connectionStatus) {
        setConnectionStatus(connectionStatus, true);
    }

    /**
     * Muda o status da conexão.
     *
     * @param connectionStatus O novo status.
     * @param fireChangeEvt Se um evento de mudança deve ser lançado ou não (notificar os listeners).
     */
    private synchronized void setConnectionStatus(int connectionStatus, boolean fireChangeEvt) {
        this.connectionStatus = connectionStatus;
        if (fireChangeEvt) fireChangeEvent();
    }

    private synchronized void setError(int errorStatus, String message) {
        setError(errorStatus, message, true);
    }

    private synchronized void setError(int errorStatus, String message, boolean fireChangeEvt) {
        this.errorStatus = errorStatus;
        this.errorMessage = message;
        if (fireChangeEvt) fireChangeEvent();
    }

//    private synchronized void setErrorStatus(int error_status) {
//        setErrorStatus(error_status, true);
//    }
//    /**
//     * Muda o status de erro.
//     *
//     * @param connection_status O novo status.
//     * @param fireChangeEvt Se um evento de mudança deve ser lançado ou não (notificar os listeners).
//     */
//    private synchronized void setErrorStatus(int error_status, boolean fireChangeEvt) {
//        this.errorStatus = error_status;
//        if (fireChangeEvt)
//            fireChangeEvent();
//    }
    public synchronized int getErrorStatus() {
        return errorStatus;
    }

    public synchronized String getErrorMessage() {
        return errorMessage;
    }

    public synchronized int getConnectionStatus() {
        return connectionStatus;
    }

    public synchronized String getConnectionStatusString() {
        return getConnectionStatusString(connectionStatus);
    }

    /**
     * Retorna uma String descrevendo o status indicado.
     *
     * @param status
     * @return
     */
    public String getConnectionStatusString(int status) {
        switch (status) {
            case DISCONNECTED:
                return String.format("Desconectado do host \"%s:%d\".", host, port);
            case CONNECTING:
                return String.format("Conectando ao host \"%s:%d\"...", host, port);
            case CONNECTED_HANDSHAKE:
                return String.format("Conectado ao host \"%s:%d\" (Início do Handshake).", host, port);
            case CONNECTED:
                return String.format("Conectado ao host \"%s:%d\" (Handshake OK).", host, port);
        }
        return "";
    }

    public synchronized String getErrorStatusStringComplete() {
        String str1 = getErrorStatusString(errorStatus);
        String str2 = getErrorMessage();
        if (str2.isEmpty()) {
            return String.format("%s", str1);
        } else {
            return String.format("%s [%s]", str1, str2);
        }
    }

    /**
     * Retorna uma String descrevendo o erro indicado.
     *
     * @param status
     * @return
     */
    public String getErrorStatusString(int error) {
        switch (error) {
            case ERROR_NO:
                return "Sem erros";
            case ERROR_TIMEOUT:
                return "ERRO: Timeout";
            case ERROR_IO:
                return "ERRO: I/O";
            case WARNING_COMMUNICATION_TIME:
                return "AVISO: Tempo excessivo sem comunicação";
            case ERROR_UNKNOWN_HOST:
                return "ERRO: Host desconhecido";
            case ERROR_HANDSHAKE_TIMEOUT:
                return "ERRO: Timeout de handshake";
            case ERROR_HANDSHAKE:
                return "ERRO: Handshake errôneo";
            case ERROR_SERVER_FULL:
                return "ERRO: Servidor cheio";
        }
        return "";
    }

    public synchronized long getLast_package_sent_time() {
        return last_package_sent_time;
    }

    public synchronized long getLast_package_received_time() {
        return last_package_received_time;
    }

    public synchronized Socket getSock() {
        return sock;
    }

    public synchronized void setInterpreter(TR_ClientCommandInterpreter interpreter) {
        this.interpreter = interpreter;
    }

    public synchronized String getHost() {
        return host;
    }

    public synchronized int getPort() {
        return port;
    }

    public int getHandshakeStatus() {
        return handshakeStatus;
    }

    public void IOError(String message) {
        setError(ERROR_IO, message);
    }
}

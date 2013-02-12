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
 * Thread que efetua e gerencia a conexão do cliente (estação base) com o servidor (robô).
 * OBS: Ver especificação do protocolo (Estação base <-> Robô) para maior esclarecimento.
 *
 * IMPORTANTE: Não esquecer de chamar o metodo start() para iniciar a Thread depois de instanciar a classe!
 *
 * @author Stefan
 */
public class TR_ClientConnector extends Thread {
    //==========================================
    //========= Constantes =====================
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Constantes">

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
            HANDSHAKE_REPLY_RECEIVED = 2, //Recebeu REPLY
            HANDSHAKE_FULL = 3; //Enviou REPLY2 (HANDSHAKE COMPLETO)
    //</editor-fold>
    //==========================================
    //========= Variáveis ======================
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Variáveis">
    //Estado atual de handshake
    private int handshakeStatus;
    //Horario (milissegundos) da ultima atividade de handshake.
    private long lastHandshakeActivity;
    //Tempo máximo (ms) para haver timeout de handshake.
    private int handshakeTimeout = 5000;
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
    //Por padrão não há interpretador, ele deve ser adicionado pelo método setInterpreter().
    //Se não houver Interpreter registrado, os comandos recebidos não são interpretados.
    public TR_ClientCommandInterpreter interpreter = null;
    //Host atual de conexão
    private String host;
    //Porta atual de conexão
    private int port;
    //Horario em milissegundos da ultima requisicao de envio de pacote
    private long last_package_sent_time;
    //Horario em milissegundos do último recebimento de pacote
    private long last_package_received_time;
    //Intervalo máximo para envio de pacotes. Usado para enviar KEEPALIVE quando necessário.
    private int package_send_interval_max = 1000;
    //Intervalos máximos para recebimento de pacotes:
    private int package_recv_interval_echo = 1000; //Envia um ECHO REQUEST
    private int package_recv_interval_warning = 8000; //Entra em estado de Warning
    private int package_recv_interval_timeout = 15000; //Timeout (desconecta do host)
    //Listeners. "Escutam" mudanças de status nesta classe (recurso usado principalmente pela interface gráfica).
    private final CopyOnWriteArrayList<MyChangeListener> listeners;
    //Indica se o loop principal deve executar
    private boolean run;
    //</editor-fold>

    /**
     * IMPORTANTE: Não esquecer de chamar o metodo start() para iniciar a Thread depois de instanciar a classe.
     */
    public TR_ClientConnector() {
//        this.interpreter = interpreter;
//        receiver.start();
//        sender.start();
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        setConnectionStatus(DISCONNECTED, false);
        setError(ERROR_NO, "");
        handshakeStatus = HANDSHAKE_NO;
        run = true;
    }

    /**
     * Rotina principal executada pela thread.
     * Contém o loop que executa as rotinas periódicas de verificação do status da conexão.
     */
    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this) {
                    //Finaliza a execução do loop se for ordenado o término da thread (instrução dentro do bloco synchronized).
                    if (!run) break;
                    while (connectionStatus == DISCONNECTED) { //Se a conexão estiver fechada, espera até outra conexão ser feita pelo método connect().
                        fireChangeEvent();
                        this.wait();
                    }

                    if (connectionStatus == CONNECTING) { //Se o status estiver setado em CONNECTING, executa o método _connect()
                        _connect();
                    } else if (connectionStatus == CONNECTED_HANDSHAKE) {
                        long current_time = System.currentTimeMillis();
                        if (handshakeStatus != HANDSHAKE_FULL && current_time - lastHandshakeActivity > handshakeTimeout) {
                            //Se o handshake não estiver completado, e houver timeout de handshake.
                            //Desconecta do host e informa um erro ao usuário.
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
                        long difftime_recv = current_time - last_package_received_time;
                        if (difftime_recv > package_recv_interval_timeout) { //TIMEOUT
                            //Se nenhum pacote tiver sido recebido no intervalo de tempo para timeout...
                            //Desconecta o socket.
                            setError(ERROR_TIMEOUT, String.format("%d ms", current_time - last_package_received_time));
                            closeConnection();
                        } else if (difftime_recv > package_recv_interval_warning) { //WARNING
                            //Se nenhum pacote tiver sido recebido no intervalo de tempo para warning...
                            //Significa que a conexão está com escesso de tempo sem comunicação (apesar de o socket não estar fechado ainda)
                            setError(WARNING_COMMUNICATION_TIME, String.format("%d ms", current_time - last_package_received_time));
                            sendEchoRequest();
                        } else if (difftime_recv > package_recv_interval_echo) { //ECHO
                            //Se nenhum pacote tiver sido recebido no intervalo de ECHO...
                            //Envia um "echo" para que o robo responda
                            sendEchoRequest();
                        }
                    }
                }
                //"Dorme" por certo tempo para deixar outras threads executarem.
                sleep(500);
            } catch (InterruptedException ex) {
                errorMessage = ex.getMessage();
                Logger.getLogger(TR_ClientConnector.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    //==========================================
    //========= Gerenciamento de conexão =======
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Gerenciamento de conexão">
    /**
     * Agenda uma conexão a um host.
     * A conexão será feita posteriormente pela thread, pelo método _connect().
     *
     * @param host IP ou URL do host
     * @param port Porta de conexão
     * @exception ClientConnectedException Caso o Connector já estaja conectado a um host.
     */
    public synchronized void connect(String host, int port) throws ClientConnectedException {
        if (isConnected()) {
            //Caso já esteja conectado, lança uma execao.
            throw new ClientConnectedException();
        }
        //Cria novas threads sender e receiver.
        sender = new TR_ClientSender(this);
        receiver = new TR_ClientReceiver(this);
        this.host = host;
        this.port = port;
        //Muda o status, o que irá indicar que uma conexão está agendada para ser feita.
        setConnectionStatus(CONNECTING, false);
        handshakeStatus = HANDSHAKE_NO;
        setError(ERROR_NO, "");
        //Acorda a thread caso esteja em wait();
        this.notifyAll();
    }

    /**
     * Conecta efetivamente ao host especificado por connect().
     */
    public synchronized void _connect() {
        //Verifica se o connector não está conectado a nenhum host.
        assert (!isConnected());
        assert (handshakeStatus == HANDSHAKE_NO);
        assert (connectionStatus == CONNECTING);
        try {
            //Cria um novo socket (tenta conectar ao host).
            sock = new Socket(host, port);
            if (sock.isConnected()) {
                //                setConnectionStatus(CONNECTED);
                setError(ERROR_NO, "");
                receiver.start();
                sender.start();
                long current_time = System.currentTimeMillis();
                //Envia o request de handshake
                sendMessageWithPriority("BELLATOR HANDSHAKE REQUEST", true);
                //Muda os status
                handshakeStatus = HANDSHAKE_REQUEST_SENT;
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

    /**
     * Desconecta do host corretamente, enviando mensagem de DISCONNECT e fechando a conexão.
     */
    public synchronized void disconnect() {
        if (isConnected()) sendMessageWithPriority("DISCONNECT", true);
        closeConnection();
    }

    /**
     * Fecha a conexão.
     */
    public void closeConnection() {
        synchronized (this) {
            if (connectionStatus != CONNECTED && connectionStatus != CONNECTED_HANDSHAKE) {
                return;
            }
        }
        setConnectionStatus(DISCONNECTED, false);
        setError(ERROR_NO, "");

        //Requisita finalização das threads
        sender.terminate();
        receiver.terminate();
        //Espera o término das threads
        int count = 0;
        while (sender.isAlive() && receiver.isAlive() && count < 5) {
            try {
                System.out.printf("[TR_ServerConnection] Aguardando sender e receiver finalizarem.... (%d)\n", count);
                //Espera o sender finalizar.
                sleep(500);
                count++;
            } catch (InterruptedException ex) {
                Logger.getLogger(TR_ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //Força o término das threads 
        sender.kill();
        receiver.kill();

        try {
            //Fecha o socket
            sock.close();
        } catch (IOException ex) {
            setError(ERROR_IO, ex.getMessage());
            Logger.getLogger(TR_ClientConnector.class.getName()).log(Level.SEVERE, null, ex);
        }
        //Muda o status da conexão.
        setConnectionStatus(DISCONNECTED, false);
        setError(ERROR_NO, "");
    }

    /**
     * Informa que um HANDSHAKE REPLY foi recebido.
     */
    public synchronized void handShakeReplyReceived() {
        if (handshakeStatus == HANDSHAKE_REQUEST_SENT) {
            handshakeStatus = HANDSHAKE_REPLY_RECEIVED;
            lastHandshakeActivity = System.currentTimeMillis();
            //Envia o REPLY2
            sendMessageWithPriority("BELLATOR HANDSHAKE REPLY2", true);
            handShakeReply2Sent();
        } else {
            System.out.println("[TR_ClientConnector] ERRO: HANDSHAKE REPLY recebido erroneamente.");
            disconnect();
        }
    }

    /**
     * Informa que um HANDSHAKE REPLY2 foi enviado.
     */
    public synchronized void handShakeReply2Sent() {
        if (handshakeStatus == HANDSHAKE_REPLY_RECEIVED) {
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
     * Informa que o host remoto está cheio.
     */
    public synchronized void serverFull() {
        if (connectionStatus == CONNECTED_HANDSHAKE) {
            setError(ERROR_SERVER_FULL, "");
            disconnect();
        }
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
        if (interpreter == null) {
            System.out.printf("[TR_ClientConnector] Não há interpretador de comandos registrado! Mensagem recebida: %s\n", command);
        } else {
            interpreter.processCommand(command);
        }
    }
//</editor-fold>
    //==========================================
    //========= Gerenciamento de listeners =====
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Gerenciamento de listeners">

    /**
     * Adiciona um listener de eventos.
     *
     * @param l
     */
    public synchronized void addMyChangeListener(MyChangeListener l) {
        this.listeners.add(l);
        fireChangeEvent();
    }

    /**
     * Remove um listener de eventos.
     *
     * @param l
     */
    public synchronized void removeMyChangeListener(MyChangeListener l) {
        this.listeners.remove(l);
    }

    /**
     * Notifica os listeners de que alguma modificacao foi feita.
     */
    protected synchronized void fireChangeEvent() {
        MyChangeEvent evt = new MyChangeEvent(this);
        for (MyChangeListener l : listeners) {
            l.changeEventReceived(evt);
        }
    }
//</editor-fold>
    //==========================================
    //========= Gerenciamento de erros =========
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Gerenciamento de erros">

    /**
     * Muda o status e a mensagem de erro e lança um evento aos listeners via fireChangeEvent().
     *
     * @param errorStatus O novo status de erro.
     * @param message Mensagem personalizada descrevendo o erro.
     */
    private synchronized void setError(int errorStatus, String message) {
        setError(errorStatus, message, true);
    }

    /**
     * Muda o status e a mensagem de erro.
     *
     * @param errorStatus O novo status de erro.
     * @param message Mensagem personalizada descrevendo o erro.
     * @param fireChangeEvt Indica se um evento deve ser lançado aos listeners via fireChangeEvent().
     */
    private synchronized void setError(int errorStatus, String message, boolean fireChangeEvt) {
        this.errorStatus = errorStatus;
        this.errorMessage = message;
        if (fireChangeEvt) fireChangeEvent();
    }

    public synchronized int getErrorStatus() {
        return errorStatus;
    }

    public synchronized String getErrorMessage() {
        return errorMessage;
    }

    /**
     * Informa que houve erro de IO.
     *
     * @param message Mensagem do erro.
     */
    public void IOError(String message) {
        setError(ERROR_IO, message);
    }
//</editor-fold>
    //==========================================
    //========= Gerenciamento de status ========
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Gerenciamento de status">

    public boolean isConnected() {
        return connectionStatus == CONNECTED;
    }

    /**
     * Muda o status da conexão e lança um evento aos listeners via fireChangeEvent().
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
     * @param fireChangeEvt Indica se um evento deve ser lançado aos listeners via fireChangeEvent().
     */
    private synchronized void setConnectionStatus(int connectionStatus, boolean fireChangeEvt) {
        this.connectionStatus = connectionStatus;
        if (fireChangeEvt) fireChangeEvent();
    }

    public synchronized int getConnectionStatus() {
        return connectionStatus;
    }

    public synchronized String getConnectionStatusString() {
        return getConnectionStatusString(connectionStatus);
    }

    /**
     * Retorna uma String descrevendo o status de conexão.
     *
     * @param status O status da conexão.
     * @return String descrevendo o status de conexão.
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

    /**
     * Retorna uma String descrevendo de forma detalhada o último erro ocorrido.
     * A String é constituida da descrição do status do erro + a mensagem de erro personalizada.
     *
     * @return String descrevendo o último erro ocorrido.
     */
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
     * Retorna uma String descrevendo o status do erro.
     *
     * @param error O status do erro.
     * @return String descrevendo o status do erro.
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
//</editor-fold>
    //==========================================
    //========= Outros Getters e Setters =======
    //==========================================
    //<editor-fold defaultstate="collapsed" desc="Outros Getters e Setters">

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
    //</editor-fold>
}

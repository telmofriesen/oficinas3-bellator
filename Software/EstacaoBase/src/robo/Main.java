//TODO:
//Usar leituras do acelerômetro e giroscópio

package robo;

import robo.ServerListener;
import robo.ServerConnection;
import com.github.sarxos.webcam.util.ImageUtils;
import comunicacao.Base64new;
import comunicacao.SenderMessage;
import dados.AmostraSensores;
import robo.gerenciamento.EnginesManager;
import robo.gerenciamento.SensorsManager;
import robo.gerenciamento.WebcamManager;
import robo.gerenciamento.old.WebcamManagerNew;
import robo.gerenciamento.old.WebcamManagerNew2;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

/**
 * Classe que contem os objetos principais do servidor (robô).
 *
 * @version 1
 * A versão atual é um simulador básico de robô. Quando a leitura dos sensores é ativada, são enviados continuamente para a estação base alguns valores previamente definidos.
 *
 * @author Stefan
 */
public class Main extends Thread {
    //listener, que escuta novas conexões de clientes (estações base)

    private ServerListener listener;
    //Amostragem dos sensores
    private SensorsManager sensorsManager;
    private SensorsInfoListnener sensorsInfoListener;
    //Amostragem da webcam
    private WebcamManagerNew2 webcamManager;
    private WebcamInfoListener webcamInfoListener;
    private EnginesManager enginesManager;
    private SerialCommunicator serialCommunicator;
//    private WebcamStreamer webcamStreamer;
    //Indica se amostras de sensores devem ser enviadas ou não à estação base.
//    private boolean send_sensor_info = false;
    //Velocidades normalizadas (valor de 1 até -1) das rodas esquerda e direita;
    //TODO criar classe com thread para controle dos motores. Remover estas variáveis.
//    private float velocidade_roda_direita = 0, velocidade_roda_esquerda = 0;
    //Porta de escuta do servidor
    private int port = ServerListener.LISTENER_DEFAULT_PORT;

    /**
     * Inicializa os objetos do Server.
     * IMPORTANTE: não esquecer de chamar o método startThreads() para iniciar as threads.
     */
    public Main(boolean enable_serial) {
        //Inicializa o listener, que escuta novas conexões de clientes (estações base)
        listener = new ServerListener(this, ServerListener.LISTENER_DEFAULT_PORT);
        sensorsManager = new SensorsManager(this, 1);
        sensorsInfoListener = new SensorsInfoListnener();
        sensorsManager.addMyChangeListener(sensorsInfoListener);
        //[176x144] [320x240] [352x288] [480x400] [640x480] [1024x768] 
        webcamManager = new WebcamManagerNew2(new Dimension(320, 240));
//        webcamManager = new WebcamManager(new Dimension(640, 480));

        webcamInfoListener = new WebcamInfoListener();
        webcamManager.addMyChangeListener(webcamInfoListener);
        enginesManager = new EnginesManager(this);
        if (enable_serial) {
            serialCommunicator = new SerialCommunicator(this);
        }
//        webcamStreamer = new WebcamStreamer(5050, Webcam.getDefault(), 30, true);
    }

    @Override
    public void run() {
    }

    /**
     * Inicia as threads do servidor.
     */
    public void startThreads() {
        listener.start();
        sensorsManager.start();
//        webcamManager.start();
        this.start();
    }

    public synchronized ServerListener getListener() {
        return listener;
    }

    public ServerConnection getMainHost() {
        if (listener.getNumServerConnections() < 1) {
            return null;
        } else {
            return listener.getServerConnection(0);
        }
    }

    /**
     * Notifica que foi estabelecida uma conexão a um host.
     *
     * @param ip Ip do host
     * @param port Porta da conexão
     */
    public void mainHostConnected(String ip, int port) {
        //Configura o ip e porta da stream da webcam.
        webcamManager.setHost(ip, WebcamManager.WEBCAM_STREAM_DEFAULT_PORT);
    }

    /**
     * Notifica que a conexão com o host principal foi finalizada.
     * Para o robô e reseta os status.
     */
    public void mainHostDisconnected() {
        //Para o robô e reseta os status
        enginesManager.setEnginesSpeed(0, 0);
        sensorsManager.stopSampling();
        sensorsManager.setSample_rate(1);
//        sensorsManager.resetTests();
        webcamManager.reset();
        //TODO mandar mensagem de parada para o robo via porta serial
    }

    /**
     * Muda a velocidade das rodas esquerda e direita.
     * O valor de velocidade é normalizado, ou seja, 1 significa velocidade máxima para frente e -1 velocidade máxima para trás.
     *
     * @param dir Velocidade normalizada da roda direita.
     * @param esq Valocidade normalizada da roda esquerda.
     */
//    public synchronized void setVelocidadeRodas(float dir, float esq) {
//        this.velocidade_roda_direita = dir;
//        this.velocidade_roda_esquerda = esq;
//        System.out.printf("[Server] Velocidade rodas: %.2f %.2f\n", dir, esq);
//    }
//
//    public synchronized float getVelocidade_roda_direita() {
//        return velocidade_roda_direita;
//    }
//
//    public synchronized void setVelocidade_roda_direita(float velocidade_roda_direita) {
//        this.velocidade_roda_direita = velocidade_roda_direita;
//    }
//
//    public synchronized float getVelocidade_roda_esquerda() {
//        return velocidade_roda_esquerda;
//    }
//
//    public synchronized void setVelocidade_roda_esquerda(float velocidade_roda_esquerda) {
//        this.velocidade_roda_esquerda = velocidade_roda_esquerda;
//    }
    public synchronized int getPort() {
        return port;
    }

    public synchronized void setPort(int port) {
        this.port = port;
    }

    public SensorsManager getSensorsSampler() {
        return sensorsManager;
    }

    public WebcamManagerNew2 getWebcamManager() {
        return webcamManager;
    }

    public EnginesManager getEnginesManager() {
        return enginesManager;
    }

    public SerialCommunicator getSerialCommunicator() {
        return serialCommunicator;
    }

    /**
     * Adiciona uma mensagem à fila de envio para o host principal (se ele estiver conectado).
     *
     * @param message Mensagem a ser enviada.
     * @param flush_buffer Indica se um flush no buffer deve ser feito.
     */
    public synchronized void sendMessageToMainHost(String message, boolean flush_buffer) {
        if (listener.getNumServerConnections() >= 1) {
            listener.getServerConnection(0).sendMessage(new SenderMessage(message, flush_buffer));
        }
    }

    class SensorsInfoListnener implements MyChangeListener {

        private AmostraSensores lastSample;
        private boolean lastSamplingStatus = false;

        @Override
        public void changeEventReceived(MyChangeEvent evt) {

//            long currentTime = System.currentTimeMillis();
            if (evt.getSource() instanceof SensorsManager) {
                SensorsManager s = (SensorsManager) evt.getSource();
                boolean samplingStatus = s.isSamplingEnabled();
                //Verifica o numero de conexões abertas
                if (listener.getNumServerConnections() >= 1) {
                    ServerConnection con = listener.getServerConnection(0);
                    //Envia o status à estação base se o mesmo mudar.
                    if (samplingStatus != lastSamplingStatus) {
                        //Envia o status 
                        con.sendMessageWithPriority(s.getStatusMessage(), false);
                        synchronized (this) {
                            lastSamplingStatus = samplingStatus;
                        }
                    }
                }

//                AmostraSensores a = s.getCurrentSample();
                //Envia a última amostra à estação base se houver uma nova (e a amostragem estiver ativada).
//                if (samplingStatus == true && lastSample != a) {
//                    //Monsta a string da mensagem a partir da amostra
//                    String str = String.format("SENSORS SAMPLE %.2f %.2f ", a.getAceleracao(), a.getAceleracaoAngular());
//                    float[] distIR = a.getDistIR();
//                    for (int i = 0; i < distIR.length; i++) {
//                        str += String.format("%.2f ", distIR[i]);
//                    }
//                    str += String.format("%d", a.getTimestamp());
//                    con.sendMessage(str, false);
//                    lastSample = a;
//                }
            }
        }
    }

    class WebcamInfoListener implements MyChangeListener {

        BufferedImage lastImage = null;
        private boolean lastSamplingStatus = false;
        private boolean lastWebcamStatus = false;
        private boolean lastStreamStatus = false;

        @Override
        public void changeEventReceived(MyChangeEvent evt) {
            if (evt.getSource() instanceof WebcamManager) {
                WebcamManager w = (WebcamManager) evt.getSource();
                boolean sampling_enabled = w.isSamplingEnabled();
                boolean webcam_status = w.isWebcam_available();
                boolean stream_status = w.isStream_open();
                //Envia o status à estação base se o mesmo mudar.
                if (sampling_enabled != lastSamplingStatus || webcam_status != lastWebcamStatus || lastStreamStatus != stream_status) {
                    if (listener.getNumServerConnections() < 1) {
                        return;
                    }
                    ServerConnection con = listener.getServerConnection(0);
                    //Envia o status 
                    con.sendMessageWithPriority(w.getStatusMessage(), true);
                    synchronized (this) {
                        lastSamplingStatus = sampling_enabled;
                        lastWebcamStatus = webcam_status;
                        lastStreamStatus = stream_status;
                    }
                }
            }
        }

        /**
         * Envia uma imagem ao host remoto pelo Sender, obedencendo ao protocolo de comunicação.
         *
         * @param img
         * @deprecated
         */
        public void sendImage(BufferedImage img) {
            byte[] byteArray = ImageUtils.toByteArray(img, "jpg");
//            String base64Image = new BASE64Encoder().encodeBuffer(byteArray);
//            String base64Image_new = base64Image.replace('\n', '\0');
            String base64Image = Base64new.encode(byteArray);
            //Verifica o numero de conexões abertas
            if (listener.getNumServerConnections() < 1) {
                return;
            }
            //Envia a última amostra à estação base se houver uma nova.
            ServerConnection con = listener.getServerConnection(0);
            con.sendMessage("WEBCAM SAMPLE " + base64Image, true);
//            System.out.println(base64Image);
        }
    }

    public static void main(String args[]) {
        boolean enable_serial = false;
//        if (args[0].equals("serial")) {
//            enable_serial = true;
//        }
        Main s = new Main(enable_serial);
        s.startThreads();
//        s.getWebcamSampler().startSampling();
    }
}

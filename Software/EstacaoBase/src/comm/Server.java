/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.WebcamStreamer;
import com.github.sarxos.webcam.util.ImageUtils;
import controle.AmostraSensores;
import controle.robo.SensorsSampler;
import controle.robo.WebcamManager;
import controle.robo.WebcamSampler;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.logging.Level;
import java.util.logging.Logger;
import sun.misc.BASE64Encoder;

/**
 * Classe que contem os objetos principais do servidor (robô).
 *
 * @version 1
 * A versão atual é um simulador básico de robô. Quando a leitura dos sensores é ativada, são enviados continuamente para a estação base alguns valores previamente definidos.
 *
 * @author Stefan
 */
public class Server extends Thread {

    //listener, que escuta novas conexões de clientes (estações base)
    private TR_ServerListener listener;
    //Amostragem dos sensores
    private SensorsSampler sensorsSampler;
    private SensorsListnener sensorsListener;
    //Amostragem da webcam
    private WebcamManager webcamManager;
    private WebcamInfoListener webcamListener;
//    private WebcamStreamer webcamStreamer;
    //Indica se amostras de sensores devem ser enviadas ou não à estação base.
//    private boolean send_sensor_info = false;
    //Velocidades normalizadas (valor de 1 até -1) das rodas esquerda e direita;
    //TODO criar classe com thread para controle dos motores. Remover estas variáveis.
    private float velocidade_roda_direita = 0, velocidade_roda_esquerda = 0;
    //Porta de escuta do servidor
    private int port = 12312;

    /**
     * Inicializa os objetos do Server.
     * IMPORTANTE: não esquecer de chamar o método startThreads() para iniciar as threads.
     */
    public Server() {
        //Inicializa o listener, que escuta novas conexões de clientes (estações base)
        listener = new TR_ServerListener(this, 12312);
        sensorsSampler = new SensorsSampler(1);
        sensorsListener = new SensorsListnener();
        sensorsSampler.addMyChangeListener(sensorsListener);
        //[176x144] [320x240] [352x288] [480x400] [640x480] [1024x768] 
//        webcamManager = new WebcamManager(1, new Dimension(176, 144));
        webcamManager = new WebcamManager(1, new Dimension(640, 480));

        webcamListener = new WebcamInfoListener();
        webcamManager.addMyChangeListener(webcamListener);
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
        sensorsSampler.start();
//        webcamManager.start();
        this.start();
    }

    public synchronized TR_ServerListener getListener() {
        return listener;
    }

    public TR_ServerConnection getMainHost() {
        if (listener.getNumServerConnections() < 1) {
            return null;
        } else {
            return listener.getServerConnection(0);
        }
    }

    public void mainHostConnected(String ip, int port) {
        webcamManager.setHost(ip, 5050);
    }

    public void mainHostDisconnected() {
        sensorsSampler.stopSampling();
        sensorsSampler.setSample_rate(1);
        sensorsSampler.resetTests();
        webcamManager.reset();
    }

    /**
     * Muda a velocidade das rodas esquerda e direita.
     * O valor de velocidade é normalizado, ou seja, 1 significa velocidade máxima para frente e -1 velocidade máxima para trás.
     *
     * @param dir Velocidade normalizada da roda direita.
     * @param esq Valocidade normalizada da roda esquerda.
     */
    public synchronized void setVelocidadeRodas(float dir, float esq) {
        this.velocidade_roda_direita = dir;
        this.velocidade_roda_esquerda = esq;
        System.out.printf("[Server] Velocidade rodas: %.2f %.2f\n", dir, esq);
    }

    public synchronized float getVelocidade_roda_direita() {
        return velocidade_roda_direita;
    }

    public synchronized void setVelocidade_roda_direita(float velocidade_roda_direita) {
        this.velocidade_roda_direita = velocidade_roda_direita;
    }

    public synchronized float getVelocidade_roda_esquerda() {
        return velocidade_roda_esquerda;
    }

    public synchronized void setVelocidade_roda_esquerda(float velocidade_roda_esquerda) {
        this.velocidade_roda_esquerda = velocidade_roda_esquerda;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized void setPort(int port) {
        this.port = port;
    }

    public SensorsSampler getSensorsSampler() {
        return sensorsSampler;
    }

    public WebcamManager getWebcamManager() {
        return webcamManager;
    }

    class SensorsListnener implements MyChangeListener {

        private AmostraSensores lastSample;
        private boolean lastSamplingStatus = false;

        @Override
        public void changeEventReceived(MyChangeEvent evt) {

//            long currentTime = System.currentTimeMillis();
            if (evt.getSource() instanceof SensorsSampler) {
                SensorsSampler s = (SensorsSampler) evt.getSource();
                boolean samplingStatus = s.isSamplingEnabled();
                //Verifica o numero de conexões abertas
                if (listener.getNumServerConnections() < 1) {
                    return;
                }
                //Envia a última amostra à estação base se houver uma nova.
                TR_ServerConnection con = listener.getServerConnection(0);
                //Envia o status à estação base se o mesmo mudar.
                if (samplingStatus != lastSamplingStatus) {
                    //Envia o status 
                    con.sendMessageWithPriority(s.getStatusMessage(), false);
                    synchronized (this) {
                        lastSamplingStatus = samplingStatus;
                    }
                }
                AmostraSensores a = s.getCurrentSample();
                //Envia a última amostra à estação base se houver uma nova (e a amostragem estiver ativada).
                if (samplingStatus == true && lastSample != a) {
                    //Monsta a string da mensagem a partir da amostra
                    String str = String.format("SENSORS SAMPLE %.2f %.2f ", a.getAceleracao(), a.getAceleracaoAngular());
                    float[] distIR = a.getDistIR();
                    for (int i = 0; i < distIR.length; i++) {
                        str += String.format("%.2f ", distIR[i]);
                    }
                    str += String.format("%d", a.getTimestamp());
                    con.sendMessage(str, false);
                    lastSample = a;
                }
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
                    TR_ServerConnection con = listener.getServerConnection(0);
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
            TR_ServerConnection con = listener.getServerConnection(0);
            con.sendMessage("WEBCAM SAMPLE " + base64Image, true);
//            System.out.println(base64Image);
        }
    }

    public static void main(String args[]) {
        Server s = new Server();
        s.startThreads();
//        s.getWebcamSampler().startSampling();
    }
}

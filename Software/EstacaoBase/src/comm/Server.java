/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.util.logging.Level;
import java.util.logging.Logger;

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
    //Indica se amostras de sensores devem ser enviadas ou não à estação base.
    private boolean send_sensor_info = false;
    //Velocidades normalizadas (valor de 1 até -1) das rodas esquerda e direita;
    private float velocidade_roda_direita = 0, velocidade_roda_esquerda = 0;
    //Taxa de amostragem (amostras/s)
    private float sample_rate = 1; 
    //Porta de escuta do servidor
    private int port = 12312;

    /**
     * Inicializa os objetos do Server.
     * IMPORTANTE: não esquecer de chamar o método startThreads() para iniciar as threads.
     */
    public Server() {
        //Inicializa o listener, que escuta novas conexões de clientes (estações base)
        listener = new TR_ServerListener(this, 12312);
    }

    @Override
    public void run() {
        boolean first_sent = false;
        while (true) {
            while (listener.getServerSocket() == null || listener.getNumServerConnections() < 1) {
                try {
                    sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
//        System.out.printf("%s:%d Bind status: %b\n", listener.getServerSocket().getInetAddress().getHostAddress(), listener.getServerSocket().getLocalPort(), listener.getServerSocket().isBound());
            if (send_sensor_info == true) {
                if (listener.getNumServerConnections() >= 1) {
                    //TODO implementar leituras reais dos sensores
                    //TEMPORARIO (apenas para testes iniciais do protocolo)
                    if (!first_sent) { //Na primeira medida enviada, envia um valor de aceleração inicial para que o robô comece a se mover.
                        listener.getServerConnection(0).sendMessage(new SenderMessage(
                                String.format("SENSORS SAMPLE 0 0 300 0 300 %d", System.currentTimeMillis()),
                                false));
                        try {
                            sleep(1000);
                        } catch (InterruptedException ex) {
                            Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        listener.getServerConnection(0).sendMessage(new SenderMessage(
                                String.format("SENSORS SAMPLE 0.1 0 300 0 300 %d", System.currentTimeMillis()),
                                false));
                        first_sent = true;
                    } else { //Nas medidas consecutivas, a aceleração é zero, ou seja, o robô fica com velocidade constante.
                        listener.getServerConnection(0).sendMessage(new SenderMessage(
                                String.format("SENSORS SAMPLE 0 0 300 0 300 %d", System.currentTimeMillis()),
                                false));
                    }
                }
            }
            try {
                long sleep_time = (long) (1000f / sample_rate);
                sleep(sleep_time); //Pausa a execução em um tempo inversamente proporcional à taxa de amostragem.
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    /**
     * Inicia as threads do servidor.
     */
    public void startThreads() {
        listener.start();
        this.start();
    }

    public synchronized TR_ServerListener getListener() {
        return listener;
    }

    /**
     * Inicia o envio de amostras para a estação base.
     */
    public synchronized void startSend_sensor_info() {
        this.send_sensor_info = true;
    }

    /**
     * Para o envio de amostras para a estação base.
     */
    public synchronized void stopSend_sensor_info() {
        this.send_sensor_info = false;
    }

    public synchronized boolean isSend_sensor_info() {
        return send_sensor_info;
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

    public synchronized float getSample_rate() {
        return sample_rate;
    }

    public synchronized void setSample_rate(float sample_rate) {
        this.sample_rate = sample_rate;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized void setPort(int port) {
        this.port = port;
    }

    public static void main(String args[]) {
        Server s = new Server();
        s.startThreads();
    }
}

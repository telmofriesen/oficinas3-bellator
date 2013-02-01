/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Classe que contem os objetos principais do servidor
 *
 * @author Stefan
 */
public class Server extends Thread {

    private TR_ServerListener listener;
    private boolean send_sensor_info = false;
    private float velocidade_roda_direita = 0, velocidade_roda_esquerda = 0;
    private float sample_rate = 1; //Taxa de amostragem (amostras/s)
    private int port = 12312;

    public Server() {
        //Inicializa o listener e o interpreter
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
                    //Temporario (apenas para testes iniciais do protocolo)
                    if (!first_sent) {

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
                    } else {
                        listener.getServerConnection(0).sendMessage(new SenderMessage(
                                String.format("SENSORS SAMPLE 0 0 300 0 300 %d", System.currentTimeMillis()),
                                false));
                    }
                }
            }
            try {
                long sleep_time = (long) (1000f / sample_rate);
                sleep(sleep_time);
            } catch (InterruptedException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public void startThreads() {
        listener.start();
    }

    public synchronized TR_ServerListener getListener() {
        return listener;
    }

    public synchronized void startSend_sensor_info() {
        this.send_sensor_info = true;
    }

    public synchronized void stopSend_sensor_info() {
        this.send_sensor_info = false;
    }

    public synchronized boolean isSend_sensor_info() {
        return send_sensor_info;
    }

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
        s.start();
    }
}

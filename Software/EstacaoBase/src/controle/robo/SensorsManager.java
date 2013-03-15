/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle.robo;

import controle.AmostraSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stefan
 */
public class SensorsManager extends Thread {
    //Indica se a amostragem está habilitada.

    private boolean sampling_enabled = false;
    private AmostraSensores currentSample;
    //Taxa de amostragem máxima
    private float sample_rate; //Amostras/s
    //Indica se o loop principal deve ser executado ou não.
    private boolean run = false;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();
    private boolean first_sent = false;
    private boolean second_sent = false;

    public SensorsManager(float sample_rate) {
        this.sample_rate = sample_rate;
    }

    @Override
    public void run() {
        synchronized (this) {
            run = true;
        }
        long sleep_time;
        while (run) {
            //Lê amostras dos sensores
            AmostraSensores a = lerSensores();
            synchronized (this) {
                currentSample = a;
                sleep_time = (long) (1000f / sample_rate);
            }
            fireChangeEvent();
            try {
                sleep(sleep_time); //Pausa a execução em um tempo inversamente proporcional à taxa de amostragem.
            } catch (InterruptedException ex) {
                Logger.getLogger(WebcamSampler.class.getName()).log(Level.SEVERE, null, ex);
            }
            synchronized (this) {
                //Entra em estado de espera se a Amostragem estiver desativada ou a Webcam estiver fechada.
                while (run && !sampling_enabled) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(WebcamSampler.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    public synchronized AmostraSensores getCurrentSample() {
        return currentSample;
    }

    public synchronized float getSample_rate() {
        return sample_rate;
    }

    public synchronized void setSample_rate(float sample_rate) {
        this.sample_rate = sample_rate;
    }

    /**
     * Inicia a amostragem.
     */
    public void startSampling() {
        synchronized (this) {
            sampling_enabled = true;
            this.notifyAll();
        }
        fireChangeEvent();
    }

    /**
     * Interrompe a amostragem.
     */
    public void stopSampling() {
        synchronized (this) {
            sampling_enabled = false;
        }
        fireChangeEvent();
    }

    public synchronized boolean isSamplingEnabled() {
        return sampling_enabled;
    }
    
    public synchronized String getStatusMessage(){
        return "SENSORS STATUS REPLY " + sampling_enabled;
    }

    public void terminate() {
        synchronized (this) {
            run = false;
            stopSampling();
            this.notifyAll();
        }
    }

    public void kill() {
        run = false;
        stopSampling();
        this.interrupt();
    }

    public void addMyChangeListener(MyChangeListener l) {
        this.listeners.add(l);
        fireChangeEvent();
    }

    public void removeMyChangeListener(MyChangeListener l) {
        this.listeners.remove(l);
    }

    // Event firing method.  Called internally by other class methods.
    protected void fireChangeEvent() {
        MyChangeEvent evt = new MyChangeEvent(this);

        for (MyChangeListener l : listeners) {
            l.changeEventReceived(evt);
        }
    }

    public synchronized void resetTests() {
        first_sent = false;
        second_sent = false;
    }

    /**
     * Ler sensores.
     * A versão atual retorna leituras de teste.
     * TODO implementar leitura real.
     *
     * @return
     */
    public AmostraSensores lerSensores() {
        synchronized (this) {
            if (!first_sent) { //Na primeira medida enviada, envia um valor de aceleração inicial para que o robô comece a se mover.
                first_sent = true;
                return new AmostraSensores(0f, 0f, new float[]{300, 0, 300}, System.currentTimeMillis());
            } else if (!second_sent) {
                try {
                    sleep(500);
                } catch (InterruptedException ex) {
                    Logger.getLogger(SensorsManager.class.getName()).log(Level.SEVERE, null, ex);
                }
                second_sent = true;
                return new AmostraSensores(0.1f, 0f, new float[]{300, 0, 300}, System.currentTimeMillis());
            } else { //Nas medidas consecutivas, a aceleração é zero, ou seja, o robô fica com velocidade constante.
                return new AmostraSensores(0f, 0f, new float[]{300, 0, 300}, System.currentTimeMillis());
            }
        }
    }
}

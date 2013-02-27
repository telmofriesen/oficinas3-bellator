/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stefan
 */
public class ContadorBytesTempoReal extends Thread {

    protected int update_delay = 1000; //Intervalo entre cada atualização;
    protected float byte_rate = 0; //bytes por segundo atual
    protected int time_window = 2000; //Tamanho da janela (ms). Número de milissegundos entre cada janela de contagem de amostras.
    protected int time_window_min = 2000;
    protected int time_window_max = 10000;
    protected int time_window_step_up = 1000;
    protected int time_window_step_down = 1000;
    protected ArrayList<Sample> samples;
    protected final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList();
//    protected int update_interval = 1000;
    protected boolean run_timer = true;
    protected boolean run = true;
//    private Timer timer;

    public ContadorBytesTempoReal() {
        samples = new ArrayList<Sample>();
        this.setName("ContadorBytesTempoReal");
//        update_delay = 1000;
//        sample_rate = 0;
//        time_window = 2000;
//        time_window_min = 2000;
//        time_window_max = 10000;
//        time_window_step_up = 1000;
//        time_window_step_down = 1000;
    }

    public ContadorBytesTempoReal(int time_window, int time_window_min, int time_window_max, int update_delay) {
        samples = new ArrayList<Sample>();
        this.setName("ContadorBytesTempoReal");
        this.time_window = time_window;
        this.time_window_min = time_window_min;
        this.time_window_max = time_window_max;
        this.update_delay = update_delay;
    }

    @Override
    public void run() {
        while (run) {
            synchronized (this) {
                while (!run_timer) {
                    try {
                        wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ContadorAmostragemTempoReal.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
            while (run_timer) {
                updateSampleRate();
                fireChangeEvent();
                try {
                    sleep(update_delay);
                } catch (InterruptedException ex) {
                    Logger.getLogger(ContadorAmostragemTempoReal.class.getName()).log(Level.SEVERE, null, ex);
                }

            }
        }
    }
//    public ContadorAmostragemTempoReal(int update_interval, int time_window, int time_window_min, int time_window_max) {
//        super(time_window, time_window_min, time_window_max);
//        this.update_interval = update_interval;
//    }

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

    /**
     * Informa o recebimento de uma nova amostra.
     *
     * @param size Tamanho em bytes da amostra.
     */
    public synchronized void novaAmostra(int size) {
        samples.add(new Sample(System.currentTimeMillis(), size));
    }

    public synchronized void updateSampleRate() {
        long timestamp = System.currentTimeMillis();
        long time_window_start = timestamp - time_window;
        //Remove os elementos que estejam fora da janela de tempo.
        while (!samples.isEmpty() && samples.get(0).time < time_window_start) {
            //Como o array está ordenado em ordem crescente, verifica apenas o começo do array a cada iteração.
            samples.remove(0);
        }
        int read_count = samples.size();

        if (read_count == 0) { //Aumenta a janela de tempo se não houver nenhuma leitura
            time_window = Math.min(time_window + time_window_step_up, time_window_max); //Máximo de 5 segundos
        } else if (read_count > 30) { //Reduz a janela de tempo se houverem muitas leituras
            time_window = Math.max(time_window - time_window_step_down, time_window_min); //Mínimo de 0,5 segundos
        }

        int total_bytes = 0;
        for (int i = 0; i < samples.size(); i++) {
            total_bytes += samples.get(i).size;
        }
        //Atualiza a taxa de transferencia
        byte_rate = (float) total_bytes / ((float) time_window / 1000); // bytes/s

//        fireChangeEvent();
    }

    public synchronized void startUpdateTimer() {
        if (!this.isAlive()) {
            this.start();
        }
        run_timer = true;
        this.notifyAll();
    }

    public synchronized void stopUpdateTimer() {
        run_timer = false;
    }

    public synchronized float getByte_rate() {
        return byte_rate;
    }

    class Sample {

        /**
         * Tempo de recebimento da amostra
         */
        public long time;
        /**
         * Tamanho da amostra em bytes
         */
        public int size;

        public Sample(long time, int size) {
            this.time = time;
            this.size = size;
        }
    }
}

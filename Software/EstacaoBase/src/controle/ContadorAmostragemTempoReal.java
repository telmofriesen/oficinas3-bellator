/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author stefan
 */
public class ContadorAmostragemTempoReal extends Thread {

    private long last_update = -1; //Inicio da janela de tempo atual (ms)
    private int time_window_read_count = 0; //Numero de leituras na janela de tempo atual
    private float sample_rate = 0; //leituras por segundo atual
    private int time_window = 2000; //Tamanho da janela. Número de milissegundos entre cada conjunto de contagem.
    private int time_window_min = 2000;
    private int time_window_max = 10000;
    private int time_window_step_up = 1000;
    private int time_window_step_down = 1000;
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList();
//    private int update_interval = 1000;
    private boolean run_timer = true;
    private boolean run = true;
//    private Timer timer;

    public ContadorAmostragemTempoReal() {
        super();
    }

    public ContadorAmostragemTempoReal(int time_window, int time_window_min, int time_window_max) {
        this.time_window = time_window;
        this.time_window_min = time_window_min;
        this.time_window_max = time_window_max;
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
                try {
                    sleep(time_window);
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

    public synchronized void novaAmostra() {
        time_window_read_count++;
    }

    public synchronized void updateSampleRate() {
        long timestamp = System.currentTimeMillis();
        if (last_update == -1) { //Executado na primeira amostra
            last_update = timestamp;
        }
        if (time_window_read_count == 0) { //Aumenta a janela de tempo se não houver nenhuma leitura
            time_window = Math.min(time_window + time_window_step_up, time_window_max); //Máximo de 5 segundos
            stopUpdateTimer();
            startUpdateTimer();
        } else if (time_window_read_count > 40) { //Reduz a janela de tempo se houverem muitas leituras
            time_window = Math.max(time_window - time_window_step_down, time_window_min); //Mínimo de 0,5 segundos
            stopUpdateTimer();
            startUpdateTimer();
        }
        //Atualiza a taxa de transferencia
        sample_rate = (float) time_window_read_count / (float) (timestamp - last_update) * 1000; //leituras por segundo
        //Inicia nova janela de tempo
        last_update = timestamp;

        fireChangeEvent();
        time_window_read_count = 0;
    }

    public synchronized void startUpdateTimer() {
        if(!this.isAlive()){
            this.start();
        }
        run_timer = true;
        this.notifyAll();
    }

    public synchronized void stopUpdateTimer() {
        run_timer = false;
    }

    public synchronized float getSample_rate() {
        return sample_rate;
    }
}

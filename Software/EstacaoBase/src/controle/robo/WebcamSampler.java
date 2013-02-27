/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle.robo;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamListener;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread responsável por obter amostras da webcam.
 *
 * @author stefan
 */
public class WebcamSampler extends Thread implements WebcamListener, WebcamDiscoveryListener {

    private Webcam webcam;
    //Indica se a amostragem está habilitada. OBS: a amostragem efetiva só ocorrerá se tanto 'sample' quanto 'webcam_available' forem verdadeiros.
    private boolean sampling_enabled = false;
    //Indica se uma webcam está disponível.
    private boolean webcam_available = false;
    //Taxa de amostragem máxima
    private float sample_rate; //Amostras/s
    //Imagem atualmente capturada
    private BufferedImage image;
    //Resolução de captura
    private Dimension resolution;
    //Indica se o loop principal deve ser executado ou não.
    private boolean run = false;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners;

    public WebcamSampler(float sample_rate, Dimension resolution) {
        listeners = new CopyOnWriteArrayList<MyChangeListener>();
        this.sample_rate = sample_rate;
        this.resolution = resolution;
        Webcam w = Webcam.getDefault();

        if (w == null) {
            webcam_available = false;
        } else {
            webcamFound(new WebcamDiscoveryEvent(w, WebcamDiscoveryEvent.ADDED));
//            webcam_available = true;
//            configWebcam();
        }
        Webcam.addDiscoveryListener(WebcamSampler.this);
    }

    public void run() {
//        System.out.println("1");
        synchronized (this) {
//            System.out.println("2");
            run = true;
        }
//        System.out.println("3");
        long sleep_time;
        while (run) {
//            System.out.println("4");
            synchronized (this) {
                if (webcam != null) {
                    BufferedImage img = webcam.getImage();
                    this.image = img;
                }
                sleep_time = (long) (1000f / sample_rate);
            }
//            System.out.println("5");
            fireChangeEvent();

            try {
                sleep(sleep_time); //Pausa a execução em um tempo inversamente proporcional à taxa de amostragem.
            } catch (InterruptedException ex) {
                Logger.getLogger(WebcamSampler.class.getName()).log(Level.SEVERE, null, ex);
            }
//            System.out.println("6");

            synchronized (this) {
                //Entra em estado de espera se a Amostragem estiver desativada ou a Webcam estiver fechada.
                while (run && (!sampling_enabled || !webcam_available)) {
                    try {
//                        System.out.println("7");
                        wait();
//                        System.out.println("8");
                    } catch (InterruptedException ex) {
                        Logger.getLogger(WebcamSampler.class.getName()).log(Level.SEVERE, null, ex);
                    }
//                    System.out.println("9");
                }
//                System.out.println("10");
            }
        }
//        System.out.println("11");
    }

    public final synchronized void configWebcam() {
//        System.out.println("config");
        assert (webcam != null);
        webcam.setViewSize(resolution);
        webcam.addWebcamListener(WebcamSampler.this);
        if (sampling_enabled) webcam.open();
    }

    public synchronized float getSample_rate() {
        return sample_rate;
    }

    public synchronized void setSample_rate(float sample_rate) {
        this.sample_rate = sample_rate;
    }

    public void startSampling() {
        synchronized (this) {
            sampling_enabled = true;
            if (webcam_available) webcam.open();
            this.notifyAll();
        }
        fireChangeEvent();
    }

    public void stopSampling() {
        synchronized (this) {
            try {
                if (webcam_available) webcam.close();
            } catch (WebcamException ex) {
            }
            sampling_enabled = false;
        }
        fireChangeEvent();
    }

    public synchronized boolean isSamplingEnabled() {
        return sampling_enabled;
    }

    public synchronized boolean isWebcam_available() {
        return webcam_available;
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

    public synchronized BufferedImage getImage() {
        return image;
    }

    public synchronized Dimension getResolution() {
        return resolution;
    }

    public void setResolution(Dimension resolution) {
        synchronized (this) {
            this.resolution = resolution;
        }
        stopSampling();
        startSampling();
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

    public synchronized String getWebcamName() {
        if (webcam != null)
            return webcam.getName();
        else
            return "";
    }

    public synchronized String getStatusMessage() {
        if (webcam_available) {
            return "WEBCAM STATUS REPLY " + sampling_enabled + " " + webcam_available + " " + webcam.getName();
        } else {
            return "WEBCAM STATUS REPLY " + sampling_enabled + " " + webcam_available;
        }
    }

    @Override
    public void webcamOpen(WebcamEvent we) {
        System.out.printf("[WebcamSampler] webcam open(): %s\n", we.getSource().getName());
//        boolean equals;
//        synchronized (this) {
//            equals = we.getSource().equals(webcam);
//        }
//        if (equals) {
//            synchronized (this) {
//                this.notifyAll();
//            }
//            fireChangeEvent();
//        }
    }

    @Override
    public void webcamClosed(WebcamEvent we) {
        System.out.printf("[WebcamSampler] webcam close(): %s\n", we.getSource().getName());
//        boolean equals;
//        synchronized (this) {
//            equals = we.getSource().equals(webcam);
//        }
//        if (equals) {
//            fireChangeEvent();
//        }
    }

    @Override
    public void webcamDisposed(WebcamEvent we) {
        System.out.printf("[WebcamSampler] webcam dispose(): %s\n", we.getSource().getName());
    }

    @Override
    public final synchronized void webcamFound(WebcamDiscoveryEvent wde) {
        System.out.printf("[WebcamSampler] Nova webcam conectada: %s\n", wde.getWebcam().getName());
        if (!webcam_available) {
            webcam = wde.getWebcam();
            webcam_available = true;
            configWebcam();
        }
    }

    @Override
    public synchronized void webcamGone(WebcamDiscoveryEvent wde) {
        System.out.printf("[WebcamSampler] Webcam desconectada: %s\n", wde.getWebcam().getName());
        if (wde.getWebcam().equals(webcam)) {
            webcam_available = false;
        }
    }
}

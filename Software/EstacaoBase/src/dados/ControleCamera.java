package dados;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Classe responsável por controlar imagens da câmera e status do recebimento de imagens.
 *
 * @author stefan
 */
public class ControleCamera implements MyChangeListener {

    private BufferedImage image;
    private ContadorAmostragemTempoReal contadorFramerate;
    private ContadorBytesTempoReal contadorByterate;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners;
    private boolean sampling_enabled = false;
    private boolean sampling_status_changing = false;
    private boolean webcam_available = false;
    private boolean stream_available = false;
    private int stream_port = 5050;
    private String webcam_name = "";

    public ControleCamera() {
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        this.image = null;
        contadorFramerate = new ContadorAmostragemTempoReal(500, 500, 1000, 500);
        contadorByterate = new ContadorBytesTempoReal(500, 500, 1000, 500);
        contadorByterate.startUpdateTimer();
        contadorFramerate.startUpdateTimer();
    }

    public synchronized void novaImagemCamera(BufferedImage image, int size) {
        synchronized (this) {
//            if (!listener_added) {
//                contadorFramerate.addMyChangeListener(this);
//                contadorByterate.addMyChangeListener(this);
//                listener_added = true;
//            }
            this.image = image;
            contadorFramerate.novaAmostra();
            contadorByterate.novaAmostra(size);
//        System.out.println(contadorAmostragem.getSample_rate());
        }
        fireChangeEvent();
    }

    public synchronized Dimension getImageDimension() {
        if (image != null) {
            return new Dimension(image.getWidth(), image.getHeight());
        } else {
            return null;
        }
    }

    public synchronized BufferedImage getImage() {
        return image;
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

    public ContadorAmostragemTempoReal getContadorFramerate() {
        return contadorFramerate;
    }

    public ContadorBytesTempoReal getContadorByterate() {
        return contadorByterate;
    }

    public float getByte_rate() {
        return contadorByterate.getByte_rate();
    }

    public float getFramerate() {
        return contadorFramerate.getSample_rate();
    }

    public synchronized boolean isSampling_enabled() {
        return sampling_enabled;
    }

    public void setSampling_enabled(boolean sampling_enabled) {
        synchronized (this) {
            this.sampling_enabled = sampling_enabled;
            this.sampling_status_changing = false;
        }
        fireChangeEvent();
    }

    public synchronized boolean isSampling_status_changing() {
        return sampling_status_changing;
    }

    public synchronized void setSampling_status_changing(boolean sampling_status_changing) {
        this.sampling_status_changing = sampling_status_changing;
        fireChangeEvent();
    }

    public synchronized boolean isWebcam_available() {
        return webcam_available;
    }

    public void setWebcam_available(boolean webcam_available) {
        synchronized (this) {
            this.webcam_available = webcam_available;
        }
        fireChangeEvent();
    }

    public synchronized boolean isStream_available() {
        return stream_available;
    }

    public void setStream_available(boolean stream_availabe) {
        synchronized (this) {
            this.stream_available = stream_availabe;
        }
        fireChangeEvent();
    }

    public synchronized int getStream_port() {
        return stream_port;
    }

    public void setStream_port(int stream_port) {
        synchronized (this) {
            this.stream_port = stream_port;
        }
        fireChangeEvent();
    }

    public synchronized String getWebcam_name() {
        return webcam_name;
    }

    public synchronized void setWebcam_name(String webcam_name) {
        this.webcam_name = webcam_name;
    }

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        fireChangeEvent();
    }
}

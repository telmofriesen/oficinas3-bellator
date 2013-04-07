/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robo.gerenciamento;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import com.github.sarxos.webcam.WebcamEvent;
import com.github.sarxos.webcam.WebcamException;
import com.github.sarxos.webcam.WebcamListener;
import com.github.sarxos.webcam.WebcamStreamer;
import comunicacao.Base64new;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.headless.DefaultHeadlessMediaPlayer;
import uk.co.caprica.vlcj.player.headless.HeadlessMediaPlayer;

/**
 * Thread responsável por obter amostras da webcam.
 *
 * @author stefan
 */
public class WebcamManager implements WebcamListener, WebcamDiscoveryListener {
    public static final int WEBCAM_STREAM_DEFAULT_PORT = 5050;

    private Webcam webcam;
//    private WebcamStreamerNew webcamStreamer;
    //Indica se a amostragem está habilitada. OBS: a amostragem efetiva só ocorrerá se tanto 'sample' quanto 'webcam_available' forem verdadeiros.
    private boolean sampling_enabled = false;
    //Indica se uma webcam está disponível.
    private boolean webcam_available = false;
    private boolean stream_available = false;
    private String host_ip = "127.0.0.1";
    private int host_port = 5050;
    MediaPlayerFactory mediaFactory;
    HeadlessMediaPlayer mediaStreamer;
    //Taxa de amostragem máxima
    private float fps = 10; //Amostras/s
    private int bitrate = 2048;
    //Resolução de captura
    private Dimension resolution;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners;

    public WebcamManager(int bitrate, Dimension resolution) {
        listeners = new CopyOnWriteArrayList<MyChangeListener>();
//        this.fps = fps;
        this.bitrate = bitrate;
        this.resolution = resolution;
        this.mediaFactory = new MediaPlayerFactory();
        this.mediaStreamer = mediaFactory.newHeadlessMediaPlayer();
        Webcam w = Webcam.getDefault();

        if (w == null) {
            webcam_available = false;
        } else {
            webcamFound(new WebcamDiscoveryEvent(w, WebcamDiscoveryEvent.ADDED));
//            webcam_available = true;
//            configWebcam();
        }
        Webcam.addDiscoveryListener(WebcamManager.this);
    }

    public final synchronized void configWebcam() {
//        System.out.println("config");
        assert (webcam != null);
        webcam.setViewSize(resolution);
        webcam.addWebcamListener(WebcamManager.this);
//        if (sampling_enabled) webcam.open();
    }

    public final synchronized void startStream() {
//        webcamStreamer = new WebcamStreamerNew(stream_port, webcam, sample_rate, true);
        if (webcam_available) {
            String[] split = webcam.getDevice().getName().split(" ");
            String device = split[split.length - 1];
            String mrl = String.format("v4l2://%s:width=%s:height=%s:fps=%.2f:chroma=mjpg", device, resolution.width, resolution.height, fps);
            String options = formatStream(resolution, bitrate, fps, "127.0.0.1", host_port);
            System.out.println("---- INICIANDO STREAM: " + mrl + " -- " + options);

            mediaStreamer.playMedia(mrl, options);
            stream_available = true;
            fireChangeEvent();
        }
    }

    public final synchronized void stopStream() {
        if (stream_available) {
            mediaStreamer.stop();
//            webcamStreamer.stop();
            stream_available = false;
            fireChangeEvent();
        }
    }

    public synchronized void setHost(String host_ip, int host_port) {
        this.host_ip = host_ip;
        this.host_port = host_port;
        if (stream_available) {
            stopStream();
            startStream();
        }
    }

    public synchronized void setOptions(Dimension resolution, int bitrate, float fps, String host_ip, int host_port) {
        this.resolution = resolution;
        this.bitrate = bitrate;
        this.fps = fps;
        this.host_ip = host_ip;
        this.host_port = host_port;
        if (stream_available) {
            stopStream();
            startStream();
        }
    }

    private static String formatStream(Dimension resolution, int bitrate, float fps, String host_ip, int host_port) {
//        StringBuilder sb = new StringBuilder(60);
//        return String.format(":sout=#transcode{vcodec=mp4v,bitrate=%d,fps=%f,width=%d,height=%d}:rtp{dst=%s,port=%d,mux=ts}",
//                             bitrate, fps, resolution.width, resolution.height, host_ip, host_port);
        return String.format(":sout=#standard{access=http,width=%d,height=%d,fps=%.2f,mux=mpjpeg,dst=%s:host_port}",
                             resolution.width, resolution.height, fps, "0.0.0.0", host_port);
        
    }

//    public synchronized float getSample_rate() {
//        return fps;
//    }
//
//    public synchronized void setSample_rate(float sample_rate) {
//        this.fps = sample_rate;
//        if (stream_available) {
//            stopStream();
//            startStream();
//        }
////        if (stream_available) webcamStreamer.setFps(sample_rate);
//    }
    public synchronized int getBitrate() {
        return bitrate;
    }

    public synchronized void setBitrate(int bitrate) {
        this.bitrate = bitrate;
        if (stream_available) {
            stopStream();
            startStream();
        }
    }

    public synchronized float getFps() {
        return fps;
    }

    public synchronized void setFps(float fps) {
        this.fps = fps;
        if (stream_available) {
            stopStream();
            startStream();
        }
    }

    public void startSampling() {
        synchronized (this) {
            sampling_enabled = true;
            if (webcam_available) startStream();
            this.notifyAll();
        }
        fireChangeEvent();
    }

    public void stopSampling() {
        synchronized (this) {
            stopStream();
            sampling_enabled = false;
        }
        fireChangeEvent();
    }

    public void reset() {
        stopSampling();
        setBitrate(1024);
//        setSample_rate(10);
    }

    public synchronized boolean isSamplingEnabled() {
        return sampling_enabled;
    }

    public synchronized boolean isWebcam_available() {
        return webcam_available;
    }

    public synchronized Dimension getResolution() {
        return resolution;
    }

    public void setResolution(Dimension resolution) {
        synchronized (this) {
            this.resolution = resolution;
        }
        if (stream_available) {
            stopStream();
            startStream();
        }
//        stopSampling();
//        startSampling();
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

    /**
     * Retorna uma mensagem de status seguindo o protocolo de comunicação
     *
     * @return
     */
    public synchronized String getStatusMessage() {
        StringBuilder str = new StringBuilder();
        str.append("WEBCAM STATUS REPLY ");
        str.append(sampling_enabled);
        str.append(" ");
        str.append(stream_available);
        str.append(" ");
        str.append(host_port);
        str.append(" ");
        str.append(webcam_available);
        if (webcam_available) {
            str.append(" ");
            str.append(webcam.getName());
//            str.append(Base64new.encode(webcam.getName().getBytes()));
        }
        return str.toString();
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
    public final void webcamFound(WebcamDiscoveryEvent wde) {
        System.out.printf("[WebcamSampler] Nova webcam conectada: %s\n", wde.getWebcam().getName());
        synchronized (this) {
            if (!webcam_available) {
                webcam = wde.getWebcam();
                webcam_available = true;
                configWebcam();
                if (sampling_enabled) startStream();
            }
        }
        fireChangeEvent();
    }

    @Override
    public void webcamGone(WebcamDiscoveryEvent wde) {
        System.out.printf("[WebcamSampler] Webcam desconectada: %s\n", wde.getWebcam().getName());
        synchronized (this) {
            if (wde.getWebcam().equals(webcam)) {
                webcam_available = false;
                stopStream();
            }
        }
        fireChangeEvent();
    }

    public synchronized boolean isSampling_enabled() {
        return sampling_enabled;
    }

    public synchronized boolean isStream_open() {
        return stream_available;
    }

    public synchronized int getStream_port() {
        return host_port;
    }
}

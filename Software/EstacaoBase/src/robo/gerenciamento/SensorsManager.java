/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robo.gerenciamento;

import comunicacao.ClientMessageProcessor;
import robo.gerenciamento.old.WebcamSampler;
import dados.AmostraSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import robo.Main;
import robo.SerialMessage;

/**
 *
 * @author stefan
 */
public class SensorsManager extends Thread {
    //Indica se a amostragem está habilitada.

    private Main main;
    private boolean sampling_enabled = false;
    private AmostraSensores currentSample;
    //Taxa de envios de syncs
    private float syncRate; //syncs/s
    //Período de amostragem da placa de baixo nível, em milissegundos
    private int lowLevelSamplePeriod = 257;
    //Indica se o loop principal deve ser executado ou não.
    private boolean run = false;
    //Timestamp UNIX em milissegundos da última leitura.
    private long lastUnixTimestamp = -1;
    //Timestamp de sequência (um contador simples que é incrementado a cada leitura dos sensores). 
    //Ele é resetado para 0 na placa de baixo nível quando passa de 65535.
    private int lastSequenceTimestamp = 65532;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();
//    private boolean first_sent = false;
//    private boolean second_sent = false;
//    private boolean third_sent = false;
//    private long first_sample_time;

    public SensorsManager(Main main, float sample_rate) {
        this.main = main;
        this.syncRate = sample_rate;
    }

    @Override
    public void run() {
        synchronized (this) {
            run = true;
        }
        long sleep_time;
        while (run) {
            synchronized (this) {
                //                currentSample = a;
                sleep_time = (long) (1000f / syncRate);
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
            //Envia comando SYNC
            if (main.getSerialCommunicator() != null) {
                main.getSerialCommunicator().sendMessage(new byte[]{ClientMessageProcessor.SYNC});
                System.out.println("[SensorsManager]: Comando SYNC enviado.");
            } else {
                System.out.println("[SensorsManager]: Comando SYNC não enviado! (main.getSerialCommunicator() == null)");
            }
            //TESTES para simulação (comentar essa seção se for usar o programa na prática com o robô)
            //--------
////                        AmostraSensores a = gerarExemplo();
//            byte[] amostra = gerarExemploEmBytes();
//            main.getSerialCommunicator().parseInput(1, new SerialMessage(amostra, amostra.length));
//            novaLeituraSensores(amostra);
            //--------

//                String str = new String(amostra, "ISO-8859-1");
//                main.sendMessageToMainHost(str, false);
        }
    }

    public static short bytesToShort(byte high, byte low) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(high);
        bb.put(low);
        return bb.getShort(0);
    }

    public synchronized void novaLeituraSensores(byte[] sensors) {
        try {
            if (sensors[0] != ClientMessageProcessor.SENSORS) {
                return;
            }
            //
            // Converter os valores para inteiro
            //
//        int encoder_esq = (int) bytesToShort(sensors[1], sensors[2]) & 0x0000ffff;
//        int encoder_dir = (int) bytesToShort(sensors[3], sensors[4]) & 0x0000ffff;
//        int[] IR = {
//            (int) bytesToShort((byte) 0, sensors[5]),
//            (int) bytesToShort((byte) 0, sensors[6]),
//            (int) bytesToShort((byte) 0, sensors[7]),
//            (int) bytesToShort((byte) 0, sensors[8]),
//            (int) bytesToShort((byte) 0, sensors[9])};
//        int AX = (int) bytesToShort(sensors[10], sensors[11]) & 0x0000ffff;
//        int AY = (int) bytesToShort(sensors[12], sensors[13]) & 0x0000ffff;
//        int AZ = (int) bytesToShort(sensors[14], sensors[15]) & 0x0000ffff;
//        int GX = (int) bytesToShort(sensors[16], sensors[17]) & 0x0000ffff;
//        int GY = (int) bytesToShort(sensors[18], sensors[19]) & 0x0000ffff;
//        int GZ = (int) bytesToShort(sensors[20], sensors[21]) & 0x0000ffff;
            int sequenceTimestamp = (int) bytesToShort(sensors[14], sensors[15]) & 0x0000ffff;

            //
            // Calcular a UNIX timestamp a partir da Sequence timestamp
            //

            if (lastUnixTimestamp == -1) { //Executado na primeira vez somente
                lastUnixTimestamp = System.currentTimeMillis();
                lastSequenceTimestamp = sequenceTimestamp;
            }
            int count = 0;
            if (sequenceTimestamp < lastSequenceTimestamp) {
                count = (65536 - lastSequenceTimestamp) + sequenceTimestamp;
                lastSequenceTimestamp = sequenceTimestamp;
            } else {
                count = sequenceTimestamp - lastSequenceTimestamp;
                lastSequenceTimestamp = sequenceTimestamp;
            }
            long unixTimestamp = lastUnixTimestamp + (lowLevelSamplePeriod * count);
            lastUnixTimestamp = unixTimestamp;

            StringBuilder sb = new StringBuilder();
            sb.append("S ");
            sb.append(new AmostraSensores((int) bytesToShort(sensors[1], sensors[2]),
                                          (int) bytesToShort(sensors[3], sensors[4]),
                                          new int[]{
                        (int) bytesToShort((byte) 0, sensors[5]), //IR1
                        (int) bytesToShort((byte) 0, sensors[6]), //IR2
                        (int) bytesToShort((byte) 0, sensors[7]), //IR3
                        (int) bytesToShort((byte) 0, sensors[8]), //IR4
                        (int) bytesToShort((byte) 0, sensors[9])},//IR5
//                                          (int) bytesToShort(sensors[10], sensors[11]), //& 0x0000ffff,
//                                          (int) bytesToShort(sensors[12], sensors[13]), //& 0x0000ffff,
//                                          (int) bytesToShort(sensors[14], sensors[15]), //& 0x0000ffff,
//                                          (int) bytesToShort(sensors[16], sensors[17]), //& 0x0000ffff,
//                                          (int) bytesToShort(sensors[18], sensors[19]), //& 0x0000ffff,
//                                          (int) bytesToShort(sensors[20], sensors[21]), //& 0x0000ffff,
                                          0, //& 0x0000ffff, //Ax
                                          (int) bytesToShort(sensors[10], sensors[11]), //& 0x0000ffff, //Ay
                                          0, //& 0x0000ffff, //Az
                                          0, //& 0x0000ffff, //Gx
                                          0, //& 0x0000ffff, //Gy
                                          (int) bytesToShort(sensors[12], sensors[13]), //& 0x0000ffff, //Gz
                                          unixTimestamp)
                    .toString());
            main.sendMessageToMainHost(sb.toString(), false);
        } catch (NullPointerException ex) {
            System.out.printf("[SensorsManager] Erro: %s\n", ex.getMessage());
        }
    }

    /**
     * Recebe mensagem SENSORS da porta serial e troca o timestamp de sequência por um timestamp UNIX.
     * Depois, envia a mensagem para a estação base.
     *
     * @param mensagem
     */
    public void novaLeituraSensores_old(byte[] mensagem) {
        if (mensagem[0] != ClientMessageProcessor.SENSORS) {
            return;
        }
        ByteBuffer bb = ByteBuffer.allocate(30);
        //
        // Insere o inicio da mensagem (leituras dos sensores).
        //
        bb.put(mensagem, 0, 22);
        //
        // Calcula o timestamp UNIX (em milissegundos). 
        // O próximo timestamp UNIX é calculado tomando-se como base o último timestamp, o último número de sequência e o número de sequência da leitura atual.
        //
        int sequenceTimestamp = (int) bytesToShort(mensagem[22], mensagem[23]) & 0x0000ffff;
        if (lastUnixTimestamp == -1) { //Executado na primeira vez somente
            lastUnixTimestamp = System.currentTimeMillis();
            lastSequenceTimestamp = sequenceTimestamp;
        }
        int count = 0;
        if (sequenceTimestamp < lastSequenceTimestamp) {
            count = (65536 - lastSequenceTimestamp) + sequenceTimestamp;
            lastSequenceTimestamp = sequenceTimestamp;
        } else {
            count = sequenceTimestamp - lastSequenceTimestamp;
            lastSequenceTimestamp = sequenceTimestamp;
        }
        long unixTimestamp = lastUnixTimestamp + (lowLevelSamplePeriod * count);
        lastUnixTimestamp = unixTimestamp;
        bb.putLong(unixTimestamp);
        byte[] bytes = bb.array();
        try {
            String str = new String(bytes, "ISO-8859-1");
            main.sendMessageToMainHost(str, false);
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(SensorsManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    private int counter1 = 0;

    /**
     * Gera leituras para teste.
     *
     * @return
     */
    public byte[] gerarExemploEmBytes() {
//        long time = System.currentTimeMillis();
        ByteBuffer buf = ByteBuffer.allocate(18);
        byte[] bytes = ByteBuffer.allocate(4).putInt(counter1).array();
        buf.put(new byte[]{ClientMessageProcessor.SENSORS,
                           0, 20,
                           0, 15,
                           (byte) 50, (byte) 0, (byte) 0, (byte) 0, (byte) 0,
                           0, 10,
                           0, 0,
                           bytes[2], bytes[3]});

        counter1 = (counter1 + 1) % 65536;
//        buf.putLong(System.currentTimeMillis());
        byte[] array = buf.array();
        return array;
    }

    /**
     * Ler sensores.
     * A versão atual retorna leituras de teste.
     * TODO implementar leitura real.
     *
     * @return
     */
    public AmostraSensores gerarExemplo() {
        return new AmostraSensores(100, 100, new int[]{150, 0, 150}, 0, 0, 0, 0, 0, 0, System.currentTimeMillis());
//        synchronized (this) {
//            if (!first_sent) { //Na primeira medida enviada, envia um valor de aceleração inicial para que o robô comece a se mover.
//                first_sent = true;
//                first_sample_time = System.currentTimeMillis();
//                return new AmostraSensores(0f, 0f, new float[]{300, 0, 300}, System.currentTimeMillis());
//            } else if (!second_sent) {
////                try {
////                    sleep(500);
////                } catch (InterruptedException ex) {
////                    Logger.getLogger(SensorsManager.class.getName()).log(Level.SEVERE, null, ex);
////                }
//                second_sent = true;
//                return new AmostraSensores(0.1f, 0f, new float[]{300, 0, 300}, first_sample_time + 1000);
//            } else if (!third_sent) {
////                try {
////                    sleep(500);
////                } catch (InterruptedException ex) {
////                    Logger.getLogger(SensorsManager.class.getName()).log(Level.SEVERE, null, ex);
////                }
//                third_sent = true;
//                return new AmostraSensores(0.0f, 0f, new float[]{300, 0, 300}, first_sample_time + 2000);
//            } else { //Nas medidas consecutivas, a aceleração é zero, ou seja, o robô fica com velocidade constante.
//                return new AmostraSensores(0f, 0f, new float[]{300, 0, 300}, System.currentTimeMillis());
//            }
//        }
    }

    public synchronized AmostraSensores getCurrentSample() {
        return currentSample;
    }

    public synchronized float getSample_rate() {
        return syncRate;
    }

    public synchronized void setSample_rate(float sample_rate) {
        this.syncRate = sample_rate;
    }

    /**
     * Inicia a amostragem.
     */
    public void startSampling() {
        synchronized (this) {
            sampling_enabled = true;
            this.notifyAll();
        }
        main.getSerialCommunicator().sendMessage(new byte[]{(byte) 0xA1}); //Envia comando CLEAR_BUFF
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

    public synchronized String getStatusMessage() {
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
//    public synchronized void resetTests() {
//        first_sent = false;
//        second_sent = false;
//        third_sent = false;
//    }
}

package comunicacao;

import dados.AmostraSensores;
import robo.ServerMessageProcessor;
import dados.GerenciadorCamera;
import dados.GerenciadorSensores;
import dados.NumIRException;
import dados.TimestampException;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Interpretador de comandos.
 * Os comandos recebidos são inseridos em uma fila, de modo a serem posteriormente executados pela thread.
 * O objetivo é que o Receiver não fique bloqueado para esprar que cada comando seja executado.
 * OBS: Ver especificação do protocolo (Estação base <-> Linux embarcado) para maior esclarecimento.
 *
 * @author Stefan
 */
public class ClientMessageProcessor extends Thread {
    //
    //Indica se as amostras dos sensores devem ser salvas em um arquivo.

    private final boolean DEBUG_RECORD_SAMPLES = true;
    private BufferedOutputStream debugBufferdOutputStream;
    //Array que armazena a fila de comandos
    private ArrayList<String> commandsList = new ArrayList();
    //Referecia ao objeto connector do cliente
    private ClientConnector connector;
    //Referecia ao objeto ControleSensores do cliente
    private GerenciadorSensores gerenciadorSensores;
    //Referencia ao objeto ControleCamera do cliente
    private GerenciadorCamera controleCamera;
    //Indica se a thread deve rodar ou não
    private boolean run = true;
    public static final byte SENSORS = (byte) 0xC0;
    public static final byte ENGINES = (byte) 0xB0;
    public static final byte SYNC = (byte) 0xA0;

    public ClientMessageProcessor(ClientConnector connector, GerenciadorSensores controleSensores, GerenciadorCamera controleCamera) {
        this.setName("TR_ClientCommandInterpreter");
        this.connector = connector;
        this.gerenciadorSensores = controleSensores;
        this.controleCamera = controleCamera;
        if (DEBUG_RECORD_SAMPLES) {
            String filename = "";
            try {
                filename = String.format("testes/%d.txt", System.currentTimeMillis());
                debugBufferdOutputStream = new BufferedOutputStream(new FileOutputStream(filename));
            } catch (IOException ex) {
                System.out.printf("Erro ao abrir arquivo para gravação: %s. (%s)\n", filename, ex.getMessage());
            }
        }
    }

    @Override
    public void run() {
        run = true;
        String command;
        int num_elementos = 0;
        while (run) {
            synchronized (this) {
                num_elementos = commandsList.size();
            }
            //Enquanto o vetor tiver elementos....
            while (num_elementos > 0) {
                synchronized (this) {
                    command = commandsList.get(0);
                    //Executa o comando da posicao 0...
                    if (!runCommand(command)) {
                        System.out.printf("[TR_ClientCommandInterpreter] Comando invalido recebido: \"%s\"\n", command);
                    }
                    //Faz a fila andar....
                    commandsList.remove(0);
                    num_elementos = commandsList.size();
                }
            }
            synchronized (this) {
                while (commandsList.isEmpty() && run) { //Enquanto a fila estiver vazia, espera até que hajam elementos.
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ServerMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Adiciona um comado à lista de execução. Ele será posteriormente executado pela thread.
     * OBS: Método thread-safe.
     *
     * @param message
     */
    public synchronized void processCommand(String message) {
        if (message != null && !message.isEmpty()) {
            commandsList.add(message);
            this.notifyAll();
        }
    }

    public static short bytesToShort(byte high, byte low) {
        ByteBuffer bb = ByteBuffer.allocate(2);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(high);
        bb.put(low);
        return bb.getShort(0);
    }

    public static long bytesToLong(byte b7, byte b6, byte b5, byte b4,
                                   byte b3, byte b2, byte b1, byte b0) {
        ByteBuffer bb = ByteBuffer.allocate(8);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.put(b7);
        bb.put(b6);
        bb.put(b5);
        bb.put(b4);
        bb.put(b3);
        bb.put(b2);
        bb.put(b1);
        bb.put(b0);
        return bb.getLong(0);
    }

    /**
     * Rotina para testes.
     *
     * @param args
     */
    public static void main(String args[]) {
        int a = (int) bytesToShort((byte) 0xFF, (byte) 0xFF) & 0x0000ffff;
//        byte[] command = {(byte) SENSORS};
        byte[] command = {(byte) SENSORS,
                          0, 1,
                          0, 1,
                          5, 5, 5, 5, 5,
                          1, 1,
                          0, 1,
                          0, 1,
                          0, 1,
                          0, 1,
                          0, 1,
                          0, 1};
        String str = "";

        try {
            str = new String(command, "ISO-8859-1");
            System.out.println(str.length());
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
        byte b = (byte) str.charAt(0);
        byte[] sensors;
        try {
            sensors = str.getBytes("ISO-8859-1");
            if (sensors[0] == SENSORS) {
                int encoder_esq = (int) bytesToShort(sensors[1], sensors[2]);
                int encoder_dir = (int) bytesToShort(sensors[3], sensors[4]);
                byte[] IR = {sensors[5], sensors[6], sensors[7], sensors[8], sensors[9]};
                int AX = (int) bytesToShort(sensors[10], sensors[11]);
                int AY = (int) bytesToShort(sensors[12], sensors[13]);
                int AZ = (int) bytesToShort(sensors[14], sensors[15]);
                int GX = (int) bytesToShort(sensors[16], sensors[17]);
                int GY = (int) bytesToShort(sensors[18], sensors[19]);
                int GZ = (int) bytesToShort(sensors[20], sensors[21]);
                int timestamp = (int) bytesToShort(sensors[22], sensors[23]);
            }
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Funcao que interpreta e executa efetivamente um comando.
     *
     * @param command O comando
     * @return true caso haja sucesso na execução, false caso contrário.
     */
    public boolean runCommand(String command) {
        //Separa a string por espaços
        String[] split = command.split(" ");
//        byte[] sensors2;

        try {
            /*
             byte[] sensors = command.getBytes("ISO-8859-1");
             if (sensors == null) {
             //Instrução adicionada apenas para criar breakpoints
             System.out.println();
             //                assert true;
             } else {
             if (sensors[0] == SENSORS) {
             //                byte[] sensors = command.getBytes("ISO-8859-1");
             int encoder_esq = (int) bytesToShort(sensors[1], sensors[2]) & 0x0000ffff;
             int encoder_dir = (int) bytesToShort(sensors[3], sensors[4]) & 0x0000ffff;
             int[] IR = {(int) bytesToShort((byte) 0, sensors[5]),
             (int) bytesToShort((byte) 0, sensors[6]),
             (int) bytesToShort((byte) 0, sensors[7]),
             (int) bytesToShort((byte) 0, sensors[8]),
             (int) bytesToShort((byte) 0, sensors[9])};
             int AX = (int) bytesToShort(sensors[10], sensors[11]) & 0x0000ffff;
             int AY = (int) bytesToShort(sensors[12], sensors[13]) & 0x0000ffff;
             int AZ = (int) bytesToShort(sensors[14], sensors[15]) & 0x0000ffff;
             int GX = (int) bytesToShort(sensors[16], sensors[17]) & 0x0000ffff;
             int GY = (int) bytesToShort(sensors[18], sensors[19]) & 0x0000ffff;
             int GZ = (int) bytesToShort(sensors[20], sensors[21]) & 0x0000ffff;
             long timestamp = bytesToLong(sensors[22], sensors[23], sensors[24], sensors[25],
             sensors[26], sensors[27], sensors[28], sensors[29]);
             AmostraSensores amostra = new AmostraSensores(encoder_esq, encoder_dir, AX, AY, AZ, GX, GY, GZ, IR, timestamp);
             if (DEBUG_RECORD_SAMPLES) {
             try {
             debugBufferdOutputStream.write(amostra.toString().getBytes("UTF-8"));
             debugBufferdOutputStream.write("\n".getBytes("UTF-8"));
             debugBufferdOutputStream.flush();
             } catch (IOException ex) {
             System.out.printf("Erro ao escrever no arquivo: %s. (%s)\n", ex.getMessage());
             //                        Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
             }
             }
             gerenciadorSensores.novaLeituraSensores(amostra);
             return true;
             }
             }
             * */
            if (split[0].equals("S")) { //SENSORS (amostra)
                int encoder_esq = Integer.parseInt(split[1]);
                int encoder_dir = Integer.parseInt(split[2]);
                int[] IR = {Integer.parseInt(split[3]),
                            Integer.parseInt(split[4]),
                            Integer.parseInt(split[5]),
                            Integer.parseInt(split[6]),
                            Integer.parseInt(split[7])};
                int AX = Integer.parseInt(split[8]);
                int AY = Integer.parseInt(split[9]);
                int AZ = Integer.parseInt(split[10]);
                int GX = Integer.parseInt(split[11]);
                int GY = Integer.parseInt(split[12]);
                int GZ = Integer.parseInt(split[13]);
                long unixTimestamp = Long.parseLong(split[14]);
                AmostraSensores amostra = new AmostraSensores(encoder_esq, encoder_dir, IR, AX, AY, AZ, GX, GY, GZ, unixTimestamp);
                if (DEBUG_RECORD_SAMPLES) {
                    try {
                        debugBufferdOutputStream.write(amostra.toString().getBytes("UTF-8"));
                        debugBufferdOutputStream.write("\n".getBytes("UTF-8"));
                        debugBufferdOutputStream.flush();
                    } catch (IOException ex) {
                        System.out.printf("Erro ao escrever no arquivo: %s. (%s)\n", ex.getMessage());
//                        Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                gerenciadorSensores.novaLeituraSensores(amostra);
                return true;
            } else if (command.equals("BELLATOR HANDSHAKE REPLY")) { //BELLATOR HANDSHAKE REPLY
                connector.handShakeReplyReceived();
                return true;
            } else if (command.equals(
                    "SERVER FULL")) { //SERVER FULL 
                connector.serverFull();
                return true;
            } else if (split[0].equals(
                    "KEEPALIVE")) { //KEEPALIVE
                connector.keepAliveReceived();
                return true;
            } else if (split[0].equals(
                    "ECHO")) { //ECHO
                if (split[1].equals("REQUEST")) {
                    // Responde ao echo request
                    connector.sendMessageWithPriority("ECHO REPLY", true);
                    return true;
                } else if (split[1].equals("REPLY")) {
                    connector.echoReplyReceived();
                    return true;
                }
            } else if (split[0].equals("SENSORS")) { //SENSORS
                if (split[1].equals("SAMPLE")) {
//                    float aceleracao = Float.parseFloat(split[2]);
//                    float aceleracao_angular = Float.parseFloat(split[3]);
//                    int num_IR_recebido = split.length - 5;
//                    float[] dist_IR = new float[num_IR_recebido];
//                    for (int i = 0, j = 4; j <= split.length - 2; i++, j++) {
//                        dist_IR[i] = Float.parseFloat(split[j]);
//                    }
//                    long timestamp = Long.parseLong(split[split.length - 1]);
////                    System.out.printf("[TR_ServerCommandInterpreter] SENSORS SAMPLE %f %f ... %d\n", aceleracao, aceleracao_angular, timestamp);
////                    controleSensores.novaLeituraSensores(aceleracao, aceleracao_angular, dist_IR, timestamp);
                    return true;
                } else if (split[1].equals("STATUS")) {
                    if (split[2].equals("REPLY")) {
                        if (split[3].equals("false")) {
                            gerenciadorSensores.setSensorSampleStatus(GerenciadorSensores.SAMPLE_STOPPED);
                            return true;
                        } else if (split[3].equals("true")) {
                            gerenciadorSensores.setSensorSampleStatus(GerenciadorSensores.SAMPLE_STARTED);
                            return true;
                        }
                    }
                }
            } else if (split[0].equals(
                    "WEBCAM")) { //WEBCAM
//                if (split[1].equals("SAMPLE")) {
////                    //Decodifica a imagem
//////                    String base64Image = split[1].replace('\0', '\n');
//////                    byte[] imgBytes = new BASE64Decoder().decodeBuffer(base64Image);
//                    byte[] imgBytes = Base64new.decode(split[2]);
//                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
////                    ImageIO.write(img, "jpg", new File("/home/stefan/oi.jpg"));
//                    controleCamera.novaImagemCamera(img, split[2].length());
//                    return true;
//                } 
                if (split[1].equals("STATUS")) {
                    if (split[2].equals("REPLY")) {
                        String webcam_name = "";
                        //Lê todo o final do comando para pegar o parâmetro webcam_name.
                        for (int i = 7; i < split.length; i++) {
                            webcam_name += split[i];
                            webcam_name += " ";
                        }
                        controleCamera.setWebcam_name(webcam_name);
                        boolean sampling_enabled = Boolean.parseBoolean(split[3]);
                        boolean stream_available = Boolean.parseBoolean(split[4]);
                        int stream_port = Integer.parseInt(split[5]);
                        boolean webcam_available = Boolean.parseBoolean(split[6]);
                        controleCamera.setSampling_enabled(sampling_enabled);
                        controleCamera.setWebcam_available(webcam_available);
                        controleCamera.setStream_available(stream_available);
                        controleCamera.setStream_port(stream_port);
                        return true;
                    }
                }
            } else if (split[0].equals(
                    "DISCONNECT")) { //DISCONNECT
                connector.closeConnection();
                return true;
            }
        } catch (ArrayIndexOutOfBoundsException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] ERRO: Comando com número inválido de parâmetros! \"%s\" (%s) (%s)\n", command, ex.getMessage(), command.length());
//            Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (NumberFormatException ex) {
            System.out.printf("[TR_ServerCommandInterpreter] ERRO: Comando com formato errado de parâmetros! \"%s\" (%s)\n", command, ex.getMessage());
//            Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        return false;
    }

    /**
     * Requisita a finalização da Thread.
     * A finalização é feita amigavelmente, ou seja, aguarda-se que o loop principal finalize.
     */
    public synchronized void terminate() {
        run = false;
        this.notifyAll();
    }

    public void setConnector(ClientConnector Connector) {
        this.connector = Connector;
    }
}
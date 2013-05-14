package dados;

import comunicacao.ClientMessageProcessor;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.core.PApplet;
import processing.core.PVector;
import robo.ServerMessageProcessor;
import visual.Ponto;

/**
 * Classe usada para atualizar a posicao do robo e pontos dos obstaculos de
 * acordo com as leituras dos sensores (acelerômetro, giroscópio, Infra-Vermelho
 * (IR)).
 *
 * @author stefan
 */
public class GerenciadorSensores extends Thread implements MyChangeListener {

    /**
     * Estados de amostragem
     */
    public static final int SAMPLE_STOPPED = 1,
            SAMPLE_CHANGING = 2,
            SAMPLE_STARTED = 3;
    //Estado atual da amostragem
    private int sensorSampleStatus;
    private Robo robo;
    //Robo usado para gravar os deslocamentos obtidos com o acelerometro e giroscópio, para comparação de erros.
    private Robo robo_aux;
    private Obstaculos obstaculos;
    private Kalman[] kalman_IR;
    //Gravação de amostras habilitada ou não
    private boolean recordEnabled = false;
    //Gravação de amostras foi interrompida e continuada posteriormente?
//    private boolean recordInterruptedAndResumed = false;
//    private int numAmostrasAposInicioGravacao = 0;
    //Contadores do total de amostras.
    private int leituras_gravadas = 0, leituras_descartadas = 0;
    //Controle de taxas de amostragem
    ContadorAmostragem amostragemRobo; //Taxa de amostragem efetiva no robô
    ContadorAmostragemTempoReal amostragemEstacaoBase; //Taxa de recebimento de amostras na estação base
    //Array que armazena a fila de comandos
    private ArrayList<AmostraSensores> sampleList = new ArrayList<AmostraSensores>();
    //Indica se a thread deve rodar ou não
    private boolean run = true;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners;
    //Indicação se esta classe (this) foi adicionada como listener no contador de amostragem de tempo real
//    private boolean listener_added = false;
    private float ultimaVelocidadeEncoders = 0;
//    private float ultimaVelocidadeAngularEncoders = 0;
    private BufferedOutputStream streamArquivoLeituras_plotagem;
//    private ArrayList<AmostraSensores> historicoAmostras = new ArrayList<AmostraSensores>();
    private boolean gravarLeiturasSensores = false;
    private String nomeArquivoGravacaoLeituras = "";
    private BufferedOutputStream streamArquivoLeituras;
    private BufferedOutputStream streamArquivoLeituras_valor_real;
    private final Object lockStreamArquivoLeituras;
    private VetorMediaMovelFloat[] media_movel_IR;
    private VetorMediaMovelFloat media_movel_acel;
    private VetorMediaMovelFloat media_movel_giro;
    private VetorMediaMovelFloat media_movel_vel_angular_encoders;
    private VetorMediaMovelFloat media_movel_acel_encoders;
    int num_periodos_media_acel = 5;
    int num_periodos_media_giro = 5;
//    private int distIR_leituras[][];//Vetor com dados das ultimas N leituras de cada sensor IR. Usado para calcular a média móvel.
//    private int distIR_leituras_next = 0;//Proximo elemento do vetor distIR_leituras.
    private float acel_zero; //"Tara" da aceleração do eixo Y.
    private float giro_zero;
    private boolean kalman_IR_enabled = false;
    private float kalman_R = 10; //Fator R (ruído) do filtro de kalman dos sensores IR.
    private float kalman_initial_X = 500;
    private boolean media_IR_enabled = false;
    private int num_periodos_media_IR = 4; //Numero de periodos da media movel
    private int limite_diferenca_kalman_media = 500;
    private float limite_diferenca_acel_encoders_acelerometro = 100f;
    private float limite_diferenca_vel_angular_encoders_giro = 0.0025f;

    /**
     *
     * @param robo O robô a ser atualizado com as leituras dos sensores.
     * @param obstaculos O objeto da classe Obstaculos a ser atualizado com as
     * leituras dos sensores.
     */
    public GerenciadorSensores(Robo robo, Obstaculos obstaculos, Robo robo_aux) {
        this.robo = robo;
        this.obstaculos = obstaculos;
        this.robo_aux = robo_aux;
        //Inicializa o filtro de kalman de cada sensor IR
        this.kalman_IR = new Kalman[robo.getNumSensoresIR()];
        for (int i = 0; i < kalman_IR.length; i++) {
            kalman_IR[i] = new Kalman(kalman_R); //Inicializa os filtos de Kalman dos sensores IR
            kalman_IR[i].setX(kalman_initial_X); //Inicializa o filtro com um valor padrão de distância
        }
        media_movel_IR = new VetorMediaMovelFloat[robo.getNumSensoresIR()];
        for (int i = 0; i < media_movel_IR.length; i++) {
            media_movel_IR[i] = new VetorMediaMovelFloat(num_periodos_media_IR);
        }
        media_movel_acel = new VetorMediaMovelFloat(num_periodos_media_acel);
        media_movel_giro = new VetorMediaMovelFloat(num_periodos_media_giro);
        media_movel_acel_encoders= new  VetorMediaMovelFloat(5);
        media_movel_vel_angular_encoders = new VetorMediaMovelFloat(5);
//        distIR_leituras = new int[robo.getNumSensoresIR()][num_periodos_media_IR];
//        distIR_leituras_next = 0;
//        for (int i = 0; i < distIR_leituras.length; i++) {
//            for (int j = 0; j < distIR_leituras[i].length; j++) {
//                distIR_leituras[i][j] = 0;
//            }
//        }
//        instant_time_window_start = System.currentTimeMillis();
        amostragemRobo = new ContadorAmostragem();
        amostragemEstacaoBase = new ContadorAmostragemTempoReal();
        amostragemEstacaoBase.startUpdateTimer();
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        sensorSampleStatus = SAMPLE_STOPPED;
        this.lockStreamArquivoLeituras = new Object();
    }

    /**
     * Método continuamente executado pela thread para checar se há amostras na fila de espera e para processá-las.
     */
    @Override
    public void run() {
        run = true;
        AmostraSensores amostra;
        int num_elementos = 0;
        while (run) {
            synchronized (this) {
                num_elementos = sampleList.size();
            }
            //Enquanto o vetor tiver elementos....
            while (num_elementos > 0) {
                synchronized (this) {
                    amostra = sampleList.get(0);
                    try {
                        //Processa a amostra da posicao 0...
                        processaAmostraSensores(amostra);
                    } catch (NumIRException ex) {
                        System.out.printf("[GerenciadorSensores] %s \"%s\"\n", ex.getMessage(), amostra);
                    } catch (TimestampException ex) {
                        System.out.printf("[GerenciadorSensores] %s \"%s\"\n", ex.getMessage(), amostra);
                    }

                    //Faz a fila andar....
                    sampleList.remove(0);
                    num_elementos = sampleList.size();
                }
            }
            synchronized (this) {
                while (sampleList.isEmpty() && run) { //Enquanto a fila estiver vazia, espera até que hajam elementos.
                    try {
                        this.wait();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(GerenciadorSensores.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Recebe nova amostra dos sensores, inserindo-a na fila de amostras se a
     * gravação do mapa estiver habilitada. 
     * Atualiza também os valores de taxa de amostras por segundo.
     *
     * @param amostra A nova amostra dos sensores.
     * @throws Exception Caso timestamp seja menor que o último timestamp
     * recebido OU o número de sensores no vetor distIR diferir do número de
     * sensores presentes no robô.
     */
    public void novaLeituraSensores(AmostraSensores amostra) {
        synchronized (this) {

            //Atualiza os contadores de amostras/egundo
            amostragemRobo.novaAmostra(amostra.unixTimestamp);
            amostragemEstacaoBase.novaAmostra();

            if (!recordEnabled) {
                leituras_descartadas++;
            } else { //Se a gravação no mapa estiver habilitada...
                leituras_gravadas++;
                //Adiciona a amostra à fila de amostras a serem processadas
                sampleList.add(amostra);
                this.notifyAll();
            }
        }
        fireChangeEvent();
    }

    /**
     * Retorna o deslocamento linear do centro de movimento do robô a partir do deslocamento de cada roda.
     *
     * @param dist_encoder_esq Deslocamento da roda esquerda em milímetros.
     * @param dist_encoder_dir Deslocamento da roda direita em milímetros.
     * @return Deslocamento do centro de movimento do robô (ponto médio entre as rodas), em milímetros.
     */
    public static int calculaDeslocamentoEncoders(int dist_encoder_esq, int dist_encoder_dir) {
        return Math.round(((float) dist_encoder_esq + (float) dist_encoder_dir) / 2f);
    }

    /**
     * Retorna o deslocamento angular do centro de movimento do robô a partir do deslocamento de cada roda.
     *
     * @param dist_encoder_esq Deslocamento da roda esquerda em milímetros.
     * @param dist_encoder_dir Deslocamento da roda direita em milímetros.
     * @param dist_entre_rodas Distância entre as duas rodas, em milímetros.
     * @return Deslocamento angular do centro de movimento do robô (ponto médio entre as rodas), em radianos.
     */
    public static float calculaDeslocamentoAngularEncoders(int dist_encoder_esq, int dist_encoder_dir, int dist_entre_rodas) {
        int h = dist_entre_rodas / 2;
        //Caso especial 1: os dois deslocamentos são iguais, o raio tende a infinito e a aceleração angular tende a 0.
        if (dist_encoder_esq == dist_encoder_dir) {
            return 0;
        }
        //Caso especial 2: um deslocamento é o inverso do outro, ou seja, o robô rotaciona em torno do seu centro de movimento. 
        //A aceleracao angular é indeterminada usando-se a fórmula geral, mas pode ser calculada pela fórmula do MCU.
        if (dist_encoder_esq == -dist_encoder_dir) {
            return dist_encoder_esq / h;
        }
        //Raio do movimento circular
        float R = (float) h * ((float) dist_encoder_esq + (float) dist_encoder_dir) / ((float) dist_encoder_esq - (float) dist_encoder_dir);
        //Deslocamento do robo
        float deslocamento_encoders = calculaDeslocamentoEncoders(dist_encoder_esq, dist_encoder_dir);
        //Deslocamento angular
        return deslocamento_encoders / R;
    }

    /**
     * Muda a posicao do robô.
     *
     * @param novaPosicao
     */
    public synchronized void mudaPosicaoRobo(Ponto novaPosicao, float novoAngulo) {
        // Primeiramente, esvazia a lista de amostras a serem processadas.
        processaFilaInteiraAmostras();
        // Depois, moda a posicao do robo
        robo.novaTrilha(novaPosicao, novoAngulo);
        robo_aux.novaTrilha(novaPosicao, novoAngulo);
        fireChangeEvent();
    }

    public synchronized void limpaFilaAmostras() {
        sampleList.clear();
    }

    /**
     * Processa todas as amostras que estajam na lista de espera.
     */
    public synchronized void processaFilaInteiraAmostras() {
        int num_elementos;
        num_elementos = sampleList.size();
        //Enquanto o vetor tiver elementos....
        while (num_elementos > 0) {
            AmostraSensores amostra = sampleList.get(0);
            try {
                //Processa a amostra da posicao 0...
                processaAmostraSensores(amostra);
            } catch (NumIRException ex) {
                System.out.printf("[GerenciadorSensores] %s \"%s\"\n", ex.getMessage(), amostra);
            } catch (TimestampException ex) {
                System.out.printf("[GerenciadorSensores] %s \"%s\"\n", ex.getMessage(), amostra);
            }

            //Faz a fila andar....
            sampleList.remove(0);
            num_elementos = sampleList.size();
        }
    }

    /**
     * Processa nova amostra dos sensores. A partir dos dados, atualiza a posição
     * do robô e insere os obstáculos que forem detectados.
     *
     * @param amostra A nova amostra dos sensores.
     * @throws Exception Caso timestamp seja menor que o último timestamp
     * recebido OU o número de sensores no vetor distIR diferir do número de
     * sensores presentes no robô.
     */
    public synchronized void processaAmostraSensores(AmostraSensores amostra) throws NumIRException, TimestampException {
        int dist_encoder_esq = amostra.transformedEncoderEsq();
        int dist_encoder_dir = amostra.transformedEncoderDir();
        float acel_acelerometro = -amostra.transformedAY();
        float vel_angular_giro = -amostra.transformedGZ();

        if (Math.abs(dist_encoder_dir) > 120 || Math.abs(dist_encoder_esq) > 120) {
            System.out.printf("[GerenciadorSensores] Distância muito grande\n");
            return;
        }

        //
        // Efetua interpretação das leituras do acelerômetro e giroscópio (levando em conta a timestamp).
        //
        //Nova posicao do robo depois de efetuar os calculos 
        PosInfo novaPosicao;
        //Se o robo tiver apenas uma posicao armazenada, com timestamp 0 significa que ele acabou de ser inicializado.
        if (robo.getNumPosicoesTrilhaAtual() == 1 && robo.getUltimaPosicaoTrilhaAtual().getTimestamp() == 0) {
            acel_zero = acel_acelerometro;
            giro_zero = vel_angular_giro;
            //Configura os valores iniciais do filtro de Kalman
            int[] transformed_IR = amostra.transformedIR();
            for (int i = 0; i < kalman_IR.length; i++) {
                kalman_IR[i].setX(transformed_IR[i]);
            }
            //Apenas muda a timestamp para a hora atual.
            robo.getUltimaPosicaoTrilhaAtual().setTimestamp(amostra.unixTimestamp);
            robo_aux.getUltimaPosicaoTrilhaAtual().setTimestamp(amostra.unixTimestamp);
            novaPosicao = robo.getUltimaPosicaoTrilhaAtual();
            //Calcula o deslocamento no centro de movimento a partir dos deslocamentos de cada roda.
            //É uma média simples das duas medidas, uma vez que o centro de movimento é o ponto médio entre as duas rodas.
        } else { //Adiciona uma nova posição
            media_movel_acel.insereValor(acel_acelerometro - acel_zero);
            media_movel_giro.insereValor(vel_angular_giro - giro_zero);
            float media_acel_acelerometro = media_movel_acel.getMedia();
            float media_vel_angular_giro = media_movel_giro.getMedia();
            PosInfo ultimaPosicao = robo.getUltimaPosicaoTrilhaAtual(); //É a ultima posicao do robô.
            PosInfo ultimaPosicao_aux = robo_aux.getUltimaPosicaoTrilhaAtual();

            if (amostra.unixTimestamp < ultimaPosicao.getTimestamp()) {
                throw new TimestampException(amostra.unixTimestamp, ultimaPosicao.getTimestamp(),
                                             String.format("O timestamp (%d) é menor do que o da última posição (%d).", amostra.unixTimestamp, ultimaPosicao.getTimestamp()));
            }

//            if (recordInterruptedAndResumed) { //Caso a gravação tenha sido interrompida e agora continuada...
//                //Copia a última posição do robô, mas com o timestamp atual.
//                //Isso indica que o robô ficou parado por todo o tempo.
//                PosInfo novaPos = ultimaPosicao.copy();
//                novaPos.setTimestamp(amostra.unixTimestamp);
//                robo.addPosicao(novaPos);
////                robo.setVelocidadeAtual(0);
//                numAmostrasAposInicioGravacao++;
//                if (numAmostrasAposInicioGravacao >= 2) {
//                    recordInterruptedAndResumed = false;
//                    numAmostrasAposInicioGravacao = 0;
//                }
//                return; //Não é necessário fazer os cálculos nessa etapa
//            }
            Ponto ultimoPonto = ultimaPosicao.getPonto();
            Ponto ultimoPonto_aux = ultimaPosicao_aux.getPonto();
            float ultimoAngulo = ultimaPosicao.getAngulo();
            float ultimoAngulo_aux = ultimaPosicao_aux.getAngulo();
            long difftime = amostra.unixTimestamp - ultimaPosicao.getTimestamp(); //(ms) Diferença de tempo entre a última e penútlima timestamps. 
            if (difftime == 0) return;

            //
            // Determinar aceleração linear e velocidade angular medida pelos encoders
            //
            float acelEncoders;//, acelAngularEncoders;
            int novoDeslocamentoEncoders = calculaDeslocamentoEncoders(dist_encoder_esq, dist_encoder_dir);
            float novoDeslocamentoAngularEncoders = calculaDeslocamentoAngularEncoders(dist_encoder_esq, dist_encoder_dir, robo.getLargura());
            float novaVelocidadeEncoders = ((float) novoDeslocamentoEncoders) / (float) difftime; //(mm) / (ms) = (m/s)
            float novaVelocidadeAngularEncoders = ((float) novoDeslocamentoAngularEncoders) * 1000 / (float) difftime;//(rad) * 1000/(ms) = rad/s
            //Se o robo tiver apenas 2 posicoes, a aceleracao ainda nao pode ser calculada (apenas pode ser calculada com 3 pontos ou mais)

            acelEncoders = (novaVelocidadeEncoders - ultimaVelocidadeEncoders) * 1000 / (float) difftime;//(m/s) * 1000/ms = (m/s^2)
//            acelAngularEncoders = (novaVelocidadeAngularEncoders - ultimaVelocidadeAngularEncoders) * 1000 / (float) difftime; //(rad/s) * 1000/ms = (rad/s^2)
            //Atualiza os ultimos deslocamentos e velocidades
            ultimaVelocidadeEncoders = novaVelocidadeEncoders;
//            ultimaVelocidadeAngularEncoders = novaVelocidadeAngularEncoders;

            
            //Calcula médias móveis (usado apenas para testes se for desejado)
            media_movel_acel_encoders.insereValor(acelEncoders);
            media_movel_vel_angular_encoders.insereValor(novaVelocidadeAngularEncoders);
            float media_acel_encoders = media_movel_acel_encoders.getMedia();
            float media_vel_angular_encoders = media_movel_vel_angular_encoders.getMedia();


            //
            // Comparar com aceleração linear e velocidade angular do acelerômetro e giroscópio e calcular pesos de cada um (encoders vs. acel&gyro).
            //
            float peso_acel_encoders = 1;
            float peso_acelerometro = 0;
            if (PApplet.abs(media_acel_acelerometro - acelEncoders) > limite_diferenca_acel_encoders_acelerometro) {
                peso_acel_encoders = 0;
                peso_acelerometro = 1;
            }

            float peso_vel_angular_encoders = 1;
            float peso_giroscopio = 0;
            if (PApplet.abs(vel_angular_giro - novaVelocidadeAngularEncoders) > limite_diferenca_vel_angular_encoders_giro) {
                peso_vel_angular_encoders = 0;
                peso_giroscopio = 1;
            }

            //
            //  Calcular a aceleracao final
            //TODO: utilizar valores do acelerometro e giroscopio
            float aceleracao = peso_acelerometro * (media_acel_acelerometro) + peso_acel_encoders * acelEncoders;
            float velocidadeAngular = peso_giroscopio * (vel_angular_giro - giro_zero) + peso_vel_angular_encoders * novaVelocidadeAngularEncoders;

            //
            // Calcular velocidades linear e angular a partir da aceleracao calculada anteriormente
            //

            //Efetua as integrações numéricas para calcular as novas posições a partir das acelerações
            float velocidade = robo.getVelocidadeAtual() + (aceleracao * difftime) / 1000; //(m/s) + (m/s^2)*(ms)/1000 = (m/s)

            float velocidadeAcelerometro = robo_aux.getVelocidadeAtual() + ((acel_acelerometro - acel_zero) * difftime) / 1000;
//            robo.setVelocidadeAtual(velocidade);
//            float velocidadeAngular = robo.getVelocidadeAngularAtual() + aceleracaoAngular * difftime / 1000; // (rad/s)

            //
            // Calcular nova posição integrando-se a velocidade.
            //

            float deslocamento = velocidade * difftime; //(m/s) * (ms) = (mm) Deslocamento 
            float deslocamentoAngular = (velocidadeAngular * difftime / 1000) % PApplet.TWO_PI;

            float deslocamentoAcelerometro = velocidadeAcelerometro * difftime;
            float deslocamentoAngularGiroscopio = ((vel_angular_giro) * difftime / 1000) % PApplet.TWO_PI;

            //Calcula as novas posições x e y. O cálculo é feito levando-se em conta o ângulo da ultima posição (armazenada) do robo e a a nova velocidade (calculada) do robo.
            int newX = ultimoPonto.x() + PApplet.round(deslocamento * PApplet.cos(ultimoAngulo));
            if (newX < 0) {
                System.out.println();
            }
            int newY = ultimoPonto.y() + PApplet.round(deslocamento * PApplet.sin(ultimoAngulo));
            float novoAngulo = (ultimaPosicao.getAngulo() + deslocamentoAngular) % PApplet.TWO_PI; //(rad) angulo
            novaPosicao = new PosInfo(new Ponto(newX, newY), novoAngulo, amostra.unixTimestamp);
            robo.addPosicao(novaPosicao);
            robo.setVelocidadeAtual(velocidade);
            robo.setVelocidadeAngularAtual(velocidadeAngular);

            //Atualiza robo_aux
            int newX_aux = ultimoPonto_aux.x() + PApplet.round(deslocamentoAcelerometro * PApplet.cos(ultimoAngulo_aux));
            if (newX < 0) {
                System.out.println();
            }
            int newY_aux = ultimoPonto_aux.y() + PApplet.round(deslocamentoAcelerometro * PApplet.sin(ultimoAngulo_aux));
            float novoAngulo_aux = (ultimaPosicao_aux.getAngulo() + deslocamentoAngularGiroscopio) % PApplet.TWO_PI; //(rad) angulo
            PosInfo novaPosicao_aux = new PosInfo(new Ponto(newX_aux, newY_aux), novoAngulo_aux, amostra.unixTimestamp);
            robo_aux.addPosicao(novaPosicao_aux);
            robo_aux.setVelocidadeAtual(velocidadeAcelerometro);
            robo_aux.setVelocidadeAngularAtual(vel_angular_giro);



            //
            // Grava os dados nos arquivos .csv
            //
            synchronized (lockStreamArquivoLeituras) {
                if (gravarLeiturasSensores) {
                    if (streamArquivoLeituras != null) {
                        try {
                            //Grava as leituras dos sensores em um arquivo de texto
                            streamArquivoLeituras.write(amostra.toString().getBytes("UTF-8"));
                            streamArquivoLeituras.write("\n".getBytes("UTF-8"));
                            streamArquivoLeituras.flush();
                            streamArquivoLeituras_valor_real.write(amostra.transformedToString().getBytes("UTF-8"));
                            streamArquivoLeituras_valor_real.write("\n".getBytes("UTF-8"));
                            streamArquivoLeituras_valor_real.flush();
//                            float modPosicaoRobo = PApplet.sqrt(newX * newX + newY * newY);
//                            float modPosicaoRobo_aux = PApplet.sqrt(newX_aux * newX_aux + newY_aux * newY_aux);
                            //
                            // Calcula os erros (encoders vs. acel&giro)
                            //
                            float erro_deslocamento = 0;
                            if (novoDeslocamentoEncoders != deslocamentoAcelerometro) {
                                if (novoDeslocamentoEncoders == 0 && deslocamentoAcelerometro != 0) {
                                    erro_deslocamento = 1;
                                } else {
                                    erro_deslocamento = PApplet.abs((novoDeslocamentoEncoders - deslocamentoAcelerometro) / novoDeslocamentoEncoders);
                                }
                            }
                            erro_deslocamento = erro_deslocamento * 100;

                            float erro_desloc_angular = 0;
                            if (novoDeslocamentoAngularEncoders != deslocamentoAngularGiroscopio) {
                                if (novoDeslocamentoEncoders == 0 && deslocamentoAngularGiroscopio != 0) {
                                    erro_desloc_angular = 1;
                                } else {
                                    erro_desloc_angular = PApplet.abs((novoDeslocamentoAngularEncoders - deslocamentoAngularGiroscopio) / novoDeslocamentoAngularEncoders);
                                }
                            }
                            erro_desloc_angular = erro_desloc_angular * 100;

                            float erro_aceleracao = 0;
                            if (acelEncoders != acel_acelerometro - acel_zero) {
                                if (acelEncoders == 0 && acel_acelerometro - acel_zero != 0) {
                                    erro_aceleracao = 1;
                                } else {
                                    erro_aceleracao = PApplet.abs((acelEncoders - (acel_acelerometro - acel_zero)) / acelEncoders);
                                }
                            }
                            erro_aceleracao = erro_aceleracao * 100;

                            float erro_vel_angular = 0;
                            if (novaVelocidadeAngularEncoders != (vel_angular_giro)) {
                                if (novaVelocidadeAngularEncoders == 0 && (vel_angular_giro) != 0) {
                                    erro_vel_angular = 1;
                                } else {
                                    erro_vel_angular = PApplet.abs((novaVelocidadeAngularEncoders - (vel_angular_giro)) / velocidadeAngular);
                                }
                            }
                            erro_vel_angular = erro_vel_angular * 100;

                            float erro_posicao = PApplet.sqrt(PApplet.sq(newX - newX_aux) + PApplet.sq(newY - newY_aux));

                            float angulo = robo.getUltimaPosicaoTrilhaAtual().getAngulo();
                            if (angulo > PApplet.PI) {
                                angulo = PApplet.TWO_PI - angulo;
                            }
                            float angulo_aux = robo_aux.getUltimaPosicaoTrilhaAtual().getAngulo();
                            if (angulo_aux > PApplet.PI) {
                                angulo_aux = PApplet.TWO_PI - angulo_aux;
                            }

                            float erro_angulo = 0;
                            if (novoAngulo != novoAngulo_aux) {
                                if (novoAngulo == 0 && novoAngulo_aux != 0) {
                                    erro_angulo = 1;
                                } else {
                                    erro_angulo = PApplet.abs((novoAngulo - novoAngulo_aux) / novoAngulo);
                                }
                            }
                            erro_angulo = erro_angulo * 100;

                            //
                            // Imprime os valores para plotagem
                            //
                            String str = String.format("%d %d %d %f %f %f %f %f %f %f %f %f %f %d %d %d %d %f %f %f %f\n",
                                                       amostra.unixTimestamp,
                                                       PApplet.round(novoDeslocamentoEncoders),
                                                       PApplet.round(deslocamentoAcelerometro),
                                                       //Erro deslocamento (a condição novoDeslocamentoEncoders!=0 evita divisão por zero)
                                                       erro_deslocamento,
                                                       deslocamentoAngular,
                                                       deslocamentoAngularGiroscopio,
                                                       //Erro deslocamento angular
                                                       erro_desloc_angular,
                                                       acelEncoders,
                                                       acel_acelerometro - acel_zero,
                                                       //Erro Aceleracao (a condição acelEncoders!=0 evita divisão por zero)
                                                       erro_aceleracao,
                                                       novaVelocidadeAngularEncoders,
                                                       vel_angular_giro,
                                                       //Erro velocidade angular
                                                       erro_vel_angular,
                                                       newX, newY, newX_aux, newY_aux,
                                                       //Erro acumulado de posicao acelerometro e giroscopio
                                                       erro_posicao,
                                                       angulo, angulo_aux,
                                                       //Erro acumulado de angulo de orientação do robô (giroscópio)
                                                       erro_angulo);
                            streamArquivoLeituras_plotagem.write(str.getBytes("UTF-8"));
                            streamArquivoLeituras_plotagem.flush();
                        } catch (IOException ex) {
                            System.out.printf("Erro ao escrever no arquivo: %s. (%s)\n", ex.getMessage());
//                        Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        }


        //
        //Interpreta leituras dos sensores IR
        //
        int[] distIR = amostra.transformedIR();
        //Detecta diferenças entre o número de valores no vetor distIR e o número de sensores do robô.
        if ((robo.getNumSensoresIR() > 0 && distIR.length != robo.getNumSensoresIR())
            || (robo.getNumSensoresIR() == 0 && distIR != null)) {
            //Lança uma exceção caso haja diferença.
            throw new NumIRException(distIR.length, robo.getNumSensoresIR(),
                                     String.format("Número de sensores no vetor distIR (%d) difere do número de sensores presentes no robô (%d).",
                                                   distIR.length, robo.getNumSensoresIR()));
        }
        //Para cada sensor IR...
        for (int i = 0; i < distIR.length; i++) {
            if (i == 2) continue;
            SensorIR sensor = robo.getSensor(i);
            //Se a distância detectada estiver entre a mínima e a máxima (30cm a 150cm)...
            if (distIR[i] > sensor.getMin_detec() && distIR[i] < sensor.getMax_detec()) {
                float d = distIR[i];

                //Indere ruído aleatório
                //------------------
                // FIXME remover para usa na prática.
//                d = d  + (int) ((Math.random() - 0.5) * 400);
                //---------------------------
//                if (kalman_IR_enabled) { //Filtro de Kalman
//                    d = kalman_IR[i].nextX((float) distIR[i]); //Aplica o filtro de kalman na medida de distância
//                } else { //Media movel
//                    this.distIR_leituras[i][distIR_leituras_next] = (int) d;
//                    distIR_leituras_next = (distIR_leituras_next + 1) % distIR_leituras[i].length;
//                    int sum = 0;
//                    for (int j = 0; j < this.distIR_leituras[i].length; j++) {
//                        sum += this.distIR_leituras[i][j];
//                    }
//                    float media = (float) sum / (float) this.distIR_leituras[i].length;
//                    d = media;
//                }
                //--------------------------
                if (kalman_IR_enabled) { //Filtro de Kalman
                    float d_flitrado = kalman_IR[i].nextX(d); //Aplica o filtro de kalman na medida de distância
                    if (media_IR_enabled) { //Média móvel junto com filtro de Kalman
                        //Atualiza o próximo elemento do vetor do histórico de medidas, para cálculo da média móvel
                        if (media_movel_IR[i].getMedia() == 0) {
                            media_movel_IR[i].setValores(d);
                        }
                        media_movel_IR[i].insereValor(d);
                        float media = media_movel_IR[i].getMedia();

                        //Se houverem diferenças significativas entre a média móvel e o filtro de Kalman, muda o valor de X do filtro de Kalman e muda o valor filtrado para refletirem o valor da média.
                        if (PApplet.abs(media - d_flitrado) > limite_diferenca_kalman_media) {
                            kalman_IR[i].reset();
                            kalman_IR[i].setX(media);
                            d_flitrado = media;
                        }
                    }
                    //Salva o valor filtrado
                    d = d_flitrado;
                } else if (media_IR_enabled) { //Média móvel simples sem filtro de Kalman
                    //Atualiza o próximo elemento do vetor do histórico de medidas, para cálculo da média móvel
                    if (media_movel_IR[i].getMedia() == 0) {
                        media_movel_IR[i].setValores(d);
                    }
                    media_movel_IR[i].insereValor(d);
                    float media = media_movel_IR[i].getMedia();
                    //Salva o valor filtrado
                    d = media;
                }
                //----------------------------------
//                else {
//                    float d = distIR[i];
//                }
                PVector pc = novaPosicao.getPonto().getPVector(); //Ultima posição do centro do robô
                PVector p1 = sensor.getPosicaoNoRobo().getPVector(); //Posição do sensor no robô (relativa ao centro)
                p1.rotate(novaPosicao.getAngulo()); //Rotaciona v1 pelo ângulo do robo
                PVector p2 = PVector.fromAngle(novaPosicao.getAngulo() + sensor.getAngulo()); //v2 é o vetor que vai do sensor até o obstáculo.
                p2.setMag((float) d);
                //Soma v2 com v1 e Vc de modo a encontrar a posição do obstáculo.
                p2.add(p1);
                p2.add(pc);
                obstaculos.addPonto2D(new Ponto(PApplet.round(p2.x), PApplet.round(p2.y)));
            }
        }

    }

    protected final void initStreamArquivoLeituras() {
        synchronized (lockStreamArquivoLeituras) {
            long time = System.currentTimeMillis();
            File f = new File(String.format("testes/%d", time));
            f.mkdirs(); // Cria a pasta "testes/[timestamp]"
            nomeArquivoGravacaoLeituras = "";
            try {
                nomeArquivoGravacaoLeituras = String.format("%d", time);
                streamArquivoLeituras = new BufferedOutputStream(new FileOutputStream(String.format("testes/%d/amostras.csv", time)));
                streamArquivoLeituras.write("t encoder_esq encoder_dir IR1 IR2 IR3 IR4 IR5 Ax Ay Az Gx Gy Gz\n".getBytes("UTF-8"));
                streamArquivoLeituras_valor_real = new BufferedOutputStream(new FileOutputStream(String.format("testes/%d/amostras_valor_real.csv", time)));
                streamArquivoLeituras_valor_real.write("t encoder_esq(mm) encoder_dir IR1(mm) IR2 IR3 IR4 IR5 Ax(m/s^2) Ay Az Gx(rad/s) Gy Gz\n".getBytes("UTF-8"));
                streamArquivoLeituras_plotagem = new BufferedOutputStream(new FileOutputStream(String.format("testes/%d/amostras_plotagem.csv", time)));
                streamArquivoLeituras_plotagem.write("t(ms) deslocamento_encoders(mm) deslocamento_acelerometro_Y(mm) erro_deslocamento_acelerometro(%) desloc_angular_encoders(rad) desloc_angular_giroscopio_Z(rad) erro_desloc_angular_giroscopio(%) acel_encoders(m/s^2) acel_acelerometro_Y(m/s^2) erro_acel_acelerometro(%) vel_angular_encoders(rad/s) vel_angular_giroscopio_Z(rad/s) erro_vel_angular_giroscopio(%) posicao_encoders_X(mm) posicao_encoders_Y(mm) posicao_acelgiro_X(mm) posicao_acelgiro_Y(mm) erro_posicao_acelgiro(mm) angulo_encoders(rad) angulo_giroscopio(rad) erro_angulo_giro(%)\n".getBytes("UTF-8"));
                //TODO inserir erro_posicao_encoder_vs_acel_giro no arquivo de plotagem

            } catch (IOException ex) {
                System.out.printf("Erro ao abrir arquivo para gravação: %s. (%s)\n", nomeArquivoGravacaoLeituras, ex.getMessage());
            }
        }
    }

    protected final void closeStreamArquivoLeituras() {
        synchronized (lockStreamArquivoLeituras) {
            if (streamArquivoLeituras != null) {
                try {
                    streamArquivoLeituras.close();
                    streamArquivoLeituras_valor_real.close();
                    streamArquivoLeituras_plotagem.close();
                } catch (IOException ex) {
                    Logger.getLogger(ClientMessageProcessor.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    public final void startGravacaoLeiturasSensores() {
        synchronized (lockStreamArquivoLeituras) {
            if (!gravarLeiturasSensores) {
                gravarLeiturasSensores = true;
                initStreamArquivoLeituras();
            }
        }
    }

    public final void stopGravacaoLeiturasSensoresArquivo() {
        synchronized (lockStreamArquivoLeituras) {
            if (gravarLeiturasSensores) {
                gravarLeiturasSensores = false;
                closeStreamArquivoLeituras();
            }
        }
    }

    public String getNomeArquivoGravacaoLeituras() {
        return nomeArquivoGravacaoLeituras;
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

    public synchronized int getLeituras_gravadas() {
        return leituras_gravadas;
    }

    public synchronized int getLeituras_descartadas() {
        return leituras_descartadas;
    }

    public synchronized boolean isRecordEnabled() {
        return recordEnabled;
    }

    public void startRecording() {
        synchronized (this) {
            if (recordEnabled) return;
            recordEnabled = true;
            //Inicia uma nova trilha da mesma posição em que o robô está atualmente.
            mudaPosicaoRobo(robo.getUltimaPosicaoTrilhaAtual().getPonto(), robo.getUltimaPosicaoTrilhaAtual().getAngulo());
//            if (robo.getNumPosicoesTrilhaAtual() > 1) {
//                recordInterruptedAndResumed = true;
//            }
        }
        fireChangeEvent();
    }

    public void stopRecording() {
        synchronized (this) {
            if (!recordEnabled) return;
            recordEnabled = false;
            processaFilaInteiraAmostras();
//            recordInterruptedAndResumed = true;
        }
        fireChangeEvent();
    }

    public float getTaxaAmostragemRobo() {
        return amostragemRobo.getSample_rate();
    }

    public float getTaxaAmostragemEstacaoBase() {
        return amostragemEstacaoBase.getSample_rate();
    }

    public synchronized void setSensorSampleStatus(int sensorSampleStatus) {
        int oldStatus = this.sensorSampleStatus;

        this.sensorSampleStatus = sensorSampleStatus;
        if (oldStatus != this.sensorSampleStatus)
            fireChangeEvent();
    }

    public synchronized int getSensorSampleStatus() {
        return sensorSampleStatus;
    }

    public String getSensorSampleStatusString() {
        switch (sensorSampleStatus) {
            case SAMPLE_STOPPED:
                return "STOPPED";
            case SAMPLE_CHANGING:
                return "CHANGING";
            case SAMPLE_STARTED:
                return "STARTED";
        }
        return "";
    }

    public ContadorAmostragem getAmostragemRobo() {
        return amostragemRobo;
    }

    public ContadorAmostragemTempoReal getAmostragemEstacaoBase() {
        return amostragemEstacaoBase;
    }

    public synchronized boolean isKalman_IR_enabled() {
        return kalman_IR_enabled;
    }

    public synchronized void setKalman_IR_enabled(boolean kalman_IR_enabled) {
        this.kalman_IR_enabled = kalman_IR_enabled;
    }

    public synchronized boolean isMedia_IR_enabled() {
        return media_IR_enabled;
    }

    public synchronized void setMedia_IR_enabled(boolean media_IR_enabled) {
        this.media_IR_enabled = media_IR_enabled;
    }

    public synchronized float getKalman_R() {
        return kalman_R;
    }

    public synchronized void setKalman_R(float kalman_R) {
        this.kalman_R = kalman_R;
    }

    public synchronized int getNum_periodos_media_IR() {
        return num_periodos_media_IR;
    }

    public synchronized void setNum_periodos_media_IR(int num_periodos_media_IR) {
        this.num_periodos_media_IR = num_periodos_media_IR;
        for (int i = 0; i < media_movel_IR.length; i++) {
            media_movel_IR[i].setNumPeriodos(num_periodos_media_IR);
        }
    }

    public synchronized int getLimite_diferenca_kalman_media() {
        return limite_diferenca_kalman_media;
    }

    public synchronized void setLimite_diferenca_kalman_media(int limite_diferenca_kalman_media) {
        this.limite_diferenca_kalman_media = limite_diferenca_kalman_media;
    }

    public synchronized float getLimite_diferenca_acel_encoders_acelerometro() {
        return limite_diferenca_acel_encoders_acelerometro;
    }

    public synchronized void setLimite_diferenca_aceleracao_encoders_acelerometro(float limite_diferenca_acel_encoders_acelerometro) {
        this.limite_diferenca_acel_encoders_acelerometro = limite_diferenca_acel_encoders_acelerometro;
    }

    public synchronized float getLimite_diferenca_vel_angular_encoders_giro() {
        return limite_diferenca_vel_angular_encoders_giro;
    }

    public synchronized void setLimite_diferenca_vel_angular_encoders_giro(float limite_diferenca_vel_angular_encoders_giro) {
        this.limite_diferenca_vel_angular_encoders_giro = limite_diferenca_vel_angular_encoders_giro;
    }

    public synchronized void resetKalmanFilters() {
        for (int i = 0; i < kalman_IR.length; i++) {
            kalman_IR[i].reset();
            kalman_IR[i].setR(kalman_R);
            kalman_IR[i].setX(kalman_initial_X);
        }
    }

    public Robo getRobo() {
        return robo;
    }

    public Robo getRobo_aux() {
        return robo_aux;
    }

    public Obstaculos getObstaculos() {
        return obstaculos;
    }

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        fireChangeEvent();
    }
}

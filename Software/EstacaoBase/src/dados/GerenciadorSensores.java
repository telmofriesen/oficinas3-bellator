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
    private boolean kalman_IR_enabled = false;

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
            kalman_IR[i] = new Kalman(100);
        }
//        instant_time_window_start = System.currentTimeMillis();
        amostragemRobo = new ContadorAmostragem();
        amostragemEstacaoBase = new ContadorAmostragemTempoReal();
        amostragemEstacaoBase.startUpdateTimer();
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        sensorSampleStatus = SAMPLE_STOPPED;
        this.lockStreamArquivoLeituras = new Object();
    }

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
                        //Executa o comando da posicao 0...
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
     * Aceleração do centro de moviemnto (encoders)
     * Velocidade angular (encoders)
     * Aceleração (acelerômetro)
     * Velocidade angular (giroscópio)
     *
     * @return
     */
//    public String valuesToString(){
//        StringBuilder sb = new StringBuilder();
//        int i = 0;
//        
//        Ponto posicaoAnterior = robo.getPosicaoTrilhaAtual(i).getPonto();
//        float anguloAnterior = robo.getPosicaoTrilhaAtual(i).getAngulo();
//        long timestampAnterior = robo.getPosicaoTrilhaAtual(i).getTimestamp();
//        
//        for(i = 1; i<robo.getNumPosicoesTrilhaAtual(); i++){
//            Ponto posicao = robo.getPosicaoTrilhaAtual(i).getPonto();
//            float angulo = robo.getPosicaoTrilhaAtual(i).getAngulo();
//            long timestamp = robo.getPosicaoTrilhaAtual(i).getTimestamp();
//            float delta_theta = angulo - anguloAnterior;
//            
//            
//            
//            posicaoAnterior = posicao;
//            timestampAnterior = timestamp;
//        }
//    }
    /**
     * Recebe nova leitura dos sensores, armazenando as informações se a
     * gravação estiver habilitada. Atualiza os valores de taxa de amostras por segundo.
     *
     * @param aceleracao Aceleracao em m/s^2, medida a partir do acelerômetro
     * (posicionado no centro de movimento do robô).
     * @param aceleracao_angular Aceleração angular em rad/s^2. Os ângulos
     * começam em 0 e crescem no sentido HORÁRIO.
     * @param distIR Vetor com cada distância detectada pelos sensores IR (em milímetros). O
     * vetor deve obrigatoriamente conter um número de elementos igual ao número
     * de sensores IR presentes no robô. Caso contrário uma exceção é lançada.
     * @param timestamp Timestamp UNIX em milissegundos do horário da leitura.
     * @throws Exception Caso timestamp seja menor que o último timestamp
     * recebido OU o número de sensores no vetor distIR diferir do número de
     * sensores presentes no robô.
     */
    public void novaLeituraSensores(AmostraSensores amostra) {
        synchronized (this) {
//            if (!listener_added) {
//                amostragemEstacaoBase.addMyChangeListener(this);
//            }
            amostragemRobo.novaAmostra(amostra.unixTimestamp);
            amostragemEstacaoBase.novaAmostra();
//        amostragemEstacaoBase.novaAmostra(System.currentTimeMillis());
//        if (amostragemRobo.isSample_rate_changed() || amostragemEstacaoBase.isSample_rate_changed())
//            fireChangeEvent(); //Se houver mudanças no valor da taxa de amostragem, avisa os listeners.
            if (!recordEnabled) {
                leituras_descartadas++;
            } else {
//                _novaLeituraSensores(aceleracao, aceleracao_angular, distIR, timestamp);
                leituras_gravadas++;
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
        return Math.round((float) dist_encoder_esq + (float) dist_encoder_dir / 2f);
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
                //Executa o comando da posicao 0...
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
     * Recebe nova leitura dos sensores. À partir dos dados, atualiza a posição
     * do robô e insere os obstáculos que forem detectados.
     *
     * @param aceleracao Aceleracao em m/s^2, medida a partir do acelerômetro
     * (posicionado no centro de movimento do robô).
     * @param aceleracao_angular Aceleração angular em rad/s^2. Os ângulos
     * começam em 0 e crescem no sentido HORÁRIO.
     * @param distIR Vetor com cada distância detectada pelos sensores IR. O
     * vetor deve obrigatoriamente conter um número de elementos igual ao número
     * de sensores IR presentes no robô. Caso contrário uma exceção é lançada.
     * @param timestamp Timestamp UNIX em milissegundos do horário da leitura.
     * @throws Exception Caso timestamp seja menor que o último timestamp
     * recebido OU o número de sensores no vetor distIR diferir do número de
     * sensores presentes no robô.
     */
    public synchronized void processaAmostraSensores(AmostraSensores amostra) throws NumIRException, TimestampException {
        int dist_encoder_esq = amostra.transformedEncoderEsq();
        int dist_encoder_dir = amostra.transformedEncoderDir();
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
            //Apenas muda a timestamp para a hora atual.
            robo.getUltimaPosicaoTrilhaAtual().setTimestamp(amostra.unixTimestamp);
            robo_aux.getUltimaPosicaoTrilhaAtual().setTimestamp(amostra.unixTimestamp);
            novaPosicao = robo.getUltimaPosicaoTrilhaAtual();
            //Calcula o deslocamento no centro de movimento a partir dos deslocamentos de cada roda.
            //É uma média simples das duas medidas, uma vez que o centro de movimento é o ponto médio entre as duas rodas.
//            ultimoDeslocamentoEncoders = calculaDeslocamentoEncoders(dist_encoder_esq, dist_encoder_dir);
//            ultimoDeslocamentoAngularEncoders = calculaDeslocamentoAngularEncoders(dist_encoder_esq, dist_encoder_dir, robo.getLargura());
        } else { //Adiciona uma nova posição
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


            //
            // Comparar com aceleração linear e velocidade angular do acelerômetro e giroscópio e calcular pesos de cada um (encoders vs. acel&gyro).
            //

            //
            //  Calcular a aceleracao final
            //TODO: utilizar valores do acelerometro e giroscopio
            float aceleracao = acelEncoders;
            float velocidadeAngular = novaVelocidadeAngularEncoders;
//            float aceleracaoAngular = acelAngularEncoders;

//            String str = String.format("%d %f %f %f %f %f %f %f %f\n",
//                                       amostra.unixTimestamp,
//                                       acelEncoders,
//                                       novaVelocidadeAngularEncoders,
//                                       amostra.transformedAX(),
//                                       amostra.transformedAY(),
//                                       amostra.transformedAZ(),
//                                       amostra.transformedGX(),
//                                       amostra.transformedGY(),
//                                       -amostra.transformedGZ() //Inverte no eixo Z pois na convenção deste projeto os angulos crescem no sentido HORÀRIO
//                    );
//            try {
//                streamArquivoLeituras_plotagem.write(str.getBytes("UTF-8"));
//                streamArquivoLeituras_plotagem.flush();
//                //            streamArquivoLeiturasGrafico.write();
//            } catch (UnsupportedEncodingException ex) {
//                Logger.getLogger(GerenciadorSensores.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (IOException ex) {
//                Logger.getLogger(GerenciadorSensores.class.getName()).log(Level.SEVERE, null, ex);
//            }

            //
            // Calcular velocidades linear e angular a partir da aceleracao calculada anteriormente
            //

            //Efetua as integrações numéricas para calcular as novas posições a partir das acelerações
            float velocidade = robo.getVelocidadeAtual() + (aceleracao * difftime) / 1000; //(m/s) + (m/s^2)*(ms)/1000 = (m/s)

            float velocidadeAcelerometro = robo_aux.getVelocidadeAtual() + (amostra.transformedAY() * difftime) / 1000;
//            robo.setVelocidadeAtual(velocidade);
//            float velocidadeAngular = robo.getVelocidadeAngularAtual() + aceleracaoAngular * difftime / 1000; // (rad/s)

            //
            // Calcular nova posição integrando-se a velocidade.
            //

            float deslocamento = velocidade * difftime; //(m/s) * (ms) = (mm) Deslocamento 
            float deslocamentoAngular = (velocidadeAngular * difftime / 1000) % PApplet.TWO_PI;

            float deslocamentoAcelerometro = velocidadeAcelerometro * difftime;
            float deslocamentoAngularGiroscopio = (-amostra.transformedGZ() * difftime / 1000) % PApplet.TWO_PI;

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
            robo_aux.setVelocidadeAngularAtual(-amostra.transformedGZ());



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
                            if (acelEncoders != amostra.transformedAY()) {
                                if (acelEncoders == 0 && acelEncoders != 0) {
                                    erro_aceleracao = 1;
                                } else {
                                    erro_aceleracao = PApplet.abs((acelEncoders - amostra.transformedAY()) / acelEncoders);
                                }
                            }
                            erro_aceleracao = erro_aceleracao * 100;

                            float erro_vel_angular = 0;
                            if (velocidadeAngular != (-amostra.transformedGZ())) {
                                if (velocidadeAngular == 0 && (-amostra.transformedGZ()) != 0) {
                                    erro_vel_angular = 1;
                                } else {
                                    erro_vel_angular = PApplet.abs((velocidadeAngular - (-amostra.transformedGZ())) / velocidadeAngular);
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
                                                       amostra.transformedAY(),
                                                       //Erro Aceleracao (a condição acelEncoders!=0 evita divisão por zero)
                                                       erro_aceleracao,
                                                       velocidadeAngular,
                                                       -amostra.transformedGZ(),
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
        //Detecta diferenças entre o número de medidas no vetor distIR e o número de sensores do robô.
        if ((robo.getNumSensoresIR() > 0 && distIR.length != robo.getNumSensoresIR()) || (robo.getNumSensoresIR() == 0 && distIR != null)) {
            throw new NumIRException(distIR.length, robo.getNumSensoresIR(),
                                     String.format("Número de sensores no vetor distIR (%d) difere do número de sensores presentes no robô (%d).",
                                                   distIR.length, robo.getNumSensoresIR()));
        }
        for (int i = 0; i < distIR.length; i++) {
            SensorIR sensor = robo.getSensor(i);
            //Se a distância detectada estiver entre a mínima e a máxima (30cm a 150cm)...
            if (distIR[i] > sensor.getMin_detec() && distIR[i] < sensor.getMax_detec()) {
                float d = distIR[i];
                if (kalman_IR_enabled) {
                    d = kalman_IR[i].nextX((float) distIR[i]); //Aplica o filtro de kalman na medida de distância
                }
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

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        fireChangeEvent();
    }
}

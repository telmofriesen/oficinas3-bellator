package dados;

import events.MyChangeEvent;
import events.MyChangeListener;
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
    private Obstaculos obstaculos;
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
    private float ultimaVelocidadeAngularEncoders = 0;

    /**
     *
     * @param robo O robô a ser atualizado com as leituras dos sensores.
     * @param obstaculos O objeto da classe Obstaculos a ser atualizado com as
     * leituras dos sensores.
     */
    public GerenciadorSensores(Robo robo, Obstaculos obstaculos) {
        this.robo = robo;
        this.obstaculos = obstaculos;
//        instant_time_window_start = System.currentTimeMillis();
        amostragemRobo = new ContadorAmostragem();
        amostragemEstacaoBase = new ContadorAmostragemTempoReal();
        amostragemEstacaoBase.startUpdateTimer();
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        sensorSampleStatus = SAMPLE_STOPPED;
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
                        processaLeituraSensores(amostra);
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

    public static int calculaDeslocamentoEncoders(int dist_encoder_esq, int dist_encoder_dir) {
        return Math.round((float) dist_encoder_esq + (float) dist_encoder_dir) / 2;
    }

    public static float calculaDeslocamentoAngularEncoders(int dist_encoder_esq, int dist_encoder_dir, int dist_entre_rodas) {
        //Caso especial 1: os dois deslocamentos são iguais, o raio tende a infinito e a aceleração angular tende a 0.
        if (dist_encoder_esq == dist_encoder_dir) {
            return 0;
        }
        //Caso especial 2: um deslocamento é o inverso do outro, ou seja, o robô rotaciona em torno do seu centro de movimento. 
        //A aceleracao angular é indeterminada usando-se a fórmula geral.
        if (dist_encoder_esq == -dist_encoder_dir) {
            return 2 * dist_encoder_esq / dist_entre_rodas;
        }
        //Raio do movimento circular
        float R = (float) dist_entre_rodas * ((float) dist_encoder_esq + (float) dist_encoder_dir) / (2f * ((float) dist_encoder_esq - (float) dist_encoder_dir));
        //Deslocamento do robo
        float deslocamento_encoders = calculaDeslocamentoEncoders(dist_encoder_esq, dist_encoder_dir);
        //Deslocamento angular
        return deslocamento_encoders * PApplet.TWO_PI / R;
    }

    /**
     * Muda a posicao do robô.
     *
     * @param novaPosicao
     */
    public synchronized void mudaPosicaoRobo(Ponto novaPosicao, float novoAngulo) {
        // Primeiramente, esvazia a lista de amostras a serem processadas.
        processaListaInteiraAmostras();
        // Depois, moda a posicao do robo
        robo.novaTrilha(novaPosicao, novoAngulo);
        fireChangeEvent();
    }

    /**
     * Processa todas as amostras que estajam na lista.
     */
    public synchronized void processaListaInteiraAmostras() {
        int num_elementos;
        num_elementos = sampleList.size();
        //Enquanto o vetor tiver elementos....
        while (num_elementos > 0) {
            AmostraSensores amostra = sampleList.get(0);
            try {
                //Executa o comando da posicao 0...
                processaLeituraSensores(amostra);
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
    protected void processaLeituraSensores(AmostraSensores amostra) throws NumIRException, TimestampException {
        int dist_encoder_esq = amostra.transformedEncoderEsq();
        int dist_encoder_dir = amostra.transformedEncoderDir();

        //
        // Efetua interpretação das leituras do acelerômetro e giroscópio (levando em conta a timestamp).
        //
        //Nova posicao do robo depois de efetuar os calculos 
        PosInfo novaPosicao;
        //Se o robo tiver apenas uma posicao armazenada, com timestamp 0 significa que ele acabou de ser inicializado.
        if (robo.getNumPosicoesTrilhaAtual() == 1 && robo.getUltimaPosicaoTrilhaAtual().getTimestamp() == 0) {
            //Apenas muda a timestamp para a hora atual.
            robo.getUltimaPosicaoTrilhaAtual().setTimestamp(amostra.unixTimestamp);
            novaPosicao = robo.getUltimaPosicaoTrilhaAtual();
            //Calcula o deslocamento no centro de movimento a partir dos deslocamentos de cada roda.
            //É uma média simples das duas medidas, uma vez que o centro de movimento é o ponto médio entre as duas rodas.
//            ultimoDeslocamentoEncoders = calculaDeslocamentoEncoders(dist_encoder_esq, dist_encoder_dir);
//            ultimoDeslocamentoAngularEncoders = calculaDeslocamentoAngularEncoders(dist_encoder_esq, dist_encoder_dir, robo.getLargura());
        } else { //Adiciona uma nova posição
            PosInfo ultimaPosicao = robo.getUltimaPosicaoTrilhaAtual(); //É a ultima posicao do robô.

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
            float ultimoAngulo = ultimaPosicao.getAngulo();
            long difftime = amostra.unixTimestamp - ultimaPosicao.getTimestamp(); //(ms) Diferença de tempo entre a última e penútlima timestamps. 

            //
            // Determinar aceleração linear e angular medida pelos encoders
            //
            float acelEncoders, acelAngularEncoders;
            int novoDeslocamentoEncoders = calculaDeslocamentoEncoders(dist_encoder_esq, dist_encoder_dir);
            float novoDeslocamentoAngularEncoders = calculaDeslocamentoAngularEncoders(dist_encoder_esq, dist_encoder_dir, robo.getLargura());
            float novaVelocidadeEncoders = ((float) novoDeslocamentoEncoders) / (float) difftime; //(mm) / (ms) = (m/s)
            float novaVelocidadeAngularEncoders = ((float) novoDeslocamentoAngularEncoders) * 1000 / (float) difftime;//(rad) * 1000/(ms) = rad/s
            //Se o robo tiver apenas 2 posicoes, a aceleracao ainda nao pode ser calculada (apenas pode ser calculada com 3 pontos ou mais)

            acelEncoders = (novaVelocidadeEncoders - ultimaVelocidadeEncoders) * 1000 / (float) difftime;//(m/s) * 1000/ms = (m/s^2)
            acelAngularEncoders = (novaVelocidadeAngularEncoders - ultimaVelocidadeAngularEncoders) * 1000 / (float) difftime; //(rad/s) * 1000/ms = (rad/s^2)
            //Atualiza os ultimos deslocamentos e velocidades
            ultimaVelocidadeEncoders = novaVelocidadeEncoders;
            ultimaVelocidadeAngularEncoders = novaVelocidadeAngularEncoders;


            //
            // Comparar com aceleração linear e angular do acelerômetro e giroscópio e calcular pesos de cada aceleração (encoders vs. acel&gyro).
            //

            //
            //  Calcular a aceleracao final
            //TODO: utilizar valores do acelerometro e giroscopio
            float aceleracao = acelEncoders;
            float aceleracaoAngular = acelAngularEncoders;

            //
            // Calcular velocidades linear e angular a partir da aceleracao calculada anteriormente
            //

            //Efetua as integrações numéricas para calcular as novas posições a partir das acelerações
            float velocidade = robo.getVelocidadeAtual() + (aceleracao * difftime) / 1000; //(m/s) + (m/s^2)*(ms)/1000 = (m/s)
//            robo.setVelocidadeAtual(velocidade);
            float velocidadeAngular = robo.getVelocidadeAngularAtual() + aceleracaoAngular * difftime / 1000; // (rad/s)

            //
            // Calcular nova posição integrando-se a velocidade.
            //

            float deslocamento = velocidade * difftime; //(m/s) * (ms) = (mm) Deslocamento 

            //Calcula as novas posições x e y. O cálculo é feito levando-se em conta o ângulo da ultima posição (armazenada) do robo e a a nova velocidade (calculada) do robo.
            int newX = ultimoPonto.x() + PApplet.round(deslocamento * PApplet.cos(ultimoAngulo));
            int newY = ultimoPonto.y() + PApplet.round(deslocamento * PApplet.sin(ultimoAngulo));
            float novoAngulo = (ultimaPosicao.getAngulo() + velocidadeAngular * difftime / 1000) % PApplet.TWO_PI; //(rad) angulo
            novaPosicao = new PosInfo(new Ponto(newX, newY), novoAngulo, amostra.unixTimestamp);
            robo.addPosicao(novaPosicao);
            robo.setVelocidadeAtual(velocidade);
            robo.setVelocidadeAngularAtual(velocidadeAngular);
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
            if (distIR[i] > sensor.getMin_detec() && distIR[i] < sensor.getMax_detec()) {
                PVector Vc = novaPosicao.getPonto().getPVector(); //Ultima posição do centro do robô
                PVector v1 = sensor.getPosicaoNoRobo().getPVector(); //Posição do sensor no robô (relativa ao centro)
                v1.rotate(novaPosicao.getAngulo()); //Rotaciona v1 pelo ângulo do robo
                PVector v2 = PVector.fromAngle(novaPosicao.getAngulo() + sensor.getAngulo()); //v2 é o vetor que vai do sensor até o obstáculo.
                v2.setMag((float) distIR[i]);
                //Soma v2 com v1 e Vc de modo a encontrar a posição do obstáculo.
                v2.add(v1);
                v2.add(Vc);
                obstaculos.addPonto2D(new Ponto(PApplet.round(v2.x), PApplet.round(v2.y)));
            }
        }
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
            recordEnabled = false;
            processaListaInteiraAmostras();
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

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        fireChangeEvent();
    }
}

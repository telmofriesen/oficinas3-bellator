package dados;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;
import processing.core.PApplet;
import processing.core.PVector;
import visual.Ponto;

/**
 * Classe usada para atualizar a posicao do robo e pontos dos obstaculos de
 * acordo com as leituras dos sensores (acelerômetro, giroscópio, Infra-Vermelho
 * (IR)).
 *
 * @author stefan
 */
public class GerenciadorSensores_new implements MyChangeListener {

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
    private boolean recordInterruptedAndResumed = false;
    //Contadores do total de amostras.
    private int leituras_gravadas = 0, leituras_descartadas = 0;
    //Controle de taxas de amostragem
    ContadorAmostragem amostragemRobo; //Taxa de amostragem efetiva no robô
    ContadorAmostragemTempoReal amostragemEstacaoBase; //Taxa de recebimento de amostras na estação base
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners;
    //Indicação se esta classe (this) foi adicionada como listener no contador de amostragem de tempo real
//    private boolean listener_added = false;

    /**
     *
     * @param robo O robô a ser atualizado com as leituras dos sensores.
     * @param obstaculos O objeto da classe Obstaculos a ser atualizado com as
     * leituras dos sensores.
     */
    public GerenciadorSensores_new(Robo robo, Obstaculos obstaculos) {
        this.robo = robo;
        this.obstaculos = obstaculos;
//        instant_time_window_start = System.currentTimeMillis();
        amostragemRobo = new ContadorAmostragem();
        amostragemEstacaoBase = new ContadorAmostragemTempoReal();
        amostragemEstacaoBase.startUpdateTimer();
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
        sensorSampleStatus = SAMPLE_STOPPED;
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
    public void novaLeituraSensores(float aceleracao, float aceleracao_angular, float[] distIR, long timestamp)
            throws NumIRException, TimestampException {
        synchronized (this) {
//            if (!listener_added) {
//                amostragemEstacaoBase.addMyChangeListener(this);
//            }
            amostragemRobo.novaAmostra(timestamp);
            amostragemEstacaoBase.novaAmostra();
//        amostragemEstacaoBase.novaAmostra(System.currentTimeMillis());
//        if (amostragemRobo.isSample_rate_changed() || amostragemEstacaoBase.isSample_rate_changed())
//            fireChangeEvent(); //Se houver mudanças no valor da taxa de amostragem, avisa os listeners.
            if (!recordEnabled) {
                leituras_descartadas++;
            } else {
                _novaLeituraSensores(aceleracao, aceleracao_angular, distIR, timestamp);
                leituras_gravadas++;
            }
        }
        fireChangeEvent();
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
    protected void _novaLeituraSensores(float aceleracao, float aceleracao_angular, float[] distIR, long timestamp) throws NumIRException, TimestampException {
        //
        // Efetua interpretação das leituras do acelerômetro e giroscópio (levando em conta a timestamp).
        //
        PosInfo new_pos;
        float angulo = 0;
        //Se o robo tiver apenas uma posicao armazenada, com timestamp 0 significa que ele acabou de ser inicializado.
        if (robo.getNumPosicoes() == 1 && robo.getUltimaPosicao().getTimestamp() == 0) {
            //Apenas muda a timestamp para a hora atual.
            robo.getUltimaPosicao().setTimestamp(timestamp);
            new_pos = robo.getUltimaPosicao();
        } else { //Adiciona uma nova posição
            PosInfo penultPos = robo.getUltimaPosicao(); //É a penúltima posição pois será adicionada uma nova posição a seguir.

            if (timestamp < penultPos.getTimestamp()) {
                throw new TimestampException(timestamp, penultPos.getTimestamp(),
                                             String.format("O timestamp (%d) é menor do que o da última posição (%d).", timestamp, penultPos.getTimestamp()));
            }

            if (recordInterruptedAndResumed) { //Caso a gravação tenha sido interrompida e agora continuada...
                //Copia a última posição do robô, mas com o timestamp atual.
                //Isso indica que o robô ficou parado por todo o tempo.
                PosInfo newPos = penultPos.copy();
                newPos.setTimestamp(timestamp);
                robo.addPosicao(newPos);
                recordInterruptedAndResumed = false;
                return; //Não é necessário fazer os cálculos nessa etapa
            }

            Ponto lastXY = penultPos.getPonto();
            float penultAngulo = penultPos.getAngulo();
            long difftime = timestamp - penultPos.getTimestamp(); //(ms) Diferença de tempo entre a última e penútlima timestamps. 

            //Efetua as integrações numéricas para calcular as novas posições a partir das acelerações
            float velocidade = robo.getVelocidadeAtual() + (aceleracao * difftime) / 1000; // (m/s)
            robo.setVelocidadeAtual(velocidade);
            float deslocamento = velocidade * difftime; //(mm) Deslocamento 
            float velocidade_angular = robo.getVelocidadeAngularAtual() + aceleracao_angular * difftime / 1000; // (rad/s)
            robo.setVelocidadeAngularAtual(velocidade_angular);
            angulo = (penultPos.getAngulo() + velocidade_angular * difftime / 1000) % PApplet.TWO_PI; //(rad) angulo

            //Calcula as novas posições x e y. O cálculo é feito levando-se em conta o ângulo da PENÚLTIMA posição e a velocidade da ÙLTIMA posição.
            int new_X = lastXY.x() + PApplet.round(deslocamento * PApplet.cos(penultAngulo));
            int new_Y = lastXY.y() + PApplet.round(deslocamento * PApplet.sin(penultAngulo));
            new_pos = new PosInfo(new Ponto(new_X, new_Y), angulo, timestamp);
            robo.addPosicao(new_pos);
        }

        //
        //Interpreta leituras dos sensores IR
        //
        //Detecta diferenças entre o número de medidas no vetor distIR e o número de sensores do robô.
        if ((robo.getNumSensoresIR() > 0 && distIR.length != robo.getNumSensoresIR()) || (robo.getNumSensoresIR() == 0 && distIR != null)) {
            throw new NumIRException(distIR.length, robo.getNumSensoresIR(),
                                     String.format("Número de sensores no vetor distIR (%d) difere do número de sensores presentes no robô (%d).",
                                                   distIR.length, robo.getNumSensoresIR()));
        }
        for (int i = 0; i < distIR.length; i++) {
            SensorIR sensor = robo.getSensor(i);
            if (distIR[i] > sensor.getMin_detec() && distIR[i] < sensor.getMax_detec()) {
                PVector Vc = new_pos.getPonto().getPVector(); //Ultima posição do centro do robô
                PVector v1 = sensor.getPosicaoNoRobo().getPVector(); //Posição do sensor no robô (relativa ao centro)
                v1.rotate(angulo); //Rotaciona v1 pelo ângulo do robo
                PVector v2 = PVector.fromAngle(angulo + sensor.getAngulo()); //v2 é o vetor que vai do sensor até o obstáculo.
                v2.setMag(distIR[i]);
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

    public void startRecord() {
        synchronized (this) {
            recordEnabled = true;
            if (robo.getNumPosicoes() > 1) {
                recordInterruptedAndResumed = true;
            }
        }
        fireChangeEvent();
    }

    public void stopRecord() {
        synchronized (this) {
            recordEnabled = false;
            recordInterruptedAndResumed = true;
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

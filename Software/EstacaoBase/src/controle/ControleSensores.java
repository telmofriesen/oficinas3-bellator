package controle;

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
public class ControleSensores {

    private Robo robo;
    private Obstaculos obstaculos;

    /**
     * 
     * @param robo O robô a ser atualizado com as leituras dos sensores.
     * @param obstaculos O objeto da classe Obstaculos a ser atualizado com as leituras dos sensores.
     */
    public ControleSensores(Robo robo, Obstaculos obstaculos) {
        this.robo = robo;
        this.obstaculos = obstaculos;
    }

    /**
     * Recebe nova leitura dos sensores. À partir dos dados, atualiza a posição do robô e insere os obstáculos que forem detectados.
     *
     * @param velocidade Velocidade em m/s, medida a partir do acelerômetro (posicionado no centro de movimento do robô).
     * @param angulo Angulo em radianos relativo à primeira posição do robô. Os ângulos começam em 0 e crescem no sentido HORÁRIO.
     * @param distIR Vetor com cada distância detectada pelos sensores IR. O vetor deve obrigatoriamente conter um número de elementos igual ao número de sensores IR  presentes no robô. Caso contrário uma exceção é lançada.
     * @param timestamp Timestamp UNIX em milissegundos do horário da leitura.
     * @throws Exception Caso timestamp seja menor que o último timestamp recebido OU o número de sensores no vetor distIR diferir do número de sensores presentes no robô.
     */
    public void novaLeituraSensores(float velocidade, float angulo, float[] distIR, int timestamp) throws Exception {
        //
        // Efetua interpretação das leituras do acelerômetro e giroscópio (levando em conta a timestamp).
        //
        PosInfo new_pos;
        //Se o robo tiver apenas uma posicao armazenada, com timestamp 0 significa que ele acabou de ser inicializado.
        if (robo.getNumPosicoes() == 1 && robo.getUltimaPosicao().getTimestamp() == 0) {
            //Apenas muda a timestamp para a hora atual.
            robo.getUltimaPosicao().setTimestamp(timestamp);
            new_pos = robo.getUltimaPosicao();
        } else { //Adiciona uma nova posição
            PosInfo penultPos = robo.getUltimaPosicao(); //É a penúltima posição pois será adicionada uma nova posição a seguir.

            if (timestamp < penultPos.getTimestamp()) {
                throw new Exception(String.format("O timestamp (%d) é menor do que o da última posição (%d).", timestamp, penultPos.getTimestamp()));
            }

            Ponto lastXY = penultPos.getPonto();
            float penultAngulo = penultPos.getAngulo();
            int difftime = timestamp - penultPos.getTimestamp(); //Diferença de tempo entre a última e penútlima timestamps. (ms)
            float deslocamento = (velocidade) * ((float) difftime) / (float) 10; //Deslocamento (cm)

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
            throw new Exception(String.format("Número de sensores no vetor distIR (%d) difere do número de sensores presentes no robô (%d).",
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
}

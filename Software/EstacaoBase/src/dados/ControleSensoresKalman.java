package dados;

/**
 * Especialização da classe ControleSensores, que efetua filtragem das medidas
 * dos sensores pelo filtro de Kalman. Utiliza para isso a classe Kalman.
 *
 * @see Kalman
 * @author stefan
 */
public class ControleSensoresKalman extends ControleSensores {

    private float R_velocidade, R_angulo, R_sensoresIR;
    private Kalman kalman_vel, kalman_angulo, kalman_IR[];

    /**
     *
     * @param robo O robô a ser atualizado com as leituras dos sensores.
     * @param obstaculos O objeto da classe Obstaculos a ser atualizado com as
     * leituras dos sensores.
     * @param R_velocidade Amplitude esperada de ruidos na velocidade
     * @param R_angulo Amplitude esperada de rúido na leitura dos ângulos
     * @param R_sensoresIR Amplitude esperada de ruidos nas medidas dos sensores
     * IR
     */
    public ControleSensoresKalman(Robo robo, Obstaculos obstaculos, float R_velocidade, float R_angulo, float R_sensoresIR) {
        super(robo, obstaculos);
        this.R_velocidade = R_velocidade;
        this.R_angulo = R_angulo;
        this.R_sensoresIR = R_sensoresIR;
        kalman_vel = new Kalman(R_velocidade);
        kalman_angulo = new Kalman(R_angulo);
        this.kalman_IR = new Kalman[robo.getNumSensoresIR()];
        for (int i = 0; i < robo.getNumSensoresIR(); i++) {
            this.kalman_IR[i] = new Kalman(R_sensoresIR);
        }
    }

    @Override
    public void novaLeituraSensores(float velocidade, float angulo, float[] distIR, long timestamp) throws NumIRException, TimestampException {
        //
        //Aplica os filtros de Kalman a cada leitura
        //
        velocidade = kalman_vel.nextX(velocidade);
        if (Math.abs(angulo - kalman_angulo.getX()) > R_angulo*2) {
            kalman_angulo.setX(angulo);
        }
        //TODO testar melhor os parametros para filtragem dos angulos
//        angulo = kalman_angulo.nextX(angulo);
        
        //Filtra a distancia dos sensores IR por Kalman.
        for (int i = 0; i < distIR.length; i++) {
            if (Math.abs(distIR[i] - kalman_IR[i].getX()) > R_sensoresIR * 2) {
                //Se a diferença entre a última e penúltima medidas passar de um certo threshold, muda o valor de X para se adequar à última medida. 
                //Isso é útil no caso de gaps, quando o sensor mede uma parede a 30cm e depois uma a 100cm, por exemplo,
                //pois o filtro em geral demora para convergir para o valor real caso hajam variações muito grandes nas medições.
                kalman_IR[i].setX(distIR[i]);
                //kalman_IR[i] = new Kalman(R_sensoresIR);
            }
            distIR[i] = kalman_IR[i].nextX(distIR[i]);
        }
        //Passa os valores filtrados ao método pai
        super.novaLeituraSensores(velocidade, angulo, distIR, timestamp);
    }
}

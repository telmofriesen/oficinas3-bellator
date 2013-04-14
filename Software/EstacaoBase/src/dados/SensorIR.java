/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

import visual.Ponto;

/**
 * Classe que representa um sensor IR do robô.
 *
 * @author stefan
 */
public class SensorIR {
    //A posicao do sensor no robo (em relacao ao centro) em milímetros

    private Ponto posicaoRobo;
    //Angulo (radianos) do sensor (angulos crescem no sentido horário)
    private float angulo;
    //Distancias minima e maxima de detecção do sensor (especificadas no Datasheet)
    private int min_detec, max_detec;

    /**
     * IMPORTANTE: Para o posicionamento dos sensores no plano cartesiano, considera-se que o robô está com a frente voltada para a DIREITA.
     * O eixo X é positivo para a DIREITA e Y é positivo para BAIXO.
     *
     * @param posicaoRobo A posicao XY do sensor IR no robo (em milímetros), em relacao ao centro de movimento dele.
     * @param angulo Ângulo em que o sensor está posicionado. Zero grau significa que o sensor está orientado no sentido do eixo X. *** Os ângulos crescem no sentido HORÁRIO
     ***
     * @param min_detec Distância mínima de detecção do sensor (mm).
     * @param max_detec Distância máxima de detecção do sensor (mm).
     */
    public SensorIR(Ponto posicaoRobo, float angulo, int min_detec, int max_detec) {
        this.posicaoRobo = posicaoRobo;
        this.angulo = angulo;
        this.min_detec = min_detec;
        this.max_detec = max_detec;
    }

    public float getAngulo() {
        return angulo;
    }

    public void setAngulo(float angulo) {
        this.angulo = angulo;
    }

    public int getMax_detec() {
        return max_detec;
    }

    public void setMax_detec(int max_detec) {
        this.max_detec = max_detec;
    }

    public int getMin_detec() {
        return min_detec;
    }

    public void setMin_detec(int min_detec) {
        this.min_detec = min_detec;
    }

    public Ponto getPosicaoNoRobo() {
        return posicaoRobo;
    }

    public void setPosicaoRobo(Ponto posicaoRobo) {
        this.posicaoRobo = posicaoRobo;
    }

    /**
     * Retorna a distancia em milimetros a partir do valor lido da conversão A/D.
     * Ver: monografia do Bellator de 2012 na página 42.
     * @param x Valor da conversão A/D
     * @return Distância em milímetros.
     */
    public static int getDistFromByte(int x) {
        return (int) Math.round(10 * (3.6404e-7 * Math.pow(x, 4) - 2.4435e-4 * Math.pow(x, 3) + 6.0732e-2 * Math.pow(x, 2) - 6.8962 * x + 339.361));
    }
}

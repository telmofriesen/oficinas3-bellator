/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import visual.Ponto;

/**
 * Classe que contém informações de uma posição do robô.
 * @author stefan
 */
public class PosInfo {
    private Ponto ponto;
    private float angulo;
    private int timestamp;

    /**
     * 
     * @param ponto Ponto cartesiano da posição (milímetros)
     * @param angulo Ângulo do robô nessa posição (àngulo relativo à primeira posição dele)
     * @param timestamp Timestamp UNIX em milissegundos do horário em que o robô esteve nessa posição
     */
    public PosInfo(Ponto ponto, float angulo, int timestamp) {
        this.ponto = ponto;
        this.angulo = angulo;
        this.timestamp = timestamp;
    }

    public float getAngulo() {
        return angulo;
    }

    public Ponto getPonto() {
        return ponto;
    }

    public int getTimestamp() {
        return timestamp;
    }

    public void setPonto(Ponto ponto) {
        this.ponto = ponto;
    }

    public void setAngulo(float angulo) {
        this.angulo = angulo;
    }

    public void setTimestamp(int timestamp) {
        this.timestamp = timestamp;
    }
    
    
    
    
}

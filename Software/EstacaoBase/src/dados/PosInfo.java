/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

import visual.Ponto;

/**
 * Classe que contém informações de uma posição do robô.
 * @author stefan
 */
public class PosInfo {
    private Ponto ponto;
    private float angulo;
    private long timestamp;

    /**
     * 
     * @param ponto Ponto no plano cartesiano da posição em milímetros.
     * @param angulo Ângulo em radianos em que o robô está. O ângulo é zero quando o robô está virado, para a direita (eixo X positivo), e os ângulos aumentam no sentido HORÁRIO (atenção).
     * @param timestamp Timestamp UNIX em milissegundos do horário em que o robô esteve nessa posição.
     */
    public PosInfo(Ponto ponto, float angulo, long timestamp) {
        this.ponto = ponto;
        this.angulo = angulo;
        this.timestamp = timestamp;
    }
    
    public PosInfo copy(){
        return new PosInfo(ponto.copy(), angulo, timestamp);
    }

    public float getAngulo() {
        return angulo;
    }

    public Ponto getPonto() {
        return ponto;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setPonto(Ponto ponto) {
        this.ponto = ponto;
    }

    public void setAngulo(float angulo) {
        this.angulo = angulo;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    
    
    
}

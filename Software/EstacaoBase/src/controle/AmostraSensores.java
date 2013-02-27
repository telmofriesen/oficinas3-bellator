/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

/**
 * Classe que representa uma amostra dos sensores (acelerometro, giroscópio e IR).
 *
 * @author stefan
 */
public class AmostraSensores {

    private final float aceleracao;
    private final float aceleracaoAngular;
    private final float[] distIR;
    private final long timestamp;

    public AmostraSensores(float aceleracao, float aceleracaoAngular, float[] distIR, long timestamp) {
        this.aceleracao = aceleracao;
        this.aceleracaoAngular = aceleracaoAngular;
        this.distIR = distIR;
        this.timestamp = timestamp;
    }

    public synchronized float getAceleracao() {
        return aceleracao;
    }

    public synchronized float getAceleracaoAngular() {
        return aceleracaoAngular;
    }

    public synchronized float[] getDistIR() {
        return distIR;
    }

    public long getTimestamp() {
        return timestamp;
    }
}

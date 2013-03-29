/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

/**
 * Contém duas velocidades: a da roda direita e da roda esquerda do robô.
 *
 * @author stefan
 */
public class EnginesSpeed {

    public final float leftSpeed, rightSpeed;

    /**
     *
     * @param leftSpeed Valor de -1 a 1, sendo 1 a máxima velocidade para frente, -1 a máxima velocidade para trás e 0 parada total.
     * @param rightSpeed Valor de -1 a 1, sendo 1 a máxima velocidade para frente, -1 a máxima velocidade para trás e 0 parada total.
     */
    public EnginesSpeed(float leftSpeed, float rightSpeed) {
        this.leftSpeed = leftSpeed;
        this.rightSpeed = rightSpeed;
    }
}

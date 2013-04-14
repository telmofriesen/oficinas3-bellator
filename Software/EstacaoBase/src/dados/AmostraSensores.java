/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

/**
 * Classe que representa uma amostra dos sensores (acelerometro, giroscópio e IR).
 *
 * @author stefan
 */
public class AmostraSensores {
//    public static final float 

    public final int encoder_esq, encoder_dir;
    public final int[] IR;
    public final int AX, AY, AZ;
    public final int GX, GY, GZ;
    public final long unixTimestamp;

    public AmostraSensores(int encoder_esq, int encoder_dir, int[] IR, int AX, int AY, int AZ, int GX, int GY, int GZ, long unixTimestamp) {
        this.encoder_esq = encoder_esq;
        this.encoder_dir = encoder_dir;
        this.IR = IR;
        this.AX = AX;
        this.AY = AY;
        this.AZ = AZ;
        this.GX = GX;
        this.GY = GY;
        this.GZ = GZ;
        this.unixTimestamp = unixTimestamp;
    }

    /**
     * Retorna uma string conendo todos os atributos separados por espaço.
     * @return 
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d %d ", encoder_esq, encoder_dir));
        for (int i = 0; i < IR.length; i++) {
            sb.append(String.format("%d ", IR[i]));
        }
        sb.append(String.format("%d %d %d %d %d %d ", AX, AY, AZ, GX, GY, GZ));
        sb.append(String.format("%d", unixTimestamp));
        return sb.toString();
    }

    /**
     * Retorna a distância percorrida pela roda esquerda, a partir da contagem obtida do encoder esquerdo.
     *
     * @return
     */
    public int transformedEncoderEsq() {
        return encoder_esq;
    }

    /**
     * Retorna a distância percorrida pela roda direita, a partir da contagem obtida do encoder direito.
     *
     * @return
     */
    public int transformedEncoderDir() {
        return encoder_dir;
    }

    /**
     * Retorna a aceleração X em m/s^2
     *
     * @return
     */
    public float transformedAX() {
        return 0;
    }

    /**
     * Retorna a aceleração Y em m/s^2
     *
     * @return
     */
    public float transformedAY() {
        return 0;
    }

    /**
     * Retorna a aceleração Z em m/s^2
     *
     * @return
     */
    public float transformedAZ() {
        return 0;
    }

    /**
     * Retorna a aceleração angular X em rad/s^2
     *
     * @return
     */
    public float transformedGX() {
        return 0;
    }

    /**
     * Retorna a aceleração angular Y em rad/s^2
     *
     * @return
     */
    public float transformedGY() {
        return 0;
    }

    /**
     * Retorna a aceleração angular Z em rad/s^2
     *
     * @return
     */
    public float transformedGZ() {
        return 0;
    }

    public int[] transformedIR() {
        int[] transformedIR = new int[IR.length];
        for (int i = 0; i < IR.length; i++) {
            transformedIR[i] = SensorIR.getDistFromByte(IR[i]);
        }
        return transformedIR;
    }
}

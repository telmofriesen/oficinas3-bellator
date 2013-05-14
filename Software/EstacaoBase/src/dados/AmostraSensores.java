/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

/**
 * Classe que representa uma amostra dos sensores (Encoders, IR, acelerometro e giroscópio).
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
    //Aceleração da gravidade
    public static final double g = 9.80665; //m/s^2
    //Circunferências (eixo da roda, eixo do encoder, roda em si) em milímetros
    public static final int C1 = 75,
            C2 = 220,
            C3 = 640;

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
     *
     * @return
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d ", unixTimestamp));
        sb.append(String.format("%d %d ", encoder_esq, encoder_dir));
        for (int i = 0; i < IR.length; i++) {
            sb.append(String.format("%d ", IR[i]));
        }
        sb.append(String.format("%d %d %d %d %d %d", AX, AY, AZ, GX, GY, GZ));
        return sb.toString();
    }

    public String transformedToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d ", unixTimestamp));
        sb.append(String.format("%d %d ", transformedEncoderEsq(), transformedEncoderDir()));
        for (int i = 0; i < IR.length; i++) {
            sb.append(String.format("%d ", SensorIR.getDistFromByte(IR[i])));
        }
        sb.append(String.format("%f %f %f %f %f %f", transformedAX(), transformedAY(), transformedAZ(), transformedGX(), transformedGY(), transformedGZ()));
        return sb.toString();
    }

    /**
     * Retorna a distância percorrida pela roda esquerda, a partir da contagem obtida do encoder esquerdo.
     *
     * @return
     */
    public int transformedEncoderEsq() {
//        return Math.round(((float) (C2 * C3) / (float) (1708*C1)) * encoder_esq);
        return Math.round(((float) (C2 * C3) / (float) (1593*C1)) * encoder_esq * 2.50f);
//        return Math.round(((float) (C2 * C3) / (float) (1800*C1)) * encoder_esq);
//        return encoder_esq;
    }

    /**
     * Retorna a distância percorrida pela roda direita, a partir da contagem obtida do encoder direito.
     *
     * @return
     */
    public int transformedEncoderDir() {
//        return Math.round(((float) (C2 * C3) / (float) (1627*C1)) * encoder_dir);
        return Math.round(((float) (C2 * C3) / (float) (1580*C1)) * encoder_dir * 2.50f);
//        return Math.round(((float) (C2 * C3) / (float) (1800*C1)) * encoder_dir);
//        return encoder_dir;
    }

    /**
     * Retorna a aceleração no eixo X em m/s^2
     *
     * @return
     */
    public float transformedAX() {
        return (float) ((double) AX / ((double) 16384 / g));
    }

    /**
     * Retorna a aceleração Y em m/s^2
     *
     * @return
     */
    public float transformedAY() {
        return (float) ((double) AY / ((double) 16384 / g));
    }

    /**
     * Retorna a aceleração Z em m/s^2
     *
     * @return
     */
    public float transformedAZ() {
        return (float) ((double) AZ / ((double) 16384 / g));
    }

    /**
     * Retorna a aceleração angular X em rad/s^2
     *
     * @return
     */
    public float transformedGX() {
        return (float) ((double) GX * Math.PI / ((double) 131 * 180));
    }

    /**
     * Retorna a aceleração angular Y em rad/s^2
     *
     * @return
     */
    public float transformedGY() {
        return (float) ((double) GY * Math.PI / ((double) 131 * 180));
    }

    /**
     * Retorna a aceleração angular no eixo Z em rad/s^2
     *
     * @return
     */
    public float transformedGZ() {
        return (float) ((double) GZ * Math.PI / ((double) 131 * 180));
    }

    /**
     * Retorna um vetor de distâncias detectadas pelos sensores IR, em milímetros.
     * @return 
     */
    public int[] transformedIR() {
        int[] transformedIR = new int[IR.length];
        for (int i = 0; i < IR.length; i++) {
            transformedIR[i] = SensorIR.getDistFromByte(IR[i]);
        }
        return transformedIR;
    }
}

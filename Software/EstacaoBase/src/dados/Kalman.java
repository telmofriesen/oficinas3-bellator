/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

/**
 * Implementação do filtro de Kalman. Segue o modelo simplificado especificado
 * em: http://bilgin.esme.org/BitsBytes/KalmanFilterforDummies.aspx
 *
 * @author stefan
 */
public class Kalman {

    private int k;
    private float x, x_;
    private float P, P_;
    private float K;
    private float R;

    public Kalman(float R) {
        k = 0;
        x = 0;

        P = 1;
        this.R = R;
    }

    public float nextX(float z) {

        //Time update
        x_ = x;
        P_ = P;

        //Measurement update
        K = P_ / (P_ + R);
        x = x_ + K * (z - x_);
        P = (1 - K) * P_;

        k++;

        return x;
    }

    public float getK() {
        return K;
    }

    public void setK(float K) {
        this.K = K;
    }

    public float getP() {
        return P;
    }

    public void setP(float P) {
        this.P = P;
    }

    public float getP_() {
        return P_;
    }

    public void setP_(float P_) {
        this.P_ = P_;
    }

    public float getR() {
        return R;
    }

    public void setR(float R) {
        this.R = R;
    }

    public int getk() {
        return k;
    }

    public void setk(int k) {
        this.k = k;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getX_() {
        return x_;
    }

    public void setX_(float x_) {
        this.x_ = x_;
    }

    /**
     * Teste do filtro
     *
     * @param args
     */
    public static void main(String[] args) {
        float x;
        Kalman kalman = new Kalman(0.1f);
        System.out.println(kalman.nextX(0.39f));
        System.out.println(kalman.nextX(0.5f));
        System.out.println(kalman.nextX(0.48f));
        System.out.println(kalman.nextX(0.29f));
        System.out.println(kalman.nextX(0.25f));
        System.out.println(kalman.nextX(0.32f));
    }
}

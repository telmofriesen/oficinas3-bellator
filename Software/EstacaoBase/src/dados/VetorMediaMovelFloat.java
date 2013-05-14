/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

/**
 *
 * @author stefan
 */
public class VetorMediaMovelFloat {

    float[] vetor;
    int next;

    public VetorMediaMovelFloat(int num_periodos) {
        vetor = new float[num_periodos];
        next = 0;
        for (int i = 0; i < num_periodos; i++) {
            vetor[i] = 0;
        }
    }

    public void insereValor(float valor) {
        vetor[next] = valor;
        next = (next + 1) % vetor.length;
    }

    public float getMedia() {
        float sum = 0;
        for (int i = 0; i < vetor.length; i++) {
            sum += vetor[i];
        }
        return sum / vetor.length;
    }

    public void setNumPeriodos(int num_periodos) {
        vetor = new float[num_periodos];
        for (int i = 0; i < num_periodos; i++) {
            vetor[i] = 0;
        }
        next = 0;
    }

    public int getNumPeriodos() {
        return vetor.length;
    }
    
    public void setValores(float valor){
        for (int i = 0; i < vetor.length; i++) {
            vetor[i] = valor;
        }
    }
}

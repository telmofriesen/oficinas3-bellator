package visual.gui;

import javax.swing.BoundedRangeModel;
import javax.swing.JSlider;

/**
 * JSlider que trabalha com representação de pontos flutuantes.
 * @author stefan
 */
public class FloatJSlider extends JSlider {

    /**
     * Representação interna dos limites mínimo e máximo (só pode ser feita com inteiros).
     */
    public static final int I_MIN = 0, I_MAX = 1000;
    /**
     * Limites inferior e superior.
     */
    public float f_max, f_min;

    public FloatJSlider() {
        super(I_MIN, I_MAX);
        this.f_min = (float) I_MIN;
        this.f_max = (float) I_MAX;
    }

    public FloatJSlider(float f_min, float f_max) {
        super(I_MIN, I_MAX);
        this.f_min = f_min;
        this.f_max = f_max;
    }

    public FloatJSlider(int i) {
        super(I_MIN, I_MAX);
        this.f_min = (float) I_MIN;
        this.f_max = (float) I_MAX;
    }

    public FloatJSlider(int i, int i1) {
        super(I_MIN, I_MAX);
        this.f_min = (float) I_MIN;
        this.f_max = (float) I_MAX;
    }

    public FloatJSlider(int i, int i1, int i2) {
        super(I_MIN, I_MAX);
        this.f_min = (float) I_MIN;
        this.f_max = (float) I_MAX;
    }

    public FloatJSlider(int i, int i1, int i2, int i3) {
        super(I_MIN, I_MAX);
        this.f_min = (float) I_MIN;
        this.f_max = (float) I_MAX;
    }

    public FloatJSlider(BoundedRangeModel brm) {
        super(I_MIN, I_MAX);
        this.f_min = (float) I_MIN;
        this.f_max = (float) I_MAX;
    }

    /**
     * Retorna o valor equivalente em ponto futuante.
     * @return 
     */
    public float getFloatValue() {
        //Calcula a proporção
        float valor_porcentagem = (float) (getValue() - I_MIN) / (float) (I_MAX - I_MIN);
        float v = (valor_porcentagem) * (f_max - f_min) + f_min;
        return v;
    }

    /**
     * Muda o valor equivalente em ponto flutuante.
     * @param value 
     */
    public void setFloatValue(float value) {
        //Calcula a proporção
        float valor_porcentagem = (float) (value - f_min) / (float) (f_max - f_min);
        int new_value = Math.round((valor_porcentagem) * (float) (I_MAX - I_MIN)) + I_MIN;
        this.setValue(new_value);
    }

    public float getF_max() {
        return f_max;
    }

    public void setF_max(float f_max) {
        this.f_max = f_max;
    }

    public float getF_min() {
        return f_min;
    }

    public void setF_min(float f_min) {
        this.f_min = f_min;
    }
}

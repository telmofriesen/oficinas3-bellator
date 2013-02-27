/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import java.awt.Color;

/**
 * Propriedades visuais da grade.
 * @author stefan
 */
public class GridDrawableProp {
    private Color corGrid = new Color(0,0,0,20);
    //Limite para desenhar as linhas da grade
    private int limite_inf_x = -10000;
    private int limite_sup_x = 10000;
    private int limite_inf_y = -10000;
    private int limite_sup_y = 10000;
    //Intervalo entre cada linha (mm)
    private int step_x = 1000;
    private int step_y = 1000;

    public Color getCorGrid() {
        return corGrid;
    }

    public void setCorGrid(Color corGrid) {
        this.corGrid = corGrid;
    }    

    public int getLimite_inf_x() {
        return limite_inf_x;
    }

    public void setLimite_inf_x(int limite_inf_x) {
        this.limite_inf_x = limite_inf_x;
    }

    public int getLimite_sup_x() {
        return limite_sup_x;
    }

    public void setLimite_sup_x(int limite_sup_x) {
        this.limite_sup_x = limite_sup_x;
    }

    public int getLimite_inf_y() {
        return limite_inf_y;
    }

    public void setLimite_inf_y(int limite_inf_y) {
        this.limite_inf_y = limite_inf_y;
    }

    public int getLimite_sup_y() {
        return limite_sup_y;
    }

    public void setLimite_sup_y(int limite_sup_y) {
        this.limite_sup_y = limite_sup_y;
    }

    public int getStep_x() {
        return step_x;
    }

    public void setStep_x(int step_x) {
        this.step_x = step_x;
    }

    public int getStep_y() {
        return step_y;
    }

    public void setStep_y(int step_y) {
        this.step_y = step_y;
    }



    
}

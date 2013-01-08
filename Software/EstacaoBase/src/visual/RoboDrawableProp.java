/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import java.awt.Color;

/**
 * Propriedades visuais do robô
 * @author stefan
 */
public class RoboDrawableProp {

    int tamanhoRobo = 8;
    private Color corRobo = new Color(0, 0, 0, 100);
    private Color corSensoresIR = new Color(0, 0, 200, 240);

    public Color getCorSensoresIR() {
        return corSensoresIR;
    }

    public void setCorSensoresIR(Color corSensoresIR) {
        this.corSensoresIR = corSensoresIR;
    }
    private Color corOrigem = new Color(0, 0, 200, 200);
    private boolean roboEnabled = true;
    private boolean coordenadasEnabled = true;

    public int getTamanhoRobo() {
        return tamanhoRobo;
    }

    public void setTamanhoRobo(int tamanhoRobo) {
        this.tamanhoRobo = tamanhoRobo;
    }

    public Color getCorRobo() {
        return corRobo;
    }

    public void setCorRobo(Color corRobo) {
        this.corRobo = corRobo;
    }

    public Color getCorOrigem() {
        return corOrigem;
    }

    public void setCorOrigem(Color corOrigem) {
        this.corOrigem = corOrigem;
    }

    public boolean isRoboEnabled() {
        return roboEnabled;
    }

    public void setRoboEnabled(boolean roboEnabled) {
        this.roboEnabled = roboEnabled;
    }

    public boolean isCoordenadasEnabled() {
        return coordenadasEnabled;
    }

    public void setCoordenadasEnabled(boolean coordenadasEnabled) {
        this.coordenadasEnabled = coordenadasEnabled;
    }
}

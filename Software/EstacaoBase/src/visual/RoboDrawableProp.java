/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import java.awt.Color;

/**
 * Propriedades visuais do robô
 *
 * @author stefan
 */
public class RoboDrawableProp {

    int tamanhoSensores = 80;
    private Color corRobo = new Color(0, 0, 0, 100);
    private Color corSensoresIR = new Color(0, 0, 200, 240);
    private Color corOrigem = new Color(0, 0, 200, 200);
    private boolean roboEnabled = true;
    private boolean coordenadasEnabled = true;
    private boolean textoEnabled = true;
    private int larguraRodas = 50;
    private int comprimentoRodas = 150;
    private Color corRodas = new Color(0, 0, 0, 200);

    public Color getCorRodas() {
        return corRodas;
    }

    public void setCorRodas(Color corRodas) {
        this.corRodas = corRodas;
    }

    public int getLarguraRodas() {
        return larguraRodas;
    }

    public void setLarguraRodas(int larguraRodas) {
        this.larguraRodas = larguraRodas;
    }

    public int getComprimentoRodas() {
        return comprimentoRodas;
    }

    public void setComprimentoRodas(int comprimentoRodas) {
        this.comprimentoRodas = comprimentoRodas;
    }

    public Color getCorSensoresIR() {
        return corSensoresIR;
    }

    public void setCorSensoresIR(Color corSensoresIR) {
        this.corSensoresIR = corSensoresIR;
    }

    public int getTamanhoSensores() {
        return tamanhoSensores;
    }

    public void setTamanhoSensores(int tamanhoRobo) {
        this.tamanhoSensores = tamanhoRobo;
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

    public boolean isTextoEnabled() {
        return textoEnabled;
    }

    public void setTextoEnabled(boolean textoEnabled) {
        this.textoEnabled = textoEnabled;
    }
}

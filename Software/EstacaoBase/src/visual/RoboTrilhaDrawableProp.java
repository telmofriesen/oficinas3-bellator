/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import java.awt.Color;

/**
 * Propriedades visuais da trilha do robô
 * @author stefan
 */
public class RoboTrilhaDrawableProp {

    private Color corTrilha = new Color(0, 255, 0);
    private Color corPontos = new Color(0, 0, 0);
    private int tamanhoPontosTrilha = 3;
    private boolean linhaEnabled = true, pontosEnabled = true, origemEnabled = true;
    private boolean coordenadasEnabled=true;

    public boolean isCoordenadasEnabled() {
        return coordenadasEnabled;
    }

    public void setCoordenadasEnabled(boolean coordenadasEnabled) {
        this.coordenadasEnabled = coordenadasEnabled;
    }

    public void setCorTrilha(Color corTrilha) {
        this.corTrilha = corTrilha;
    }

    public void setCorPontos(Color corPontos) {
        this.corPontos = corPontos;
    }

    public void setTamanhoPontosTrilha(int tamanhoPontosTrilha) {
        this.tamanhoPontosTrilha = tamanhoPontosTrilha;
    }

    public void setLinhaEnabled(boolean trilhaEnabled) {
        this.linhaEnabled = trilhaEnabled;
    }

    public void setPontosEnabled(boolean pontosEnabled) {
        this.pontosEnabled = pontosEnabled;
    }

    public void setOrigemEnabled(boolean origemEnabled) {
        this.origemEnabled = origemEnabled;
    }

    public Color getCorTrilha() {
        return corTrilha;
    }

    public Color getCorPontos() {
        return corPontos;
    }

    public int getTamanhoPontosTrilha() {
        return tamanhoPontosTrilha;
    }

    public boolean isLinhaEnabled() {
        return linhaEnabled;
    }

    public boolean isPontosEnabled() {
        return pontosEnabled;
    }

    public boolean isOrigemEnabled() {
        return origemEnabled;
    }
    
    
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import java.awt.Color;

/**
 * Propriedades visuais dos obstáculos.
 * @author stefan
 */
public class ObstaculosDrawableProp {
    Color corPontos = new Color(255,0,0);
    Color corPontoSelecionado = new Color(0,255,0);
    
    int tamanhoPontos = 5;

    public Color getCorPontos() {
        return corPontos;
    }

    public void setCorPontos(Color corPontos) {
        this.corPontos = corPontos;
    }

    public int getTamanhoPontos() {
        return tamanhoPontos;
    }

    public void setTamanhoPontos(int tamanhoPontos) {
        this.tamanhoPontos = tamanhoPontos;
    }

    public Color getCorPontoSelecionado() {
        return corPontoSelecionado;
    }

    public void setCorPontoSelecionado(Color corPontoSelecionado) {
        this.corPontoSelecionado = corPontoSelecionado;
    }
    
    
}

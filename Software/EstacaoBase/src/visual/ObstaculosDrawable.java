/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import controle.*;
import java.awt.Color;
import processing.core.PApplet;
import processing.core.PVector;

/**
 * Classe que implementa o desenho dos pontos dos obstáculos na interface.
 *
 * @author stefan
 */
public class ObstaculosDrawable implements Drawable2D, MouseListener2D {

    private Obstaculos obstaculos;
    private ObstaculosDrawableProp propriedades = new ObstaculosDrawableProp();

    public ObstaculosDrawable(Obstaculos obstaculos) {
        this.obstaculos = obstaculos;
    }

    @Override
    /**
     * Desenha os pontos dos obstáculos.
     */
    public void draw2D(PApplet a) {
        Viewer2D viewer2D = (Viewer2D) a;
        float escala = viewer2D.getEscala();

        a.pushMatrix();
        a.pushStyle();
        viewer2D.transform();

        Color color = propriedades.getCorPontos();
        a.fill(color.getRGB());
        a.noStroke();
        a.ellipseMode(PApplet.CENTER);
//        applet.rotate(angulo);
        for (int i = 0; i < obstaculos.getNumPontos(); i++) {
            Ponto ponto = obstaculos.getPonto2D(i);
            a.ellipse((float) ponto.x() * escala, (float) ponto.y() * escala, propriedades.getTamanhoPontos(), propriedades.getTamanhoPontos());
//            PontoXY ponto_new = Util.getTransformedCoordinates(ponto, viewPortStartX, viewPortStartY, escala);
//            applet.ellipse((float) ponto_new.getX(), (float) ponto_new.getY(), propriedades.getTamanhoPontos(), propriedades.getTamanhoPontos());
        }

        a.popStyle();
        a.popMatrix();
    }

    /**
     * Se o mouse passar por cima de um ponto, mostra em que posição ele está.
     *
     * @param a
     * @param mousePosReal Posição do mouse real (cm)
     */
    @Override
    public void mouseChanged(PApplet a, PVector mousePosReal) {
        Viewer2D viewer2D = (Viewer2D) a;
        float escala = viewer2D.getEscala();
        float angulo = viewer2D.getAngulo();
        a.pushMatrix();
        a.pushStyle();
        viewer2D.transform();

        for (int i = 0; i < obstaculos.getNumPontos(); i++) {
            Ponto ponto = obstaculos.getPonto2D(i);
            if (mousePosReal.x > ponto.x - propriedades.getTamanhoPontos() / escala
                && mousePosReal.x < ponto.x + propriedades.getTamanhoPontos() / escala
                && mousePosReal.y > ponto.y - propriedades.getTamanhoPontos() / escala
                && mousePosReal.y < ponto.y + propriedades.getTamanhoPontos() / escala) {
                a.fill(propriedades.getCorPontoSelecionado().getRGB());
                a.translate((float) ponto.x() * escala, (float) ponto.y() * escala);
                a.ellipse(0, 0, propriedades.getTamanhoPontos(), propriedades.getTamanhoPontos());
                a.rotate(-angulo);
                a.textAlign(PApplet.LEFT);
                a.text(String.format("(%d, %d)", ponto.x, ponto.y), 5, -10);
                break;
            }
        }
        a.popStyle();
        a.popMatrix();
    }

    public Obstaculos getObstaculos() {
        return obstaculos;
    }

    public void setObstaculos(Obstaculos obstaculos) {
        this.obstaculos = obstaculos;
    }

    public ObstaculosDrawableProp getPropriedades() {
        return propriedades;
    }

    public void setPropriedades(ObstaculosDrawableProp propriedades) {
        this.propriedades = propriedades;
    }
}

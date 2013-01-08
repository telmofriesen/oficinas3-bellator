/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import controle.Robo;
import java.awt.Color;
import processing.core.PApplet;

/**
 * Classe responsável por desenhar a trilha percorrida pelo robô na interface gráfica
 * @author stefan
 */
public class RoboTrilhaDrawable implements Drawable2D {

    private Robo robo;
    private RoboTrilhaDrawableProp propriedades = new RoboTrilhaDrawableProp();

    public RoboTrilhaDrawable(Robo robo) {
        this.robo = robo;
    }

    @Override
    public void draw2D(PApplet a) {
        Viewer2D viewer2D = (Viewer2D) a;
        float escala = viewer2D.getEscala();

        a.pushMatrix();
        a.pushStyle();
        viewer2D.transform();

        //
        //Desenha os pontos
        //
        Color corPontos = propriedades.getCorPontos();
        if (propriedades.isPontosEnabled()) {
            a.noStroke();
            a.fill(corPontos.getRGB());  //Cor dos pontos        

            Ponto ponto = robo.getPosicao(0).getPonto();
            a.ellipseMode(PApplet.CENTER);
            a.ellipse(ponto.x() * escala, ponto.y() * escala, propriedades.getTamanhoPontosTrilha(), propriedades.getTamanhoPontosTrilha());

            a.fill(corPontos.getRGB());
            for (int i = 1; i < robo.getNumPosicoes() - 1; i++) {
                ponto = robo.getPosicao(i).getPonto();
                a.ellipse((float) ponto.x() * escala, (float) ponto.y() * escala, propriedades.getTamanhoPontosTrilha(), propriedades.getTamanhoPontosTrilha());
            }
        }

        //
        //Desenha a linha da trilha
        //
        Color corTrilha = propriedades.getCorTrilha();
        if (propriedades.isTrilhaEnabled()) {
            a.noFill();
            a.stroke(corTrilha.getRGB());
            a.beginShape();
            for (int i = 0; i < robo.getNumPosicoes(); i++) {
                Ponto ponto = robo.getPosicao(i).getPonto();
                a.vertex(ponto.x() * escala, ponto.y() * escala);
            }
            a.endShape();
        }
        a.popMatrix();
        
        
        a.pushMatrix();
        //
        // Desenha a origem (0,0) real
        //
        viewer2D.transform_translation();
        a.fill(100);
        a.text("(0,0)", 5, 10);
        viewer2D.transform_rotation();
        //Desenha o indicador do (0,0) real
        a.fill(0);
        a.stroke(0);
        a.ellipse(0, 0, 5, 5);
        //text("x", 10, 0);
        //text("y", 0, 10);
        a.stroke(0, 200, 0);
        viewer2D.arrowLine(0, 0, 15, 0, 0, PApplet.radians(20), false);
        a.stroke(200, 0, 0);
        viewer2D.arrowLine(0, 0, 0, 15, 0, PApplet.radians(20), false);

        a.popStyle();
        a.popMatrix();
    }

    public RoboTrilhaDrawableProp getPropriedades() {
        return propriedades;
    }

    public void setPropriedades(RoboTrilhaDrawableProp propriedades) {
        this.propriedades = propriedades;
    }

    public Robo getRobo() {
        return robo;
    }

    public void setRobo(Robo robo) {
        this.robo = robo;
    }
}

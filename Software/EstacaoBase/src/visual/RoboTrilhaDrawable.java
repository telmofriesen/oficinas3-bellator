/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import dados.PosInfo;
import dados.Robo;
import java.awt.Color;
import java.util.ArrayList;
import processing.core.PApplet;

/**
 * Classe responsável por desenhar a trilha percorrida pelo robô na interface gráfica
 *
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

        if (propriedades.isLinhaEnabled()) {
            //
            //Desenha a linhas das trilhas
            //
            Color corTrilha = propriedades.getCorTrilha();
            ArrayList<ArrayList<PosInfo>> posInfos = robo.getPosInfos();
            //Para cada trilha do robo...
            for (int k = 0; k < posInfos.size(); k++) {
                ArrayList<PosInfo> trilhaAtual = posInfos.get(k);
                //
                // Desenha a linha da trilha
                //
                if (propriedades.isLinhaEnabled()) {
                    a.pushMatrix();
                    a.pushStyle();
                    viewer2D.transform();
                    a.noFill();
                    a.stroke(corTrilha.getRGB());
                    a.beginShape();
                    //Para cada ponto da trilha...
                    for (int i = 0; i < trilhaAtual.size(); i++) {
                        Ponto ponto = trilhaAtual.get(i).getPonto();
                        //Desenha a linha
                        a.vertex(ponto.x() * escala, ponto.y() * escala);
                    }
                    a.endShape();
                    a.pushStyle();
                    a.popMatrix();
                }


                if (propriedades.isPontosEnabled()) {

                    //
                    //Desenha os pontos
                    //

                    if (propriedades.isPontosEnabled()) {
                        a.pushMatrix();
                        a.pushStyle();
                        viewer2D.transform();
                        Color corPontos = propriedades.getCorPontos();
                        a.noStroke();
                        a.fill(corPontos.getRGB());  //Cor dos pontos        

                        Ponto ponto = robo.getPosicaoTrilhaAtual(0).getPonto();
                        a.ellipseMode(PApplet.CENTER);
                        a.ellipse(ponto.x() * escala, ponto.y() * escala, propriedades.getTamanhoPontosTrilha(), propriedades.getTamanhoPontosTrilha());

                        a.fill(corPontos.getRGB());
                        //Para cada ponto da trilha...
                        for (int i = 0; i < trilhaAtual.size(); i++) {
                            Ponto ponto1 = trilhaAtual.get(i).getPonto();
                            a.ellipse((float) ponto1.x() * escala, (float) ponto1.y() * escala,
                                      propriedades.getTamanhoPontosTrilha(), propriedades.getTamanhoPontosTrilha());
                        }
                        a.popStyle();
                        a.popMatrix();
                    }
                    //
                    //Desenha o ponto da primeira posicao da trilha de forma destacada
                    //
                    Ponto primeiroPonto = trilhaAtual.get(0).getPonto();

                    a.pushMatrix();
                    a.pushStyle();

                    viewer2D.transform_translation();
                    a.fill(100);
                    a.text(String.format("(%d,%d)",
                                         (int) (primeiroPonto.x() / 10),
                                         (int) (primeiroPonto.y() / 10)),
                           primeiroPonto.x() * escala + 5, primeiroPonto.y() * escala + 10);
                    viewer2D.transform_rotation();
                    //Desenha o indicador do (0,0) real
                    a.fill(0);
                    a.stroke(0);
                    a.ellipse(primeiroPonto.x() * escala, primeiroPonto.y() * escala, 5, 5);
                    //text("x", 10, 0);
                    //text("y", 0, 10);
                    a.stroke(0, 200, 0);
                    viewer2D.arrowLine(primeiroPonto.x() * escala,
                                       primeiroPonto.y() * escala,
                                       primeiroPonto.x() * escala + 15,
                                       primeiroPonto.y() * escala,
                                       0, PApplet.radians(20), false);
                    a.stroke(200, 0, 0);
                    viewer2D.arrowLine(primeiroPonto.x() * escala,
                                       primeiroPonto.y() * escala,
                                       primeiroPonto.x() * escala,
                                       primeiroPonto.y() * escala + 15,
                                       0, PApplet.radians(20), false);

                    a.popStyle();
                    a.popMatrix();
                }
            }
        }

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

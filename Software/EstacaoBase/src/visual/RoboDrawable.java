/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import dados.PosInfo;
import dados.Robo;
import java.awt.Color;
import processing.core.PApplet;

/**
 * Classe responsável por desenhar o robô na interface gráfica
 *
 * @author stefan
 */
public class RoboDrawable implements Drawable2D {

    private final Robo robo;
    RoboTrilhaDrawable robo_trilha;
    private RoboDrawableProp propriedades = new RoboDrawableProp();

    public RoboDrawable(Robo robo) {
        this.robo = robo;
        this.robo_trilha = new RoboTrilhaDrawable(robo);
    }

    @Override
    public void draw2D(PApplet a) {
        synchronized (robo) {
            Viewer2D viewer2D = (Viewer2D) a;
            float escala = viewer2D.getEscala();
            float angulo_interface = viewer2D.getAngulo();
            PosInfo lastPos = robo.getUltimaPosicaoTrilhaAtual(); //Pega a ultima posicao do robo
            if (lastPos == null || robo.getCentroMovimento() == null) return;
            Ponto pontoLastPos = lastPos.getPonto();

            //
            // Desenha a trilha percorrida pelo robô
            //
            robo_trilha.draw2D(a);

            if (propriedades.isRoboEnabled()) {

                if (propriedades.isTextoEnabled()) {

                    //
                    // Desenha os textos com informações sobre o robô
                    //
                    a.pushMatrix();
                    a.pushStyle();
                    a.fill(0);
                    a.textAlign(PApplet.RIGHT);
                    a.text(String.format("robo.pos: (%d, %d) cm @ %.2f deg", pontoLastPos.x / 10, pontoLastPos.y / 10, PApplet.degrees(lastPos.getAngulo())), a.width - 5, 15);
                    a.text(String.format("robo.velocidade=%.3f m/s", robo.getVelocidadeAtual()), a.width - 5, 30);
                    a.text(String.format("robo.velocidade_angular=%.3f rad/s", robo.getVelocidadeAngularAtual()), a.width - 5, 45);
                    a.popMatrix();
                    a.popStyle();
//        a.text(String.format("robo.y=%d cm", pontoLastPos.y), a.width - 5, 30);
//        a.text(String.format("robo.angulo=%.2f deg", PApplet.degrees(lastPos.getAngulo())), a.width - 5, 45);
                }


                //
                //Desenha o icone do robo
                //
                a.pushMatrix();
                viewer2D.transform();
                float anguloRobo = lastPos.getAngulo();
                a.translate(pontoLastPos.x() * escala, pontoLastPos.y() * escala);
                Color corRobo = propriedades.getCorRobo();
                a.fill(corRobo.getRGB());
                a.stroke(corRobo.getRGB());
                a.rotate(anguloRobo);
                a.ellipseMode(PApplet.CENTER);
                //Desenha o circulo do centro de movimento
                a.ellipse(0, 0, propriedades.getTamanhoSensores() * escala, propriedades.getTamanhoSensores() * escala);
                a.line(0, 0, propriedades.getTamanhoSensores() * escala, 0);

                //Desenha o retangulo principal
                a.rectMode(PApplet.CORNER);
                a.rect(-robo.getComprimento() * escala - robo.getCentroMovimento().x() * escala,
                       -robo.getCentroMovimento().y() * escala,
                       robo.getComprimento() * escala,
                       robo.getLargura() * escala);

                //Desenha as rodas
                a.fill(propriedades.getCorRodas().getRGB());
                //Roda esquerda
                a.rect(-propriedades.getComprimentoRodas() * escala / 2,
                       -robo.getCentroMovimento().y() * escala - propriedades.getLarguraRodas() * escala,
                       propriedades.getComprimentoRodas() * escala,
                       propriedades.getLarguraRodas() * escala);
                //Roda direita
                a.rect(-propriedades.getComprimentoRodas() * escala / 2,
                       robo.getCentroMovimento().y() * escala + propriedades.getLarguraRodas() * escala,
                       propriedades.getComprimentoRodas() * escala,
                       -propriedades.getLarguraRodas() * escala);
                a.popMatrix();



                //
                //Desenha os sensores IR
                //
                a.fill(propriedades.getCorSensoresIR().getRGB());
                for (int i = 0; i < robo.getNumSensoresIR(); i++) {
                    Ponto posSensor = robo.getSensor(i).getPosicaoNoRobo();
                    a.pushMatrix();
                    viewer2D.transform();
                    a.translate(pontoLastPos.x() * escala, pontoLastPos.y() * escala);
                    a.rotate(anguloRobo);
                    a.translate(posSensor.x() * escala, posSensor.y() * escala);
                    a.rotate(robo.getSensor(i).getAngulo());
                    a.ellipse(0, 0, propriedades.getTamanhoSensores() * escala, propriedades.getTamanhoSensores() * escala);
                    a.line(0, 0, propriedades.getTamanhoSensores() * escala, 0);
                    a.popMatrix();
                }

                //
                //Desenha o texto com as coordenadas atuais do robo
                //
                if (propriedades.isCoordenadasEnabled()) {
                    a.pushMatrix();
                    viewer2D.transform();
                    a.fill(50);
                    a.translate(pontoLastPos.x() * escala, pontoLastPos.y() * escala);
                    a.rotate(-angulo_interface);
                    a.textAlign(PApplet.LEFT);
                    a.text(String.format("(%d,%d) @ %.2f deg", pontoLastPos.x / 10, pontoLastPos.y / 10, PApplet.degrees(lastPos.getAngulo())),
                           +5, -5);
                    a.popMatrix();
                }


//                a.popStyle();
//                a.popMatrix();
            }
        }
    }

    public RoboTrilhaDrawable getRobo_trilha() {
        return robo_trilha;
    }

    public RoboDrawableProp getPropriedades() {
        return propriedades;
    }

    public void setPropriedades(RoboDrawableProp propriedades) {
        this.propriedades = propriedades;
    }

    public Robo getRobo() {
        return robo;
    }
//    public void setRobo(Robo robo) {
//        this.robo = robo;
//    }
}

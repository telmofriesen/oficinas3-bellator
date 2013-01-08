/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import processing.core.PApplet;

/**
 * Desenha a escala grafica no canto inferior direito da tela.
 * @author stefan
 */
public class EscalaDrawable implements Drawable2D {

    @Override
    public void draw2D(PApplet applet) {
        applet.pushMatrix();
        applet.pushStyle();
        
        int comprimentoLinha = 50;
        float escala = ((Viewer2D) applet).getEscala();
        Ponto canto_esq = new Ponto(applet.width - 10, applet.height - 10);
        Ponto canto_dir = new Ponto(canto_esq.x() - comprimentoLinha, canto_esq.y());

        applet.stroke(0);
        applet.fill(0);
        applet.line(canto_esq.x(), canto_esq.y(), canto_dir.x(), canto_dir.y()); //Linha horizontal
        applet.line(canto_esq.x(), canto_esq.y(), canto_esq.x(), canto_esq.y() - 5); //Pequena linha vertical da esqueda
        applet.line(canto_dir.x(), canto_dir.y(), canto_dir.x(), canto_dir.y() - 5); //Pequena linha vertical da direita
        
        String txt = String.format("%.1f cm", (float) comprimentoLinha / escala); //Texto da escala
        applet.textAlign(PApplet.RIGHT); 
        applet.text(txt, canto_esq.x(), canto_esq.y() - 15);
        
        applet.popStyle();
        applet.popMatrix();
    }
}

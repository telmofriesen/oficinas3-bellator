/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import java.awt.Color;
import processing.core.PApplet;

/**
 * Desenha as linhas de grade na interface.
 *
 * @author stefan
 */
public class GridDrawable implements Drawable2D {

    GridDrawableProp propriedades = new GridDrawableProp();

    @Override
    public void draw2D(PApplet a) {
        Viewer2D viewer2D = ((Viewer2D) a);
        float escala = viewer2D.getEscala();
        a.pushMatrix();
        a.pushStyle();
        viewer2D.transform();
        Color corGrid = propriedades.getCorGrid();
        a.stroke(corGrid.getRGB());
        for (int i = propriedades.getLimite_inf_x(); i <= propriedades.getLimite_sup_x(); i += propriedades.getStep_x()) {
            //Linha vertical
            a.line(PApplet.round((float) i * escala),
                   PApplet.round(propriedades.getLimite_inf_y() * escala),
                   PApplet.round((float) i * escala),
                   PApplet.round(propriedades.getLimite_sup_y() * escala));
        }
        for (int i = propriedades.getLimite_inf_y(); i <= propriedades.getLimite_sup_y(); i += propriedades.getStep_y()) {
            //Linha horizontal
            a.line(PApplet.round(propriedades.getLimite_inf_x() * escala),
                   PApplet.round((float) i * escala),
                   PApplet.round(propriedades.getLimite_sup_x() * escala),
                   PApplet.round((float) i * escala));
        }
        a.popMatrix();
        a.popStyle();
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * Interface para ações em caso de alterações na posição do mouse.
 * @author stefan
 */
public interface MouseListener2D {
    public void mouseChanged(PApplet a, PVector mousePos);
}

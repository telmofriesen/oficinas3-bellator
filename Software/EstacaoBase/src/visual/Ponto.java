/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * Ponto cartesiano XY
 * @author stefan
 */
public class Ponto {

    public int x, y;

    public Ponto(int X, int Y) {
        this.x = X;
        this.y = Y;
    }

//    public float getXScaled(float escala){
//        return (float)X*escala;        
//    }
//    
//    public float getYScaled(float escala){
//        return (float)Y*escala;        
//    }
//    
//    public float getXTranslated(float translation){
//        return (float)X + translation;        
//    }    
    public int x() {
        return x;
    }

    public void x(int x) {
        this.x = x;
    }

    public int y() {
        return y;
    }

    public void y(int y) {
        this.y = y;
    }

    public void addX(int x) {
        this.x += x;
    }

    public void addY(int y) {
        this.y += y;
    }

    public void scale(float escala) {
        x = PApplet.floor((float) x * escala);
        y = PApplet.floor((float) y * escala);
    }

    public PVector getPVector() {
        return new PVector(x, y);
    }
}

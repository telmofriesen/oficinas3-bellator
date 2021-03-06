/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import processing.core.PApplet;
import processing.core.PVector;

/**
 * Ponto cartesiano XY
 *
 * @author stefan
 */
public class Ponto {

    public int x, y;

    public Ponto(int x, int y) {
        this.x = x;
        this.y = y;
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
    public Ponto copy() {
        return new Ponto(x, y);
    }

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

    /**
     * Retorna a distância euclideana entre este ponto e p2
     * @param p2
     * @return 
     */
    public float distEuclideana(Ponto p2) {
        return PApplet.sqrt(PApplet.sq(x - p2.x) + PApplet.sq(y - p2.y));
    }
}

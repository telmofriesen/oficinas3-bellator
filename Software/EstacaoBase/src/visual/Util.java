/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual;

import processing.core.PApplet;

/**
 *
 * @author stefan
 */
public class Util {

    public static Ponto getTransformedCoordinates(Ponto ponto, float viewPortCenterX, float viewPortCenterY, float escala) {
        //Calcula o ponto na interface (sem rotação) a partir do ponto real
        float x = (float) ((float) ponto.x() - viewPortCenterX) * escala;
        float y = (float) ((float) ponto.y() - viewPortCenterY) * escala;

//        //Calcula modulo e argumento do vetor
//        float abs = PApplet.sqrt(x * x + y * y);
//        float arg_old;
//        if (x == 0) {
//            arg_old = PApplet.HALF_PI;
//        } else {
//            arg_old = PApplet.atan(y / x);
//        }
////        if (y <= 0 && x <= 0) {
////            arg_old = -arg_old;
////        }
//        float arg_new = arg_old + angulo; //Efetua a rotacao do vetor (muda o argumento)
//
//        //Calcula os novos x e y.
//        x = abs * PApplet.cos(arg_new);
//        y = abs * PApplet.sin(arg_new);
        return new Ponto(PApplet.floor(x), PApplet.floor(y));
    }
}

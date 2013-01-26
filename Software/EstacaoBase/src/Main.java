/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

import controle.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import javax.swing.*;
import processing.core.PApplet;
import visual.*;

/**
 * Programa para testes do plotador de mapa do Bellator.
 *
 * @author stefan
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        JFrame f = new JFrame("Bellator [mapa]");
        f.setResizable(true);

        //
        //Inicializa Obstaculos
        //
        Obstaculos obstaculos = new Obstaculos();
        ObstaculosDrawable obstaculosDrawable = new ObstaculosDrawable(obstaculos);

        //
        //Inicializa Obstaculos2
        //
//        Obstaculos obstaculos2 = new Obstaculos();
//        ObstaculosDrawable obstaculosDrawable2 = new ObstaculosDrawable(obstaculos2);
//        obstaculosDrawable2.getPropriedades().setCorPontos(new Color(0, 0, 255));

        //
        //Iniciliza o Robo
        //
        Robo robo = new Robo(400, 500, new Ponto(-200, 200));
        robo.addSensorIR(new SensorIR(new Ponto(200, -200), PApplet.radians(-60), 200, 1500));
//        robo.addSensorIR(new SensorIR(new Ponto(20, -10), PApplet.radians(-30), 20, 150));
        robo.addSensorIR(new SensorIR(new Ponto(200, 0), PApplet.radians(0), 200, 1500));
//        robo.addSensorIR(new SensorIR(new Ponto(20, 10), PApplet.radians(30), 20, 150));
        robo.addSensorIR(new SensorIR(new Ponto(200, 200), PApplet.radians(60), 200, 1500));
        
        Mapa mapa = new Mapa(robo, obstaculos);

        RoboDrawable roboDrawable = new RoboDrawable(robo);

        //
        //Iniciliza o Robo2
        //
//        Robo robo2 = new Robo(40, 50, new Ponto(-20, 20));
//        robo2.addSensorIR(new SensorIR(new Ponto(20, -20), PApplet.radians(-60), 20, 150));
////        robo.addSensorIR(new SensorIR(new Ponto(20, -10), PApplet.radians(-30), 20, 150));
//        robo2.addSensorIR(new SensorIR(new Ponto(20, 0), PApplet.radians(0), 20, 150));
////        robo.addSensorIR(new SensorIR(new Ponto(20, 10), PApplet.radians(30), 20, 150));
//        robo2.addSensorIR(new SensorIR(new Ponto(20, 20), PApplet.radians(60), 20, 150));
//
//
//        RoboDrawable roboDrawable2 = new RoboDrawable(robo2);
//        roboDrawable2.getRobo_trilha().getPropriedades().setCorPontos(new Color(0, 0, 255));
//        roboDrawable2.getRobo_trilha().getPropriedades().setCorTrilha(new Color(0, 0, 255));

//        RoboTrilhaDrawable roboTrilhaDrawable = new RoboTrilhaDrawable(robo);


        //
        //Inicializa o Viewer2D e Adiciona os Drawable2D a ele
        //
        Viewer2D viewer2D = new Viewer2D();
        viewer2D.addDrawable2D(obstaculosDrawable);
        viewer2D.addMouseListener2D(obstaculosDrawable);
//        viewer2D.addDrawable2D(obstaculosDrawable2);
        viewer2D.addDrawable2D(roboDrawable);
//        viewer2D.addDrawable2D(roboDrawable2);
        viewer2D.addDrawable2D(new EscalaDrawable());
        viewer2D.addDrawable2D(new GridDrawable());
        viewer2D.init();

        //
        //Inicializa a janela principal
        //
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(viewer2D, BorderLayout.CENTER);

        f.add(panel);
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        f.setSize(800, 600);
        f.setVisible(true);

        //
        // Inicializa o controle dos sensores
        //
        ControleSensores controleSensores = new ControleSensores(robo, obstaculos); //Sem filtragem de ruidos por filtro de Kalman
//        ControleSensoresKalman controleSensores = new ControleSensoresKalman(robo, obstaculos, 0.1f, 0.05f, 5f); //Com filtragem por filtro de Kalman
        //
        // Insere leituras de teste
        //
        try {
//Outros testes
            controleSensores.novaLeituraSensores(0, PApplet.radians(0), new float[]{300, 0, 300}, 1000);
            controleSensores.novaLeituraSensores(1, PApplet.radians(10), new float[]{300, 0, 300}, 2000);
            controleSensores.novaLeituraSensores(0, PApplet.radians(10), new float[]{300, 0, 300}, 3000);
            controleSensores.novaLeituraSensores(0, PApplet.radians(-10), new float[]{300, 0, 300}, 4000);
            controleSensores.novaLeituraSensores(-0.5f, PApplet.radians(-20), new float[]{300, 0, 300}, 5000);
            controleSensores.novaLeituraSensores(0, PApplet.radians(-25), new float[]{300, 0, 300}, 6000);
            controleSensores.novaLeituraSensores(0, PApplet.radians(0), new float[]{300, 0, 300}, 7000);
            
//            controleSensores.novaLeituraSensores(1, PApplet.radians(45), new float[]{30, 0, 30}, 2500);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(45), new float[]{30, 0, 30}, 2600);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(45), new float[]{30, 0, 30}, 2700);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(45), new float[]{30, 0, 30}, 2800);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(45), new float[]{40, 0, 40}, 2900);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(45), new float[]{50, 50, 50}, 3000);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{0, 0, 0}, 3100);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{0, 0, 0}, 3200);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{0, 0, 0}, 3300);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{0, 0, 0}, 4000);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{50, 0, 50}, 4200);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{50, 0, 50}, 4500);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(20), new float[]{50, 0, 50}, 4700);

//            for (int i = 0; i < 15; i++) {
//                //Insere ruidos aleatorios para testar o fitro de Kalman
//                float ruido = viewer2D.random(-0.1f, 0.1f);
//                float ruido_IR = viewer2D.random(-5f, 5f);
//                float ruido_IR2 = viewer2D.random(-5f, 5f);
//                controleSensores.novaLeituraSensores(1 + ruido, PApplet.radians(0), new float[]{50 + ruido_IR, 0, 50 + ruido_IR2}, 1000 + i * 100);
//            }
//            for (int i = 0; i < 15; i++) {
//                float ruido = viewer2D.random(-0.1f, 0.1f);
//                float ruido_IR = viewer2D.random(-5f, 5f);
//                float ruido_IR2 = viewer2D.random(-5f, 5f);
//                controleSensores.novaLeituraSensores(1 + ruido, PApplet.radians(0), new float[]{100 + ruido_IR, 0, 100 + ruido_IR2}, 2600 + i * 100);
//            }
//            System.out.println(robo.pontosToString());
//            System.out.println(obstaculos.pontosToString());
            
//            mapa.save("save3.txt");
//            mapa.load("save3.txt");

        } catch (Exception ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        viewer2D.redraw();
    }
}

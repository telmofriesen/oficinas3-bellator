/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import processing.core.PApplet;
import visual.Drawable2D;
import visual.Ponto;

/**
 * Classe que contém o mapa. Ela armazena somente as informações essenciais, que consistem no robô e os obstáculos detectados por ele.
 *
 * @author stefan
 */
public class Mapa {

    private Robo robo;
    private Obstaculos obstaculos;

    public Mapa(Robo robo, Obstaculos obstaculos) {
        this.robo = robo;
        this.obstaculos = obstaculos;
    }

    public void save(String filename) throws IOException {
        BufferedWriter out;
        out = new BufferedWriter(new FileWriter(filename));
        out.write(robo.infoToString() + "\n");
        out.write(robo.sensoresToString() + "\n");
        out.write(robo.pontosToString() + "\n");
        out.write(obstaculos.pontosToString());
        out.close();
    }

    public void load(String filename) throws FileNotFoundException, IOException, Exception {

        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        int sequencia = 0;
        while ((line = br.readLine()) != null) {
            // process the line.
            if (line.equals("")) {
                sequencia++;
                continue;
            }
            if (sequencia == 0) { //Informacoes do robo
                String[] str_split = line.split(" ");
                int largura = Integer.parseInt(str_split[0]);
                int comprimento = Integer.parseInt(str_split[1]);
                Ponto centro = new Ponto(Integer.parseInt(str_split[2]), Integer.parseInt(str_split[3]));
                robo.reset();
                robo.setLargura(largura);
                robo.setComprimento(comprimento);
                robo.setCentroMovimento(centro);
            } else if (sequencia == 1) { //sensores IR
                String[] str_split = line.split(" ");
                int x = Integer.parseInt(str_split[0]);
                int y = Integer.parseInt(str_split[1]);
                float angulo = Float.parseFloat(str_split[2]);
                int min_detec = Integer.parseInt(str_split[3]);
                int max_detec = Integer.parseInt(str_split[4]);
                robo.addSensorIR(new SensorIR(new Ponto(x, y), angulo, min_detec, max_detec));
            } else if (sequencia == 2) { //Posicoes que o robo percorreu
                String[] str_split = line.split(" ");
                int x = Integer.parseInt(str_split[0]);
                int y = Integer.parseInt(str_split[1]);
                float angulo = Float.parseFloat(str_split[2]);
                int timestamp = Integer.parseInt(str_split[3]);
                robo.addPosicao(new PosInfo(new Ponto(x, y), angulo, timestamp));
            } else if (sequencia == 3) { //Obstaculos detectados
                String[] str_split = line.split(" ");
                int x = Integer.parseInt(str_split[0]);
                int y = Integer.parseInt(str_split[1]);
                obstaculos.addPonto2D(new Ponto(x, y));
            }
        }
        br.close();
    }

    public Robo getRobo() {
        return robo;
    }

    public void setRobo(Robo robo) {
        this.robo = robo;
    }

    public Obstaculos getObstaculos() {
        return obstaculos;
    }

    public void setObstaculos(Obstaculos obstaculos) {
        this.obstaculos = obstaculos;
    }
}

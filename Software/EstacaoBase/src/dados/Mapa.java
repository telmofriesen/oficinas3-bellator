package dados;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
 * Classe que representa o mapa.
 * Somente as informações essenciais são armazenadas. Elas consistem em:
 * - Robô
 * - Obstáculos detectados
 *
 * @author stefan
 */
public class Mapa {

    private final Robo robo;
    private final Robo robo_aux;
    private final Obstaculos obstaculos;

    public Mapa(Robo robo, Obstaculos obstaculos, Robo robo_aux) {
        this.robo = robo;
        this.obstaculos = obstaculos;
        this.robo_aux = robo_aux;
    }

    /**
     * Salva o mapa em um arquivo (texto). Se o arquivo já existir ele será substituído.
     *
     * @param filename Nome do arquivo para salvar.
     * @throws IOException
     */
    public void save(String filename) throws IOException {
        BufferedWriter out;
        out = new BufferedWriter(new FileWriter(filename));
        out.write(robo.infoToString());
        out.write("\n");
        out.write(robo.sensoresToString());
        out.write("\n");
        out.write(robo.pontosToString());
        out.write("\n");
        out.write(obstaculos.pontosToString());
        out.close();
    }

    /**
     * Carrega o mapa a partir de um arquivo (texto).
     * TODO verificar se o formato do arquivo está correto.
     *
     * @param filename O nome do arquivo.
     * @throws FileNotFoundException
     * @throws IOException
     * @throws Exception
     */
    public void load(String filename) throws FileNotFoundException, IOException, Exception {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        synchronized (robo) {

            robo.reset(); //Limpa as informações do robo
            obstaculos.reset(); //Limpa as informações de obstaculos
            robo_aux.reset();
            //Linha atual lida do arquivo
            String line;
            //Grupo de dados atualmente sendo lido 
            int sequencia = 0;
            //Indica se uma trilha adicional está presente no arquivo.
            boolean trilhaAdicional = false;
            //Loop de leitura do arquivo
            while ((line = br.readLine()) != null) {
                //Processa a linha
                if (line.equals("")) {
                    //Caso seja lida uma linha em branco, incrementa o numero de sequencia
                    sequencia++;
                    continue;
                } else if (line.equals("/")) {
                    trilhaAdicional = true;
                    continue;
                }
                if (sequencia == 0) { //Informacoes do robo
                    String[] str_split = line.split(" ");
                    int largura = Integer.parseInt(str_split[0]);
                    int comprimento = Integer.parseInt(str_split[1]);
                    Ponto centro = new Ponto(Integer.parseInt(str_split[2]), Integer.parseInt(str_split[3]));
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
                    long timestamp = Long.parseLong(str_split[3]);
                    PosInfo novaPosicao = new PosInfo(new Ponto(x, y), angulo, timestamp);
                    if (trilhaAdicional) {
                        robo.novaTrilha(novaPosicao);
                        trilhaAdicional = false;
                    } else {
                        robo.addPosicao(novaPosicao);
                    }
                } else if (sequencia == 3) { //Obstaculos detectados
                    String[] str_split = line.split(" ");
                    int x = Integer.parseInt(str_split[0]);
                    int y = Integer.parseInt(str_split[1]);
                    obstaculos.addPonto2D(new Ponto(x, y));
                }
            }
        }
        br.close();
    }

    public Robo getRobo() {
        return robo;
    }

//    public void setRobo(Robo robo) {
//        this.robo = robo;
//    }
    public Obstaculos getObstaculos() {
        return obstaculos;
    }

//    public void setObstaculos(Obstaculos obstaculos) {
//        this.obstaculos = obstaculos;
//    }
    public Robo getRobo_aux() {
        return robo_aux;
    }

    public void reset() {
        robo.reset();
        obstaculos.reset();
        robo_aux.reset();
    }
}

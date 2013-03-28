/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

import java.util.ArrayList;
import processing.core.PApplet;
import visual.Ponto;

/**
 * Robô Bellator. É representado por um retângulo com largura, comprimento e
 * centro de movimento (ponto central entre as duas rodas).
 *
 * @author stefan
 */
public class Robo {

    private int largura, comprimento;
    //O ponto do robo em que se encontra o centro entre as duas rodas. Considera-se a origem como sendo o canto frontal esquerdo.
    private Ponto centroMovimento;
    private ArrayList<PosInfo> posInfos = new ArrayList<PosInfo>();
    private ArrayList<SensorIR> sensoresIR = new ArrayList<SensorIR>();
    //TODO armazenar valores da velocidade na memoria ao inves de calcular toda vez.
    private float velocidadeAtual = 0;
    private float velocidadeAngularAtual = 0;

    /**
     *
     * @param largura Largura do robô (tamanho no eixo Y) em milímetros.
     * @param comprimento Comprimento do robô (tamanho no eixo X) em milímetros.
     * @param centroMovimento Ponto central entre as duas rodas do robô. As
     * coordenadas são relativas ao canto frontal esquerdo do robô, com ele
     * virado para a direita na tela. O eixo X é negativo para a esquerda e o Y
     * positivo para baixo.
     */
    public Robo(int largura, int comprimento, Ponto centroMovimento) {
        this.largura = largura;
        this.comprimento = comprimento;
        this.centroMovimento = centroMovimento;
        posInfos.add(new PosInfo(new Ponto(0, 0), 0, 0)); //Adiciona a posição inicial
    }

    public synchronized float getVelocidadeAtual() {
        return velocidadeAtual;
    }

    public synchronized void setVelocidadeAtual(float velocidadeAtual) {
        this.velocidadeAtual = velocidadeAtual;
    }

    public synchronized float getVelocidadeAngularAtual() {
        return velocidadeAngularAtual;
    }

    public synchronized void setVelocidadeAngularAtual(float velocidadeAngularAtual) {
        this.velocidadeAngularAtual = velocidadeAngularAtual;
    }

    /**
     * Retorna a velocidade atual do robô, a partir dos dois últimos pontos de
     * posição;
     *
     * @return Velocidade atual do robô em m/s. Se houver apenas um ponto de
     * posição armazenado, retorna 0.
     * @deprecated
     */
    public synchronized float getVelocidade() {
        if (posInfos.size() < 2)
            return 0;
        Ponto p2 = posInfos.get(posInfos.size() - 1).getPonto(); //Ultima posicao
        Ponto p1 = posInfos.get(posInfos.size() - 2).getPonto(); //Penultima posicao
        //Norma do vetor do ultimo deslocamento
        float delta_x = PApplet.sqrt(((float) p2.x - (float) p1.x) * ((float) p2.x - (float) p1.x)
                                     + ((float) p2.y - (float) p1.y) * ((float) p2.y - (float) p1.y)); // mm
        //Intervalo de tempo do ultimo deslocamento
        float delta_t = posInfos.get(posInfos.size() - 1).getTimestamp() - posInfos.get(posInfos.size() - 2).getTimestamp(); // ms
        return delta_x / delta_t; // m/s
    }

    /**
     * Retorna a velocidade angular atual do robô, a partir dos dois ultimos
     * pontos de posição;
     *
     * @return Velocidade angular atual do robô em rad/s. Se houver apenas um
     * ponto de posição armazenado, retorna 0.
     * @deprecated
     */
    public synchronized float getVelocidadeAngular() {
        if (posInfos.size() < 2)
            return 0;
        float delta_theta = posInfos.get(posInfos.size() - 1).getAngulo() - posInfos.get(posInfos.size() - 2).getAngulo(); //rad
        //Intervalo de tempo do ultimo deslocamento
        float delta_t = posInfos.get(posInfos.size() - 1).getTimestamp() - posInfos.get(posInfos.size() - 2).getTimestamp(); // ms

        return delta_theta / (float) delta_t * (float) 1000; // rad/s
    }

    /**
     * Adiciona a próxima posição do robô.
     *
     * @param pos Posição a ser adicionada.
     */
    public synchronized void addPosicao(PosInfo pos) {
        posInfos.ensureCapacity(posInfos.size() + 20 - posInfos.size() % 20); //Aumenta a capacidade do ArrayList de 20 em 20 pontos.
        posInfos.add(pos);
    }

    public synchronized void removePosicao(int i) {
        posInfos.remove(i);
    }

    public synchronized PosInfo getPosicao(int i) {
        return posInfos.get(i);
    }

    public synchronized PosInfo getUltimaPosicao() {
        return posInfos.get(posInfos.size() - 1);
    }

    public synchronized int getNumPosicoes() {
        return posInfos.size();
    }

    public synchronized void addSensorIR(SensorIR sensor) {
        sensoresIR.add(sensor);
    }

    public synchronized SensorIR getSensor(int i) {
        return sensoresIR.get(i);
    }

    public synchronized int getNumSensoresIR() {
        return sensoresIR.size();
    }

    public synchronized Ponto getCentroMovimento() {
        return centroMovimento;
    }

    public synchronized int getComprimento() {
        return comprimento;
    }

    public synchronized int getLargura() {
        return largura;
    }

    public synchronized void setLargura(int largura) {
        this.largura = largura;
    }

    public synchronized void setComprimento(int comprimento) {
        this.comprimento = comprimento;
    }

    public synchronized void setCentroMovimento(Ponto centroMovimento) {
        this.centroMovimento = centroMovimento;
    }

    public String infoToString() {
        return String.format("%d %d %d %d\n",
                             largura,
                             comprimento,
                             centroMovimento.x,
                             centroMovimento.y);
    }

    public String sensoresToString() {
        String str = "";
        for (int i = 0; i < sensoresIR.size(); i++) {
            str = str + String.format("%d %d %f %d %d\n",
                                      sensoresIR.get(i).getPosicaoNoRobo().x,
                                      sensoresIR.get(i).getPosicaoNoRobo().y,
                                      sensoresIR.get(i).getAngulo(),
                                      sensoresIR.get(i).getMin_detec(),
                                      sensoresIR.get(i).getMax_detec());
        }
        return str;
    }

    public String pontosToString() {
        String str = "";
        for (int i = 0; i < posInfos.size(); i++) {
            str = str + String.format("%d %d %f %d\n",
                                      posInfos.get(i).getPonto().x,
                                      posInfos.get(i).getPonto().y,
                                      posInfos.get(i).getAngulo(),
                                      posInfos.get(i).getTimestamp());
        }
        return str;
    }

    public synchronized void clearPosInfos() {
        posInfos.clear();
        posInfos.add(new PosInfo(new Ponto(0, 0), 0, 0)); //Adiciona a posição inicial
    }

    public synchronized void reset() {
        posInfos.clear();
        sensoresIR.clear();
        largura = 0;
        comprimento = 0;
        centroMovimento = null;
        posInfos.add(new PosInfo(new Ponto(0, 0), 0, 0)); //Adiciona a posição inicial
    }
}
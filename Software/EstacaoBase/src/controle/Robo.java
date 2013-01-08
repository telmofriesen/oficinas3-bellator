/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import java.util.ArrayList;
import processing.core.PApplet;
import visual.Ponto;

/**
 * Robô Bellator. É representado por um retângulo com largura, comprimento e centro de movimento (ponto central entre as duas rodas).
 * @author stefan
 */
public class Robo {

    private int largura, comprimento;
    //O ponto do robo em que se encontra o centro entre as duas rodas. Considera-se a origem como sendo o canto frontal esquerdo.
    private Ponto centroMovimento;
    private ArrayList<PosInfo> posInfos = new ArrayList<PosInfo>();
    private ArrayList<SensorIR> sensoresIR = new ArrayList<SensorIR>();

    /**
     * 
     * @param largura Largura do robô (tamanho no eixo Y).
     * @param comprimento Comprimento do robô (tamanho no eixo X).
     * @param centroMovimento Ponto central entre as duas rodas do robô. As coordenadas são relativas ao canto frontal esquerdo do robô, com ele virado para a direita na tela. O eixo X é negativo para a esquerda e o Y positivo para baixo. 
     */
    public Robo(int largura, int comprimento, Ponto centroMovimento) {
        this.largura = largura;
        this.comprimento = comprimento;
        this.centroMovimento = centroMovimento;
        posInfos.add(new PosInfo(new Ponto(0, 0), 0, 0)); //Adiciona a posição inicial
    }

    /**
     * Retorna a velocidade atual do robô, a partir dos dois últimos pontos de posição;
     * @return Velocidade atual do robô
     */
    public float getVelocidade() {
        if (posInfos.size() < 2)
            return 0;
        Ponto p2 = posInfos.get(posInfos.size() - 1).getPonto(); //Ultima posicao
        Ponto p1 = posInfos.get(posInfos.size() - 2).getPonto(); //Penultima posicao
        //Norma do vetor do ultimo deslocamento
        float delta_x = PApplet.sqrt(((float) p2.x - (float) p1.x) * ((float) p2.x - (float) p1.x)
                                     + ((float) p2.y - (float) p1.y) * ((float) p2.y - (float) p1.y)); // cm
        //Intervalo de tempo do ultimo deslocamento
        float delta_t = posInfos.get(posInfos.size() - 1).getTimestamp() - posInfos.get(posInfos.size() - 2).getTimestamp(); // ms
        return 10 * delta_x / delta_t; // m/s
    }

    /**
     * Adiciona a próxima posição do robô.
     * @param pos Posição a ser adicionada.
     */
    public synchronized void addPosicao(PosInfo pos) {
        posInfos.ensureCapacity(posInfos.size() + 20 - posInfos.size() % 20); //Aumenta a capacidade do ArrayList de 20 em 20 pontos.
        posInfos.add(pos);
    }
    
    public synchronized void removePosicao(int i){
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

    public Ponto getCentroMovimento() {
        return centroMovimento;
    }

    public int getComprimento() {
        return comprimento;
    }

    public int getLargura() {
        return largura;
    }
}

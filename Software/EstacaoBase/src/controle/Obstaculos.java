package controle;

import java.util.ArrayList;
import visual.Ponto;

/**
 * Classe que contém os obstáculos detectados pelo robô.
 * @author stefan
 */
public class Obstaculos {

    private ArrayList<Ponto> obstaculosPos = new ArrayList<Ponto>();

    /**
     *
     * @param ponto
     */
    public synchronized void addPonto2D(Ponto ponto) {
        obstaculosPos.ensureCapacity(obstaculosPos.size() + 20 - obstaculosPos.size() % 20); //Aumenta a capacidade do ArrayList de 20 em 20 pontos.
        obstaculosPos.add(ponto);
    }

    public synchronized void removePonto2D(int i) {
        obstaculosPos.remove(i);
    }

    public synchronized Ponto getPonto2D(int i) {
        return obstaculosPos.get(i);
    }

    public synchronized int getNumPontos(){
        return obstaculosPos.size();
    }

    public String pontosToString() {
        String str = "";
        for (int i = 0; i < obstaculosPos.size(); i++) {
            str = str + String.format("%d %d\n", obstaculosPos.get(i).x, obstaculosPos.get(i).y);
        }
        return str;
    }
}

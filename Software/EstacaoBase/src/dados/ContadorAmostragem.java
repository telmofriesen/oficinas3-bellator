/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

/**
 * Implementa um contador que calcula a taxa de amostragem de acordo com as amostras recebidas.
 * A atualização da taxa de amostragem é feita na chamada da função novaAmostra(), quando uma amostra é recebida.
 *
 * @author stefan
 */
public class ContadorAmostragem {

    private long time_window_start = -1; //Inicio da janela de tempo atual (ms)
    private int time_window_read_count = 0; //Numero de leituras na janela de tempo atual
    private float sample_rate = 0; //leituras por segundo atual
    private int time_window = 2000; //Tamanho da janela. Número de milissegundos entre cada conjunto de contagem.
    private int time_window_min = 500; //Tamanho mínimo da janela de tempo.
    private int time_window_max = 5000; //Tamanho máximo da janela de tempo.
    private int time_window_step_up = 500; //Step para cima da janela de tempo.
    private int time_window_step_down = 1000; //Step para baixo da janela de tempo.

    public ContadorAmostragem() {
    }

    public ContadorAmostragem(int time_window, int time_window_min, int time_window_max) {
        this.time_window = time_window;
        this.time_window_min = time_window_min;
        this.time_window_max = time_window_max;
    }

    /**
     * Informa o recebimento de uma nova amostra.
     *
     * @param timestamp Horario do recebimento da amostra.
     */
    public synchronized void novaAmostra(long timestamp) {
        if (time_window_start == -1) { //Executado na primeira amostra
            time_window_start = timestamp;
        }
        time_window_read_count++;
        if (timestamp - time_window_start > time_window) {
            if (time_window_read_count == 1) { //Aumenta a janela de tempo se houver apenas uma leitura.
                time_window = Math.min(time_window + time_window_step_up, time_window_max); //Máximo de 5 segundos
            } else {
                if (time_window_read_count > 20) { //Reduz a janela de tempo se houverem muitas leituras
                    time_window = Math.max(time_window - time_window_step_down, time_window_min); //Mínimo de 0,5 segundos
                }
                //Atualiza a taxa de transferencia
                sample_rate = (float) time_window_read_count / (float) (timestamp - time_window_start) * 1000; //leituras por segundo
                //Inicia nova janela de tempo
                time_window_start = timestamp;
                time_window_read_count = 0;
            }
        }
    }

    public synchronized float getSample_rate() {
        return sample_rate;
    }
}

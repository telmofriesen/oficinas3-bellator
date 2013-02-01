/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

/**
 *
 * @author stefan
 */
public class TimestampException extends Exception {

    private long timestamp_recebido;
    private long ultimo_timestamp;

    public TimestampException(long timestamp_recebido, long ultimo_timestamp) {
        super(String.format("O timestamp (%d) é menor do que o da última posição (%d).",
                            timestamp_recebido, ultimo_timestamp));
        this.timestamp_recebido = timestamp_recebido;
        this.ultimo_timestamp = ultimo_timestamp;
    }

    public TimestampException(long timestamp_recebido, long ultimo_timestamp, String string) {
        super(string);
        this.timestamp_recebido = timestamp_recebido;
        this.ultimo_timestamp = ultimo_timestamp;
    }

    public long getTimestamp_recebido() {
        return timestamp_recebido;
    }

    public long getUltimo_timestamp() {
        return ultimo_timestamp;
    }
}
package comm;

/**
 * Mensagem a ser enviada por um Sender. 
 * @author stefan
 */
public class SenderMessage {
    private String message;
    private boolean flushBuffer;

    /**
     * @param message Mensagem a ser enviada
     * @param flushBuffer Indica se o buffer de envio deve sofrer um flush ou não.
     */
    public SenderMessage(String message, boolean flushBuffer) {
        this.message = message;
        this.flushBuffer = flushBuffer;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFlushBuffer() {
        return flushBuffer;
    }
        
}

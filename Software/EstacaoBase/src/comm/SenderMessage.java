/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

/**
 *
 * @author stefan
 */
public class SenderMessage {
    private String message;
    private boolean flush_buffer;

    public SenderMessage(String message, boolean flush_buffer) {
        this.message = message;
        this.flush_buffer = flush_buffer;
    }

    public String getMessage() {
        return message;
    }

    public boolean isFlush_buffer() {
        return flush_buffer;
    }
        
}

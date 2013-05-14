/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robo;

/**
 *
 * @author stefan
 */
public class SerialMessage {

    public final byte[] buffer;
    public final int numBytes;

    public SerialMessage(byte[] buffer, int numBytes) {
        this.buffer = buffer;
        this.numBytes = numBytes;
    }
}

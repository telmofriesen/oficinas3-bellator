/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robo.gerenciamento;

import dados.EnginesSpeed;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;
import robo.Main;

/**
 *
 * @author stefan
 */
public class EnginesManager {

//    private float leftSpeed = 0;
//    private float rightSpeed = 0;
    private EnginesSpeed enginesSpeed = new EnginesSpeed(0.0f, 0.0f);
    private Main main;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();

    public EnginesManager(Main main) {
        this.main = main;
    }

    public synchronized EnginesSpeed getEnginesSpeed() {
        return enginesSpeed;
    }

    public synchronized void setEnginesSpeed(EnginesSpeed enginesSpeed) {
        this.enginesSpeed = enginesSpeed;
        System.out.printf("[EnginesManager] EnginesSpeed: %.2f %.2f\n", enginesSpeed.leftSpeed, enginesSpeed.rightSpeed);
        byte[] message = new byte[5];
        message[0] = (byte) 0xB0;
        message[1] = (byte) (Math.abs((int) Math.floor((float) 127 * enginesSpeed.leftSpeed))); //Calcula os 7 bits menos significativos
        if (enginesSpeed.leftSpeed > 0) { //Se o movimento for para frente, muda o bit mais significativo para 1
            message[1] = (byte) ((byte) message[1] | (byte) 0x80);
        }
        message[2] = (byte) (Math.abs((int) Math.floor((float) 127 * enginesSpeed.rightSpeed)));
        if (enginesSpeed.rightSpeed > 0) { //Se o movimento for para frente, muda o bit mais significativo para 1
            message[2] = (byte) ((byte) message[2] | (byte) 0x80);
        }

        int sum = 0;
        for (int i = 0; i < message.length - 2; i++) {
            sum = (sum + (short)(message[i]) & 0x00FF ) % 65536;
        }
        short sum_short = (short) sum;
        message[message.length - 2] = (byte) ((sum_short >> 8) & 0XFF);
        message[message.length - 1] = (byte) ((sum_short) & 0XFF);

        System.out.print("[SERIAL] enviando mensagem: ");
        for (int i = 0; i < message.length; i++) {
            System.out.printf("%X ", message[i]);
        }

        //TODO descomentar a linha seguinte para que os comandos sejam mandados à placa de baixo nivel via serial
//        main.getSerialCommunicator().sendMessage(message);
        fireChangeEvent();
    }

    public synchronized void setEnginesSpeed(float left, float right) {
        setEnginesSpeed(new EnginesSpeed(left, right));
    }

//    public synchronized float getLeftSpeed() {
//        return leftSpeed;
//    }
//
//    public synchronized void setLeftSpeed(float leftSpeed) {
//        this.leftSpeed = leftSpeed;
//    }
//
//    public synchronized float getRightSpeed() {
//        return rightSpeed;
//    }
//
//    public synchronized void setRightSpeed(float rightSpeed) {
//        this.rightSpeed = rightSpeed;
//    }
    public void addMyChangeListener(MyChangeListener l) {
        this.listeners.add(l);
        fireChangeEvent();
    }

    public void removeMyChangeListener(MyChangeListener l) {
        this.listeners.remove(l);
    }

    // Event firing method.  Called internally by other class methods.
    protected void fireChangeEvent() {
        MyChangeEvent evt = new MyChangeEvent(this);

        for (MyChangeListener l : listeners) {
            l.changeEventReceived(evt);
        }
    }
}

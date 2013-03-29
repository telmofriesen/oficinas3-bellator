/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package robo.gerenciamento;

import dados.EnginesSpeed;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author stefan
 */
public class EnginesManager {

//    private float leftSpeed = 0;
//    private float rightSpeed = 0;
    private EnginesSpeed enginesSpeed = new EnginesSpeed(0.0f, 0.0f);
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();

    public synchronized EnginesSpeed getEnginesSpeed() {
        return enginesSpeed;
    }

    public synchronized void setEnginesSpeed(EnginesSpeed enginesSpeed) {
        this.enginesSpeed = enginesSpeed;
        System.out.printf("[EnginesManager] EnginesSpeed: %.2f %.2f\n", enginesSpeed.leftSpeed, enginesSpeed.rightSpeed);
        fireChangeEvent();
    }
    
    public synchronized void setEnginesSpeed(float left, float right){
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

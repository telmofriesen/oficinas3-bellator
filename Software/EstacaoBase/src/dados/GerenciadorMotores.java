/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 *
 * @author stefan
 */
public class GerenciadorMotores implements MyChangeListener {

    private EnginesSpeed currentEngineSpeed = new EnginesSpeed(0, 0);
    private float currentMultiplier = 1;
    private int movementType = 1;
    public static final int STOP = 1,
            FORWARD = 2,
            FORWARD_LEFT = 3,
            FORWARD_RIGHT = 4,
            BACKWARD = 5,
            BACKWARD_LEFT = 6,
            BACKWARD_RIGHT = 7,
            ROTATE_LEFT = 8,
            ROTATE_RIGHT = 9;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();

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

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof MovementKeyboardListener) {
            MovementKeyboardListener k = (MovementKeyboardListener) evt.getSource();
            int movType = k.getMovementType();
            synchronized (this) {
                if (movType != movementType) {
                    this.movementType = k.getMovementType();
                }
            }
            fireChangeEvent();
//            switch (k.getMovementType()) {
//                case ControleMotores.STOP:
//                    break;
//                case ControleMotores.FORWARD:
//                    break;
//                case ControleMotores.FORWARD_LEFT:
//                    break;
//                case ControleMotores.FORWARD_RIGHT:
//                    break;
//                case ControleMotores.BACKWARD:
//                    break;
//                case ControleMotores.BACKWARD_LEFT:
//                    break;
//                case ControleMotores.BACKWARD_RIGHT:
//                    break;
//                case ControleMotores.ROTATE_LEFT:
//                    break;
//                case ControleMotores.ROTATE_RIGHT:
//            }
        }
    }

    public synchronized int getMovementType() {
        return movementType;
    }

    public synchronized EnginesSpeed getNewEngineSpeed() {
        return getNewEngineSpeed(movementType, currentMultiplier);
    }

    public EnginesSpeed getNewEngineSpeed(int movementType) {
        switch (movementType) {
            case GerenciadorMotores.STOP:
                return new EnginesSpeed(0, 0);
            case GerenciadorMotores.FORWARD:
                return new EnginesSpeed(1, 1);
            case GerenciadorMotores.FORWARD_LEFT:
                return new EnginesSpeed(0.5f, 1);
            case GerenciadorMotores.FORWARD_RIGHT:
                return new EnginesSpeed(1, 0.5f);
            case GerenciadorMotores.BACKWARD:
                return new EnginesSpeed(-1, -1);
            case GerenciadorMotores.BACKWARD_LEFT:
                return new EnginesSpeed(-0.7f, -1);
            case GerenciadorMotores.BACKWARD_RIGHT:
                return new EnginesSpeed(-1, -0.7f);
            case GerenciadorMotores.ROTATE_LEFT:
                return new EnginesSpeed(-0.7f, 0.7f);
            case GerenciadorMotores.ROTATE_RIGHT:
                return new EnginesSpeed(0.7f, -0.7f);
            default:
                return new EnginesSpeed(0, 0);
        }
    }

    public EnginesSpeed getNewEngineSpeed(int movementType, float multiplier) {
        EnginesSpeed speed = getNewEngineSpeed(movementType);
        return new EnginesSpeed(speed.leftSpeed * multiplier, speed.rightSpeed * multiplier);
    }

    public synchronized EnginesSpeed getCurrentEngineSpeed() {
        return currentEngineSpeed;
    }

    public synchronized void setCurrentEngineSpeed(EnginesSpeed currentEngineSpeed) {
        this.currentEngineSpeed = currentEngineSpeed;
        fireChangeEvent();
    }

    public synchronized float getCurrentMultiplier() {
        return currentMultiplier;
    }

    public synchronized void setCurrentMultiplier(float currentMultiplier) {
        this.currentMultiplier = currentMultiplier;
        fireChangeEvent();
    }
}

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
public class ControleMotores implements MyChangeListener {

    private EnginesSpeed currentEngineSpeed = new EnginesSpeed(0, 0);
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
        return getNewEngineSpeed(movementType);
    }

    public EnginesSpeed getNewEngineSpeed(int movementType) {
        switch (movementType) {
            case ControleMotores.STOP:
                return new EnginesSpeed(0, 0);
            case ControleMotores.FORWARD:
                return new EnginesSpeed(1, 1);
            case ControleMotores.FORWARD_LEFT:
                return new EnginesSpeed(0.5f, 1);
            case ControleMotores.FORWARD_RIGHT:
                return new EnginesSpeed(1, 0.5f);
            case ControleMotores.BACKWARD:
                return new EnginesSpeed(-1, -1);
            case ControleMotores.BACKWARD_LEFT:
                return new EnginesSpeed(-0.5f, -1);
            case ControleMotores.BACKWARD_RIGHT:
                return new EnginesSpeed(-1, -0.5f);
            case ControleMotores.ROTATE_LEFT:
                return new EnginesSpeed(-1, 1);
            case ControleMotores.ROTATE_RIGHT:
                return new EnginesSpeed(1, -1);
            default:
                return new EnginesSpeed(0, 0);
        }
    }

    public synchronized EnginesSpeed getCurrentEngineSpeed() {
        return currentEngineSpeed;
    }

    public synchronized void setCurrentEngineSpeed(EnginesSpeed currentEngineSpeed) {
        this.currentEngineSpeed = currentEngineSpeed;
        fireChangeEvent();
    }
}

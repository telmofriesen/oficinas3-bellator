/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.concurrent.CopyOnWriteArrayList;
import processing.core.PApplet;
import visual.KeyboardListener;

/**
 *
 * @author stefan
 */
public class MovementKeyboardListener implements KeyboardListener {
    //Listeners de eventos da classe

    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();
    private int lastMovementType = -1;

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
    private boolean up = false, down = false, left = false, right = false;

    public int getMovementType() {
        if (up) {
            if (left) {
                return ControleMotores.FORWARD_LEFT;
            }
            if (right) {
                return ControleMotores.FORWARD_RIGHT;
            }
            return ControleMotores.FORWARD;
        }
        if (down) {
            if (left) {
                return ControleMotores.BACKWARD_LEFT;
            }
            if (right) {
                return ControleMotores.BACKWARD_RIGHT;
            }
            return ControleMotores.BACKWARD;
        }
        if (left) {
            return ControleMotores.ROTATE_LEFT;
        }
        if (right) {
            return ControleMotores.ROTATE_RIGHT;
        }
        return ControleMotores.STOP;
    }

    @Override
    public void keyPressed(int keyCode) {
        switch (keyCode) {
            case PApplet.UP:
                up = true;
                break;
            case PApplet.DOWN:
                down = true;
                break;
            case PApplet.LEFT:
                left = true;
                break;
            case PApplet.RIGHT:
                right = true;
                break;
        }
        int movType = getMovementType();
        if (movType != lastMovementType) {
            lastMovementType = movType;
            fireChangeEvent();
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        switch (keyCode) {
            case PApplet.UP:
                up = false;
                break;
            case PApplet.DOWN:
                down = false;
                break;
            case PApplet.LEFT:
                left = false;
                break;
            case PApplet.RIGHT:
                right = false;
                break;
        }
        int movType = getMovementType();
        if (movType != lastMovementType) {
            lastMovementType = movType;
            fireChangeEvent();
        }
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package dados;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;
import processing.core.PApplet;
import visual.KeyboardListener;

/**
 *
 * @author stefan
 */
public class MovementKeyboardListener implements KeyboardListener {
    //Listeners de eventos da classe

    private final long TIMER_DELAY = 50;
    private final CopyOnWriteArrayList<MyChangeListener> listeners = new CopyOnWriteArrayList<MyChangeListener>();
    private int lastMovementType = -1;
    private final Object lock;
    private boolean up = false, down = false, left = false, right = false;
    private boolean up_new = false, down_new = false, left_new = false, right_new = false;
    //Timer usado para corrigir o problema de repetição automática de teclas.
    //Quando uma tecla é segurada por algum tempo, o sistema operacional gera vários eventos repetidos de soltar e pressionar a tecla.
    private Timer timer;
    private TimerTask timerTask;

    public MovementKeyboardListener() {
        lock = new Object();

        timerTask = new TimerTask() {
            @Override
            public void run() {
                synchronized (lock) {
                    //Atualiza as teclas pressionadas se o timer expirar
                    up = up_new;
                    down = down_new;
                    left = left_new;
                    right = right_new;
                    int movType = getMovementType();
                    if (movType != lastMovementType) {
                        lastMovementType = movType;
                        System.out.printf("movType=%d\n", movType);
                        fireChangeEvent();
                    }
                }
            }
        };
        timer = new Timer("KeyboardListener Timer");
    }

    public int getMovementType() {
        if (up) {
            if (left) {
                return GerenciadorMotores.FORWARD_LEFT;
            }
            if (right) {
                return GerenciadorMotores.FORWARD_RIGHT;
            }
            return GerenciadorMotores.FORWARD;
        }
        if (down) {
            if (left) {
                return GerenciadorMotores.BACKWARD_LEFT;
            }
            if (right) {
                return GerenciadorMotores.BACKWARD_RIGHT;
            }
            return GerenciadorMotores.BACKWARD;
        }
        if (left) {
            return GerenciadorMotores.ROTATE_LEFT;
        }
        if (right) {
            return GerenciadorMotores.ROTATE_RIGHT;
        }
        return GerenciadorMotores.STOP;
    }

    @Override
    public void keyPressed(int keyCode) {
        synchronized (lock) {
            //Reagenda o evento do Timer 
            timerTask.cancel();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (lock) {
                        //Atualiza as teclas pressionadas se o timer expirar
                        up = up_new;
                        down = down_new;
                        left = left_new;
                        right = right_new;
                        int movType = getMovementType();
                        if (movType != lastMovementType) {
                            lastMovementType = movType;
//                            System.out.printf("movType=%d\n", movType);
                        }
                    }
                    fireChangeEvent();
                }
            };
            timer.schedule(timerTask, TIMER_DELAY);
            switch (keyCode) {
                case PApplet.UP:
                    up_new = true;
                    break;
                case PApplet.DOWN:
                    down_new = true;
                    break;
                case PApplet.LEFT:
                    left_new = true;
                    break;
                case PApplet.RIGHT:
                    right_new = true;
                    break;
            }
        }
    }

    @Override
    public void keyReleased(int keyCode) {
        synchronized (lock) {
            //Reagenda o evento do Timer 
            timerTask.cancel();
            timerTask = new TimerTask() {
                @Override
                public void run() {
                    synchronized (lock) {
                        //Atualiza as teclas pressionadas se o timer expirar
                        up = up_new;
                        down = down_new;
                        left = left_new;
                        right = right_new;
                        int movType = getMovementType();
                        if (movType != lastMovementType) {
                            lastMovementType = movType;
//                        System.out.printf("movType=%d\n", movType);
                        }
                    }
                    fireChangeEvent();
                }
            };
            timer.schedule(timerTask, TIMER_DELAY);
            synchronized (lock) {
                switch (keyCode) {
                    case PApplet.UP:
                        up_new = false;
                        break;
                    case PApplet.DOWN:
                        down_new = false;
                        break;
                    case PApplet.LEFT:
                        left_new = false;
                        break;
                    case PApplet.RIGHT:
                        right_new = false;
                        break;
                }
            }
        }
    }

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

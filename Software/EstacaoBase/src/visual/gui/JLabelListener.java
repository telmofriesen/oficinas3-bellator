/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import events.MyChangeEvent;
import events.MyChangeListener;
import java.util.Observable;
import java.util.Observer;
import javax.swing.JLabel;

/**
 * Classe abstrata para JLabels que são listeners.
 *
 * @author stefan
 */
public abstract class JLabelListener extends JLabel implements MyChangeListener {

    @Override
    public abstract void changeEventReceived(MyChangeEvent evt);
}

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
 *
 * @author stefan
 */
public class JLabelListener extends JLabel implements MyChangeListener  {

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        throw new UnsupportedOperationException("A classe deve ser instanciada para usar este método corretamente!");
    }    
}

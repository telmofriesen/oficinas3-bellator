/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import comm.TR_ClientConnector;
import controle.ControleSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import javax.swing.JButton;

/**
 *
 * @author stefan
 */
public class SensoresButtonListener extends JButton implements MyChangeListener {

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof TR_ClientConnector) {
            TR_ClientConnector c = (TR_ClientConnector) evt.getSource();
            if (!c.isConnected()) {
                this.setEnabled(false);
            } else {
                this.setEnabled(true);
            }
        }
        if (evt.getSource() instanceof ControleSensores) {
            ControleSensores c = (ControleSensores) evt.getSource();
            int status = c.getSensorSampleStatus();
            switch (status) {
                case ControleSensores.SAMPLE_STOPPED:
                     setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/camera-web_red.png")));
                    break;
                case ControleSensores.SAMPLE_STARTED:
                     setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/camera-web.png")));
                    break;
            }
        }
    }
}
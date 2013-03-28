/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package zz_old;

import dados.ControleSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import javax.swing.JButton;

/**
 *
 * @author stefan
 */
public class ControleSensoresListener_old extends JButton implements MyChangeListener {

    int lastStatus = -1;

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof ControleSensores) {
            ControleSensores controle = (ControleSensores) evt.getSource();
            int status = controle.getSensorSampleStatus();
            switch (status) {
                case ControleSensores.SAMPLE_STOPPED:
                    this.setEnabled(true);
                    this.setText("Ativar");
                    break;
                case ControleSensores.SAMPLE_CHANGING:
                    if (lastStatus == ControleSensores.SAMPLE_STOPPED)
                        this.setText("Ativando...");
                    if (lastStatus == ControleSensores.SAMPLE_STARTED)
                        this.setText("Desativando...");
                    this.setEnabled(false);
                    break;
                case ControleSensores.SAMPLE_STARTED:
                    this.setEnabled(true);
                    this.setText("Desativar");
                    break;
            }
            lastStatus = status;
        }
    }
}

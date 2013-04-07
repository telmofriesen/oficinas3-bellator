/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package zz_old;

import dados.GerenciadorSensores;
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
        if (evt.getSource() instanceof GerenciadorSensores) {
            GerenciadorSensores controle = (GerenciadorSensores) evt.getSource();
            int status = controle.getSensorSampleStatus();
            switch (status) {
                case GerenciadorSensores.SAMPLE_STOPPED:
                    this.setEnabled(true);
                    this.setText("Ativar");
                    break;
                case GerenciadorSensores.SAMPLE_CHANGING:
                    if (lastStatus == GerenciadorSensores.SAMPLE_STOPPED)
                        this.setText("Ativando...");
                    if (lastStatus == GerenciadorSensores.SAMPLE_STARTED)
                        this.setText("Desativando...");
                    this.setEnabled(false);
                    break;
                case GerenciadorSensores.SAMPLE_STARTED:
                    this.setEnabled(true);
                    this.setText("Desativar");
                    break;
            }
            lastStatus = status;
        }
    }
}

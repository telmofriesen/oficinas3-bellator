package visual.gui;

import comm.TR_ClientConnector;
import controle.ControleSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import javax.swing.JButton;

/**
 * Botão "Sensores" que escuta mudanças importantes de status.
 *
 * @author stefan
 */
public class SensoresButtonListener extends JButton implements MyChangeListener {

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof TR_ClientConnector) {
            //Mudanças no status da conexão
            TR_ClientConnector c = (TR_ClientConnector) evt.getSource();
            if (!c.isConnected()) { //Desabilita o botão se estiver desconectado.
                this.setEnabled(false);
            } else { //Habilita caso contrário.
                this.setEnabled(true);
            }
        }
        //Detecta mudanças no status da amostragem.
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
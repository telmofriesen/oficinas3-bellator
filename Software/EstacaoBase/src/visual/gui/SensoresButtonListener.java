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

    public int lastStatus = -1;

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof TR_ClientConnector) {
            //Mudanças no status da conexão
            TR_ClientConnector c = (TR_ClientConnector) evt.getSource();
            if (!c.isConnected()) { //Desabilita o botão se estiver desconectado.
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        setEnabled(false);
                    }
                });
            } else { //Habilita caso contrário.
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        setEnabled(true);
                    }
                });
            }
        }
        //Detecta mudanças no status da amostragem.
        if (evt.getSource() instanceof ControleSensores) {
            ControleSensores c = (ControleSensores) evt.getSource();
            int status = c.getSensorSampleStatus();
            synchronized (this) {
                if (status != lastStatus) {
                    switch (status) {
                        case ControleSensores.SAMPLE_STOPPED:
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/camera-web_red.png")));
                                }
                            });
                            break;
                        case ControleSensores.SAMPLE_STARTED:
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/camera-web.png")));
                                }
                            });
                            break;
                    }
                    lastStatus = status;
                }
            }
        }
    }
}
package visual.gui;

import comunicacao.ClientConnector;
import dados.GerenciadorSensores;
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
        if (evt.getSource() instanceof ClientConnector) {
            //Mudanças no status da conexão
            ClientConnector c = (ClientConnector) evt.getSource();
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
        if (evt.getSource() instanceof GerenciadorSensores) {
            GerenciadorSensores c = (GerenciadorSensores) evt.getSource();
            int status = c.getSensorSampleStatus();
            synchronized (this) {
                if (status != lastStatus) {
                    switch (status) {
                        case GerenciadorSensores.SAMPLE_STOPPED:
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/camera-web_red.png")));
                                }
                            });
                            break;
                        case GerenciadorSensores.SAMPLE_STARTED:
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
package visual.gui;

import comm.TR_ClientConnector;
import events.MyChangeEvent;
import events.MyChangeListener;
import javax.swing.JButton;

/**
 * Botão de conexão que escuta o Connector em busca de mudanças.
 * Troca a cor do ícone de conexão caso mudanças na conexão sejam detectadas.
 * @author stefan
 */
public class ConnectionButtonListener extends JButton implements MyChangeListener {

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof TR_ClientConnector) {
            TR_ClientConnector connector = (TR_ClientConnector) evt.getSource();
            int status = connector.getConnectionStatus();
            int error = connector.getErrorStatus();

            if (status == TR_ClientConnector.CONNECTED) {
                if (error == TR_ClientConnector.WARNING_COMMUNICATION_TIME) {
                    setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/wifi2-blue_dark.png")));
                    setToolTipText("Conectado ao robô (tempo excessivo sem comunicação)");
                } else {
                    setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/wifi2-blue.png")));
                    setToolTipText("Conectado ao robô");
                }
            } else if (status == TR_ClientConnector.CONNECTED_HANDSHAKE) {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/wifi2-yellow.png")));
                setToolTipText("Conectado (handshake...)");
            } else if (status == TR_ClientConnector.CONNECTING) {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/wifi2-yellow.png")));
                setToolTipText("Conectando...");
            } else if (status == TR_ClientConnector.DISCONNECTED) {
                setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/wifi2-red.png")));
                setToolTipText("Desconectado do robô");
            }
        }
    }
}

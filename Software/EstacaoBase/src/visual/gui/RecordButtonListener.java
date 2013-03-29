package visual.gui;

import comunicacao.ClientConnector;
import dados.ControleSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import javax.swing.JToggleButton;

/**
 * Botão de gravação, que escuta mudanças importantes de status.
 *
 * @author stefan
 */
public class RecordButtonListener extends JToggleButton implements MyChangeListener {

//    int lastConnectionStatus = TR_ClientConnector.DISCONNECTED;
    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        //Mudanças em ControleSensores
        if (evt.getSource() instanceof ControleSensores) {
            ControleSensores controle = (ControleSensores) evt.getSource();
//            this.setSelected(controle.isRecordEnabled());
            //Se o estado do botao e do ControleSensores forem diferentes, muda o estado do botao para mostrar o estado real.
            if ((this.isSelected() && !controle.isRecordEnabled())
                || (!this.isSelected() && controle.isRecordEnabled())) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        doClick();
                    }
                });
            }
        }
        //Mudanças no Connector
        if (evt.getSource() instanceof ClientConnector) {
            ClientConnector connector = (ClientConnector) evt.getSource();
            if (connector.getConnectionStatus() != ClientConnector.CONNECTED) {
                //Se não houver conexão, desabilita a gravação de leituras.
                if (this.isSelected()) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            doClick();
                        }
                    });
                }
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        setEnabled(false);
                    }
                });
            } else {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        setEnabled(true);
                    }
                });

            }
        }
    }
}

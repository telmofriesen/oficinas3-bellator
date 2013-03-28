/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import comunicacao.ClientConnector;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JTextArea;

/**
 * Área de log para status de conexão.
 * Escuta o Connector em busca de mudanças nos status.
 *
 * @author stefan
 */
public class ConnectionLogTextArea extends JTextArea implements MyChangeListener {

    private int lastStatus = ClientConnector.DISCONNECTED;
    private int lastErrorStatus = ClientConnector.ERROR_NO;

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof ClientConnector) {
            final ClientConnector connector = (ClientConnector) evt.getSource();
            synchronized (connector) {
                int status = connector.getConnectionStatus();
                int error = connector.getErrorStatus();
                final String date_str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                final String status_str_complete = connector.getErrorStatusStringComplete();
                final String status_str = connector.getConnectionStatusString();
                final String error_str_complete = connector.getErrorStatusStringComplete();
                
                //
                // Efetua as impressões dos status no log de conexão.
                //

                //Se o status mudar...
                if (status != lastStatus) {
                    //Imprime uma mensagem de erro se o status mudar e houver um erro (qualquer que seja)
                    if (error != ClientConnector.ERROR_NO) {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                append(String.format("%s - %s\n",
                                                     date_str, status_str_complete));
                            }
                        });
                        lastErrorStatus = error;
                    }
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            //Imprime o status atual
                            append(String.format("%s - %s\n",
                                                 date_str, status_str));
                        }
                    });
                }
                //Imprime uma mensagem de erro se o status do erro mudar OU
                //se um aviso de tempo excessivo sem comunicação estiver ativo
                if (error != lastErrorStatus && error != ClientConnector.ERROR_NO
                    || error == lastErrorStatus && error == ClientConnector.WARNING_COMMUNICATION_TIME) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            append(String.format("%s - %s\n",
                                                 date_str, error_str_complete));
                        }
                    });
                }

                lastStatus = status;
                lastErrorStatus = error;
            }
        }
    }
}

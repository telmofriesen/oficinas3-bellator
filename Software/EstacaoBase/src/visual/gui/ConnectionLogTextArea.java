/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import comm.TR_ClientConnector;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JTextArea;

/**
 *
 * @author stefan
 */
public class ConnectionLogTextArea extends JTextArea implements MyChangeListener {

    private int lastStatus = TR_ClientConnector.DISCONNECTED;
    private int lastErrorStatus = TR_ClientConnector.ERROR_NO;

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof TR_ClientConnector) {
            TR_ClientConnector connector = (TR_ClientConnector) evt.getSource();
            synchronized (connector) {
                int status = connector.getConnectionStatus();
                int error = connector.getErrorStatus();
                String date_str = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                //
                // Efetua as impressões dos status no log de conexão.
                //

                //Se o status mudar...
                if (status != lastStatus) {
                    //Imprime uma mensagem de erro se o status mudar e houver um erro (qualquer que seja)
                    if (error != TR_ClientConnector.ERROR_NO) {
                        this.append(String.format("%s - %s\n",
                                                  date_str, connector.getErrorStatusStringComplete()));
                        lastErrorStatus = error;
                    }
                    //Imprime o status atual
                    this.append(String.format("%s - %s\n",
                                              date_str, connector.getConnectionStatusString()));
                }
                //Imprime uma mensagem de erro se o status do erro mudar OU
                //se um aviso de tempo excessivo sem comunicação estiver ativo
                if (error != lastErrorStatus && error != TR_ClientConnector.ERROR_NO
                    || error == lastErrorStatus && error == TR_ClientConnector.WARNING_COMMUNICATION_TIME) {
                    this.append(String.format("%s - %s\n",
                                              date_str, connector.getErrorStatusStringComplete()));
                }

                lastStatus = status;
                lastErrorStatus = error;
            }
        }
    }
}

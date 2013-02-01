/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.io.*;

/**
 *
 * @author Stefan
 */
public class TR_ClientReceiver extends Thread {

    //O Connector da conexão aceita
    TR_ClientConnector connector;
    //Stream de entrada
    private BufferedReader input;
    private boolean run = true;

    //---------Classes construtoras---------
    public TR_ClientReceiver(TR_ClientConnector connector) {
        this.connector = connector;
    }

    //-----------------------------
    @Override
    public void run() {
        run = true;

        TR_ClientCommandInterpreter interpreter;
        String message = "";
        try {
            //System.out.println("[TR_ClientReceiver] Entrando na função de recebimento.");
            //Cria a Stream de saida
            input = new BufferedReader(new InputStreamReader(connector.getSock().getInputStream()));
            while (run) {
                while ((message = input.readLine()) != null) {
                    System.out.println("[TR_ClientReceiver] Mensagem recebida: " + message);
                    connector.packetReceived(); 
                    connector.processCommand(message);
                }
            }
            input.close();
        } catch (IOException ex) {
            System.out.println("[TR_ClientReceiver] Erro de IO: " + ex.getMessage());
            connector.IOError(ex.getMessage());
            connector.closeConnection();
//            Logger.getLogger(TR_ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public synchronized void terminate() {
        run = false;
    }
}

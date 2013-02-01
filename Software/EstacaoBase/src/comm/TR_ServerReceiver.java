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
public class TR_ServerReceiver extends Thread {

    //O Connector da conexão aceita
    private final TR_ServerConnection connection;
    //Stream de entrada
    private BufferedReader input;
    private boolean run = true;

    //---------Classes construtoras---------
    public TR_ServerReceiver(TR_ServerConnection connection) {
        this.connection = connection;
        this.setName(this.getClass().getName());
    }

    //-----------------------------
    @Override
    public void run() {
        run = true;

        String message = "";
        try {
            //System.out.println("[TR_ClientReceiver] Entrando na função de recebimento.");
            //Cria a Stream de saida
            input = new BufferedReader(new InputStreamReader(connection.getSock().getInputStream()));
            while (run) {
                while ((message = input.readLine()) != null) {
                    System.out.println("[TR_ServerReceiver] Mensagem recebida: " + message);
                    connection.packetReceived();
                    connection.processCommand(message);
                }
            }
            input.close();
        } catch (IOException ex) {
            System.out.println("[TR_ServerReceiver] Erro de IO: " + ex.getMessage());
            connection.closeConnection();
//            Logger.getLogger(TR_ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
//        synchronized (connection) {
//            connection.notifyAll();
//        }
    }

    public synchronized void terminate() {
        run = false;
    }
}

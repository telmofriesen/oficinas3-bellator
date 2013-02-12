/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread responsável por receber mensagens de um host de uma conexão.
 *
 * @author Stefan
 */
public class TR_ServerReceiver extends Thread {

    //O Connector da conexão aceita
    private final TR_ServerConnection connection;
    //Stream de entrada
    private BufferedReader input;
    //Indica se o loop principal deve executar ou não
    private boolean run = true;

    public TR_ServerReceiver(TR_ServerConnection connection) {
        this.connection = connection;
        this.setName(this.getClass().getName());
    }

    @Override
    public void run() {
        run = true;

        String message;
        try {
            //System.out.println("[TR_ClientReceiver] Entrando na função de recebimento.");
            //Cria a Stream de saida
            input = new BufferedReader(new InputStreamReader(connection.getSock().getInputStream()));
            while (run) {
                synchronized (this) {
                    //Loop de recebimento de mensagens 
                    while ((message = input.readLine()) != null) {
                        System.out.println("[TR_ServerReceiver] Mensagem recebida: " + message);
                        //Passa a mensagem recebida ao ServerConnection.
                        connection.messageReceived();
                        connection.processCommand(message);
                    }
                }
            }
            input.close();
        } catch (IOException ex) {
            System.out.println("[TR_ServerReceiver] Erro de IO: " + ex.getMessage());
            connection.closeConnection();
        }
    }

    /**
     * Requisita a finalização da Thread.
     * A finalização é feita amigavelmente, ou seja, fecha-se o stream de saída antes.
     */
    public void terminate() {
        run = false;
        try {
            input.close();
        } catch (IOException ex) {
            connection.closeConnection();
            Logger.getLogger(TR_ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Força a finalização da thread.
     */
    public void kill() {
        try {
            input.close();
        } catch (IOException ex) {
            Logger.getLogger(TR_ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.interrupt();
    }
    
}

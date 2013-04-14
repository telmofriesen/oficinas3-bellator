package comunicacao;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread responsável por receber mensagens de um host de uma conexão.
 *
 * @author Stefan
 */
public class ClientReceiver extends Thread {

    //O Connector da conexão aceita
    private final ClientConnector connector;
    //Stream de entrada
    private BufferedReader input;
    //Indica se o loop principal deve executar ou não
    private boolean run = true;

    public ClientReceiver(ClientConnector connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        run = true;
        String message = "";
        try {
            //System.out.println("[TR_ClientReceiver] Entrando na função de recebimento.");
            //Cria a Stream de saida
            input = new BufferedReader(new InputStreamReader(connector.getSock().getInputStream()), 100000);
            while (run) {
                synchronized (this) {
                    //Loop de recebimento de mensagens 
                    if ((message = input.readLine()) != null) {
                        System.out.println("[TR_ClientReceiver] Mensagem recebida: " + message);
                        //Passa a mensagem recebida ao connector
                        connector.messageReceived();
                        connector.processCommand(message);
                    }
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

    /**
     * Requisita a finalização da Thread.
     * A finalização é feita corretamente, ou seja, fecha-se o stream de saída antes.
     */
    public void terminate() {
        run = false;
        try {
            input.close();
        } catch (IOException ex) {
            connector.closeConnection();
            Logger.getLogger(ClientReceiver.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Força a finalização da thread.
     */
    public void kill() {
        try {
            if(input != null) input.close();
        } catch (IOException ex) {
            Logger.getLogger(ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.interrupt();
    }
}

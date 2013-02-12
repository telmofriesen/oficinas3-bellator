/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.io.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thread responsável por enviar mensagens ao host de uma conexão.
 *
 * @author Stefan
 */
public class TR_ClientSender extends Thread {

    //O Connector da conexão aceita
    private final TR_ClientConnector connector;
    //Stream de saida
    private BufferedWriter output;
    //Fila de mensagens normais
    private CopyOnWriteArrayList<SenderMessage> messages = new CopyOnWriteArrayList();
    //Fila de mensagens prioritárias
    private CopyOnWriteArrayList<SenderMessage> priorityMessages = new CopyOnWriteArrayList();
    //Indica se o loop principal deve executar ou não
    private boolean run = true;

    public TR_ClientSender(TR_ClientConnector connector) {
        this.connector = connector;
    }

    @Override
    public void run() {
        run = true;
        SenderMessage msg = new SenderMessage("", false);
        boolean sendPriorityMessage;
        boolean sendNormalMessage;
        try {
            //Cria a Stream de saida
            output = new BufferedWriter(new OutputStreamWriter(connector.getSock().getOutputStream()));
            while (run) {
                synchronized (this) {
                    while (messages.isEmpty() && priorityMessages.isEmpty() && run) {
                        this.wait();
                    }
                }
                sendPriorityMessage = false;
                sendNormalMessage = false;
                //Se o vetor de mensagens não estiver vazio....
                if (!priorityMessages.isEmpty()) {
                    msg = priorityMessages.get(0);
                    sendPriorityMessage = true;
                    sendNormalMessage = false;
                } else if (!messages.isEmpty()) {
                    msg = messages.get(0);
                    sendPriorityMessage = false;
                    sendNormalMessage = true;
                }
                if (sendPriorityMessage || sendNormalMessage) {
                    String str = msg.getMessage();
                    //Envia a mensagem
                    System.out.println("[TR_ClientSender] Mensagem a ser enviada: " + str);
                    output.write(str, 0, str.length());
                    output.newLine();
                    if (msg.isFlushBuffer()) output.flush();
                    System.out.println("[TR_ClientSender] Mensagem enviada!");
                    if (sendPriorityMessage) priorityMessages.remove(0); //Faz a fila andar.
                    else if (sendNormalMessage) messages.remove(0); //Faz a fila andar.
                }
            }
            output.close();
        } catch (IOException ex) {
            System.out.println("[TR_ClientSender] Erro de IO: " + ex.getMessage());
//            synchronized (this) {
//                run = false;
//            }
            connector.IOError(ex.getMessage());
            connector.closeConnection();
//            Logger.getLogger(TR_ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        } catch (InterruptedException ex) {
            Logger.getLogger(TR_ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Requisita a finalização da Thread.
     * A finalização é feita amigavelmente, ou seja, aguarda-se que as mensagens da fila de envio sejam todas enviadas.
     */
    public synchronized void terminate() {
        run = false;
        this.notifyAll();
    }

    /**
     * Força a finalização da thread.
     */
    public void kill() {
        try {
            output.close();
        } catch (IOException ex) {
            Logger.getLogger(TR_ClientSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        this.interrupt();
    }

    /**
     * Adiciona uma mensagem à fila prioritária de envio.
     *
     * @param message Mensagem a ser enviada.
     * @param flush_buffer Indica se um flush no buffer deve ser feito.
     */
    public synchronized void sendMessageWithPriority(String message, boolean flush_buffer) {
        sendMessageWithPriority(new SenderMessage(message, flush_buffer));
    }

    /**
     * Adiciona uma mensagem à fila prioritária de envio.
     *
     * @param senderMessage Mensagem a ser enviada.
     */
    public synchronized void sendMessageWithPriority(SenderMessage senderMessage) {
        //Se o parametro nao for vazio....
        if (!senderMessage.getMessage().isEmpty() && senderMessage.getMessage() != null) {
            priorityMessages.add(senderMessage);
            this.notifyAll();
        }
    }

    /**
     * Adiciona uma mensagem à fila de envio.
     *
     * @param message Mensagem a ser enviada.
     * @param flush_buffer Indica se um flush no buffer deve ser feito.
     */
    public synchronized void sendMessage(String message, boolean flush_buffer) {
        sendMessage(new SenderMessage(message, flush_buffer));
    }

    /**
     * Adiciona uma mensagem à fila de envio.
     *
     * @param senderMessage Mensagem a ser enviada.
     */
    public synchronized void sendMessage(SenderMessage senderMessage) {
        //Funcao que envia mensagens
        //Se o parametro nao for vazio....
        if (!senderMessage.getMessage().isEmpty() && senderMessage.getMessage() != null) {
            messages.add(senderMessage);
            this.notifyAll();
        }
    }
}

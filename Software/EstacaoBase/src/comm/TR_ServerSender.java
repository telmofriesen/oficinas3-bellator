/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Stefan
 * TODO resolver problema com flush do buffer. Se o tempo de limite de tempo de ECHO é muito pequeno, o buffer demora muito para ser enviado com amostras de leituras dos sensores.
 */
public class TR_ServerSender extends Thread {

    //O Connector da conexão aceita
    private final TR_ServerConnection connection;
    //Stream de saida
    private BufferedWriter output;
    private CopyOnWriteArrayList<SenderMessage> messages = new CopyOnWriteArrayList();
    private CopyOnWriteArrayList<SenderMessage> priorityMessages = new CopyOnWriteArrayList();
//    //Mensagem a ser enviada
//    private String message = "";
    private boolean run = true;
    private boolean terminated = false;

    //--------Classe construtora-------------
    public TR_ServerSender(TR_ServerConnection connection) {
        this.connection = connection;
        this.setName(this.getClass().getName());
    }
    //---------------------------------------

    @Override
    public void run() {
        run = true;
        SenderMessage msg = new SenderMessage("", false);
        boolean sendPriorityMessage;
        boolean sendNormalMessage;
        try {
//Cria a Stream de saida
            output = new BufferedWriter(new OutputStreamWriter(connection.getSock().getOutputStream()));
            while (run) {
                synchronized (this) {
                    while (messages.isEmpty() && priorityMessages.isEmpty() && run) {
                        this.wait();
                    }
                }
                sendPriorityMessage = false;
                sendNormalMessage = false;
                //Loop de envio.
                //Se o vetor não estiver vazio....
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
                    System.out.println("[TR_ServerSender] Mensagem a ser enviada: " + str);
                    output.write(str, 0, str.length());
                    output.newLine();
                    if (msg.isFlush_buffer()) output.flush();
                    System.out.println("[TR_ServerSender] Mensagem enviada!");
                    if (sendPriorityMessage) priorityMessages.remove(0); //Faz a fila andar.
                    else if (sendNormalMessage) messages.remove(0); //Faz a fila andar.
                }
            }
            output.close();
        } catch (IOException ex) {
            System.out.println("[TR_ServerSender] Erro de IO: " + ex.getMessage());
            synchronized (this) {
                run = false;
            }
            connection.closeConnection();
            messages.clear();
        } catch (InterruptedException ex) {
            Logger.getLogger(TR_ServerSender.class.getName()).log(Level.SEVERE, null, ex);
        }
        messages.clear(); //TODO verificar se é melhor solucao para liberar memoria
    }

    public synchronized void terminate() {
        run = false;
        this.notifyAll();
    }

    public synchronized void sendMessageWithPriority(String message) {
        sendMessageWithPriority(new SenderMessage(message, true));
    }

    public synchronized void sendMessageWithPriority(String message, boolean flush_buffer) {
        sendMessageWithPriority(new SenderMessage(message, flush_buffer));
    }

    public synchronized void sendMessageWithPriority(SenderMessage senderMessage) {
        //Funcao que envia mensagens
        //Se o parametro nao for vazio....
        if (!senderMessage.getMessage().isEmpty() && senderMessage.getMessage() != null) {
            priorityMessages.add(senderMessage);
            this.notifyAll();
        }
    }

    /**
     * Agenda envio de uma mensagem pelo socket (com flush do buffer). Uma quebra de linha é automaticamente adicionada no final da String antes do envio.
     *
     * @param message
     */
    public synchronized void sendMessage(String message) {
        sendMessage(new SenderMessage(message, true));
    }

    public synchronized void sendMessage(String message, boolean flush_buffer) {
        sendMessage(new SenderMessage(message, flush_buffer));
    }

    public synchronized void sendMessage(SenderMessage senderMessage) {
        //Funcao que envia mensagens
        //Se o parametro nao for vazio....
        if (!senderMessage.getMessage().isEmpty() && senderMessage.getMessage() != null) {
            messages.add(senderMessage);
            this.notifyAll();
        }
    }

    public boolean isTerminated() {
        return terminated;
    }
}

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package comm;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.*;
import java.io.*;
import java.util.ArrayList;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Timer;

/**
 * Thread que escuta novas conexões de clientes
 *
 * @author Stefan
 */
public class TR_ServerListener extends Thread {

    //O Socket principal do servidor
    private ServerSocket serverSocket;
    //O socket de conexões aceitas
    private Socket acceptedSocket = new Socket();
    //As listas que armazenam as conexões abertas
    private ArrayList<TR_ServerConnection> connectionsArray = new ArrayList<TR_ServerConnection>();
    //O identificador de cada conexão(ip), sendo que cada elemento
    //do vetor tem o mesmo indice que em ServiceSocketVector
    private int listenerPort;
    private Server server;

    public TR_ServerListener(Server server, int port) {
        this.server = server;
        this.listenerPort = port;
        try {
            serverSocket = new ServerSocket(listenerPort, 3);
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    @Override
    public void run() {
        System.out.println("[TR_ServerListener] Listener rodando!");
        //Lê a porta nas configurações
//        listenerPort = 12312;
        System.out.println("Porta: " + listenerPort);
        //Cria o socket na porta

        while (true) {
            try {
//                System.out.println("[TR_ServerListener] Antes do accept: " + acceptedSocket);
                acceptedSocket = serverSocket.accept();
                System.out.println("[TR_ServerListener] Conexão aceita:" + acceptedSocket);
                //Adiciona instancias para a conexao aceita.
                //As threads para envio(sender) e recebimento(receiver) são armazenadas nos vetores.
                final TR_ServerConnection connection = new TR_ServerConnection(this, acceptedSocket);
                //Servidor já conectado a um cliente.
                if (getNumServerConnections() >= 1) {
                    System.out.println("[TR_Serverlistener] Servidor cheio!");
                    connection.getSender().start();
                    //Manda uma mensagem informando que o servidor está cheio.
                    connection.sendMessageWithPriority("SERVER FULL", true);
                    try {
                        sleep(200);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(TR_ServerListener.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    Thread t = new Thread() {
                        @Override
                        public void run() {
                            connection.disconnect();
                        }
                    };
                    t.start(); //Roda a função de disconnect em background (outra Thread).
                } else {
                    //Inicializa as threads da conexão
                    connection.startThreads();
                    //System.out.println("[TR_ServerListener] Sender: " + Sender);
                    //Armazena os objetos nos vetores
                    synchronized (this) {
                        connectionsArray.add(connection);
                    }
                    String a = acceptedSocket.getRemoteSocketAddress().toString();
                    String first = a.split(":")[0];
                    String ip = first.split("/")[1];

                    System.out.println(ip + " -- " + acceptedSocket.getPort());
                    server.mainHostConnected(ip, acceptedSocket.getPort());
                }
            } catch (IOException ex) {
                //System.out.println(ex);
                Logger.getLogger(TR_ServerListener.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    public Server getServer() {
        return server;
    }

    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    public int getServerSocketPort() {
        return serverSocket.getLocalPort();
    }

    public synchronized void removeConnection(TR_ServerConnection connection) {
//        connection.disconnect();
        //Se o host desconectado for o host principal, informa o fato ao servidor.
        if (connectionsArray.indexOf(connection) == 0) {
            server.mainHostDisconnected();
        }
        connectionsArray.remove(connection);
    }

    public synchronized TR_ServerConnection getServerConnection(int index) {
        if (index < getNumServerConnections()) {
            return connectionsArray.get(index);
        } else {
            return null;
        }
    }

    /**
     * Retorna o número de conexões ativas.
     *
     * @return O número de conexões ativas.
     */
    public synchronized int getNumServerConnections() {
        return connectionsArray.size();
    }
    /*
     public void sendToAll(String Message) {
     for (int i = 0; i <= SenderVector.size(); i++) {
     System.out.println("[Listener]SendToAll: " + Message);
     SenderVector.elementAt(i).sendMessage(Message);
     }
     }
    
     //Funcao que procura o indice da thread Sender, pelo socket.
     public int searchSenderIndexBySocket(Socket S) {
     for (int i = 0; i < SenderVector.size(); i++) {
     if (SenderVector.elementAt(i).getSocket() == S) {
     return i;
     }
     }
     //Se nao for encontrado, retorna -1.
     return -1;
     }
     public int searchSenderIndexByObject(Object O) {
     for (int i = 0; i < SenderVector.size(); i++) {
     if (SenderVector.elementAt(i) == O) {
     return i;
     }
     }
     //Se nao for encontrado, retorna -1.
     return -1;
     }
    
     //Funcao que envia uma mensagem a partir de um indice.
     public boolean sendMessageByIndex(String Message, int index) {
    
     //Se o elemento existir...
     if (index < SenderVector.size() && index >= 0) {
     //Envia a mensagem.
     SenderVector.elementAt(index).sendMessage(Message);
     return true;
     } else {
     return false;
     }
     }
    
     //Uma combinacao das duas funcoes anteriores.
     //Procura o indice a partir do Socket, e envia a mensagem.
     public boolean sendMessageBySocket(String Message,Socket S){
     int index = searchSenderIndexBySocket(S);
     //Se o indice for valido...
     if(index >= 0){
     //Envia a mensagem.
     if(sendMessageByIndex(Message, index) == true){
     return true;
     }
     }
     return false;
     }
     //Funcao que deleta as threads de envio e recebimento, fechando o Socket.
     public void deleteThreadBySocket(Socket S) throws IOException{
     int index = searchSenderIndexBySocket(S);
     if(index >=0){
     SenderVector.elementAt(index).getSocket().close();
     SenderVector.remove(index);
     ReceiverVector.remove(index);
     }
     }
     *
     */
}

package comm;

/**
 * Exceção lançada quando o cliente já está conectado e uma nova conexão é requerida.
 * @author stefan
 */
public class ClientConnectedException extends Exception {

    public ClientConnectedException() {
        super(String.format("O cliente já está conectado."));
    }
        
}

package events;

/**
 * Interface para listeners que "escutam" outras classes em busca de mudanças nelas.
 * Cada listener deve ser adicionado à classe que deseja-se escutar, e o lançamento de eventos é feito de forma manual na classe "escutada". Para isso, a classe escutada usa o método changeEventReceived().
 *
 * @author stefan
 */
public interface MyChangeListener {

    /**
     * Método chamado toda vez que houver uma modificação importante na classe "escutada".
     *
     * @param evt Evento de mudança ocorrido. Deve conter a classe que chamou o método.
     */
    public void changeEventReceived(MyChangeEvent evt);
}
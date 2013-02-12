package events;

/**
 * Interface para listeners que "escutam" outras classes em busca de mudanças nelas.
 * Cada listener deve ser adicionado à classe que deseja-se escutar, e o lançamento de eventos é feito de forma manual nessa classe através do método changeEventReceived().
 *
 * @author stefan
 */
public interface MyChangeListener {

    /**
     * Método executado toda vez que houver uma modificação importante na classe alvo.
     *
     * @param evt Evento ocorrido. Deve conter a classe que chamou este método.
     */
    public void changeEventReceived(MyChangeEvent evt);
}
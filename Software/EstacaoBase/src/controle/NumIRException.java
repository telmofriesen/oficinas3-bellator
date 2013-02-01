/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package controle;

public class NumIRException extends Exception {

    private int IR_recebido;
    private int IR_correto;

    public NumIRException(int IR_recebido, int IR_correto) {
        super(String.format("Número de sensores no vetor distIR (%d) difere do número de sensores presentes no robô (%d).",
                            IR_correto, IR_recebido));
        this.IR_correto = IR_correto;
        this.IR_recebido = IR_recebido;
    }

    public NumIRException(int IR_recebido, int IR_correto, String string) {
        super(string);
        this.IR_correto = IR_correto;
        this.IR_recebido = IR_recebido;
    }

    public int getIR_recebido() {
        return IR_recebido;
    }

    public int getIR_correto() {
        return IR_correto;
    }
}
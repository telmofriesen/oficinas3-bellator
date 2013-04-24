/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import comunicacao.ClientMessageProcessor;
import dados.AmostraSensores;
import dados.GerenciadorSensores;
import dados.NumIRException;
import dados.TimestampException;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author stefan
 */
public class JanelaAvancado extends javax.swing.JFrame {

    private final JanelaPrincipal janelaPrincipal;
    private final ClientMessageProcessor clientMessageProcessor;
    private final GerenciadorSensores gerenciadorSensores;
    private final CustomFileChooser fc;

    public JanelaAvancado(JanelaPrincipal janelaPrincipal, ClientMessageProcessor clientMessageProcessor, GerenciadorSensores gerenciadorSensores) {
        this.janelaPrincipal = janelaPrincipal;
        this.clientMessageProcessor = clientMessageProcessor;
        this.gerenciadorSensores = gerenciadorSensores;
        fc = new CustomFileChooser(new File("./testes").getAbsolutePath(), "csv");
        initComponents();

    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        recordCheckBox = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        fileNameJLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        fileLoadButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        saveButton = new javax.swing.JButton();

        recordCheckBox.setText("Gravar leituras dos sensores em arquivo");
        recordCheckBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordCheckBoxActionPerformed(evt);
            }
        });

        jLabel1.setText("Nome do arquivo:");

        fileNameJLabel.setText("--");

        jLabel2.setText("Carregar arquivo de leituras de sensores:");

        fileLoadButton.setText("CARREGAR");
        fileLoadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileLoadButtonActionPerformed(evt);
            }
        });

        jLabel3.setText("Salvar valores para plotagem");

        saveButton.setText("SALVAR");
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(fileNameJLabel))
                    .addComponent(recordCheckBox)
                    .addComponent(jLabel2)
                    .addComponent(fileLoadButton)
                    .addComponent(jLabel3)
                    .addComponent(saveButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(recordCheckBox)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(fileNameJLabel))
                .addGap(18, 18, 18)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(fileLoadButton)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(saveButton)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void recordCheckBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordCheckBoxActionPerformed
        // TODO add your handling code here:
        if (recordCheckBox.isSelected()) {
            clientMessageProcessor.startGravacaoLeiturasSensores();
            fileNameJLabel.setText(clientMessageProcessor.getNomeArquivoGravacaoLeituras());
        } else {
            clientMessageProcessor.stopGravacaoLeiturasSensoresArquivo();
            fileNameJLabel.setText("--");
        }
    }//GEN-LAST:event_recordCheckBoxActionPerformed

    private void fileLoadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileLoadButtonActionPerformed
        // TODO add your handling code here:
        new Thread() {
            @Override
            public void run() {

                int returnVal = fc.showOpenDialog(fileLoadButton);
                try {
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
//                janelaPrincipal.stopRecording();
//                synchronized (gerenciadorSensores) {
                        clientMessageProcessor.stopGravacaoLeiturasSensoresArquivo();
                        clientMessageProcessor.setCarregandoLeiturasSensores(true);
                        gerenciadorSensores.limpaFilaAmostras();
                        gerenciadorSensores.startRecording();
                        janelaPrincipal.getMapa().getRobo().clearPosInfos();
                        janelaPrincipal.getMapa().getObstaculos().reset();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            //line = line.replaceAll("^", "S ");
                            String[] split = line.split(" ");
                            int encoder_esq = Integer.parseInt(split[0]);
                            int encoder_dir = Integer.parseInt(split[1]);
                            int[] IR = {Integer.parseInt(split[2]),
                                        Integer.parseInt(split[3]),
                                        Integer.parseInt(split[4]),
                                        Integer.parseInt(split[5]),
                                        Integer.parseInt(split[6])};
                            int AX = Integer.parseInt(split[7]);
                            int AY = Integer.parseInt(split[8]);
                            int AZ = Integer.parseInt(split[9]);
                            int GX = Integer.parseInt(split[10]);
                            int GY = Integer.parseInt(split[11]);
                            int GZ = Integer.parseInt(split[12]);
                            long unixTimestamp = Long.parseLong(split[13]);
                            AmostraSensores amostra = new AmostraSensores(encoder_esq, encoder_dir, IR, AX, AY, AZ, GX, GY, GZ, unixTimestamp);
                            try {
                                gerenciadorSensores.processaAmostraSensores(amostra);
                                //                        clientMessageProcessor.processCommand(line);
                            } catch (NumIRException ex) {
                                System.out.printf("[GerenciadorSensores] %s \"%s\"\n", ex.getMessage(), amostra);
                            } catch (TimestampException ex) {
                                System.out.printf("[GerenciadorSensores] %s \"%s\"\n", ex.getMessage(), amostra);
                            }
                        }
                        reader.close();
                        janelaPrincipal.getViewer2D().redraw();
//                gerenciadorSensores.processaFilaInteiraAmostras();
                        clientMessageProcessor.setCarregandoLeiturasSensores(false);
//                }
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(JanelaAvancado.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(JanelaAvancado.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }.start();
    }//GEN-LAST:event_fileLoadButtonActionPerformed

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_saveButtonActionPerformed
    /**
     * @param args the command line arguments
     */
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(JanelaAvancado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(JanelaAvancado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(JanelaAvancado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(JanelaAvancado.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new JanelaAvancado().setVisible(true);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton fileLoadButton;
    private javax.swing.JLabel fileNameJLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JCheckBox recordCheckBox;
    private javax.swing.JButton saveButton;
    // End of variables declaration//GEN-END:variables
}

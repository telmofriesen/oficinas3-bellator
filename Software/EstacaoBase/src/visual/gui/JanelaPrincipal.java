package visual.gui;

import comm.TR_ClientCommandInterpreter;
import comm.TR_ClientConnector;
import controle.*;
import events.MyChangeEvent;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import processing.core.PApplet;
import visual.*;

/**
 * Janela principal da interface gráfica do Bellator.
 *
 * @author stefan
 */
public class JanelaPrincipal extends javax.swing.JFrame {

    //Create a file chooser
    private CustomFileChooser fc;
    private Mapa mapa;
    private Viewer2D viewer2D;
    private boolean saved = true;
    private ControleSensores controleSensores;
    private JanelaConexao janelaConexao;
    private TR_ClientConnector connector;
    private JanelaSensores janelaSensores;

    /**
     * Creates new form JanelaPrincipal
     */
    public JanelaPrincipal() {
        initComponents();
        //Inicializa Robo, Obstaculos, Mapa, Viewer2D's e Drawable2D's.
        initMapa();
        //Cria um objeto seletor de arquivos na pasta atual, com a extensão "bellator"
        fc = new CustomFileChooser(new File(".").getAbsolutePath(), "bellator");
        //Inicializa conector e interpretador de comandos.
        connector = new TR_ClientConnector();
        TR_ClientCommandInterpreter interpreter = new TR_ClientCommandInterpreter(connector, controleSensores);
        connector.setInterpreter(interpreter);
        //Inicializa as janelas de configuração.
        janelaConexao = new JanelaConexao(connector);
        janelaSensores = new JanelaSensores(controleSensores, connector);
        //Inicia as threads.
        interpreter.start();
        connector.start();
        //Inicializa listeners.
        initListeners();

        //Acao de fechamento de janela
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                boolean exit = false;
                if (!saved) {
                    int response = JOptionPane.showConfirmDialog(we.getWindow(),
                                                                 "O mapa atual não foi salvo. Você deseja finalizar o programa e perder todas as alterações?",
                                                                 "Finalizar (Mapa não salvo)", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (response == JOptionPane.YES_OPTION)
                        exit = true;
                    else
                        exit = false;
                } else {
                    int response = JOptionPane.showConfirmDialog(we.getWindow(),
                                                                 "Você deseja realmente fechar o programa?",
                                                                 "Finalizar", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION)
                        exit = true;
                    else
                        exit = false;
                }
                if (exit) {
                    connector.disconnect();
//                    int count = 1;
//                    while (connector.isConnected() && count < 5) {
//                        System.out.printf("Aguardando connector finalizar.... (%d)\n", count);
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException ex) {
//                            Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
//                            System.exit(1);
//                        }
//                        count++;
//                    }
                    //Finaliza o programa.
                    dispose();
                    System.exit(0);
                }
            }
        });


//        recordButton.doClick();
//        connectionButtonListener.changeEventReceived(new MyChangeEvent(connector));
//        connector.connect("127.0.0.1", 1231);

        //
        // Insere leituras de teste
        //
//        try {
//            //Outros testes
//            controleSensores.novaLeituraSensores(0, PApplet.radians(0), new float[]{300, 0, 300}, 1000);
//            controleSensores.novaLeituraSensores(1, PApplet.radians(10), new float[]{300, 0, 300}, 2000);
//            controleSensores.novaLeituraSensores(0, PApplet.radians(10), new float[]{300, 0, 300}, 3000);
//            controleSensores.novaLeituraSensores(0, PApplet.radians(-10), new float[]{300, 0, 300}, 4000);
//            controleSensores.novaLeituraSensores(-0.5f, PApplet.radians(-20), new float[]{300, 0, 300}, 5000);
//            controleSensores.novaLeituraSensores(0, PApplet.radians(-25), new float[]{300, 0, 300}, 6000);
//            controleSensores.novaLeituraSensores(0, PApplet.radians(0), new float[]{300, 0, 300}, 7000);
//        } catch (Exception ex) {
//            Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
//        }
        viewer2D.redraw();
    }

//    private void initMapa(){
//        //
//        //Inicializa Obstaculos
//        //
//        Obstaculos obstaculos = new Obstaculos();
//        ObstaculosDrawable obstaculosDrawable = new ObstaculosDrawable(obstaculos);
//        
//        //
//        //Iniciliza o Robo
//        //
//        Robo robo = new Robo(400, 500, new Ponto(-200, 200));
//        robo.addSensorIR(new SensorIR(new Ponto(200, -200), PApplet.radians(-60), 200, 1500));
////        robo.addSensorIR(new SensorIR(new Ponto(20, -10), PApplet.radians(-30), 20, 150));
//        robo.addSensorIR(new SensorIR(new Ponto(200, 0), PApplet.radians(0), 200, 1500));
////        robo.addSensorIR(new SensorIR(new Ponto(20, 10), PApplet.radians(30), 20, 150));
//        robo.addSensorIR(new SensorIR(new Ponto(200, 200), PApplet.radians(60), 200, 1500));
//        
//        RoboDrawable roboDrawable = new RoboDrawable(robo);
//
//        mapa = new Mapa(robo, obstaculos);
//    }
    private void initMapa() {

        //
        //Inicializa Obstaculos
        //
        Obstaculos obstaculos = new Obstaculos();
        ObstaculosDrawable obstaculosDrawable = new ObstaculosDrawable(obstaculos);

        //
        //Inicializa o Robo
        //
        Robo robo = new Robo(400, 500, new Ponto(-200, 200));
        robo.addSensorIR(new SensorIR(new Ponto(200, -200), PApplet.radians(-60), 200, 1500));
//        robo.addSensorIR(new SensorIR(new Ponto(20, -10), PApplet.radians(-30), 20, 150));
        robo.addSensorIR(new SensorIR(new Ponto(200, 0), PApplet.radians(0), 200, 1500));
//        robo.addSensorIR(new SensorIR(new Ponto(20, 10), PApplet.radians(30), 20, 150));
        robo.addSensorIR(new SensorIR(new Ponto(200, 200), PApplet.radians(60), 200, 1500));

        RoboDrawable roboDrawable = new RoboDrawable(robo);

        mapa = new Mapa(robo, obstaculos);

        //
        //Inicializa o Viewer2D e Adiciona os Drawable2D a ele
        //
        viewer2D = new Viewer2D();
        viewer2D.addDrawable2D(obstaculosDrawable);
        viewer2D.addMouseListener2D(obstaculosDrawable);
        viewer2D.addDrawable2D(roboDrawable);
        viewer2D.addDrawable2D(new EscalaDrawable());
        viewer2D.addDrawable2D(new GridDrawable());
        viewer2D.init();

//        jPanelPricipal.setLayout(new BorderLayout());
        mainPanel.add(viewer2D, BorderLayout.CENTER);
        //
        // Inicializa o controle dos sensores
        //
        controleSensores = new ControleSensores(robo, obstaculos); //Sem filtragem de ruidos por filtro de Kalman
    }

    /**
     * Inicializa os listeners.
     */
    private void initListeners() {
        //
        //Labels presentes no canto inferior da janela.
        //
        JLabelListener bottomLabel1 = new JLabelListener() {
            @Override
            public void changeEventReceived(MyChangeEvent evt) {
                if (evt.getSource() instanceof ControleSensores) {
                    ControleSensores controle = (ControleSensores) evt.getSource();
                    //Atualiza a taxa de amostragem
                    this.setText(String.format("Amostragem Robô: %.2f/s ",
                                               controle.getTaxaAmostragemRobo()));
                }
            }
        };
        JLabelListener bottomLabel2 = new JLabelListener() {
            @Override
            public void changeEventReceived(MyChangeEvent evt) {
                if (evt.getSource() instanceof ControleSensores) {
                    ControleSensores controle = (ControleSensores) evt.getSource();
                    //Atualiza a taxa de amostragem
                    this.setText(String.format("Amostragem Estação: %.2f/s ",
                                               controle.getTaxaAmostragemEstacaoBase()));
                }
                if (evt.getSource() instanceof ContadorAmostragemTempoReal) {
                    ContadorAmostragemTempoReal c = (ContadorAmostragemTempoReal) evt.getSource();
                    this.setText(String.format("Amostragem Estação: %.2f/s ",
                                               c.getSample_rate()));
                }
            }
        };
        JLabelListener bottomLabel3 = new JLabelListener() {
            @Override
            public void changeEventReceived(MyChangeEvent evt) {
                if (evt.getSource() instanceof ControleSensores) {
                    ControleSensores controle = (ControleSensores) evt.getSource();
                    //Atualiza o numero de amostras gravadas
                    this.setText(String.format("Gravadas: %d ",
                                               controle.getLeituras_gravadas()));
                }
            }
        };
        JLabelListener bottomLabel4 = new JLabelListener() {
            @Override
            public void changeEventReceived(MyChangeEvent evt) {
                if (evt.getSource() instanceof ControleSensores) {
                    ControleSensores controle = (ControleSensores) evt.getSource();
                    //Atualiza o numero de amostras descartadas
                    this.setText(String.format("Descartadas: %d ",
                                               controle.getLeituras_descartadas()));
                }
            }
        };
        //Adiciona as labels (listeners) aos objetos.
        controleSensores.addMyChangeListener(bottomLabel1);
        controleSensores.addMyChangeListener(bottomLabel2);
        controleSensores.addMyChangeListener(bottomLabel3);
        controleSensores.addMyChangeListener(bottomLabel4);
        controleSensores.getAmostragemEstacaoBase().addMyChangeListener(bottomLabel2);

        //Adiciona as labels à barra inferior.
        bottomToolBar.add(bottomLabel1);
        bottomToolBar.add(new javax.swing.JToolBar.Separator());
        bottomToolBar.add(bottomLabel2);
        bottomToolBar.add(new javax.swing.JToolBar.Separator());
        bottomToolBar.add(bottomLabel3);
        bottomToolBar.add(new javax.swing.JToolBar.Separator());
        bottomToolBar.add(bottomLabel4);

        //Adiciona outros listeners aos objetos.
        controleSensores.addMyChangeListener(janelaSensores);
        controleSensores.addMyChangeListener(recordButton);
        controleSensores.addMyChangeListener(sensoresButtonListener);
        connector.addMyChangeListener(sensoresButtonListener);
        connector.addMyChangeListener(janelaSensores);
        connector.addMyChangeListener(connectionButtonListener);
        connector.addMyChangeListener(recordButton);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        topToolBar = new javax.swing.JToolBar();
        novoButton = new javax.swing.JButton();
        openButton = new javax.swing.JButton();
        saveButton = new javax.swing.JButton();
        topSeparator1 = new javax.swing.JToolBar.Separator();
        connectionButtonListener = new visual.gui.ConnectionButtonListener();
        recordButton = new visual.gui.RecordButtonListener();
        sensoresButtonListener = new visual.gui.SensoresButtonListener();
        mainPanel = new javax.swing.JPanel();
        bottomToolBar = new javax.swing.JToolBar();

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Bellator");
        setMinimumSize(new java.awt.Dimension(530, 250));

        topToolBar.setFloatable(false);
        topToolBar.setRollover(true);

        novoButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/document-new.png"))); // NOI18N
        novoButton.setText("Novo");
        novoButton.setFocusable(false);
        novoButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        novoButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        novoButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                novoButtonActionPerformed(evt);
            }
        });
        topToolBar.add(novoButton);

        openButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/document-open.png"))); // NOI18N
        openButton.setText("Abrir");
        openButton.setFocusable(false);
        openButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        openButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        openButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openButtonActionPerformed(evt);
            }
        });
        topToolBar.add(openButton);

        saveButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/document-save.png"))); // NOI18N
        saveButton.setText("Salvar");
        saveButton.setFocusable(false);
        saveButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        saveButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        saveButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveButtonActionPerformed(evt);
            }
        });
        topToolBar.add(saveButton);
        topToolBar.add(topSeparator1);

        connectionButtonListener.setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/wifi2-red.png"))); // NOI18N
        connectionButtonListener.setText("Conexão");
        connectionButtonListener.setToolTipText("Desconectado.");
        connectionButtonListener.setFocusable(false);
        connectionButtonListener.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        connectionButtonListener.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        connectionButtonListener.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectionButtonListenerActionPerformed(evt);
            }
        });
        topToolBar.add(connectionButtonListener);

        recordButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/gtk-media-record.png"))); // NOI18N
        recordButton.setText("NÃO gravando");
        recordButton.setFocusable(false);
        recordButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        recordButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        recordButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                recordButtonActionPerformed(evt);
            }
        });
        topToolBar.add(recordButton);

        sensoresButtonListener.setIcon(new javax.swing.ImageIcon(getClass().getResource("/visual/gui/icons/camera-web.png"))); // NOI18N
        sensoresButtonListener.setText("Sensores");
        sensoresButtonListener.setFocusable(false);
        sensoresButtonListener.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        sensoresButtonListener.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        sensoresButtonListener.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sensoresButtonListenerActionPerformed(evt);
            }
        });
        topToolBar.add(sensoresButtonListener);

        mainPanel.setLayout(new java.awt.BorderLayout());

        bottomToolBar.setFloatable(false);
        bottomToolBar.setRollover(true);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(topToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(mainPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(bottomToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(topToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(mainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 322, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(bottomToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void saveButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveButtonActionPerformed
        // TODO add your handling code here:
        int returnVal = fc.showSaveDialog(saveButton);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                mapa.save(file.getAbsolutePath());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "ERRO: " + ex.getMessage());
                Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            JOptionPane.showMessageDialog(this, "Arquivo \"" + file.getName() + "\" salvo com sucesso!");
            saved = true;
        }
    }//GEN-LAST:event_saveButtonActionPerformed

    private void openButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openButtonActionPerformed
        int returnVal = fc.showOpenDialog(openButton);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            if (!saved) {
                int response = JOptionPane.showConfirmDialog(this,
                                                             "O mapa atual não foi salvo. Você deseja carregar o mapa do arquivo \"" + file.getName() + "\"  e perder todas as alterações?",
                                                             "Mapa não salvo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION)
                    return;
            } else {
                int response = JOptionPane.showConfirmDialog(this,
                                                             "Você deseja realmente fechar este mapa e carregar o arquivo \"" + file.getName() + "\"?",
                                                             "Carregar mapa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (response != JOptionPane.YES_OPTION)
                    return;
            }
            try {
                if (recordButton.isSelected()) { //Desabilita a gravação de movimentos se ela estiver ativa
                    recordButton.doClick();
                }
                //Carrega o mapa a partir do arquivo
                mapa.load(file.getAbsolutePath());
            } catch (FileNotFoundException ex) {
                JOptionPane.showMessageDialog(this, "ERRO: " + ex.getMessage());
                Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "ERRO: " + ex.getMessage());
                Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
                return;
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "ERRO: " + ex.getMessage());
                Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
                return;
            }
            viewer2D.resetView();
            JOptionPane.showMessageDialog(this, "Arquivo \"" + file.getName() + "\" carregado com sucesso!");
            saved = true;
        }
    }//GEN-LAST:event_openButtonActionPerformed

    private void novoButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_novoButtonActionPerformed
        if (!saved) {
            int response = JOptionPane.showConfirmDialog(this,
                                                         "O mapa atual não foi salvo. Você deseja fazer um novo mapa e perder todas as alterações?",
                                                         "Mapa não salvo", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (response != JOptionPane.YES_OPTION)
                return;
        }
        if (recordButton.isSelected()) { //Desabilita a gravação de movimentos se ela estiver ativa
            recordButton.doClick();
        }
//        initMapaTeste();
        mapa.getRobo().clearPosInfos();
        mapa.getObstaculos().reset();
        viewer2D.resetView();
    }//GEN-LAST:event_novoButtonActionPerformed

    private void recordButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_recordButtonActionPerformed
        // TODO add your handling code here:
        boolean selected = recordButton.isSelected();
        if (selected) {
            controleSensores.startRecord();
            recordButton.setText("Gravando");
            recordButton.setToolTipText("O programa está gravando movimentos do robô.");
            saved = false;
        } else {
            controleSensores.stopRecord();
            recordButton.setText("Não Gravando");
            recordButton.setToolTipText("O programa não está gravando movimentos do robô.");
        }
//        System.out.println(controleSensores.isRecordEnabled());
    }//GEN-LAST:event_recordButtonActionPerformed

    private void connectionButtonListenerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectionButtonListenerActionPerformed
        janelaConexao.setVisible(true);
    }//GEN-LAST:event_connectionButtonListenerActionPerformed

    private void sensoresButtonListenerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sensoresButtonListenerActionPerformed
        // TODO add your handling code here:
        janelaSensores.setVisible(true);
    }//GEN-LAST:event_sensoresButtonListenerActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(JanelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JanelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JanelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JanelaPrincipal.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new JanelaPrincipal().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToolBar bottomToolBar;
    private visual.gui.ConnectionButtonListener connectionButtonListener;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JButton novoButton;
    private javax.swing.JButton openButton;
    private visual.gui.RecordButtonListener recordButton;
    private javax.swing.JButton saveButton;
    private visual.gui.SensoresButtonListener sensoresButtonListener;
    private javax.swing.JToolBar.Separator topSeparator1;
    private javax.swing.JToolBar topToolBar;
    // End of variables declaration//GEN-END:variables
}

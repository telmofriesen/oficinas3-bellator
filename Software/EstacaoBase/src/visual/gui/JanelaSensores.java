package visual.gui;

import comm.TR_ClientConnector;
import controle.ControleCamera;
import controle.ControleSensores;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

/**
 * Janela responsável pela configuração dos sensores.
 *
 * @author stefan
 */
public class JanelaSensores extends javax.swing.JFrame implements MyChangeListener {

    /**
     * Método chamado quando é recebido um evento de mudança.
     *
     * @param evt
     */
    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        //Se o status da conexão mudar...
        if (evt.getSource() instanceof TR_ClientConnector) {
            TR_ClientConnector c = (TR_ClientConnector) evt.getSource();
            if (!c.isConnected()) {
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        //Fecha a janela se a conexão for perdida.
                        setVisible(false);
                    }
                });
            } else {
                synchronized (this) {
                    if (lastConnectionStatus != c.getConnectionStatus() && c.isConnected()) {
                        //Envia o valor de sample rate ao robô quando a conexão for iniciada
                        sendSampleRateValue();
                        c.sendMessageWithPriority("SENSORS STATUS REQUEST", true);
                        c.sendMessageWithPriority("WEBCAM STATUS REQUEST", true);
                    }
                }
            }
            synchronized (this) {
                lastConnectionStatus = c.getConnectionStatus();
            }
        }
        //Se o status em ControleSensores mudar...
        if (evt.getSource() instanceof ControleSensores) {
            ControleSensores contr = (ControleSensores) evt.getSource();
            int status = contr.getSensorSampleStatus();
            switch (status) {
                case ControleSensores.SAMPLE_STOPPED:
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            sensorsStatusLabel.setText("Recebimento de amostras: Desativado.");
                            sensorsActivateButton.setEnabled(true);
                            sensorsActivateButton.setText("ATIVAR");
                        }
                    });
                    break;
                case ControleSensores.SAMPLE_CHANGING:
                    synchronized (this) {
                        if (lastSamplingStatus == ControleSensores.SAMPLE_STOPPED) {
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    sensorsStatusLabel.setText("Recebimento de amostras: Ativando...");
                                    sensorsActivateButton.setText("Ativando...");
                                }
                            });
                        }
                        if (lastSamplingStatus == ControleSensores.SAMPLE_STARTED) {
                            java.awt.EventQueue.invokeLater(new Runnable() {
                                public void run() {
                                    sensorsStatusLabel.setText("Recebimento de amostras: Desativando...");
                                    sensorsActivateButton.setText("Desativando...");
                                }
                            });
                        }
                        lastSamplingStatus = status;
                    }
//                    java.awt.EventQueue.invokeLater(new Runnable() {
//                        public void run() {
//                            sensorsActivateButton.setEnabled(false);
//                        }
//                    });
                    break;
                case ControleSensores.SAMPLE_STARTED:
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            sensorsStatusLabel.setText("Recebimento de amostras: Ativado.");
                            sensorsActivateButton.setEnabled(true);
                            sensorsActivateButton.setText("DESATIVAR");
                        }
                    });
                    break;
            }
        }
        //Se o status em ControleSensores mudar...
        if (evt.getSource() instanceof ControleCamera) {
            ControleCamera cc = (ControleCamera) evt.getSource();
            final boolean sampling_enabled = cc.isSampling_enabled();
            final boolean sampling_status_changing = cc.isSampling_status_changing();
            final boolean webcam_available = cc.isWebcam_available();
            final String webcam_name = cc.getWebcam_name();
            synchronized (this) {
                if (sampling_enabled != lastCameraSamplingStatus) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            if (sampling_status_changing) {
                                if (lastCameraSamplingStatus == false)
                                    cameraSamplingStatusLabel.setText("Recebimento de amostras: Ativando...");
                                else
                                    cameraSamplingStatusLabel.setText("Recebimento de amostras: Desativando...");
                            } else if (sampling_enabled) {
                                cameraSamplingStatusLabel.setText("Recebimento de amostras: Ativado.");
                                webcamActivateButton.setText("DESATIVAR");
                            } else {
                                cameraSamplingStatusLabel.setText("Recebimento de amostras: Desativado.");
                                webcamActivateButton.setText("ATIVAR");
                            }
                        }
                    });
                    lastCameraSamplingStatus = sampling_enabled;
                }
                if (lastCameraAvailableStatus != webcam_available) {
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            if (!webcam_available)
                                cameraStatusLabel.setText("Status da webcam: Desconectada");
                            else
                                cameraStatusLabel.setText(String.format("Status da webcam: Conectada (%s)", webcam_name));
                        }
                    });
                    lastCameraAvailableStatus = webcam_available;
                }
            }
        }
    }
    int lastSamplingStatus = -1;
    int lastConnectionStatus = -1;
    boolean lastCameraAvailableStatus = false;
    boolean lastCameraSamplingStatus = false;
    ControleSensores controleSensores;
    ControleCamera controleCamera;
    TR_ClientConnector connector;

    /**
     * Creates new form JanelaSensores
     */
    public JanelaSensores(ControleSensores controle, ControleCamera controleCamera, TR_ClientConnector connector) {
        this.controleSensores = controle;
        this.controleCamera = controleCamera;
        this.connector = connector;
        initComponents();
//        controle.addMyChangeListener(this);
        //Valores minimo e maximo do FloatJSlider.
        sampleRateSlider.setF_min(0.2f);
        sampleRateSlider.setF_max(10f);
//        sampleRateSlider.setValue(100);
        sampleRateSlider.setFloatValue(1);
        sampleRateTextField.setText(String.format("%.1f", sampleRateSlider.getFloatValue()));

        webcamFramerateSlider.setF_min(0.2f);
        webcamFramerateSlider.setF_max(50f);
//        webcamSampleRateSlider.setValue(100);
        webcamFramerateSlider.setFloatValue(20);
        webcamFramerateTextField.setText(String.format("%.1f", webcamFramerateSlider.getFloatValue()));
        webcamBitrateSlider.setValue(2048);
        webcamBitrateTextField.setText(String.format("%d", webcamBitrateSlider.getValue()));
        webcamResolutionComboBox.setSelectedIndex(4);
        webcamFramerateSliderStateChanged(new ChangeEvent(webcamFramerateSlider));
        webcamBitrateSliderStateChanged(new ChangeEvent(webcamBitrateSlider));
        webcamResolutionComboBoxActionPerformed(null);
//        jToolBar1.setVisible(false);
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

        sensorsStatusLabel = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jToolBar1 = new javax.swing.JToolBar();
        sensorsActivateButton = new javax.swing.JButton();
        sampleRateSlider = new visual.gui.FloatJSlider();
        sampleRateTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel3 = new javax.swing.JLabel();
        cameraStatusLabel = new javax.swing.JLabel();
        cameraSamplingStatusLabel = new javax.swing.JLabel();
        webcamActivateButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        webcamBitrateTextField = new javax.swing.JTextField();
        webcamBitrateSlider = new javax.swing.JSlider();
        jLabel4 = new javax.swing.JLabel();
        webcamFramerateSlider = new visual.gui.FloatJSlider();
        webcamFramerateTextField = new javax.swing.JTextField();
        webcamResolutionComboBox = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();

        setTitle("Bellator - Sensores");

        sensorsStatusLabel.setText("Recebimento de amostras: Desativado");

        jLabel2.setText("Taxa de amostragem (amostras/s):");

        jToolBar1.setFloatable(false);
        jToolBar1.setBorderPainted(false);
        jToolBar1.setEnabled(false);
        jToolBar1.setFocusable(false);
        jToolBar1.setMinimumSize(new java.awt.Dimension(0, 0));
        jToolBar1.setPreferredSize(new java.awt.Dimension(0, 0));

        sensorsActivateButton.setText("Ativar");
        sensorsActivateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sensorsActivateButtonActionPerformed(evt);
            }
        });

        sampleRateSlider.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sampleRateSliderMouseReleased(evt);
            }
        });
        sampleRateSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                sampleRateSliderStateChanged(evt);
            }
        });

        sampleRateTextField.setEditable(false);
        sampleRateTextField.setText("1");
        sampleRateTextField.setFocusable(false);
        sampleRateTextField.setMinimumSize(new java.awt.Dimension(20, 26));
        sampleRateTextField.setPreferredSize(new java.awt.Dimension(40, 26));
        sampleRateTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleRateTextFieldActionPerformed(evt);
            }
        });

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("-- SENSORES --");

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel3.setText("-- WEBCAM --");

        cameraStatusLabel.setText("Status da webcam:  Desconectada");

        cameraSamplingStatusLabel.setText("Recebimento de amostras: Desativado");

        webcamActivateButton.setText("Ativar");
        webcamActivateButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webcamActivateButtonActionPerformed(evt);
            }
        });

        jLabel6.setText("Bitrate (kbps):");

        webcamBitrateTextField.setEditable(false);
        webcamBitrateTextField.setText("1");
        webcamBitrateTextField.setPreferredSize(new java.awt.Dimension(40, 26));

        webcamBitrateSlider.setMaximum(10000);
        webcamBitrateSlider.setMinimum(10);
        webcamBitrateSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                webcamBitrateSliderStateChanged(evt);
            }
        });

        jLabel4.setText("Framerate (FPS):");

        webcamFramerateSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                webcamFramerateSliderStateChanged(evt);
            }
        });

        webcamFramerateTextField.setEditable(false);
        webcamFramerateTextField.setText("1");
        webcamFramerateTextField.setFocusable(false);
        webcamFramerateTextField.setMinimumSize(new java.awt.Dimension(20, 26));
        webcamFramerateTextField.setPreferredSize(new java.awt.Dimension(40, 26));
        webcamFramerateTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webcamFramerateTextFieldActionPerformed(evt);
            }
        });

        webcamResolutionComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "176x144", "320x240", "352x288", "480x400", "640x480", "1024x768" }));
        webcamResolutionComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                webcamResolutionComboBoxActionPerformed(evt);
            }
        });

        jLabel5.setText("Resolução:");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel2))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sampleRateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 291, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sampleRateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(sensorsStatusLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sensorsActivateButton))
                            .addGroup(layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(webcamBitrateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 288, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(webcamBitrateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jSeparator2)
                            .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cameraStatusLabel)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(cameraSamplingStatusLabel)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(webcamActivateButton))
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel4)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(webcamFramerateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, 288, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(webcamFramerateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addComponent(jLabel5)
                                    .addComponent(webcamResolutionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(0, 0, Short.MAX_VALUE)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jToolBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(sensorsStatusLabel)
                    .addComponent(sensorsActivateButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(sampleRateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(sampleRateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(cameraStatusLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cameraSamplingStatusLabel)
                    .addComponent(webcamActivateButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(webcamFramerateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(webcamFramerateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(webcamBitrateSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(webcamBitrateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(webcamResolutionComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(50, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void sensorsActivateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sensorsActivateButtonActionPerformed
        // TODO add your handling code here:
        int status = controleSensores.getSensorSampleStatus();
        switch (status) {
            case ControleSensores.SAMPLE_STOPPED:
                controleSensores.setSensorSampleStatus(ControleSensores.SAMPLE_CHANGING);
                connector.sendMessageWithPriority("SENSORS START", true);
                break;
            case ControleSensores.SAMPLE_STARTED:
                controleSensores.setSensorSampleStatus(ControleSensores.SAMPLE_CHANGING);
                connector.sendMessageWithPriority("SENSORS STOP", true);
                break;
        }
    }//GEN-LAST:event_sensorsActivateButtonActionPerformed

    private void sampleRateTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleRateTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sampleRateTextFieldActionPerformed

    private void sampleRateSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sampleRateSliderStateChanged
        // TODO add your handling code here:
        sampleRateTextField.setText(String.format("%.1f", sampleRateSlider.getFloatValue()));
        if (!sampleRateSlider.getValueIsAdjusting()) {
            //Ajusta o valor do sample rate no robô
            sendSampleRateValue();
        }
    }//GEN-LAST:event_sampleRateSliderStateChanged

    private void sampleRateSliderMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sampleRateSliderMouseReleased
        // TODO add your handling code here:
    }//GEN-LAST:event_sampleRateSliderMouseReleased

    private void webcamActivateButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webcamActivateButtonActionPerformed
        // TODO add your handling code here:
        boolean sampling_enabled = controleCamera.isSampling_enabled();
        controleCamera.setSampling_status_changing(true);
        if (sampling_enabled) {
            connector.sendMessageWithPriority("WEBCAM STOP", true);
        } else {
            connector.sendMessageWithPriority("WEBCAM START", true);
        }
    }//GEN-LAST:event_webcamActivateButtonActionPerformed

    private void webcamBitrateSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_webcamBitrateSliderStateChanged
        webcamBitrateTextField.setText(String.format("%d", webcamBitrateSlider.getValue()));
        if (!webcamBitrateSlider.getValueIsAdjusting()) {
            //Ajusta o valor do sample rate no robô
            sendWebcamBitrateValue();
        }
    }//GEN-LAST:event_webcamBitrateSliderStateChanged

    private void webcamFramerateTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webcamFramerateTextFieldActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_webcamFramerateTextFieldActionPerformed

    private void webcamFramerateSliderStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_webcamFramerateSliderStateChanged
        // TODO add your handling code here:
        webcamFramerateTextField.setText(String.format("%.1f", webcamFramerateSlider.getFloatValue()));
        if (!webcamFramerateSlider.getValueIsAdjusting()) {
            //Ajusta o valor do sample rate no robô
            sendWebcamFramerateValue();
        }
    }//GEN-LAST:event_webcamFramerateSliderStateChanged

    private void webcamResolutionComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_webcamResolutionComboBoxActionPerformed
        // TODO add your handling code here:
        sendWebcamResolutionValue();
    }//GEN-LAST:event_webcamResolutionComboBoxActionPerformed

    /**
     * Envia o valor de sample rate (especificado na configuração) para o robô.
     */
    public void sendSampleRateValue() {
        if (connector.isConnected()) {
            connector.sendMessage(String.format("SENSORS SAMPLE_RATE %.1f", sampleRateSlider.getFloatValue()), true);
        }
    }

    /**
     * Envia o valor de bitrate da webcam (especificado na configuração) para o robô.
     */
    public void sendWebcamFramerateValue() {
        if (connector.isConnected()) {
            connector.sendMessage(String.format("WEBCAM FRAMERATE %.1f", webcamFramerateSlider.getFloatValue()), true);
        }
    }

    /**
     * Envia o valor de bitrate da webcam (especificado na configuração) para o robô.
     */
    public void sendWebcamBitrateValue() {
        if (connector.isConnected()) {
            connector.sendMessage(String.format("WEBCAM BITRATE %d", webcamBitrateSlider.getValue()), true);
        }
    }

    public void sendWebcamResolutionValue() {
        if (connector.isConnected()) {
            Object selectedItem = webcamResolutionComboBox.getSelectedItem();
            if (selectedItem != null) {
                connector.sendMessage(String.format("WEBCAM RESOLUTION %s", selectedItem.toString()), true);
            }
        }
    }

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
            java.util.logging.Logger.getLogger(JanelaSensores.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(JanelaSensores.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(JanelaSensores.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(JanelaSensores.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
//                new JanelaSensores().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel cameraSamplingStatusLabel;
    private javax.swing.JLabel cameraStatusLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JToolBar jToolBar1;
    private visual.gui.FloatJSlider sampleRateSlider;
    private javax.swing.JTextField sampleRateTextField;
    private javax.swing.JButton sensorsActivateButton;
    private javax.swing.JLabel sensorsStatusLabel;
    private javax.swing.JButton webcamActivateButton;
    private javax.swing.JSlider webcamBitrateSlider;
    private javax.swing.JTextField webcamBitrateTextField;
    private visual.gui.FloatJSlider webcamFramerateSlider;
    private javax.swing.JTextField webcamFramerateTextField;
    private javax.swing.JComboBox webcamResolutionComboBox;
    // End of variables declaration//GEN-END:variables
}

package visual.gui;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamPanel;
import comm.TR_ClientCommandInterpreter;
import comm.TR_ClientConnector;
import controle.*;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import processing.core.PApplet;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;
import uk.co.caprica.vlcj.player.list.MediaListPlayer;
import uk.co.caprica.vlcj.player.list.MediaListPlayerEventListener;
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
    private ControleCamera controleCamera;
    private ControleMotores controleMotores;
    private JanelaConexao janelaConexao;
    private TR_ClientConnector connector;
    private JanelaSensores janelaSensores;
    private ControleSensoresListener infoListenerControleSensores;
    private CameraInfoListener cameraInfoListener;
    private MovementKeyboardListener movementKeyboardListener;
    private MotoresListener motoresListener;
//    private Player webcamPlayer;
//    private EmbeddedMediaPlayerComponent mediaPlayerComponent;
//    Canvas canvas;
    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;
    private VideoInfoListener videoInfoListener;

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
        controleCamera = new ControleCamera();
        controleMotores = new ControleMotores();
        TR_ClientCommandInterpreter interpreter = new TR_ClientCommandInterpreter(connector, controleSensores, controleCamera);
        connector.setInterpreter(interpreter);
        //Inicializa as janelas de configuração.
        janelaConexao = new JanelaConexao(connector);
        janelaSensores = new JanelaSensores(controleSensores, controleCamera, connector);
        //Inicia as threads.
        interpreter.start();
        connector.start();

        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        webcamImagePanel.add(mediaPlayerComponent, BorderLayout.CENTER);
        mediaPlayerComponent.setSize(webcamImagePanel.getSize());
        webcamImagePanel.addComponentListener(new webcamImagePanelListener());
//        mediaPlayerComponent.getMediaPlayer().addMediaPlayerEventListener();

//        MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
//        canvas = new Canvas();
//        canvas.setBackground(Color.black);
//        canvas.setSize(320, 180);
//        CanvasVideoSurface videoSurface = mediaPlayerFactory.newVideoSurface(canvas);
//        mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
//        mediaPlayer.setVideoSurface(videoSurface);
//        webcamImagePanel.add(canvas, BorderLayout.CENTER);



        //TESTE DE WEBCAM
//        controleCamera = new ControleCamera();

//        Thread t = new Thread(new Runnable() {
//            private Webcam webcam;
//
//            @Override
//            public void run() {
//                webcam = Webcam.getDefault();
//                webcam.setViewSize(new Dimension(320, 240));
//                webcam.open();
//                while (true) {
//                    BufferedImage image = webcam.getImage();
////                    System.out.printf("%dX%d\n", image.getWidth(), image.getHeight());
//                    controleCamera.novaImagemCamera(image, 1000);
//                }
//            }
//        });
//        t.start();

        //Inicializa listeners.
        initListeners();

        this.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                System.out.println(e.getKeyCode());
            }

            @Override
            public void keyReleased(KeyEvent e) {
            }
        });

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
        mapLeftPanel.add(viewer2D, BorderLayout.CENTER);
        //
        // Inicializa o controle dos sensores
        //
        controleSensores = new ControleSensores(robo, obstaculos); //Sem filtragem de ruidos por filtro de Kalman
    }

    /**
     * Inicializa os listeners.
     */
    private void initListeners() {
        infoListenerControleSensores = new ControleSensoresListener();
        cameraInfoListener = new CameraInfoListener();
        videoInfoListener = new VideoInfoListener();
        videoInfoListener.start();
        movementKeyboardListener = new MovementKeyboardListener();
        motoresListener = new MotoresListener();
        //Adiciona os listeners aos objetos.
        controleSensores.addMyChangeListener(infoListenerControleSensores);
        controleSensores.getAmostragemEstacaoBase().addMyChangeListener(infoListenerControleSensores);
        controleSensores.addMyChangeListener(janelaSensores);
        controleSensores.addMyChangeListener(recordButton);
        controleSensores.addMyChangeListener(sensoresButtonListener);
        controleCamera.addMyChangeListener(cameraInfoListener);
        controleCamera.addMyChangeListener(janelaSensores);
        controleCamera.getContadorByterate().addMyChangeListener(cameraInfoListener);
        controleCamera.getContadorFramerate().addMyChangeListener(cameraInfoListener);
//        imagePanelListener1.addMyChangeListener(cameraInfoListener);
//        controleCamera.addMyChangeListener(imagePanelListener1);
        connector.addMyChangeListener(sensoresButtonListener);
        connector.addMyChangeListener(janelaSensores);
        connector.addMyChangeListener(connectionButtonListener);
        connector.addMyChangeListener(recordButton);

        viewer2D.addKeyboardListener(movementKeyboardListener);
        movementKeyboardListener.addMyChangeListener(controleMotores);
        controleMotores.addMyChangeListener(motoresListener);

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
        bottomToolBar = new javax.swing.JToolBar();
        bottomLabel1 = new javax.swing.JLabel();
        jSeparator1 = new javax.swing.JToolBar.Separator();
        bottomLabel2 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JToolBar.Separator();
        bottomLabel3 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JToolBar.Separator();
        bottomLabel4 = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        mapLeftPanel = new javax.swing.JPanel();
        jSplitPane2 = new javax.swing.JSplitPane();
        topRightPanel = new javax.swing.JPanel();
        webcamTopLabel = new javax.swing.JLabel();
        webcamBottomLabel = new javax.swing.JLabel();
        webcamImagePanel = new javax.swing.JLayeredPane();
        bottomRightPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        movementImagePanel = new visual.gui.ImagePanel();

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

        bottomToolBar.setFloatable(false);
        bottomToolBar.setRollover(true);

        bottomLabel1.setText("Amostragem robô: ");
        bottomToolBar.add(bottomLabel1);
        bottomToolBar.add(jSeparator1);

        bottomLabel2.setText("Amostragem estação: ");
        bottomToolBar.add(bottomLabel2);
        bottomToolBar.add(jSeparator2);

        bottomLabel3.setText("Gravadas: ");
        bottomToolBar.add(bottomLabel3);
        bottomToolBar.add(jSeparator3);

        bottomLabel4.setText("Descartadas: ");
        bottomToolBar.add(bottomLabel4);

        mapLeftPanel.setMinimumSize(new java.awt.Dimension(500, 200));
        mapLeftPanel.setName(""); // NOI18N
        mapLeftPanel.setPreferredSize(new java.awt.Dimension(500, 300));
        mapLeftPanel.setLayout(new java.awt.BorderLayout());
        jSplitPane1.setLeftComponent(mapLeftPanel);

        jSplitPane2.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);

        topRightPanel.setPreferredSize(new java.awt.Dimension(535, 200));

        webcamTopLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        webcamTopLabel.setText("WEBCAM [OFF] - ---X--- <- resolução");
        webcamTopLabel.setToolTipText("");
        webcamTopLabel.setMinimumSize(new java.awt.Dimension(0, 0));

        webcamBottomLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        webcamBottomLabel.setText("Framerate: ---- fps (--- kb/s).  Escala: -----");
        webcamBottomLabel.setMinimumSize(new java.awt.Dimension(0, 0));

        javax.swing.GroupLayout topRightPanelLayout = new javax.swing.GroupLayout(topRightPanel);
        topRightPanel.setLayout(topRightPanelLayout);
        topRightPanelLayout.setHorizontalGroup(
            topRightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, topRightPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(topRightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(webcamImagePanel)
                    .addComponent(webcamTopLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
                    .addComponent(webcamBottomLabel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 511, Short.MAX_VALUE))
                .addContainerGap())
        );
        topRightPanelLayout.setVerticalGroup(
            topRightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, topRightPanelLayout.createSequentialGroup()
                .addComponent(webcamTopLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(webcamImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, 156, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(webcamBottomLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane2.setTopComponent(topRightPanel);

        bottomRightPanel.setPreferredSize(new java.awt.Dimension(200, 200));

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("MOVIMENTAÇÃO");

        javax.swing.GroupLayout movementImagePanelLayout = new javax.swing.GroupLayout(movementImagePanel);
        movementImagePanel.setLayout(movementImagePanelLayout);
        movementImagePanelLayout.setHorizontalGroup(
            movementImagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        movementImagePanelLayout.setVerticalGroup(
            movementImagePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 166, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout bottomRightPanelLayout = new javax.swing.GroupLayout(bottomRightPanel);
        bottomRightPanel.setLayout(bottomRightPanelLayout);
        bottomRightPanelLayout.setHorizontalGroup(
            bottomRightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, bottomRightPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(bottomRightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(movementImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE))
                .addContainerGap())
        );
        bottomRightPanelLayout.setVerticalGroup(
            bottomRightPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(bottomRightPanelLayout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(movementImagePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setRightComponent(bottomRightPanel);

        jSplitPane1.setRightComponent(jSplitPane2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 741, Short.MAX_VALUE)
                    .addComponent(bottomToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(topToolBar, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(topToolBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1)
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
     * Classe responsável por escutar mudanças importantes em ControleSensores e atualizar o display de informações caso necessário.
     */
    class ControleSensoresListener implements MyChangeListener {

        @Override
        public void changeEventReceived(MyChangeEvent evt) {
            if (evt.getSource() instanceof ControleSensores) {
                final ControleSensores controle = (ControleSensores) evt.getSource();
                final float amostragemRobo = controle.getTaxaAmostragemRobo();
                final float amostragemEstacaoBase = controle.getTaxaAmostragemEstacaoBase();
                final int leiturasGravadas = controle.getLeituras_gravadas();
                final int leiturasDescartadas = controle.getLeituras_descartadas();
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        //Atualiza a taxa de amostragem
                        bottomLabel1.setText(String.format("Amostragem Robô: %.2f/s ", amostragemRobo));
                        //Atualiza a taxa de amostragem
                        bottomLabel2.setText(String.format("Amostragem Estação: %.2f/s ", amostragemEstacaoBase));
                        //Atualiza o numero de amostras gravadas
                        bottomLabel3.setText(String.format("Gravadas: %d ", leiturasGravadas));
                        //Atualiza o numero de amostras descartadas
                        bottomLabel4.setText(String.format("Descartadas: %d ", leiturasDescartadas));
                    }
                });
            }
            if (evt.getSource() instanceof ContadorAmostragemTempoReal) {
                final ContadorAmostragemTempoReal c = (ContadorAmostragemTempoReal) evt.getSource();
                final float sample_rate = c.getSample_rate();
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        bottomLabel2.setText(String.format("Amostragem Estação: %.2f/s ",
                                                           sample_rate));
                    }
                });
            }
        }
    }

    /**
     * Classe responsável por escutar mudanças importantes em ControleCamera e ImagePanelListener e atualizar o display de informações caso necessário.
     */
    class CameraInfoListener implements MyChangeListener {

        private float prevFramerate = 0;
        private float prevByterate = 0;
        private float prevEscala = 1;
        private boolean lastStreamStatus = false;
        private int lastStreamPort = -1;

        private void startStreamPlayer(final String ip, final int port) {
//            final String url = String.format("http://%s:%d", ip, port);
            final String url = String.format("rtp://@%s:%d", ip, port);
            System.out.println("---- INICIANDO STREAM: " + url);
            mediaPlayerComponent.getMediaPlayer().playMedia(url);
//            mediaPlayer.getVi
        }

        private void stopStreamPlayer() {
//            webcamPlayer.stop();
//            webcamPlayer.close();
            mediaPlayerComponent.getMediaPlayer().stop();
        }

        @Override
        public void changeEventReceived(MyChangeEvent evt) {
            if (evt.getSource() instanceof ControleCamera) {
                final ControleCamera c = (ControleCamera) evt.getSource();
                final boolean stream_available = c.isStream_available();
                final int stream_port = c.getStream_port();
                synchronized (this) {
                    //Se o status da stream mudar
                    if (stream_available != lastStreamStatus) {
                        if (stream_available) { //Abre o player
                            startStreamPlayer(connector.getHost(), stream_port);
                        } else { //Remove o player
                            stopStreamPlayer();
//                            java.awt.EventQueue.invokeLater(new Runnable() {
//                                public void run() {
//                                    webcamTopLabel.setText(String.format("WEBCAM [OFF]"));
//                                }
//                            });
                        }
                        lastStreamStatus = stream_available;
                        lastStreamPort = stream_port;
                    } else if (stream_port != lastStreamPort && stream_available) {
                        //Se a porta mudar, fecha e abre o player para rodar na nova porta.
                        stopStreamPlayer();
                        startStreamPlayer(connector.getHost(), stream_port);
                        lastStreamPort = stream_port;
                    }
                }
//                final Dimension dim;
//                if (c.getImage() != null) {
//                    dim = c.getImageDimension();
//                } else {
//                    dim = new Dimension(0, 0);
//                }

//                final boolean sampling_enabled = c.isSampling_enabled();
//                final boolean webcam_available = c.isWebcam_available();
//                java.awt.EventQueue.invokeLater(new Runnable() {
//                    public void run() {
//                        if (sampling_enabled && webcam_available) {
//                            webcamTopLabel.setText(String.format("WEBCAM [ON] (%dx%d)", dim.width, dim.height));
//                        } else {
//                            webcamTopLabel.setText(String.format("WEBCAM [OFF]"));
//                        }
//                    }
//                });
            }
//            if (evt.getSource() instanceof ContadorAmostragemTempoReal) {
//                final ContadorAmostragemTempoReal c = (ContadorAmostragemTempoReal) evt.getSource();
//                synchronized (this) {
//                    prevFramerate = c.getSample_rate();
//                }
//                java.awt.EventQueue.invokeLater(new Runnable() {
//                    public void run() {
//                        webcamBottomLabel.setText(String.format("Framerate: %.1f fps (%.1f kb/s).  Escala: %.2f",
//                                                                prevFramerate, prevByterate / 1000, prevEscala));
//                    }
//                });
//            }
//            if (evt.getSource() instanceof ContadorBytesTempoReal) {
//                final ContadorBytesTempoReal c = (ContadorBytesTempoReal) evt.getSource();
//                synchronized (this) {
//                    prevByterate = c.getByte_rate();
//                }
//                java.awt.EventQueue.invokeLater(new Runnable() {
//                    public void run() {
//                        webcamBottomLabel.setText(String.format("Framerate: %.1f fps (%.1f kb/s).  Escala: %.2f",
//                                                                prevFramerate, prevByterate / 1000, prevEscala));
//                    }
//                });
//            }
//            if (evt.getSource() instanceof ImagePanelListener) {
//                final ImagePanelListener i = (ImagePanelListener) evt.getSource();
//                synchronized (this) {
//                    prevEscala = i.getScaleFactor();
//                }
//                java.awt.EventQueue.invokeLater(new Runnable() {
//                    public void run() {
//                        webcamBottomLabel.setText(String.format("Framerate: %.1f fps (%.1f kb/s).  Escala: %.2f",
//                                                                prevFramerate, prevByterate / 1000, prevEscala));
//                    }
//                });
//            }
        }
    }

    /**
     * Muda o tamanho do mediaPlayer se o painel for redimensionado.
     */
    class webcamImagePanelListener implements ComponentListener {

        @Override
        public void componentResized(ComponentEvent e) {
            mediaPlayerComponent.setSize(e.getComponent().getSize());
        }

        @Override
        public void componentMoved(ComponentEvent e) {
        }

        @Override
        public void componentShown(ComponentEvent e) {
        }

        @Override
        public void componentHidden(ComponentEvent e) {
        }
    }

    /**
     * Escuta repetidamente por mudanças nas estatísticas do vídeo da webcam (FPS, etc...).
     */
    class VideoInfoListener extends Thread {

        @Override
        public void run() {
//            boolean sampling_enabled;
//            boolean webcam_available;
//            Dimension dim;
            long play_start_time = System.currentTimeMillis();
            final MediaPlayer m = mediaPlayerComponent.getMediaPlayer();
            while (true) {
                final boolean sampling_enabled = controleCamera.isSampling_enabled();
                final boolean webcam_available = controleCamera.isWebcam_available();
                final Dimension dim = mediaPlayerComponent.getMediaPlayer().getVideoDimension();
                final float fps = m.isPlaying()
                        ? m.getFps()
                        : 0f;
                final long time = (System.currentTimeMillis() - play_start_time) / 1000;
                final float kbps = m.isPlaying() && time > 0
                        ? (float) m.getMediaStatistics().i_read_bytes / (float) time / (float) 1024
                        : 0f;
                final float scale = m.isPlaying() && dim != null
                        ? Math.min((float) webcamImagePanel.getSize().width / (float) dim.width, (float) webcamImagePanel.getSize().height / (float) dim.height)
                        : 0f;
                java.awt.EventQueue.invokeLater(new Runnable() {
                    public void run() {
                        if (sampling_enabled && webcam_available && dim != null) {
                            webcamTopLabel.setText(String.format("WEBCAM [ON] (%dx%d)", dim.width, dim.height));
                        } else {
                            webcamTopLabel.setText(String.format("WEBCAM [OFF]"));
                        }
                        webcamBottomLabel.setText(String.format("Framerate: %.1f fps (%.1f kb/s).  Escala: %.2f",
                                                                fps, kbps, scale));
                    }
                });
                try {
                    sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
                }
                //Indica se entrou no loop 
                boolean a = false;
                while (!m.isPlaying()) {
                    try {
                        sleep(2000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(JanelaPrincipal.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    if (a = false) {
                        java.awt.EventQueue.invokeLater(new Runnable() {
                            public void run() {
                                webcamTopLabel.setText(String.format("WEBCAM [OFF]"));
                            }
                        });
                    }
                    a = true;
                }
                if (a) play_start_time = System.currentTimeMillis();
            }
        }
    }

    class MotoresListener implements MyChangeListener {

        int lastMovType = -1;

        @Override
        public void changeEventReceived(MyChangeEvent evt) {
            if (evt.getSource() instanceof ControleMotores) {
                ControleMotores c = (ControleMotores) evt.getSource();
                int movType = c.getMovementType();
                String icon = "";
                synchronized (this) {
                    if (movType != lastMovType) {
                        switch (movType) {
                            case ControleMotores.STOP:
                                icon = "/visual/gui/icons/arrows/stop.png";
                                break;
                            case ControleMotores.FORWARD:
                                icon = "/visual/gui/icons/arrows/forward.png";
                                break;
                            case ControleMotores.FORWARD_LEFT:
                                icon = "/visual/gui/icons/arrows/forward_left.png";
                                break;
                            case ControleMotores.FORWARD_RIGHT:
                                icon = "/visual/gui/icons/arrows/forward_right.png";
                                break;
                            case ControleMotores.BACKWARD:
                                icon = "/visual/gui/icons/arrows/backward.png";
                                break;
                            case ControleMotores.BACKWARD_LEFT:
                                icon = "/visual/gui/icons/arrows/backward_left.png";
                                break;
                            case ControleMotores.BACKWARD_RIGHT:
                                icon = "/visual/gui/icons/arrows/backward_right.png";
                                break;
                            case ControleMotores.ROTATE_LEFT:
                                icon = "/visual/gui/icons/arrows/rotate_left.png";
                                break;
                            case ControleMotores.ROTATE_RIGHT:
                                icon = "/visual/gui/icons/arrows/rotate_right.png";
                                break;
                        }
                    }
                    movementImagePanel.changeImageFromRelativePath(icon);
                    lastMovType = movType;
                }
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
    private javax.swing.JLabel bottomLabel1;
    private javax.swing.JLabel bottomLabel2;
    private javax.swing.JLabel bottomLabel3;
    private javax.swing.JLabel bottomLabel4;
    private javax.swing.JPanel bottomRightPanel;
    private javax.swing.JToolBar bottomToolBar;
    private visual.gui.ConnectionButtonListener connectionButtonListener;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JToolBar.Separator jSeparator1;
    private javax.swing.JToolBar.Separator jSeparator2;
    private javax.swing.JToolBar.Separator jSeparator3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JPanel mapLeftPanel;
    private visual.gui.ImagePanel movementImagePanel;
    private javax.swing.JButton novoButton;
    private javax.swing.JButton openButton;
    private visual.gui.RecordButtonListener recordButton;
    private javax.swing.JButton saveButton;
    private visual.gui.SensoresButtonListener sensoresButtonListener;
    private javax.swing.JPanel topRightPanel;
    private javax.swing.JToolBar.Separator topSeparator1;
    private javax.swing.JToolBar topToolBar;
    private javax.swing.JLabel webcamBottomLabel;
    private javax.swing.JLayeredPane webcamImagePanel;
    private javax.swing.JLabel webcamTopLabel;
    // End of variables declaration//GEN-END:variables
}

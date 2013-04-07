/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import dados.GerenciadorCamera;
import events.MyChangeEvent;
import events.MyChangeListener;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.JPanel;

/**
 * JPanel que mostra uma imagem.
 *
 * @author stefan
 */
public class ImagePanelListener extends JPanel implements MyChangeListener {

    private Image image = null;
    private Image scaledImage = null;
    private float scaleFactor = 1;
    //Listeners de eventos da classe
    private final CopyOnWriteArrayList<MyChangeListener> listeners;
//    private boolean imageChanged = false;

    public ImagePanelListener() {
        this.listeners = new CopyOnWriteArrayList<MyChangeListener>();
//        try {
//            image = ImageIO.read(new File("images/images.jpg"));
//        } catch (IOException ex) {
//            Logger.getLogger(ImagePanelListener.class.getName()).log(Level.SEVERE, null, ex);
//        }
    }

    @Override
    public void changeEventReceived(MyChangeEvent evt) {
        if (evt.getSource() instanceof GerenciadorCamera) {
            synchronized (this) {
                GerenciadorCamera c = (GerenciadorCamera) evt.getSource();
                image = c.getImage();
                if (image != null) {
                    //Redimensiona a imagem para caber no JPanel
                    scaleFactor = calculateScaleFactorToFit(new Dimension(image.getWidth(null), image.getHeight(null)), getSize());

                    int scaleWidth = Math.max(1, (int) Math.round(image.getWidth(null) * scaleFactor));
                    int scaleHeight = Math.max(1, (int) Math.round(image.getHeight(null) * scaleFactor));

                    scaledImage = image.getScaledInstance(scaleWidth, scaleHeight, Image.SCALE_DEFAULT);

//            imageChanged = true;
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            repaint();
                        }
                    });
                }
            }
            fireChangeEvent();
        }
    }

    public synchronized Dimension getImageDimension() {
        if (image != null) {
            return new Dimension(image.getWidth(this), image.getHeight(this));
        } else {
            return null;
        }
    }

    public synchronized float getScaleFactor() {
        return scaleFactor;
    }

    /**
     * Exemplo de: http://stackoverflow.com/questions/11959758/java-maintaining-aspect-ratio-of-jpanel-background-image
     *
     * @param iMasterSize
     * @param iTargetSize
     * @return
     */
    public float calculateScaleFactor(int iMasterSize, int iTargetSize) {
        return (float) iTargetSize / (float) iMasterSize;
    }

    public float calculateScaleFactorToFit(Dimension original, Dimension toFit) {

        float dScale = 1f;

        if (original != null && toFit != null) {

            float dScaleWidth = calculateScaleFactor(original.width, toFit.width);
            float dScaleHeight = calculateScaleFactor(original.height, toFit.height);

            dScale = Math.min(dScaleHeight, dScaleWidth);

        }

        return dScale;

    }

    @Override
    protected synchronized void paintComponent(Graphics g) {

        super.paintComponent(g);

        if (image == null) {
            return;
        }

//        System.out.printf("%dX%d\n", image.getWidth(null), image.getHeight(null));

        int width = getWidth() - 1;
        int height = getHeight() - 1;

        int x = (width - scaledImage.getWidth(this)) / 2;
        int y = (height - scaledImage.getHeight(this)) / 2;

        g.drawImage(scaledImage, x, y, this);
    }

    public void addMyChangeListener(MyChangeListener l) {
        this.listeners.add(l);
        fireChangeEvent();
    }

    public void removeMyChangeListener(MyChangeListener l) {
        this.listeners.remove(l);
    }

    // Event firing method.  Called internally by other class methods.
    protected void fireChangeEvent() {
        MyChangeEvent evt = new MyChangeEvent(this);

        for (MyChangeListener l : listeners) {
            l.changeEventReceived(evt);
        }
    }
}

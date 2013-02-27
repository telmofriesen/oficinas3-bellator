/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package visual.gui;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 *
 * @author stefan
 */
public class ImagePanel extends JPanel {

    private BufferedImage image;

    public synchronized void changeImage(BufferedImage image) {
        this.image = image;
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                repaint();
            }
        });
    }

    public synchronized void changeImage(String fileName) {
        BufferedImage img;
        try {
            img = ImageIO.read(new File(fileName));
        } catch (IOException ex) {
            Logger.getLogger(ImagePanel.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        this.changeImage(img);
    }

    public synchronized void changeImageFromRelativePath(String path) {
        BufferedImage img;
        try {
//            System.out.println(getClass().getResource(path).toString());
            img = ImageIO.read(new File(getClass().getResource(path).toURI()));
        } catch (URISyntaxException ex) {
            Logger.getLogger(ImagePanel.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (IOException ex) {
            Logger.getLogger(ImagePanel.class.getName()).log(Level.SEVERE, null, ex);
            return;
        } catch (NullPointerException ex){
            Logger.getLogger(ImagePanel.class.getName()).log(Level.SEVERE, null, ex);
            return;
        }
        this.changeImage(img);
    }

    @Override
    public synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (image != null) {
            int width = getWidth() - 1;
            int height = getHeight() - 1;
            //Calcula x e y para que a imagem fique no centro do painel
            int x = (width - image.getWidth(this)) / 2;
            int y = (height - image.getHeight(this)) / 2;
            g.drawImage(image, x, y, null); // see javadoc for more info on the parameters  
        }
    }
}
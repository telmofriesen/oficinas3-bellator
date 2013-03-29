/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package zz_old;

import java.awt.Canvas;
import java.awt.Color;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import uk.co.caprica.vlcj.component.EmbeddedMediaListPlayerComponent;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CanvasVideoSurface;

/**
 *
 * @author stefan
 */
public class NewClass {

    public static void main(String args[]) {
        //JFrame
    JFrame frame = new JFrame("vlcj Fullscreen Test");
//    frame.setUndecorated(true);
    frame.setLocation(0, 0);
    frame.setSize(500,500);
//    frame.setSize((int)Math.ceil(dim.getWidth()), (int)Math.ceil(dim.getHeight()));
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

    //JPanel
    JLayeredPane panel = new JLayeredPane();
    panel.setLocation(50, 50);
    panel.setSize(frame.getWidth(), frame.getHeight());
    frame.add(panel);

    //EmbeddedMediaPlayerComponent
    EmbeddedMediaPlayerComponent mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
    mediaPlayerComponent.setLocation(0, 0);
    mediaPlayerComponent.setSize(1050, 600);
    panel.add(mediaPlayerComponent);

    //show GUI
    frame.setVisible(true);
    mediaPlayerComponent.getMediaPlayer().playMedia("http://127.0.0.1:5050");
//        JFrame frame = new JFrame();
//        final JPanel panel = new JPanel();
//        panel.setSize(frame.getWidth(), frame.getHeight());
////        final EmbeddedMediaPlayer mediaPlayer = new EmbeddedMediaPlayer();
//        frame.setSize(1050, 600);
//        frame.setContentPane(panel);
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        EmbeddedMediaPlayerComponent e = new EmbeddedMediaListPlayerComponent();
//        e.setLocation(0, 0);
//        e.setSize(300, 300);
//        panel.add(e);
//        panel.setSize(300, 300);
//        e.setSize(300, 300);
//        e.getMediaPlayer().playMedia("http://127.0.0.1:5050");
//        frame.setVisible(true);

//        frame.pack();
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
////                Canvas canvas = new Canvas();
////                canvas.setBackground(Color.black);
////                canvas.setSize(320, 180);
////                MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory("--no-video-title-show");
////                CanvasVideoSurface videoSurface = mediaPlayerFactory.newVideoSurface(canvas);
////                EmbeddedMediaPlayer mediaPlayer = mediaPlayerFactory.newEmbeddedMediaPlayer();
////                mediaPlayer.setVideoSurface(videoSurface);
//                EmbeddedMediaPlayerComponent e = new EmbeddedMediaListPlayerComponent();
//                e.setLocation(0, 0);
//                e.setSize(300, 300);
//                panel.add(e);
//                e.getMediaPlayer().playMedia("http://127.0.0.1:5050");
////                e.setSize(1000, 1000);
////                e.setSize(1000, 1000);
//
////                mediaPlayer.playMedia("http://127.0.0.1:5050");
////                canvas.setSize(10,10);
//            }
//        });
    }
}

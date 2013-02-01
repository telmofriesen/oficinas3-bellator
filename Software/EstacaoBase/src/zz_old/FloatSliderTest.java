/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package zz_old;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import visual.gui.FloatJSlider;

/**
 *
 * @author stefan
 */
public class FloatSliderTest {

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
//                JFrame f = new JFrame("TESTE");
                JSlider js = new JSlider(1, 100, 60);
                js.setPaintLabels(true);
//                FloatJSlider fs = new FloatJSlider(0, 10);
                final JFrame frame = new JFrame();
                final JTextField text = new JTextField(20);
                final FloatJSlider slider = new FloatJSlider(0, 10);
                slider.addChangeListener(new ChangeListener() {
                    @Override
                    public void stateChanged(ChangeEvent e) {
                        text.setText(String.valueOf(slider.getFloatValue()));
                    }
                });
                text.addKeyListener(new KeyAdapter() {
                    @Override
                    public void keyReleased(KeyEvent ke) {
                        String typed = text.getText();
                        slider.setValue(0);
                        if (!typed.matches("\\d+") || typed.length() > 3) {
                            return;
                        }
                        int value = Integer.parseInt(typed);
                        slider.setValue(value);
                    }
                });
                frame.setLayout(new BorderLayout());
                frame.add(text, BorderLayout.NORTH);
                frame.add(slider, BorderLayout.CENTER);
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setVisible(true);
                frame.pack();
            }
        });

    }
}
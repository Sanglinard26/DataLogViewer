/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package gui;

import java.awt.BasicStroke;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.plaf.basic.BasicButtonUI;

/**
 * Component to be used as tabComponent; Contains a JLabel to show the text and a JButton to close the tab it belongs to
 */
public class ButtonTabComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    private final JTabbedPane pane;
    private final CardLayout cl = new CardLayout();
    private final JPanel panel = new JPanel(cl);
    private boolean onEditing = false;

    public ButtonTabComponent(final JTabbedPane pane) {
        // unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;

        setOpaque(false);

        // make JLabel read titles from JTabbedPane
        final JLabel label = new JLabel() {
            private static final long serialVersionUID = 1L;

            public String getText() {
                int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) {
                    return pane.getTitleAt(i);
                }
                return null;
            }
        };

        final JTextField textfield = new JTextField();

        panel.setOpaque(false);

        panel.add(label, "label component");
        panel.add(textfield, "textfield component");

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() > 1) {
                    textfield.setText(label.getText());
                    cl.show(panel, "textfield component");
                    textfield.requestFocusInWindow();
                    onEditing = true;
                } else {
                    int idx = pane.indexOfTabComponent(ButtonTabComponent.this);
                    if (idx != -1) {
                        pane.setSelectedIndex(idx);
                    }
                }
            }
        });

        textfield.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                    if (i != -1) {
                        String newName = textfield.getText();
                        int idx = pane.indexOfTab(newName);
                        if (idx == -1 || idx == i) {
                            pane.setTitleAt(i, textfield.getText());
                            cl.show(panel, "label component");
                            onEditing = false;
                        } else {
                            JOptionPane.showMessageDialog(null, "Il existe d\u00e9j\u00e0 une fen\u00eatre avec ce nom", "INFO",
                                    JOptionPane.INFORMATION_MESSAGE);
                        }

                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    cl.show(panel, "label component");
                    onEditing = false;
                }
            }
        });

        add(panel);
        // add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        // tab button
        JButton button = new TabButton();
        add(button);
        // add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        setToolTipText("Double click pour renommer");
    }

    public final void stopEditing() {
        if (onEditing) {
            cl.show(panel, "label component");
            onEditing = false;
        }
    }

    private class TabButton extends JButton implements ActionListener {
        private static final long serialVersionUID = 1L;

        public TabButton() {
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Fermer la fenetre");
            // Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            // Make it transparent
            setContentAreaFilled(false);
            // No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            // Making nice rollover effect
            // we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            // Close the proper tab by clicking the button
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                int res = JOptionPane.showConfirmDialog(null, "Etes-vous certain de vouloir supprimer cet onglet?", "", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.OK_OPTION) {
                    ((ChartView) pane.getComponentAt(i)).delObservateur();
                    pane.remove(i);
                }
            }
        }

        // we don't want to update UI for this button
        public void updateUI() {
        }

        // paint the cross
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            // shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 5;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}

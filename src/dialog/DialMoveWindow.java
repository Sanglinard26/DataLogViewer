/*
 * Creation : 20 juin 2022
 */
package dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import org.jfree.ui.tabbedui.VerticalLayout;

public final class DialMoveWindow extends JDialog {

    private static final long serialVersionUID = 1L;
    private JList<String> listTab;
    private DefaultListModel<String> listModel;

    final String ICON_UP = "/icon_up_24.png";
    final String ICON_DOWN = "/icon_down_24.png";

    public DialMoveWindow(JFrame owner, JTabbedPane windows) {
        super(owner);
        setTitle("Disposition des fenetres");
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        setLayout(new BorderLayout());

        listModel = new DefaultListModel<String>();
        for (int i = 0; i < windows.getTabCount(); i++) {
            listModel.addElement(windows.getTitleAt(i));
        }
        listTab = new JList<>(listModel);
        listTab.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        listTab.setCellRenderer(new ListRenderer());

        add(listTab, BorderLayout.CENTER);

        JPanel panelUpDown = new JPanel(new VerticalLayout());

        JButton up = new JButton(new ImageIcon(getClass().getResource(ICON_UP)));
        up.setContentAreaFilled(false);
        up.setActionCommand("UP");
        up.addActionListener(new UpDownListener());
        panelUpDown.add(up);

        JButton down = new JButton(new ImageIcon(getClass().getResource(ICON_DOWN)));
        down.setContentAreaFilled(false);
        down.setActionCommand("DOWN");
        down.addActionListener(new UpDownListener());
        panelUpDown.add(down);

        add(panelUpDown, BorderLayout.EAST);

        JButton btOK = new JButton(new AbstractAction("OK") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                for (int i = 0; i < listModel.getSize(); i++) {
                    String title = listModel.get(i);
                    int idxTab = windows.indexOfTab(title);
                    Component comp = windows.getComponentAt(idxTab);
                    Component tabComp = windows.getTabComponentAt(idxTab);
                    windows.removeTabAt(idxTab);
                    windows.insertTab(title, null, comp, null, i);
                    windows.setTabComponentAt(i, tabComp);
                }

                DialMoveWindow.this.dispose();

            }
        });
        add(btOK, BorderLayout.SOUTH);

        setMinimumSize(new Dimension(300, 500));
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    class UpDownListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {

            int moveMe = listTab.getSelectedIndex();

            if (listModel.getSize() > 1 && moveMe > -1) {
                if (e.getActionCommand().equals("UP")) {

                    if (moveMe != 0) {
                        swap(moveMe, moveMe - 1);
                        listTab.setSelectedIndex(moveMe - 1);
                        listTab.ensureIndexIsVisible(moveMe - 1);
                    }
                } else {
                    if (moveMe != listTab.getModel().getSize() - 1) {
                        swap(moveMe, moveMe + 1);
                        listTab.setSelectedIndex(moveMe + 1);
                        listTab.ensureIndexIsVisible(moveMe + 1);
                    }
                }
            }

        }
    }

    private void swap(int a, int b) {
        String aObject = listModel.getElementAt(a);
        String bObject = listModel.getElementAt(b);
        listModel.set(a, bObject);
        listModel.set(b, aObject);
    }

    private final class ListRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        private JLabel label;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            label.setOpaque(true);

            if (!isSelected && index % 2 != 0) {
                label.setBackground(new Color(128, 128, 128, 30));
            }

            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setPreferredSize(new Dimension(200, 20));
            label.setFont(new Font(null, Font.PLAIN, 12));

            return label;
        }

    }
}

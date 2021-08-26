package dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

import gui.Ihm;
import log.Formula;
import log.Measure;

public final class DialManageFormula extends JDialog {

    private static final long serialVersionUID = 1L;

    public DialManageFormula(final Ihm ihm) {

        super(ihm, "Gestion des formules", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        final GridBagConstraints gbc = new GridBagConstraints();

        setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.WEST;
        add(new JLabel("Liste des formules :"), gbc);

        final DefaultListModel<Measure> listModel = new DefaultListModel<Measure>();
        for (Measure form : ihm.getListFormula()) {
            listModel.addElement(form);
        }
        final JList<Measure> listFormula = new JList<Measure>(listModel);
        listFormula.setCellRenderer(new ListRenderer());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 3;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JScrollPane(listFormula), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 5);
        gbc.anchor = GridBagConstraints.NORTH;
        add(new JButton(new AbstractAction("Supprimer") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                int res = JOptionPane.showConfirmDialog(null, "Voulez-vous supprimer cette formule?", "Question", JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    ihm.deleteMeasure(listFormula.getSelectedValue());
                    listModel.removeElement(listFormula.getSelectedValue());
                }
            }
        }), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.NORTH;
        add(new JButton(new AbstractAction("Modifer") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                new DialNewFormula(ihm, ((Formula) listFormula.getSelectedValue()));
            }
        }), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.NORTH;
        add(new JButton(new AbstractAction("Valider") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                ihm.refresh();
                dispose();
            }
        }), gbc);

        setMinimumSize(new Dimension(300, 400));
        setLocationRelativeTo(ihm);
        setResizable(true);
        setVisible(true);
    }

    private final class ListRenderer extends DefaultListCellRenderer {

        private static final long serialVersionUID = 1L;

        public ListRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
            return this;
        }
    }

}

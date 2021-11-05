/*
 * Creation : 10 oct. 2021
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import log.Measure;

public final class FilteredListMeasure extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JList<Measure> listMeasure;
    private final FilterField filterField;

    public FilteredListMeasure(FilteredListModel listModel) {
        super(new BorderLayout());

        listMeasure = new JList<>();
        listMeasure.setModel(listModel);
        listMeasure.setCellRenderer(new ListLabelRenderer());
        listMeasure.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listMeasure.setDragEnabled(true);

        add(new JScrollPane(listMeasure), BorderLayout.CENTER);

        filterField = new FilterField();
        add(filterField, BorderLayout.NORTH);
    }

    public JList<Measure> getListMeasure() {
        return listMeasure;
    }

    public final Measure getSelectedValue() {
        return listMeasure.getSelectedValue();

    }

    public FilteredListModel getModel() {
        return (FilteredListModel) listMeasure.getModel();
    }

    public final FilterField getFilterField() {
        return filterField;
    }

    protected final class FilterField extends JComponent implements DocumentListener {

        private static final long serialVersionUID = 1L;

        private final JTextField txtFiltre;
        private final JPanel panelBt;
        private final JButton delSearchBt;

        final String ICON_CLEAR = "/icon_clearText_24.png";

        public FilterField() {
            super();
            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));

            panelBt = new JPanel(new GridLayout(1, 2));

            txtFiltre = new JTextField(15);
            txtFiltre.setBorder(BorderFactory.createEmptyBorder());
            txtFiltre.getDocument().addDocumentListener(this);

            delSearchBt = new JButton(new ImageIcon(getClass().getResource(ICON_CLEAR)));
            delSearchBt.setPreferredSize(new Dimension(32, 24));
            delSearchBt.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    txtFiltre.setText("");
                }
            });
            panelBt.add(delSearchBt);

            add(txtFiltre, BorderLayout.CENTER);
            add(panelBt, BorderLayout.EAST);
        }

        private final void doFilter() {

            Measure selectedMeasure = null;
            if (listMeasure.getSelectedIndex() > -1) {
                selectedMeasure = getModel().getElementAt(listMeasure.getSelectedIndex());
            }
            if (selectedMeasure != null) {
                listMeasure.clearSelection();

                final String filter = txtFiltre.getText().toLowerCase();

                getModel().setFilter(filter);

                listMeasure.setSelectedIndex(0);
                if (getModel().getSize() > 0) {
                    listMeasure.setSelectedValue(selectedMeasure, true);
                } else {
                    listMeasure.setSelectedIndex(-1);
                }

            } else {

                final String filter = txtFiltre.getText().toLowerCase();

                getModel().setFilter(filter);

            }
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            doFilter();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            doFilter();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            doFilter();
        }

    }

}

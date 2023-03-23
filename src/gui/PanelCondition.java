/*
 * Creation : 19 nov. 2020
 */
package gui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import org.jfree.chart.plot.IntervalMarker;

public final class PanelCondition extends JPanel {

    private static final long serialVersionUID = 1L;

    private TableCondition tableCondition;
    private JComboBox<String> cbZones;
    private DefaultComboBoxModel<String> cbModel;
    private List<IntervalMarker> listBoxAnnotation;

    public PanelCondition() {
        super(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        tableCondition = new TableCondition();
        tableCondition.setPreferredScrollableViewportSize(tableCondition.getPreferredSize());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(new JScrollPane(tableCondition), gbc);

        JLabel labelZone = new JLabel("Zones détectées:");
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(labelZone, gbc);

        cbModel = new DefaultComboBoxModel<String>();
        cbZones = new JComboBox<>(cbModel);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 100;
        gbc.weighty = 100;
        gbc.insets = new Insets(5, 5, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(cbZones, gbc);

    }

    public void setListBoxAnnotation(List<IntervalMarker> listBoxAnnotation) {
        this.listBoxAnnotation = listBoxAnnotation;

        populateList(listBoxAnnotation);
    }

    public final void populateList(List<IntervalMarker> listBoxAnnotation) {

        cbModel.removeAllElements();

        String textZone;

        cbModel.addElement("...");

        for (int i = 0; i < listBoxAnnotation.size(); i++) {
            textZone = String.format("%4.3fs -  %4.3fs", listBoxAnnotation.get(i).getStartValue(), listBoxAnnotation.get(i).getEndValue());
            cbModel.addElement(textZone);
        }
    }

    public final JComboBox<String> getCbZones() {
        return cbZones;
    }

    public final double getStartZone() {
        int idx = cbZones.getSelectedIndex();
        if (idx > 0) {
            return listBoxAnnotation.get(--idx).getStartValue();
        }
        return Double.NaN;
    }

    public final double getEndZone() {
        int idx = cbZones.getSelectedIndex();
        if (idx > 0) {
            return listBoxAnnotation.get(--idx).getEndValue();
        }
        return Double.NaN;
    }

    public final double getDurationZone() {
        return getEndZone() - getStartZone();
    }

    public TableCondition getTableCondition() {
        return tableCondition;
    }

    static final class DecimalFormatRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        private static final DecimalFormat formatter = new DecimalFormat();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (value != null) {
                value = formatter.format(value);
            }

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}

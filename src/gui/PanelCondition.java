/*
 * Creation : 19 nov. 2020
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.plot.IntervalMarker;

public final class PanelCondition extends JPanel {

    private static final long serialVersionUID = 1L;

    private TableCondition tableCondition;
    private JTable tableListCondition;
    private DefaultTableModel modelListCond;
    private List<IntervalMarker> listBoxAnnotation;

    public PanelCondition() {
        super(new BorderLayout());

        tableCondition = new TableCondition();
        tableCondition.setPreferredScrollableViewportSize(tableCondition.getPreferredSize());
        add(new JScrollPane(tableCondition), BorderLayout.NORTH);

        modelListCond = new DefaultTableModel(new String[] { "Numéro", "t1", "t2" }, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tableListCondition = new JTable(modelListCond);

        tableListCondition.setDefaultRenderer(Object.class, new DecimalFormatRenderer());
        tableListCondition.setPreferredScrollableViewportSize(tableListCondition.getPreferredSize());
        add(new JScrollPane(tableListCondition), BorderLayout.CENTER);
    }

    public void setListBoxAnnotation(List<IntervalMarker> listBoxAnnotation) {
        this.listBoxAnnotation = listBoxAnnotation;

        populateList();
    }

    public final void populateList() {

        modelListCond.setRowCount(0);

        for (int i = 0; i < listBoxAnnotation.size(); i++) {
            modelListCond.addRow(new Object[] { i + 1, listBoxAnnotation.get(i).getStartValue(), listBoxAnnotation.get(i).getEndValue() });
        }
    }

    public TableCondition getTableCondition() {
        return tableCondition;
    }

    public JTable getTableListCondition() {
        return tableListCondition;
    }

    static final class DecimalFormatRenderer extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;
        private static final DecimalFormat formatter = new DecimalFormat();

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            value = formatter.format(value);

            return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        }
    }

}

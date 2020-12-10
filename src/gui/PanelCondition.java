/*
 * Creation : 19 nov. 2020
 */
package gui;

import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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

        tableCondition.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {

                int idx = tableCondition.getSelectedRow();

                if (idx == -1 || listBoxAnnotation == null) {
                    return;
                }

                populateList(idx);

            }
        });

        modelListCond = new DefaultTableModel(new String[] { "Numéro", "t1", "t2" }, 0);
        tableListCondition = new JTable(modelListCond);
        tableListCondition.setPreferredScrollableViewportSize(tableListCondition.getPreferredSize());
        add(new JScrollPane(tableListCondition), BorderLayout.CENTER);
    }

    public void setListBoxAnnotation(List<IntervalMarker> listBoxAnnotation) {
        this.listBoxAnnotation = listBoxAnnotation;
    }

    public final void populateList(int idx) {
        if (!(boolean) tableCondition.getModel().getValueAt(idx, 0)) {
            listBoxAnnotation.clear();
        }

        modelListCond.setRowCount(0);

        for (int i = 0; i < listBoxAnnotation.size(); i++) {
            modelListCond.addRow(new Object[] { i + 1, listBoxAnnotation.get(i).getStartValue(), listBoxAnnotation.get(i).getEndValue() });
        }
    }

    public TableCondition getTableCondition() {
        return tableCondition;
    }

}

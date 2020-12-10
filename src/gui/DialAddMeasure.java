/*
 * Creation : 15 nov. 2020
 */
package gui;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import org.jfree.chart.plot.XYPlot;

public final class DialAddMeasure extends JPanel {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> cbAxis;
    private JCheckBox checkNewAxis;
    private final String measureName;

    public DialAddMeasure(XYPlot plot, String measureName) {

        super();

        this.measureName = measureName;
        checkNewAxis = new JCheckBox("Nouvel axe");
        add(checkNewAxis);

        cbAxis = new JComboBox<String>();
        for (int i = 0; i < plot.getRangeAxisCount(); i++) {
            cbAxis.addItem(plot.getRangeAxis(i).getLabel());
        }
        add(cbAxis);

    }

    public final String getAxisName() {
        return checkNewAxis.isSelected() ? measureName : cbAxis.getSelectedItem().toString();
    }

}

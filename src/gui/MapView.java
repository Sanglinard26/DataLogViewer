/*
 * Creation : 1 nov. 2020
 */
package gui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import calib.MapCal;
import calib.Variable;
import net.ericaro.surfaceplotter.surface.JSurface;
import net.ericaro.surfaceplotter.surface.Projector;

public final class MapView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JList<Variable> listVariable;
    private CalTable dataTable;
    private SurfaceChart surfaceChart;
    private JSplitPane splitPane;

    public MapView(MapCal mapCal) {

        super();
        setLayout(new GridBagLayout());

        final GridBagConstraints gbc = new GridBagConstraints();

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true);
        splitPane.setOneTouchExpandable(true);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 80;
        gbc.weighty = 100;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(splitPane, gbc);

        List<Variable> variables = mapCal.getListVariable();
        Collections.sort(variables);
        listVariable = new JList<Variable>(mapCal.getListVariable().toArray(new Variable[variables.size()]));

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 20;
        gbc.weighty = 100;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(new JScrollPane(listVariable), gbc);

        listVariable.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (listVariable.getSelectedIndex() > -1 && !e.getValueIsAdjusting()) {
                    Variable variable = listVariable.getSelectedValue();

                    remove(dataTable);
                    dataTable = new CalTable(variable);
                    splitPane.setTopComponent(dataTable);
                    revalidate();
                    repaint();

                    splitPane.setDividerLocation(dataTable.getTableHeight());

                    updateChart(variable);

                }
            }
        });

        dataTable = new CalTable(null);
        splitPane.setTopComponent(dataTable);

        surfaceChart = new SurfaceChart();
        splitPane.setBottomComponent(surfaceChart);
        surfaceChart.setVisible(false);

    }

    public final void updateChart(Variable variable) {

        JSurface jSurface = surfaceChart.getSurface();

        Projector proj = surfaceChart.getArraySurfaceModel().getProjector();
        float savedscalingfactor = proj.get2DScaling();

        // int h = splitPane.getHeight() - splitPane.getDividerLocation();
        // jSurface.setPreferredSize(new Dimension(splitPane.getWidth(), h - 50));

        boolean chartVisible = false;

        if (variable.getDimX() * variable.getDimY() < 1) {
            return;
        }

        switch (variable.getDimY()) {
        case 1:
            break;
        case 2:
            float[][] zValuesOrigin = variable.getZvalues();

            int length = zValuesOrigin[0].length;
            float[][] zValuesNew = new float[2][length];

            zValuesNew[0] = Arrays.copyOf(zValuesOrigin[0], length);
            zValuesNew[1] = Arrays.copyOf(zValuesOrigin[0], length);

            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(), new float[] { 0, 1 }, zValuesNew);
            jSurface.setXLabel("X");
            chartVisible = true;
            break;
        default:
            surfaceChart.getArraySurfaceModel().setValues(variable.getXAxis(), variable.getYAxis(), variable.getZvalues());
            jSurface.setXLabel("X");
            jSurface.setYLabel("Y");
            chartVisible = true;
            break;
        }

        surfaceChart.setVisible(chartVisible);
    }

}

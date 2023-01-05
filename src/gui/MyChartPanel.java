/*
 * Creation : 25 nov. 2022
 */
package gui;

import java.awt.event.MouseEvent;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

public final class MyChartPanel extends ChartPanel {

    private static final long serialVersionUID = 1L;

    public MyChartPanel(JFreeChart chart, int width, int height, int minimumDrawWidth, int minimumDrawHeight, int maximumDrawWidth,
            int maximumDrawHeight, boolean useBuffer, boolean properties, boolean save, boolean print, boolean zoom, boolean tooltips) {
        super(chart, width, height, minimumDrawWidth, minimumDrawHeight, maximumDrawWidth, maximumDrawHeight, useBuffer, properties, save, print,
                zoom, tooltips);
    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        // En laissant le corps vide, on évite de redessiner le graphique à chaque click
    }

}

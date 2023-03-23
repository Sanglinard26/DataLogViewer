package dialog;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;

import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.DatasetRenderingOrder;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.function.LineFunction2D;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.statistics.Regression;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import gui.ChartView;
import utils.PolynomialRegression;

public final class DialTrendLine extends JDialog {

    private static final long serialVersionUID = 1L;
    private static final String[] LIST_TYPE = new String[] { "Régession linéaire", "Polynome" };
    private final JComboBox<String> trendType;
    private final ChartView chartView;

    public DialTrendLine(ChartView chartView) {

        super(null, "Courbe de tendance", ModalityType.MODELESS);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.chartView = chartView;

        final GridBagConstraints gbc = new GridBagConstraints();

        setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 5, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JLabel("Type :"), gbc);

        trendType = new JComboBox<String>(LIST_TYPE);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 0, 0, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        add(trendType, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        add(new JButton(new AbstractAction("OK") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (trendType.getSelectedIndex() == 0) {
                    addLinearTrendLine();
                } else {
                    addPolynomeTrendLine();
                }
                dispose();
            }
        }), gbc);

        pack();
        setLocationRelativeTo(null);
        setResizable(false);
        setVisible(true);
    }

    private final void addLinearTrendLine() {
        this.chartView.getChart().setNotify(false);
        XYPlot plot = (XYPlot) this.chartView.getPlot().getSubplots().get(0);

        plot.clearAnnotations();

        double[] coeff = Regression.getOLSRegression(plot.getDataset(0), 0);

        double xMin = DatasetUtils.findMinimumDomainValue(plot.getDataset(0)).doubleValue();
        double xMax = DatasetUtils.findMaximumDomainValue(plot.getDataset(0)).doubleValue();

        LineFunction2D lf = new LineFunction2D(coeff[0], coeff[1]);
        XYDataset trendLine = DatasetUtils.sampleFunction2D(lf, xMin, xMax, 2, "Courbe tendance");

        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(3f));
        renderer.setSeriesPaint(0, Color.GRAY);
        plot.setRenderer(1, renderer);
        plot.setDataset(1, trendLine);
        plot.mapDatasetToRangeAxis(1, 0);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        XYTextAnnotation textAnnotation = new XYTextAnnotation(String.format("y = %.3fx + %.3f", coeff[1], coeff[0]), trendLine.getXValue(0, 1) * 0.9,
                trendLine.getYValue(0, 1));
        textAnnotation.setBackgroundPaint(Color.WHITE);
        textAnnotation.setOutlineVisible(true);
        plot.addAnnotation(textAnnotation);

        this.chartView.getChart().setNotify(true);
    }

    private final void addPolynomeTrendLine() {
        this.chartView.getChart().setNotify(false);
        XYPlot plot = (XYPlot) this.chartView.getPlot().getSubplots().get(0);

        plot.clearAnnotations();

        final int nbPoints = plot.getDataset(0).getItemCount(0);

        double x[] = new double[nbPoints];
        double y[] = new double[nbPoints];
        for (int n = 0; n < nbPoints; n++) {
            x[n] = plot.getDataset(0).getXValue(0, n);
            y[n] = plot.getDataset(0).getYValue(0, n);
        }

        double[] res = PolynomialRegression.polyRegression(x, y);

        XYSeries series = new XYSeries("Courbe tendance");
        for (int n = 0; n < nbPoints; n++) {
            series.add(x[n], res[n], false);
        }
        XYDataset trendLine = new XYSeriesCollection(series);

        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(3f));
        renderer.setSeriesPaint(0, Color.GRAY);
        plot.setRenderer(1, renderer);
        plot.setDataset(1, trendLine);
        plot.mapDatasetToRangeAxis(1, 0);

        plot.setDatasetRenderingOrder(DatasetRenderingOrder.FORWARD);

        this.chartView.getChart().setNotify(true);

    }

}

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JInternalFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import log.Measure;

public final class ChartView extends JInternalFrame {

    private static final long serialVersionUID = 1L;

    private final ChartPanel chartPanel;
    private JFreeChart chart = null;

    public ChartView(String name, Dimension dim, NumberAxis timeAxis, Measure time, Measure measure) {
        super("#" + name, true, true, true, true);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        chartPanel = new ChartPanel(null, 680, 420, 300, 200, 1920, 1080, true, true, false, false, true, false);

        final XYSeries series = new XYSeries(measure.getName());
        final XYSeriesCollection collections = new XYSeriesCollection(series);
        final NumberAxis domainAxis = timeAxis;
        final NumberAxis yAxis = new NumberAxis(measure.getName());
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
        final XYPlot plot = new XYPlot(collections, domainAxis, yAxis, renderer);

        final List<Double> temps = time.getData();
        final int nbPoint = temps.size();

        for (int n = 0; n < nbPoint; n++) {

            int sizeData = measure.getData().size();

            if (n < sizeData) {
                series.add(temps.get(n), measure.getData().get(n));
            }
        }

        plot.setDataset(collections);
        plot.setDomainPannable(true);
        plot.setOrientation(PlotOrientation.VERTICAL);

        chart = new JFreeChart(plot);

        chartPanel.setChart(chart);
        chartPanel.setRangeZoomable(false);

        chartPanel.setPopupMenu(null);
        add(chartPanel, BorderLayout.CENTER);

        setSize(dim);
        show();
    }

    public final void addMeasure(Measure measure) {
        XYSeriesCollection collection = (XYSeriesCollection) chart.getXYPlot().getDataset();
        XYSeries serie = collection.getSeries(0);

        XYSeries newSerie = new XYSeries(measure.getName());

        int nbPoint = serie.getItemCount();

        for (int n = 0; n < nbPoint; n++) {

            int sizeData = measure.getData().size();

            if (n < sizeData) {
                newSerie.add(serie.getX(n), measure.getData().get(n));
            }
        }

        collection.addSeries(newSerie);

    }

    public final ChartPanel getChartPanel() {
        return chartPanel;
    }

}

/*
 * Creation : 11 déc. 2021
 */
package gui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;

import log.Measure;

public final class ChartZoomReference extends ChartPanel {

    private static final long serialVersionUID = 1L;

    private final XYPlot plot;
    private XYSeriesCollection dataset;
    private XYSeries serie;
    private IntervalMarker intervalMarker;

    private boolean isAdjusting = false;

    private byte actionType; // 0 = Move zone, 1 = Move start marker, 2 = Move end marker

    private double oldXCursor;

    public ChartZoomReference() {

        super(null, 680, 420, 300, 200, 1920, 1080, true, false, false, false, false, false);

        serie = new XYSeries("Reference");
        dataset = new XYSeriesCollection(serie);
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, Color.BLACK);

        plot = new XYPlot(dataset, xAxis, yAxis, renderer);
        plot.setDataset(dataset);

        intervalMarker = new IntervalMarker(xAxis.getRange().getLowerBound(), xAxis.getRange().getUpperBound(), new Color(0, 255, 0, 30));
        plot.addDomainMarker(intervalMarker, Layer.FOREGROUND);

        intervalMarker.setOutlinePaint(Color.GREEN);
        intervalMarker.setOutlineStroke(new BasicStroke(2));

        JFreeChart chart = new JFreeChart(plot);
        chart.removeLegend();

        setChart(chart);
        setRangeZoomable(false);
        setDomainZoomable(false);

        addMouseListener(new MyChartMouseListener());
        addMouseMotionListener(new MyChartMouseListener());

    }

    public final IntervalMarker getIntervalMarker() {
        return intervalMarker;
    }

    public final void changeDomainAxisRange(Range range) {
        if (!isAdjusting) {
            plot.getDomainAxis().setRange(range);
            changeIntervalMarkerRange(range);
        }
    }

    public final void changeIntervalMarkerRange(Range range) {
        if (!isAdjusting) {
            intervalMarker.setStartValue(range.getLowerBound());
            intervalMarker.setEndValue(range.getUpperBound());
        }

    }

    public final void addMeasure(Measure time, Measure measure) {

        final int nbPoint = measure.getData().size();

        serie.clear();

        for (int n = 0; n < nbPoint; n++) {
            serie.add(time.getData().get(n), measure.getData().get(n), false);
        }

        serie.fireSeriesChanged();

        isAdjusting = true;
        intervalMarker.setStartValue(plot.getDomainAxis().getRange().getLowerBound());
        intervalMarker.setEndValue(plot.getDomainAxis().getRange().getUpperBound());

    }

    private final class MyChartMouseListener extends MouseAdapter {

        @Override
        public void mouseReleased(MouseEvent e) {
            isAdjusting = false;
        }

        @Override
        public void mouseMoved(MouseEvent e) {

            Point2D p = translateScreenToJava2D(e.getPoint());

            ChartEntity chartEntity = getEntityForPoint((int) p.getX(), (int) p.getY());

            if (chartEntity instanceof PlotEntity || chartEntity instanceof XYItemEntity) {

                ValueAxis xAxis = plot.getDomainAxis();

                Rectangle2D dataArea = getScreenDataArea();

                double xCursorMin = xAxis.java2DToValue(p.getX() - 2, dataArea, RectangleEdge.BOTTOM);
                double xCursorMax = xAxis.java2DToValue(p.getX() + 2, dataArea, RectangleEdge.BOTTOM);
                double xStartMarker = intervalMarker.getStartValue();
                double xEndMarker = intervalMarker.getEndValue();

                if (xCursorMin > xStartMarker && xCursorMax < xEndMarker) {
                    setCursor(new Cursor(Cursor.MOVE_CURSOR));
                    actionType = 0;
                    oldXCursor = xAxis.java2DToValue(p.getX(), dataArea, RectangleEdge.BOTTOM);
                } else if (xCursorMin < xStartMarker && xStartMarker < xCursorMax) {
                    setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
                    actionType = 1;
                    oldXCursor = xStartMarker;
                } else if (xCursorMin < xEndMarker && xEndMarker < xCursorMax) {
                    setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
                    actionType = 2;
                    oldXCursor = xEndMarker;
                } else {
                    setCursor(Cursor.getDefaultCursor());
                    actionType = -1;
                }
            } else {
                setCursor(Cursor.getDefaultCursor());
                actionType = -1;
            }

        }

        @Override
        public void mouseDragged(MouseEvent e) {

            isAdjusting = true;

            Point2D p = translateScreenToJava2D(e.getPoint());
            ValueAxis xAxis = plot.getDomainAxis();
            Range rangeAxis = xAxis.getRange();

            Rectangle2D dataArea = getScreenDataArea();

            double xCursor = xAxis.java2DToValue(p.getX(), dataArea, RectangleEdge.BOTTOM);
            double xStartMarker = intervalMarker.getStartValue();
            double xEndMarker = intervalMarker.getEndValue();
            double xMove = xCursor - oldXCursor;

            double newStartMarker = Math.max(rangeAxis.getLowerBound(), xStartMarker + xMove);
            double newEndMarker = Math.min(rangeAxis.getUpperBound(), xEndMarker + xMove);

            switch (actionType) {
            case 0:
                if ((xEndMarker - xStartMarker - 0.1) <= (newEndMarker - newStartMarker)) {
                    intervalMarker.setStartValue(newStartMarker);
                    intervalMarker.setEndValue(newEndMarker);
                }
                break;
            case 1:
                intervalMarker.setStartValue(newStartMarker);
                break;
            case 2:
                intervalMarker.setEndValue(newEndMarker);
                break;
            default:
                break;
            }

            oldXCursor = xCursor;

        }
    }

}

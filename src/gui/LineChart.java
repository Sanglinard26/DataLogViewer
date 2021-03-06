/*
 * Creation : 19 mai 2021
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.entity.XYItemEntity;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public final class LineChart extends JPanel implements ChartMouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;

    private final ListLegend listSeries;

    private final JFreeChart chart;
    private final ChartPanel chartPanel;
    private final XYItemRenderer renderer;
    private final XYSeriesCollection dataset;

    private XYItemEntity xyItemEntity = null;
    private float initialMovePointY = Float.NaN;
    private float finalMovePointY = Float.NaN;

    private CalTable dataTable;

    public LineChart() {

        super(new BorderLayout());

        dataset = new XYSeriesCollection(new XYSeries("vide"));

        chart = ChartFactory.createXYLineChart("", "", "", dataset, PlotOrientation.VERTICAL, false, false, false);
        chart.getXYPlot().setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setDomainGridlinePaint(Color.GRAY);
        chart.getXYPlot().setRangeGridlinePaint(Color.GRAY);

        renderer = new XYLineAndShapeRenderer(true, true);
        chart.getXYPlot().setRenderer(renderer);

        chartPanel = new ChartPanel(chart);
        chartPanel.addChartMouseListener(this);
        chartPanel.addMouseMotionListener(this);

        listSeries = new ListLegend(new XYSeries[1]);

        listSeries.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    List<XYSeries> selectedKey = listSeries.getSelectedValuesList();

                    if (selectedKey.size() == listSeries.getModel().getSize()) {
                        listSeries.clearSelection();
                    }

                    XYSeries keySerie;

                    for (int i = 0; i < listSeries.getModel().getSize(); i++) {

                        keySerie = listSeries.getModel().getElementAt(i);

                        int idx = dataset.indexOf(keySerie);

                        if (idx > -1) {
                            if (selectedKey.size() != 0) {
                                renderer.setSeriesVisible(idx, selectedKey.contains(keySerie));
                            } else {
                                renderer.setSeriesVisible(idx, true);
                            }

                        }
                    }
                }
            }
        });

        JScrollPane sp = new JScrollPane(listSeries);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        add(sp, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);
    }

    public final void setTable(CalTable table) {
        this.dataTable = table;
    }

    public final void changeSeries(XYSeries[] series) {

        listSeries.clearSelection();
        dataset.removeAllSeries();

        double size = 6;
        double delta = size / 2.0;
        Shape shape2 = new Ellipse2D.Double(-delta, -delta, size, size);

        for (int idx = 0; idx < series.length; idx++) {
            dataset.addSeries(series[idx]);

            renderer.setSeriesShape(idx, shape2);

            float hue = (float) (idx) / (float) (series.length);

            renderer.setSeriesPaint(idx, Color.getHSBColor(hue, 1, 1));
        }

        listSeries.setListData(series);
    }

    @Override
    public void chartMouseClicked(ChartMouseEvent paramChartMouseEvent) {

        ChartEntity entity = paramChartMouseEvent.getEntity();

        if (entity instanceof PlotEntity) {
            PlotEntity plotEntity = (PlotEntity) entity;
            XYPlot xyPlot = (XYPlot) plotEntity.getPlot();
            xyPlot.handleClick(paramChartMouseEvent.getTrigger().getX(), paramChartMouseEvent.getTrigger().getY(),
                    chartPanel.getChartRenderingInfo().getPlotInfo());
        }

    }

    @Override
    public void chartMouseMoved(ChartMouseEvent paramChartMouseEvent) {

        Point pt = paramChartMouseEvent.getTrigger().getPoint();
        XYPlot plot = chart.getXYPlot();

        if (plot != null && plot.getDatasetCount() > 0) {

            Point2D p2d = chartPanel.translateScreenToJava2D(pt);

            EntityCollection entities = chartPanel.getChartRenderingInfo().getEntityCollection();

            ChartEntity entity = entities.getEntity(p2d.getX(), p2d.getY());

            if ((entity != null) && (entity instanceof XYItemEntity)) {
                xyItemEntity = (XYItemEntity) entity;
            } else if (!(entity instanceof XYItemEntity)) {
                xyItemEntity = null;
                chartPanel.setDomainZoomable(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                return;
            }
            if (xyItemEntity == null) {
                chartPanel.setDomainZoomable(true);
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                return; // return if not pressed on any series point
            }

            int serieIndex = xyItemEntity.getSeriesIndex();

            initialMovePointY = xyItemEntity.getDataset().getY(serieIndex, xyItemEntity.getItem()).floatValue();

            setCursor(new Cursor(Cursor.HAND_CURSOR));

        }

    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (getCursor().getType() != Cursor.DEFAULT_CURSOR) {

            setCursor(new Cursor(Cursor.N_RESIZE_CURSOR));

            chartPanel.setDomainZoomable(false);
            chartPanel.setRangeZoomable(false);

            int itemIndex = xyItemEntity.getItem();

            int serieIndex = xyItemEntity.getSeriesIndex();

            Point pt = e.getPoint();
            XYPlot xy = chart.getXYPlot();
            if (xy == null) {
                return;
            }
            XYSeries series = ((XYSeriesCollection) xyItemEntity.getDataset()).getSeries(serieIndex);
            Point2D p = chartPanel.translateScreenToJava2D(pt);

            Rectangle2D dataArea = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();

            finalMovePointY = (float) xy.getRangeAxis().java2DToValue(p.getY(), dataArea, xy.getRangeAxisEdge());

            float difference = finalMovePointY - initialMovePointY;

            float targetPoint = series.getY(itemIndex).floatValue() + difference;

            series.updateByIndex(itemIndex, Float.valueOf(targetPoint));

            if (dataset.getSeriesCount() > 1) {

                int nbCol = dataTable.getTable().getColumnCount();

                if (dataset.getSeriesCount() == nbCol) {
                    dataTable.setValue(targetPoint, itemIndex, serieIndex);
                } else {
                    dataTable.setValue(targetPoint, serieIndex, itemIndex);
                }

            } else {
                dataTable.setValue(targetPoint, 0, itemIndex);
            }

            initialMovePointY = finalMovePointY;

        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub

    }
}

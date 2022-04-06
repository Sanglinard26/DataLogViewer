/*
 * Creation : 19 mai 2021
 */
package gui;

import java.awt.BasicStroke;
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
import org.jfree.chart.annotations.XYDrawableAnnotation;
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
    private final char type; // x : IsoX, y : IsoY

    private double zMin;
    private double zMax;

    private final JScrollPane sp;

    private boolean onMove = false;

    private static final CircleDrawer cd = new CircleDrawer(Color.BLACK, new BasicStroke(2.0f), null);
    private static Shape shape2 = new Ellipse2D.Double(-3, -3, 6, 6);

    public LineChart(char type) {

        super(new BorderLayout());

        this.type = type;

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

        sp = new JScrollPane(listSeries);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

        add(sp, BorderLayout.WEST);
        add(chartPanel, BorderLayout.CENTER);
    }

    public final void setTable(CalTable table) {
        this.dataTable = table;
    }

    public final void changeSeries(XYSeries[] series, double min, double max) {

        this.zMin = min;
        this.zMax = max;

        chart.getXYPlot().clearAnnotations();

        if (onMove) {
            return;
        }

        listSeries.clearSelection();
        dataset.removeAllSeries();

        for (int idx = 0; idx < series.length; idx++) {
            if (!"NaN".equals(series[idx].getKey().toString())) {
                dataset.addSeries(series[idx]);

                renderer.setSeriesShape(idx, shape2);

                float hue = (float) (idx) / (float) (series.length);

                renderer.setSeriesPaint(idx, Color.getHSBColor(hue, 1, 1));
            }

        }

        listSeries.setListData(series);
        if (series.length > 1) {
            sp.setVisible(true);
        } else {
            sp.setVisible(false);
        }
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

            int itemIndex = xyItemEntity.getItem();

            if (dataset.getSeriesCount() > 1) {

                if (type == 'x') {
                    this.dataTable.getTable().changeSelection(itemIndex, serieIndex, false, false);
                } else {
                    this.dataTable.getTable().changeSelection(serieIndex, itemIndex, false, false);
                }

            } else {
                this.dataTable.getTable().changeSelection(0, itemIndex, false, false);
            }
        }

    }

    public final void updateAnnotation(int[] rows, int[] cols) {

        XYPlot plot = chart.getXYPlot();
        plot.clearAnnotations();

        if (dataset.getSeriesCount() > 1) {

            if (type == 'x') {

                listSeries.setSelectedIndices(cols);

                for (int col : cols) {
                    for (int row : rows) {
                        double xVal = dataset.getXValue(col, row);
                        double yVal = dataset.getYValue(col, row);
                        plot.addAnnotation(new XYDrawableAnnotation(xVal, yVal, 11, 11, cd));
                    }
                }

            } else {

                listSeries.setSelectedIndices(rows);

                for (int col : cols) {
                    for (int row : rows) {
                        double xVal = dataset.getXValue(row, col);
                        double yVal = dataset.getYValue(row, col);
                        plot.addAnnotation(new XYDrawableAnnotation(xVal, yVal, 11, 11, cd));
                    }
                }

            }

        } else {
            for (int col : cols) {
                double xVal = dataset.getXValue(0, col);
                double yVal = dataset.getYValue(0, col);
                plot.addAnnotation(new XYDrawableAnnotation(xVal, yVal, 11, 11, cd));
            }
        }

    }

    @Override
    public void mouseDragged(MouseEvent e) {

        if (getCursor().getType() != Cursor.DEFAULT_CURSOR) {

            onMove = true;

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

            if (finalMovePointY >= this.zMax) {
                finalMovePointY = (float) this.zMax;
            } else if (finalMovePointY <= this.zMin) {
                finalMovePointY = (float) this.zMin;
            }

            float difference = finalMovePointY - initialMovePointY;

            double targetPoint = series.getY(itemIndex).floatValue() + difference;

            series.updateByIndex(itemIndex, Double.valueOf(targetPoint));

            if (dataset.getSeriesCount() > 1) {

                if (type == 'x') {
                    dataTable.setValue(targetPoint, itemIndex, serieIndex);
                } else {
                    dataTable.setValue(targetPoint, serieIndex, itemIndex);
                }

            } else {
                dataTable.setValue(targetPoint, 0, itemIndex);
            }

            onMove = false;

            initialMovePointY = finalMovePointY;

        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

}

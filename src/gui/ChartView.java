package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Scrollbar;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler.DropLocation;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.AxisEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.event.AxisChangeEvent;
import org.jfree.chart.event.AxisChangeListener;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.chart.ui.Layer;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.data.Range;
import org.jfree.data.general.DatasetUtils;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;

import dialog.DialogProperties;
import log.Log;
import log.Measure;
import observer.Observable;
import observer.Observateur;
import utils.Utilitaire;

public final class ChartView extends JPanel implements ActionListener, AdjustmentListener, AxisChangeListener, Observable {

    private static final long serialVersionUID = 1L;

    private final ChartPanel chartPanel;
    private final CombinedDomainXYPlot parentPlot;
    private static Stroke oldStrokePlot;
    private static Point2D popUpLocation;

    private ValueMarker marker;
    private static double oldXValue = Double.NaN;
    private static double xValue = Double.NaN;
    private static final HashMap<String, Double> tableValue = new HashMap<String, Double>();

    private List<Observateur> listObservateur = new ArrayList<Observateur>();

    private JScrollBar scrollBar;

    public ChartView() {

        super(new BorderLayout(), true);

        this.chartPanel = new ChartPanel(null, 680, 420, 300, 200, 1920, 1080, true, false, false, false, false, false);
        add(this.chartPanel, BorderLayout.CENTER);

        parentPlot = new CombinedDomainXYPlot();

        parentPlot.setDomainPannable(true);
        parentPlot.setOrientation(PlotOrientation.VERTICAL);
        parentPlot.setGap(10);

        oldStrokePlot = parentPlot.getOutlineStroke();

        JFreeChart chart = new JFreeChart(parentPlot);
        chart.removeLegend();

        chartPanel.setChart(chart);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(true);

        chartPanel.addMouseListener(new MyChartMouseListener());
        chartPanel.addMouseMotionListener(new MyChartMouseListener());

        scrollBar = new JScrollBar(Scrollbar.HORIZONTAL);
        // scrollBar.addAdjustmentListener(this);
        add(scrollBar, BorderLayout.SOUTH);
    }

    public ChartView(JFreeChart serializedChart) {

        super(new BorderLayout(), true);
        this.chartPanel = new ChartPanel(serializedChart, 680, 420, 300, 200, 1920, 1080, true, false, false, false, true, false);
        add(this.chartPanel, BorderLayout.CENTER);

        serializedChart.removeLegend();

        this.parentPlot = (CombinedDomainXYPlot) serializedChart.getXYPlot();
        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = parentPlot.getSubplots();
        for (XYPlot plot : subPlots) {
            plot.addChangeListener(parentPlot);

            // Collection<?> listMarker = plot.getDomainMarkers(Layer.FOREGROUND);
            Collection<?> listMarker = plot.getDomainMarkers(Layer.FOREGROUND);
            if (listMarker != null) {
                marker = (ValueMarker) listMarker.iterator().next();
                plot.clearDomainMarkers();
                plot.addDomainMarker(marker);
            }

        }

        oldStrokePlot = parentPlot.getOutlineStroke();

        this.chartPanel.setRangeZoomable(false);
        this.chartPanel.setDomainZoomable(true);

        this.chartPanel.addMouseListener(new MyChartMouseListener());
        this.chartPanel.addMouseMotionListener(new MyChartMouseListener());
    }

    private final class MyChartMouseListener extends MouseAdapter {

        private List<Range> actualRanges = new ArrayList<Range>();
        private XYPlot plot;

        @Override
        public void mouseMoved(MouseEvent e) {

            Rectangle2D dataArea = chartPanel.getScreenDataArea();

            Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());

            ValueAxis xAxis = parentPlot.getDomainAxis();
            double xMin = xAxis.java2DToValue(p.getX() - 2, dataArea, org.jfree.chart.ui.RectangleEdge.BOTTOM);
            double xMax = xAxis.java2DToValue(p.getX() + 2, dataArea, org.jfree.chart.ui.RectangleEdge.BOTTOM);

            if (xValue >= xMin && xValue <= xMax) {
                setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
                return;
            }

            ChartEntity chartEntity = chartPanel.getEntityForPoint((int) p.getX(), (int) p.getY());

            if (chartEntity instanceof AxisEntity && getDatasetType() == 1) {
                setCursor(new Cursor(Cursor.HAND_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
                chartPanel.setPopupMenu(createChartMenu());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

            plot = parentPlot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), chartPanel.translateScreenToJava2D(e.getPoint()));

            if (plot == null) {
                return;
            }

            for (int i = 0; i < plot.getRangeAxisCount(); i++) {
                actualRanges.add(plot.getRangeAxis(i).getRange());
            }

            if (SwingUtilities.isLeftMouseButton(e)) {
                chartPanel.setDomainZoomable(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                updateTableValue(e);
            }

            if (e.getClickCount() == 2 && getDatasetType() == 1) {
                Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());

                ChartEntity chartEntity = chartPanel.getEntityForPoint((int) p.getX(), (int) p.getY());

                if (chartEntity instanceof AxisEntity) {
                    AxisEntity axisEntity = (AxisEntity) chartEntity;
                    if (axisEntity.getAxis().equals(parentPlot.getDomainAxis())) {
                        return;
                    }
                    String res = JOptionPane.showInputDialog(null, "Nom de l'axe :", axisEntity.getAxis().getLabel());
                    if (res != null && !res.equals(axisEntity.getAxis().getLabel())) {
                        axisEntity.getAxis().setLabel(res);
                    }
                }
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            chartPanel.setDomainZoomable(true);

            if (plot != null && !actualRanges.isEmpty()) {
                for (int i = 0; i < plot.getRangeAxisCount(); i++) {
                    if (!plot.getRangeAxis(i).getRange().equals(actualRanges.get(i))) {
                        plot.getRangeAxis(i).setRange(actualRanges.get(i));
                    }
                }
            }

            actualRanges.clear();

            popUpLocation = chartPanel.translateScreenToJava2D(e.getPoint());

            ChartEntity chartEntity = chartPanel.getEntityForPoint((int) popUpLocation.getX(), (int) popUpLocation.getY());

            if (!(chartEntity instanceof PlotEntity)) {
                chartPanel.getPopupMenu().setVisible(false);
            }

            if (chartPanel.getPopupMenu().isVisible()) {

                boolean visibleZScale = false;
                if (getDatasetType() > 2) {
                    visibleZScale = true;
                }

                for (Component c : chartPanel.getPopupMenu().getComponents()) {
                    if ("Echelle_Z".equals(c.getName())) {
                        c.setVisible(visibleZScale);
                        break;
                    }
                }
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            int onmask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;

            if (SwingUtilities.isRightMouseButton(e) || ((e.getModifiersEx() & onmask) == onmask)) {
                return;
            }

            updateTableValue(e);
        }
    }

    private final void updateTableValue(MouseEvent e) {
        Rectangle2D dataArea = chartPanel.getScreenDataArea();

        Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());

        if (getDatasetType() > 1) {
            return;
        }
        ValueAxis xAxis = parentPlot.getDomainAxis();
        xValue = xAxis.java2DToValue(p.getX(), dataArea, org.jfree.chart.ui.RectangleEdge.BOTTOM);

        if (!xAxis.getRange().contains(xValue)) {
            xValue = Double.NaN;
        }

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();

        if (subplots.isEmpty()) {
            return;
        }

        boolean xValueUpdated = false;

        for (XYPlot subplot : subplots) {
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {

                    if (i == 0 && j == 0) {
                        int[] indices = DatasetUtils.findItemIndicesForX(subplot.getDataset(i), j, xValue);

                        if (indices[0] > -1 && indices[1] > -1 && !xValueUpdated) {
                            double x1 = subplot.getDataset(i).getXValue(j, indices[0]);
                            double x2 = subplot.getDataset(i).getXValue(j, indices[1]);

                            double moy = (x2 + x1) / 2;

                            if (xValue < moy) {
                                xValue = x1;
                            } else {
                                xValue = x2;
                            }

                            xValueUpdated = true;
                        }
                    }
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtils.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        if (xValue != oldXValue) {
            marker.setValue(xValue);
            updateObservateur("values", tableValue);
        }

        oldXValue = xValue;

    }

    public final void moveMarker(double value) {
        xValue = value;
        parentPlot.getDomainAxis().setRange(value - 5, value + 5);
        updateTableValue();
    }

    public final void updateTableValue() {

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();

        if (subplots.isEmpty()) {
            return;
        }

        for (XYPlot subplot : subplots) {
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtils.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        if (getDatasetType() < 2) {
            marker.setValue(xValue);
        }

        updateObservateur("values", tableValue);
    }

    public final void applyCondition(boolean active, BitSet bitCondition, Log log) {

        XYPlot plot = (XYPlot) parentPlot.getSubplots().get(0);

        String xLabel = plot.getDomainAxis().getLabel();
        String yLabel = plot.getRangeAxis().getLabel();

        Measure x = log.getMeasure(xLabel);
        Measure y = log.getMeasure(yLabel);

        if (getDatasetType() == 3) {
            String zLabel = ((PaintScaleLegend) chartPanel.getChart().getSubtitle(0)).getAxis().getLabel();
            Measure z = log.getMeasure(zLabel);

            final DefaultXYZDataset xyzDataset = new DefaultXYZDataset();

            if (active) {
                double[][] filterdXYZarray = { x.getDoubleValue(bitCondition), y.getDoubleValue(bitCondition), z.getDoubleValue(bitCondition) };
                xyzDataset.addSeries("Series 1", filterdXYZarray);
            } else {
                double[][] xYZarray = { x.getDoubleValue(), y.getDoubleValue(), z.getDoubleValue() };
                xyzDataset.addSeries("Series 1", xYZarray);
            }

            plot.setDataset(0, xyzDataset);
        } else {
            final DefaultXYDataset xyDataset = new DefaultXYDataset();

            if (active) {
                double[][] filterdXYarray = { x.getDoubleValue(bitCondition), y.getDoubleValue(bitCondition) };
                xyDataset.addSeries("Series 1", filterdXYarray);
            } else {
                double[][] xYarray = { x.getDoubleValue(), y.getDoubleValue() };
                xyDataset.addSeries("Series 1", xYarray);
            }

            plot.setDataset(0, xyDataset);
        }

    }

    public final List<IntervalMarker> applyCondition(boolean active, BitSet bitCondition, Color color) {

        List<IntervalMarker> listZone = new ArrayList<IntervalMarker>();

        XYSeries serie = null;

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();
        for (XYPlot subplot : subplots) {
            subplot.clearDomainMarkers();
            subplot.addDomainMarker(marker);

            if (((XYSeriesCollection) subplot.getDataset()).getSeries(0).getItemCount() > 0 && serie == null) {
                serie = ((XYSeriesCollection) subplot.getDataset()).getSeries(0);
            }
        }

        if (serie != null && active) {

            int begin;
            int end;

            for (int i = 0; i < bitCondition.size(); i++) {
                if (bitCondition.get(i)) {
                    begin = i;
                    end = bitCondition.nextClearBit(i) - 1;
                    i = end;

                    if (end - begin > 0) {
                        listZone.add(new IntervalMarker(serie.getX(begin).doubleValue(), serie.getX(end).doubleValue(), color));
                    }
                }
            }

            for (XYPlot subplot : subplots) {
                for (IntervalMarker zone : listZone) {
                    subplot.addDomainMarker(zone, Layer.BACKGROUND);
                }
            }
        }

        return listZone;
    }

    public final void applyCondition(List<IntervalMarker> listZone) {

        if (listZone == null) {
            removeCondition();
            return;
        }

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();
        for (XYPlot subplot : subplots) {
            subplot.clearDomainMarkers();
            subplot.addDomainMarker(marker);

            for (IntervalMarker zone : listZone) {
                subplot.addDomainMarker(zone, Layer.BACKGROUND);
            }

        }
    }

    public final void removeCondition() {
        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();
        for (XYPlot subplot : subplots) {

            if (subplot.getDomainMarkers(Layer.BACKGROUND) == null) {
                return;
            }

            subplot.clearDomainMarkers();
            if (marker != null) {
                subplot.addDomainMarker(marker);
            }

        }
    }

    public final XYPlot addPlot(Measure time, String backGroundColor) {

        final XYSeriesCollection collections = new XYSeriesCollection();
        final NumberAxis yAxis = new NumberAxis();
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        final XYPlot plot = new XYPlot(collections, null, yAxis, renderer);

        if (parentPlot.getSubplots().size() == 0) {

            if (parentPlot.getDomainAxis().getLabel() == null) {
                parentPlot.setDomainAxis(new NumberAxis(time.getName()));
                parentPlot.getDomainAxis().addChangeListener(this);
            }

            xValue = Double.NaN;

            marker = new ValueMarker(xValue, Color.BLUE, new BasicStroke(1.5f));
        }

        plot.setBackgroundPaint(Utilitaire.parseRGBColor(backGroundColor, 255));

        plot.addDomainMarker(0, marker, Layer.FOREGROUND);

        parentPlot.add(plot, 1);

        return plot;
    }

    public final XYPlot addPlot(Measure time, Measure measure) {

        final XYSeries series = new XYSeries(measure.getName());
        final XYSeriesCollection collections = new XYSeriesCollection(series);
        final NumberAxis yAxis = new NumberAxis(measure.getName());
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        final XYPlot plot = new XYPlot(collections, null, yAxis, renderer);

        final List<Number> temps = time.getData();
        final int nbPoint = temps.size();
        final int sizeData = measure.getData().size();

        if (parentPlot.getSubplots().size() == 0) {

            if (parentPlot.getDomainAxis().getLabel() == null) {
                parentPlot.setDomainAxis(new NumberAxis(time.getName()));
                parentPlot.getDomainAxis().addChangeListener(this);
            }
            if (nbPoint > 1) {
                xValue = temps.get(nbPoint / 2).doubleValue();
            } else {
                xValue = Double.NaN;
            }

            marker = new ValueMarker(xValue, Color.BLUE, new BasicStroke(1.5f));

        }

        plot.addDomainMarker(0, marker, Layer.FOREGROUND);

        for (int n = 0; n < nbPoint; n++) {

            if (n < sizeData) {
                series.add(temps.get(n), measure.getData().get(n), false);
            }
        }

        series.fireSeriesChanged();

        parentPlot.add(plot, 1);

        if (parentPlot.getSubplots().size() == 1) {
            parentPlot.getDomainAxis().setDefaultAutoRange(parentPlot.getDomainAxis().getRange());
            scrollBar.setMaximum((int) parentPlot.getDomainAxis().getRange().getUpperBound());
            scrollBar.getModel().setExtent(scrollBar.getMaximum());
            scrollBar.addAdjustmentListener(this);
        }

        return plot;
    }

    public final void add2DScatterPlot(Measure x, Measure y) {

        if (parentPlot.getSubplots().size() == 0) {
            final DefaultXYDataset dataset = new DefaultXYDataset();
            double[][] arrayOfDouble = { x.getDoubleValue(), y.getDoubleValue() };
            dataset.addSeries("Series 1", arrayOfDouble);
            final NumberAxis xAxis = new NumberAxis(x.getName());
            final NumberAxis yAxis = new NumberAxis(y.getName());
            final XYShapeRenderer renderer = new XYShapeRenderer();

            renderer.setDrawOutlines(true);
            renderer.setDefaultOutlinePaint(Color.BLACK);

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            chartPanel.getChart().removeLegend();

            parentPlot.setDomainAxis(xAxis);

            parentPlot.add(plot, 1);

            scrollBar.setVisible(false);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void add2DScatterPlot(Measure x, Measure y, String bckGroundColor, String shapeSize, String shapeColor) {

        if (parentPlot.getSubplots().size() == 0) {
            final DefaultXYDataset dataset = new DefaultXYDataset();
            double[][] arrayOfDouble = { x.getDoubleValue(), y.getDoubleValue() };
            dataset.addSeries("Series 1", arrayOfDouble);
            final NumberAxis xAxis = new NumberAxis(x.getName());
            final NumberAxis yAxis = new NumberAxis(y.getName());
            final XYShapeRenderer renderer = new XYShapeRenderer();

            renderer.setDrawOutlines(true);
            renderer.setDefaultOutlinePaint(Color.BLACK);

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            double size = Double.parseDouble(shapeSize);
            Shape shape = new Ellipse2D.Double(-size / 2, -size / 2, size, size);
            plot.getRenderer().setDefaultShape(shape);
            plot.getRenderer().setSeriesPaint(0, Utilitaire.parseRGBColor(shapeColor, 255));
            plot.setBackgroundPaint(Utilitaire.parseRGBColor(bckGroundColor, 255));

            chartPanel.getChart().removeLegend();

            parentPlot.setDomainAxis(xAxis);

            parentPlot.add(plot, 1);

            scrollBar.setVisible(false);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void add3DScatterPlot(Measure x, Measure y, Measure z) {

        if (parentPlot.getSubplots().size() == 0) {
            final DefaultXYZDataset dataset = new DefaultXYZDataset();
            double[][] arrayOfDouble = { x.getDoubleValue(), y.getDoubleValue(), z.getDoubleValue() };
            dataset.addSeries("Series 1", arrayOfDouble);
            final NumberAxis xAxis = new NumberAxis(x.getName());
            final NumberAxis yAxis = new NumberAxis(y.getName());

            final XYShapeRenderer renderer = new XYShapeRenderer();

            renderer.setDrawOutlines(true);
            renderer.setDefaultOutlinePaint(Color.BLACK);

            double delta = z.getMax() - z.getMin();
            double min;
            double max;

            if (delta == 0) {
                double offset = Math.abs(z.getMax() / 100);
                min = z.getMin() - offset;
                max = z.getMax() + offset;
            } else if (Double.isInfinite(delta)) {
                min = -0.1;
                max = 0.1;
            } else {
                min = z.getMin();
                max = z.getMax();
            }

            ColorPaintScale localLookupPaintScale = new ColorPaintScale(min, max);

            renderer.setPaintScale(localLookupPaintScale);

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            final NumberAxis zAxis = new NumberAxis(z.getName());

            PaintScaleLegend localPaintScaleLegend = new PaintScaleLegend(localLookupPaintScale, zAxis);
            localPaintScaleLegend.setPosition(org.jfree.chart.ui.RectangleEdge.RIGHT);
            localPaintScaleLegend.setMargin(4.0D, 4.0D, 40.0D, 4.0D);
            localPaintScaleLegend.setAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
            chartPanel.getChart().addSubtitle(localPaintScaleLegend);
            chartPanel.getChart().removeLegend();

            parentPlot.setDomainAxis(xAxis);

            parentPlot.add(plot, 1);

            scrollBar.setVisible(false);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void add3DScatterPlot(Measure x, Measure y, Measure z, String bckGroundColor, String shapeSize, String zRange) {

        if (parentPlot.getSubplots().size() == 0) {
            final DefaultXYZDataset dataset = new DefaultXYZDataset();
            double[][] arrayOfDouble = { x.getDoubleValue(), y.getDoubleValue(), z.getDoubleValue() };
            dataset.addSeries("Series 1", arrayOfDouble);
            final NumberAxis xAxis = new NumberAxis(x.getName());
            final NumberAxis yAxis = new NumberAxis(y.getName());
            final XYShapeRenderer renderer = new XYShapeRenderer();

            renderer.setDrawOutlines(true);
            renderer.setDefaultOutlinePaint(Color.BLACK);

            ColorPaintScale localLookupPaintScale;

            if (zRange != null && !zRange.trim().isEmpty()) {
                String[] splitRange = zRange.split(";", 2);
                try {
                    ;
                    double min = Double.parseDouble(splitRange[0]);
                    double max = Double.parseDouble(splitRange[1]);
                    localLookupPaintScale = new ColorPaintScale(min, max);

                } catch (NumberFormatException nfe) {
                    localLookupPaintScale = new ColorPaintScale();
                }
            } else {
                localLookupPaintScale = new ColorPaintScale();
            }

            renderer.setPaintScale(localLookupPaintScale);

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            double size = Double.parseDouble(shapeSize);
            Shape shape = new Ellipse2D.Double(-size / 2, -size / 2, size, size);
            plot.getRenderer().setDefaultShape(shape);

            plot.setBackgroundPaint(Utilitaire.parseRGBColor(bckGroundColor, 255));

            final NumberAxis zAxis = new NumberAxis(z.getName());

            PaintScaleLegend localPaintScaleLegend = new PaintScaleLegend(localLookupPaintScale, zAxis);
            localPaintScaleLegend.setPosition(RectangleEdge.RIGHT);
            localPaintScaleLegend.setMargin(4.0D, 4.0D, 40.0D, 4.0D);
            localPaintScaleLegend.setAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
            chartPanel.getChart().addSubtitle(localPaintScaleLegend);
            chartPanel.getChart().removeLegend();

            parentPlot.setDomainAxis(xAxis);

            parentPlot.add(plot, 1);

            scrollBar.setVisible(false);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void addMeasure(XYPlot plot, Measure time, Measure measure, String color, String width, String axisName) {

        final int nbDataset = plot.getDatasetCount();

        XYSeriesCollection selectedCollection = null;

        ValueAxis axis = null;
        boolean newAxis = true;

        if (plot.getRangeAxis() == null) {
            NumberAxis yAxis = new NumberAxis(axisName);
            plot.setRangeAxis(yAxis);
        } else {
            for (int i = 0; i < plot.getRangeAxisCount(); i++) {
                if (plot.getRangeAxis(i).getLabel() == null || axisName.equals(plot.getRangeAxis(i).getLabel())) {
                    axis = plot.getRangeAxis(i);

                    int idxAxis = plot.getRangeAxisIndex(axis);
                    selectedCollection = (XYSeriesCollection) plot.getDataset(idxAxis);
                    if (selectedCollection.getSeriesCount() == 0) {
                        axis.setLabel(axisName);
                    }
                    newAxis = false;
                    break;
                }
            }
        }

        if (axis == null) {
            axis = new NumberAxis(axisName);
        }

        final XYSeries newSerie = new XYSeries(measure.getName());
        final int nbPoint = measure.getData().size();

        for (int n = 0; n < nbPoint; n++) {
            newSerie.add(time.getData().get(n), measure.getData().get(n), false);
        }

        if (!newAxis) {
            selectedCollection.addSeries(newSerie);
            plot.getRenderer().setSeriesStroke(selectedCollection.getSeriesCount() - 1, new BasicStroke(Float.parseFloat(width)));
            plot.getRenderer().setSeriesPaint(selectedCollection.getSeriesCount() - 1, Utilitaire.parseRGBColor(color, 255));
        } else {
            XYSeriesCollection newCollection = new XYSeriesCollection(newSerie);
            final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
            renderer.setSeriesStroke(0, new BasicStroke(Float.parseFloat(width)));
            renderer.setSeriesPaint(0, Utilitaire.parseRGBColor(color, 255));
            plot.setRenderer(nbDataset, renderer);
            plot.setDataset(nbDataset, newCollection);
            plot.setRangeAxis(nbDataset, axis);
            plot.setRangeAxisLocation(nbDataset, AxisLocation.TOP_OR_LEFT);
            plot.mapDatasetToRangeAxis(nbDataset, nbDataset);
        }

        plot.setOutlineStroke(oldStrokePlot);

        if (parentPlot.getSubplots().size() == 1) {
            scrollBar.setMaximum((int) parentPlot.getDomainAxis().getRange().getUpperBound());
            scrollBar.getModel().setExtent(scrollBar.getMaximum());
            scrollBar.addAdjustmentListener(this);
        }

    }

    public final void addMeasure(XYPlot plot, Measure time, Measure measure, String axisName) {

        final int nbDataset = plot.getDatasetCount();

        XYSeriesCollection selectedCollection = null;

        ValueAxis axis = null;
        boolean newAxis = true;

        if (plot.getRangeAxis() == null) {
            NumberAxis yAxis = new NumberAxis(axisName);
            plot.setRangeAxis(yAxis);
        } else {
            for (int i = 0; i < plot.getRangeAxisCount(); i++) {
                if (plot.getRangeAxis(i).getLabel() == null || axisName.equals(plot.getRangeAxis(i).getLabel())) {
                    axis = plot.getRangeAxis(i);

                    int idxAxis = plot.getRangeAxisIndex(axis);
                    selectedCollection = (XYSeriesCollection) plot.getDataset(idxAxis);
                    if (selectedCollection.getSeriesCount() == 0) {
                        axis.setLabel(measure.getName());
                    }
                    newAxis = false;
                    break;
                }
            }
        }

        if (axis == null) {
            axis = new NumberAxis(axisName);
        }

        final XYSeries newSerie = new XYSeries(measure.getName());
        final int nbPoint = measure.getData().size();

        for (int n = 0; n < nbPoint; n++) {
            newSerie.add(time.getData().get(n), measure.getData().get(n), false);
        }

        if (!newAxis) {
            selectedCollection.addSeries(newSerie);
            plot.getRenderer().setSeriesStroke(selectedCollection.getSeriesCount() - 1, new BasicStroke(1.5f));
        } else {
            XYSeriesCollection newCollection = new XYSeriesCollection(newSerie);
            final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
            renderer.setSeriesStroke(0, new BasicStroke(1.5f));
            plot.setRenderer(nbDataset, renderer);
            plot.setDataset(nbDataset, newCollection);
            plot.setRangeAxis(nbDataset, axis);
            plot.setRangeAxisLocation(nbDataset, AxisLocation.TOP_OR_LEFT);
            plot.mapDatasetToRangeAxis(nbDataset, nbDataset);
        }

        plot.setOutlineStroke(oldStrokePlot);

    }

    public final void removeMeasure(String measureName) {
        XYPlot plot = getPlot(measureName);

        if (plot == null) {
            return;
        }

        XYSeriesCollection dataset;

        for (int i = 0; i < plot.getDatasetCount(); i++) {
            dataset = (XYSeriesCollection) plot.getDataset(i);

            int idxSerie = dataset.getSeriesIndex(measureName);

            if (idxSerie > -1) {
                dataset.removeSeries(idxSerie);
                tableValue.remove(measureName);
            }
        }

        updateObservateur("remove", measureName);
    }

    public final void highlightPlot(DropLocation dropLocation) {

        ChartEntity chartEntity = chartPanel.getEntityForPoint(dropLocation.getDropPoint().x, dropLocation.getDropPoint().y);
        if (chartEntity instanceof PlotEntity) {
            PlotEntity plotEntity = (PlotEntity) chartEntity;
            plotEntity.getPlot().setOutlineStroke(new BasicStroke(2f));
        } else {
            @SuppressWarnings("unchecked")
            List<XYPlot> subPlots = parentPlot.getSubplots();
            for (XYPlot plot : subPlots) {
                plot.setOutlineStroke(oldStrokePlot);
            }

        }
    }

    private final JPopupMenu createChartMenu() {

        final String ICON_PROPERTIES = "/icon_editPlot_16.png";
        final String ICON_ZOOMAUTO = "/icon_autoZoom_16.png";
        final String ICON_REMOVE = "/icon_removePlot_16.png";
        final String ICON_SCALE_Z = "/icon_scaleZ_16.png";

        JPopupMenu popUp = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Propri\u00e9t\u00e9s des graphiques", new ImageIcon(getClass().getResource(ICON_PROPERTIES)));
        menuItem.setActionCommand("PROPERTIES");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Echelle Y auto", new ImageIcon(getClass().getResource(ICON_ZOOMAUTO)));
        menuItem.setActionCommand("Y_AUTO");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Echelle X auto", new ImageIcon(getClass().getResource(ICON_ZOOMAUTO)));
        menuItem.setActionCommand("X_AUTO");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Supprimer le graphique", new ImageIcon(getClass().getResource(ICON_REMOVE)));
        menuItem.setActionCommand("DELETE_PLOT");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Echelle Z", new ImageIcon(getClass().getResource(ICON_SCALE_Z)));
        menuItem.setName("Echelle_Z");
        menuItem.setActionCommand("ECHELLE_Z");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        return popUp;
    }

    public final CombinedDomainXYPlot getPlot() {
        return this.parentPlot;
    }

    public final int getDatasetType() {

        int datasetType = 0;
        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = parentPlot.getSubplots();
        XYDataset dataset;

        if (subPlots.isEmpty()) {
            return datasetType;
        }

        for (XYPlot plot : subPlots) {
            dataset = plot.getDataset();
            if (dataset instanceof XYSeriesCollection) {
                datasetType += 1;
            } else if (dataset instanceof XYZDataset) {
                datasetType += 3;
            } else {
                datasetType += 2;
            }
        }

        return datasetType / subPlots.size();
    }

    public final double getXValue() {
        return xValue;
    }

    public final Color getMeasureColor(XYPlot xyPlot, String measureName) {

        XYItemRenderer renderer;

        for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

            int idx = xyPlot.getDataset(nDataset).indexOf(measureName);

            renderer = xyPlot.getRenderer(nDataset);

            if (idx > -1) {
                return (Color) renderer.getSeriesPaint(idx);
            }
        }

        return null;
    }

    public final XYPlot getPlot(String signalName) {
        XYPlot xyPlot;

        for (Object plot : parentPlot.getSubplots()) {
            xyPlot = (XYPlot) plot;

            switch (getDatasetType()) {
            case 1:
                for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

                    int idx = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeriesIndex(signalName);

                    if (idx > -1) {
                        return xyPlot;
                    }

                }
                break;
            case 2:

                String[] tabLabelXYChart = new String[2];
                tabLabelXYChart[0] = xyPlot.getDomainAxis().getLabel();
                tabLabelXYChart[1] = xyPlot.getRangeAxis().getLabel();

                int idx = Arrays.binarySearch(tabLabelXYChart, signalName);

                return idx >= 0 ? xyPlot : null;

            case 3:

                String[] tabLabelXYZChart = new String[3];
                tabLabelXYZChart[0] = xyPlot.getDomainAxis().getLabel();
                tabLabelXYZChart[1] = xyPlot.getRangeAxis().getLabel();
                tabLabelXYZChart[2] = ((PaintScaleLegend) chartPanel.getChart().getSubtitle(0)).getAxis().getLabel();

                int idx2 = Arrays.binarySearch(tabLabelXYZChart, signalName);

                return idx2 >= 0 ? xyPlot : null;

            default:
                return null;
            }

        }
        return null;
    }

    public final Map<String, Color> getMeasuresColors() {

        Map<String, Color> listMeasure = new TreeMap<String, Color>();
        XYPlot xyPlot;
        XYSeries serie;
        Comparable<?> key;

        if (getDatasetType() > 1) {
            return listMeasure;
        }

        for (Object plot : parentPlot.getSubplots()) {
            xyPlot = (XYPlot) plot;

            XYItemRenderer renderer;

            for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {
                int nbSerie = xyPlot.getDataset(nDataset).getSeriesCount();

                renderer = xyPlot.getRenderer(nDataset);

                for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

                    serie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeries(nSerie);

                    key = serie.getKey();

                    listMeasure.put(key.toString(), (Color) renderer.getSeriesPaint(nSerie));
                }
            }
        }
        return listMeasure;
    }

    @Override
    public void addObservateur(Observateur obs) {
        this.listObservateur.add(obs);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void updateObservateur(String type, Object object) {

        for (Observateur obs : this.listObservateur) {
            if ("values".equals(type)) {
                obs.updateValues((HashMap<String, Double>) object);
            } else {
                obs.updateData(type, object);
            }

        }
    }

    @Override
    public void delObservateur() {
        this.listObservateur = new ArrayList<Observateur>();
    }

    @Override
    public void actionPerformed(ActionEvent event) {

        String command = event.getActionCommand();

        XYPlot plot = parentPlot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), popUpLocation);

        if (plot == null && !command.equals("ECHELLE_Z")) {
            return;
        }

        switch (command) {
        case "PROPERTIES":
            if (getDatasetType() > 0) {

                DialogProperties propertiesPanel = new DialogProperties(plot);
                int res = JOptionPane.showConfirmDialog(this, propertiesPanel, "Propri\u00e9t\u00e9s", 2, -1);
                if (res == JOptionPane.OK_OPTION) {
                    propertiesPanel.updatePlot(this, plot);
                    chartPanel.getChart().fireChartChanged();
                }
            }

            break;
        case "Y_AUTO":
            plot.getRangeAxis().setAutoRange(true);
            plot.configureRangeAxes();
            parentPlot.plotChanged(new PlotChangeEvent(plot));
            break;
        case "X_AUTO":
            plot.getDomainAxis().setAutoRange(true);
            plot.getDomainAxis().configure();
            parentPlot.plotChanged(new PlotChangeEvent(plot));
            break;
        case "DELETE_PLOT":

            for (int nDataset = 0; nDataset < plot.getDatasetCount(); nDataset++) {
                for (int nSerie = 0; nSerie < plot.getDataset(nDataset).getSeriesCount(); nSerie++) {
                    updateObservateur("remove", plot.getDataset(nDataset).getSeriesKey(nSerie));
                }
            }

            parentPlot.remove(plot);
            if (plot.getRenderer() instanceof XYShapeRenderer) {
                parentPlot.getDomainAxis().setLabel(null);
                chartPanel.getChart().clearSubtitles();
            }
            break;
        case "ECHELLE_Z":
            @SuppressWarnings("rawtypes")
            Iterator iterator = chartPanel.getChart().getSubtitles().iterator();
            while (iterator.hasNext()) {
                Object o = iterator.next();
                if (o instanceof PaintScaleLegend) {

                    PaintScaleLegend paintScale = (PaintScaleLegend) o;
                    ColorPaintScale colorScale = (ColorPaintScale) paintScale.getScale();

                    String range = JOptionPane.showInputDialog("Entrer la nouvelle plage (ex : 1000;6000) :", colorScale.toString());

                    if (range != null && !range.trim().isEmpty()) {
                        String[] splitRange = range.split(";", 2);
                        try {
                            ;
                            double zMin = Double.parseDouble(splitRange[0]);
                            double zMax = Double.parseDouble(splitRange[1]);
                            colorScale.setBounds(zMin, zMax);
                            paintScale.getAxis().setRange(zMin, zMax);
                            chartPanel.getChart().fireChartChanged();
                        } catch (NumberFormatException nfe) {

                        }
                    }
                    break;
                }
            }
            break;
        default:
            break;
        }
    }

    public ChartPanel getChartPanel() {
        return chartPanel;
    }

    public JFreeChart getChart() {
        return this.chartPanel.getChart();
    }

    public DefaultBoundedRangeModel getScrollBarModel() {
        return (DefaultBoundedRangeModel) scrollBar.getModel();
    }

    public final void setScrollBarProperties(DefaultBoundedRangeModel model) {
        scrollBar.setModel(model);
    }

    public final void configureScrollbar() {
        parentPlot.getDomainAxis().setDefaultAutoRange(parentPlot.getDomainAxis().getRange());
        scrollBar.setMaximum((int) parentPlot.getDomainAxis().getRange().getUpperBound());
        scrollBar.getModel().setExtent(scrollBar.getMaximum());
        scrollBar.addAdjustmentListener(this);
    }

    public final void removeAxisSynchro(Measure time) {
        DefaultBoundedRangeModel barModel = new DefaultBoundedRangeModel(this.getScrollBarModel().getValue(), this.getScrollBarModel().getExtent(),
                this.getScrollBarModel().getMinimum(), this.getScrollBarModel().getMaximum());
        setScrollBarProperties(barModel);
        Range xAxisRange = this.getPlot().getDomainAxis().getRange();
        Range defaultRange = this.getPlot().getDomainAxis().getDefaultAutoRange();
        this.getPlot().setDomainAxis(new NumberAxis(time.getName()));
        this.getPlot().getDomainAxis().setDefaultAutoRange(defaultRange);
        this.getPlot().getDomainAxis().setRange(xAxisRange);
        this.getPlot().getDomainAxis().addChangeListener(this);
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        ValueAxis axis = parentPlot.getDomainAxis();
        Range range = axis.getRange();
        axis.setRange(scrollBar.getValue(), scrollBar.getValue() + range.getLength());
    }

    @Override
    public void axisChanged(AxisChangeEvent arg0) {

        NumberAxis axis = (NumberAxis) arg0.getAxis();
        Range axisRange = axis.getRange();

        if (!axis.getDefaultAutoRange().equals(axisRange)) {
            if (axisRange.getLowerBound() < axis.getDefaultAutoRange().getLowerBound()) {
                axis.setLowerBound(axis.getDefaultAutoRange().getLowerBound());
            }

            if (axisRange.getUpperBound() > axis.getDefaultAutoRange().getUpperBound() && axisRange.getLowerBound() > 0) {
                axis.setUpperBound(axis.getDefaultAutoRange().getUpperBound());
            }
        }

        scrollBar.removeAdjustmentListener(this);

        int value = Math.max((int) axis.getRange().getLowerBound(), 0);
        int min = Math.max(scrollBar.getMinimum(), 0);

        scrollBar.getModel().setRangeProperties(value, (int) axisRange.getLength(), min, scrollBar.getMaximum(), false);

        scrollBar.addAdjustmentListener(this);

    }
}

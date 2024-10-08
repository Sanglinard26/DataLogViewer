package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Point;
import java.awt.Scrollbar;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.DefaultBoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.SwingUtilities;

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
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.panel.CrosshairOverlay;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.Crosshair;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.SamplingXYLineRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
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

import dialog.DialTrendLine;
import dialog.DialogProperties;
import log.Log;
import log.Measure;
import observer.CursorObservable;
import observer.CursorObservateur;
import observer.Observable;
import observer.Observateur;
import utils.Utilitaire;

public final class ChartView extends JPanel implements ActionListener, AdjustmentListener, AxisChangeListener, Observable, CursorObservable {

    private static final long serialVersionUID = 1L;

    private final MyChartPanel chartPanel;
    private final CombinedDomainXYPlot parentPlot;
    private double xMaxRef = -1;
    private static Point2D popUpLocation;

    private CrosshairOverlay crosshairOverlay;
    private Crosshair crosshair;
    private static double oldXValue = Double.NaN;
    private static double xValue = Double.NaN;

    public static boolean cursorAnchored = false;
    private static double cursorPos2Dvalue = 0f;
    private static final HashMap<String, Double> tableValue = new HashMap<String, Double>();

    private List<Observateur> listObservateur = new ArrayList<Observateur>();
    private List<CursorObservateur> listCursorObservateur = new ArrayList<CursorObservateur>();
    private HashMap<String, Boolean> yAxisAutoAdapt = new HashMap<String, Boolean>();

    private JScrollBar scrollBar;

    public ChartView() {

        super(new BorderLayout(), true);

        this.chartPanel = new MyChartPanel(null, 680, 420, 300, 200, 1920, 1080, true, false, false, false, false, false);

        add(this.chartPanel, BorderLayout.CENTER);

        parentPlot = new CombinedDomainXYPlot();

        parentPlot.setDomainPannable(true);
        parentPlot.setOrientation(PlotOrientation.VERTICAL);
        parentPlot.setGap(10);

        JFreeChart chart = new JFreeChart(parentPlot);
        chart.removeLegend();

        chartPanel.setChart(chart);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(true);

        crosshairOverlay = new CrosshairOverlay();
        chartPanel.addOverlay(crosshairOverlay);

        chart.addChangeListener(new ChartChangeListener() {

            @Override
            public void chartChanged(ChartChangeEvent var1) {

            }
        });

        chartPanel.addMouseListener(new MyChartMouseListener());
        chartPanel.addMouseMotionListener(new MyChartMouseListener());
        chartPanel.addMouseWheelListener(new MyChartMouseListener());

        chartPanel.setPopupMenu(createChartMenu()); // Déplacé de mouseMoved à ici

        scrollBar = new JScrollBar(Scrollbar.HORIZONTAL);
        add(scrollBar, BorderLayout.SOUTH);
    }

    private final class MyChartMouseListener extends MouseAdapter {

        private HashMap<String, Range> actualRanges2 = new HashMap<String, Range>();
        private Set<String> yAutoAxis = new HashSet<String>();
        private XYPlot plot;

        private final Toolkit toolkit = Toolkit.getDefaultToolkit();
        private final ImageIcon imgIcon = new ImageIcon(getClass().getResource("/icon_shift_32.png"));
        private final Cursor myShiftCursor = toolkit.createCustomCursor(imgIcon.getImage(), new Point(16, 16), "cursor_shift");

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
                if (((AxisEntity) chartEntity).getAxis() == xAxis) {
                    setCursor(myShiftCursor);
                } else {
                    setCursor(new Cursor(Cursor.HAND_CURSOR));
                }

            } else {
                setCursor(Cursor.getDefaultCursor());
            }

        }

        @Override
        public void mousePressed(MouseEvent e) {

            plot = parentPlot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), chartPanel.translateScreenToJava2D(e.getPoint()));

            if (plot == null) {
                return;
            }

            for (int i = 0; i < plot.getRangeAxisCount(); i++) {
                if (plot.getRangeAxis(i) != null) {
                    actualRanges2.put(plot.getRangeAxis(i).getLabel(), plot.getRangeAxis(i).getRange());
                    if (plot.getRangeAxis(i).isAutoRange()) {
                        yAutoAxis.add(plot.getRangeAxis(i).getLabel()); // Permet de remettre l'échelle auto après
                    } // un zoom sur l'axe X
                }

            }

            if (SwingUtilities.isLeftMouseButton(e)) {
                chartPanel.setDomainZoomable(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {

            if (SwingUtilities.isLeftMouseButton(e)) {

                // Test
                if (false) {
                    Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());

                    ChartEntity chartEntity = chartPanel.getEntityForPoint((int) p.getX(), (int) p.getY());

                    if (chartEntity instanceof AxisEntity) {
                        AxisEntity axisEntity = (AxisEntity) chartEntity;
                        if (axisEntity.getAxis().equals(parentPlot.getDomainAxis())) {
                            return;
                        }

                        plot = (XYPlot) axisEntity.getAxis().getPlot();

                        for (int i = 0; i < plot.getRangeAxisCount(); i++) {
                            // plot.getRangeAxis(i).setVisible(true);
                        }

                        // axisEntity.getAxis().setLabel("empty_axis");
                        int idx = plot.getRangeAxisIndex((ValueAxis) axisEntity.getAxis());

                        // axisEntity.getAxis().setVisible(false);
                        plot.setRangeAxis(idx, null);

                        XYSeriesCollection dataset = (XYSeriesCollection) plot.getDataset(idx);

                        for (Object serie : dataset.getSeries()) {

                            tableValue.remove(((XYSeries) serie).getKey());
                            updateObservateur("remove", ((XYSeries) serie).getKey());
                        }

                        dataset.removeAllSeries();

                    }
                }
                // Fin test

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

            plot = parentPlot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), chartPanel.translateScreenToJava2D(e.getPoint()));

            chartPanel.setDomainZoomable(true);

            if (plot != null && !actualRanges2.isEmpty() && SwingUtilities.isRightMouseButton(e)) {
                for (int i = 0; i < plot.getRangeAxisCount(); i++) {

                    Range range = actualRanges2.get(plot.getRangeAxis(i).getLabel());

                    Boolean adaptON = yAxisAutoAdapt.getOrDefault(plot.getRangeAxis(i).getLabel(), false);

                    if (range != null) {
                        if (!plot.getRangeAxis(i).getRange().equals(range)) {
                            if (yAutoAxis.contains(plot.getRangeAxis(i).getLabel())) {
                                plot.getRangeAxis(i).setAutoRange(true);
                                if (scrollBar.getModel().getExtent() != scrollBar.getMaximum() && !adaptON) {
                                    plot.getRangeAxis(i).setRange(range, false, true);
                                }
                            } else {
                                plot.getRangeAxis(i).setRange(range);
                            }
                        }
                    }

                }
            }

            actualRanges2.clear();
            yAutoAxis.clear();

            popUpLocation = chartPanel.translateScreenToJava2D(e.getPoint());

            ChartEntity chartEntity = chartPanel.getEntityForPoint((int) popUpLocation.getX(), (int) popUpLocation.getY());

            if (!(chartEntity instanceof PlotEntity)) {
                chartPanel.getPopupMenu().setVisible(false);
            }

            if (chartPanel.getPopupMenu().isVisible()) {

                boolean visibleZScale = false;
                boolean visibleTrendLine = false;
                boolean visibleYAutoAdapt = false;

                if (getDatasetType() == 3) {
                    visibleZScale = true;
                }

                if (getDatasetType() == 2) {
                    visibleTrendLine = true;
                }

                if (getDatasetType() == 1) {
                    visibleYAutoAdapt = true;
                }

                for (Component c : chartPanel.getPopupMenu().getComponents()) {

                    JMenuItem item = (JMenuItem) c;

                    if ("ECHELLE_Z".equals(item.getActionCommand())) {
                        item.setVisible(visibleZScale);
                    }

                    if ("Y_AUTO_ADAPT".equals(item.getActionCommand())) {
                        item.setVisible(visibleYAutoAdapt);
                        Boolean adaptON = yAxisAutoAdapt.getOrDefault(plot.getRangeAxis().getLabel(), false);
                        ((JCheckBoxMenuItem) item).setSelected(adaptON);
                    }

                    if ("TRENDLINE".equals(item.getActionCommand())) {
                        item.setVisible(visibleTrendLine);
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

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (getCursor().getName().equals(myShiftCursor.getName())) {
                ValueAxis axis = parentPlot.getDomainAxis();
                Range range = axis.getRange();
                Range newRange = null;

                if (!e.isControlDown()) {
                    double shift = (range.getLength() / 10) * -e.getPreciseWheelRotation();
                    newRange = Range.shift(range, shift);
                } else {
                    double scale = 1;
                    if (e.getPreciseWheelRotation() < 0) {
                        scale = 0.95;
                    } else {
                        scale = 1.05;
                    }

                    double minRange = range.getCentralValue() - (range.getLength() * scale / 2);
                    double maxRange = range.getCentralValue() + (range.getLength() * scale / 2);

                    if (maxRange > minRange) {
                        newRange = new Range(minRange, maxRange);
                    } else {
                        newRange = range;
                    }

                }

                if (newRange.getLowerBound() >= 0 && newRange.getUpperBound() <= xMaxRef) {
                    axis.setRange(newRange);
                }
            }
        }

    }

    private final void updateTableValue(MouseEvent e) {
        Rectangle2D dataArea = chartPanel.getScreenDataArea();

        Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());

        if (getDatasetType() > 1 || !dataArea.contains(p)) {
            return;
        }
        ValueAxis xAxis = parentPlot.getDomainAxis();
        xValue = xAxis.java2DToValue(p.getX(), dataArea, org.jfree.chart.ui.RectangleEdge.BOTTOM);

        // Test curseur ancré
        if (cursorAnchored) {
            cursorPos2Dvalue = xAxis.valueToJava2D(xValue, dataArea, org.jfree.chart.ui.RectangleEdge.BOTTOM);
        }
        // ------------------

        if (!xAxis.getRange().contains(xValue)) {
            xValue = Double.NaN;
        }

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();

        if (subplots.isEmpty()) {
            return;
        }

        boolean xValueUpdated = false;
        int cursorIndex = 0;

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
                                cursorIndex = indices[0];
                            } else {
                                xValue = x2;
                                cursorIndex = indices[1];
                            }

                            xValueUpdated = true;
                        }
                    }
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtils.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        if (xValue != oldXValue) {
            updateObservateur("values", tableValue);
            updateCursorObservateur(cursorIndex);
            crosshair.setValue(xValue);
            crosshair.setLabelYOffset(dataArea.getHeight() - p.getY());
        }

        oldXValue = xValue;

    }

    public final void setCursorBehaviour(boolean anchor) {
        cursorAnchored = anchor;
        Rectangle2D dataArea = chartPanel.getScreenDataArea();
        ValueAxis xAxis = parentPlot.getDomainAxis();
        cursorPos2Dvalue = xAxis.valueToJava2D(xValue, dataArea, org.jfree.chart.ui.RectangleEdge.BOTTOM);
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
            crosshair.setValue(xValue);
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

            // final DefaultXYZDataset xyzDataset = new DefaultXYZDataset();

            final DefaultXYZDataset xyzDataset = (DefaultXYZDataset) ((XYPlot) parentPlot.getSubplots().get(0)).getDataset();
            xyzDataset.removeSeries("Series 1");

            if (active) {
                double[][] filterdXYZarray = { x.getDoubleValue(bitCondition), y.getDoubleValue(bitCondition), z.getDoubleValue(bitCondition) };
                xyzDataset.addSeries("Series 1", filterdXYZarray);
            } else {
                double[][] xYZarray = { x.getData(), y.getData(), z.getData() };
                if ((xYZarray[0].length == xYZarray[1].length) && (xYZarray[1].length == xYZarray[2].length)) {
                    xyzDataset.addSeries("Series 1", xYZarray);
                } else {
                    xyzDataset.addSeries("Series 1", new double[3][1]);
                }
            }

            // plot.setDataset(0, xyzDataset);
        } else {
            final DefaultXYDataset xyDataset = new DefaultXYDataset();

            if (active) {
                double[][] filterdXYarray = { x.getDoubleValue(bitCondition), y.getDoubleValue(bitCondition) };
                xyDataset.addSeries("Series 1", filterdXYarray);
            } else {
                double[][] xYarray = { x.getData(), y.getData() };
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
            subplot.setNotify(false);
            subplot.clearDomainMarkers();

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
                for (int i = 0; i < listZone.size(); i++) {
                    subplot.addDomainMarker(0, listZone.get(i), Layer.BACKGROUND, false);
                }
                // this.parentPlot.plotChanged(new PlotChangeEvent(subplot));
                subplot.setNotify(true);
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
            subplot.setNotify(false);
            subplot.clearDomainMarkers();

            for (int i = 0; i < listZone.size(); i++) {
                subplot.addDomainMarker(0, listZone.get(i), Layer.BACKGROUND, false);
            }

            // this.parentPlot.plotChanged(new PlotChangeEvent(subplot));
            subplot.setNotify(true);
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
        }
    }

    public final XYPlot addPlot(Measure time, String backGroundColor, String cursorColor, BasicStroke cursorStroke) {

        final XYSeriesCollection collections = new XYSeriesCollection();
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRangeIncludesZero(false);
        // final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        final XYItemRenderer renderer = new SamplingXYLineRenderer();
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        final XYPlot plot = new XYPlot(collections, null, yAxis, renderer);

        if (parentPlot.getSubplots().size() == 0) {

            if (parentPlot.getDomainAxis().getLabel() == null) {
                parentPlot.setDomainAxis(new NumberAxis(time.getName()));
                parentPlot.getDomainAxis().addChangeListener(this);
            }

            Color cursorPaint = Utilitaire.parseRGBColor(cursorColor, 255);

            crosshair = new Crosshair(Double.NaN, cursorPaint, cursorStroke);
            crosshair.setLabelVisible(true);
            crosshair.setLabelBackgroundPaint(Color.WHITE);
            crosshair.setLabelFont(new Font("Verdana", 0, 10));
            crosshairOverlay.addDomainCrosshair(crosshair);

            xValue = Double.NaN;
        }

        plot.setBackgroundPaint(Utilitaire.parseRGBColor(backGroundColor, 255));

        parentPlot.add(plot, 1);

        return plot;
    }

    public final XYPlot addPlot(Measure time, Measure measure) {

        final XYSeries series = new XYSeries(measure.getName());
        final XYSeriesCollection collections = new XYSeriesCollection(series);
        final NumberAxis yAxis = new NumberAxis(measure.getName());
        yAxis.setAutoRangeIncludesZero(false);

        final XYItemRenderer renderer = new SamplingXYLineRenderer();

        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        final XYPlot plot = new XYPlot(collections, null, yAxis, renderer);

        final double[] temps = time.getData();
        final int nbPoint = temps.length;
        final int sizeData = measure.getDataLength();

        parentPlot.setNotify(false);

        if (parentPlot.getSubplots().size() == 0) {

            if (parentPlot.getDomainAxis().getLabel() == null) {
                parentPlot.setDomainAxis(new NumberAxis(time.getName()));
                parentPlot.getDomainAxis().addChangeListener(this);
            }

            crosshair = new Crosshair(Double.NaN, Color.BLUE, new BasicStroke(2));
            crosshair.setLabelVisible(true);
            crosshair.setLabelBackgroundPaint(Color.WHITE);
            crosshair.setLabelFont(new Font("Verdana", 0, 10));
            crosshairOverlay.addDomainCrosshair(crosshair);

            if (nbPoint > 1) {
                xValue = temps[nbPoint / 2];
            } else {
                xValue = Double.NaN;
            }
        }

        for (int n = 0; n < nbPoint; n++) {

            if (n < sizeData) {
                series.add(temps[n], measure.get(n), false);
            }
        }

        series.fireSeriesChanged();

        parentPlot.add(plot, 1);

        if (parentPlot.getSubplots().size() == 1) {
            parentPlot.getDomainAxis().setDefaultAutoRange(parentPlot.getDomainAxis().getRange());
            scrollBar.setMaximum((int) (parentPlot.getDomainAxis().getRange().getUpperBound() * 1000));
            scrollBar.getModel().setExtent(scrollBar.getMaximum());
            scrollBar.setUnitIncrement(1000);
            scrollBar.setBlockIncrement(10000);
            scrollBar.addAdjustmentListener(this);
        }

        parentPlot.setNotify(true);

        return plot;
    }

    public final void add2DScatterPlot(Measure x, Measure y) {

        if (parentPlot.getSubplots().size() == 0) {

            chartPanel.getChart().setNotify(false);

            final DefaultXYDataset dataset = new DefaultXYDataset();
            double[][] arrayOfDouble = { x.getData(), y.getData() };
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

            chartPanel.getChart().setNotify(true);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void add2DScatterPlot(Measure x, Measure y, String bckGroundColor, String shapeSize, String shapeColor) {

        if (parentPlot.getSubplots().size() == 0) {
            final DefaultXYDataset dataset = new DefaultXYDataset();
            double[][] arrayOfDouble = { x.getData(), y.getData() };
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

            chartPanel.getChart().setNotify(false);

            final DefaultXYZDataset dataset = new DefaultXYZDataset();
            double[][] arrayOfDouble = { x.getData(), y.getData(), z.getData() };
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

            chartPanel.getChart().setNotify(true);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void add3DScatterPlot(Measure x, Measure y, Measure z, String bckGroundColor, String shapeSize, String zRange) {

        if (parentPlot.getSubplots().size() == 0) {

            final DefaultXYZDataset dataset = new DefaultXYZDataset();
            double[][] arrayOfDouble = { x.getData(), y.getData(), z.getData() };
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
        final int nbPoint = measure.getDataLength();

        for (int n = 0; n < nbPoint; n++) {
            newSerie.add(time.get(n), measure.get(n), false);
        }

        if (!newAxis) {
            selectedCollection.addSeries(newSerie);
            plot.getRenderer().setSeriesStroke(selectedCollection.getSeriesCount() - 1, new BasicStroke(Float.parseFloat(width)));
            plot.getRenderer().setSeriesPaint(selectedCollection.getSeriesCount() - 1, Utilitaire.parseRGBColor(color, 255));
        } else {
            XYSeriesCollection newCollection = new XYSeriesCollection(newSerie);
            // final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
            final XYItemRenderer renderer = new SamplingXYLineRenderer();
            renderer.setSeriesStroke(0, new BasicStroke(Float.parseFloat(width)));
            renderer.setSeriesPaint(0, Utilitaire.parseRGBColor(color, 255));
            plot.setRenderer(nbDataset, renderer);
            plot.setDataset(nbDataset, newCollection);
            plot.setRangeAxis(nbDataset, axis);
            plot.setRangeAxisLocation(nbDataset, AxisLocation.TOP_OR_LEFT);
            plot.mapDatasetToRangeAxis(nbDataset, nbDataset);
        }

        if (parentPlot.getSubplots().size() == 1) {
            // scrollBar.setMaximum((int) parentPlot.getDomainAxis().getRange().getUpperBound());
            scrollBar.setMaximum((int) (parentPlot.getDomainAxis().getRange().getUpperBound() * 1000));
            scrollBar.getModel().setExtent(scrollBar.getMaximum());
            scrollBar.setUnitIncrement(1000);
            scrollBar.setBlockIncrement(10000);
            scrollBar.addAdjustmentListener(this);
        }
    }

    public final void addMeasure(XYPlot plot, Measure time, Measure measure, String axisName) {

        parentPlot.setNotify(false);

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
        final int nbPoint = measure.getDataLength();

        for (int n = 0; n < nbPoint; n++) {
            newSerie.add(time.get(n), measure.get(n), false);
        }

        if (!newAxis) {
            selectedCollection.addSeries(newSerie);
            plot.getRenderer().setSeriesStroke(selectedCollection.getSeriesCount() - 1, new BasicStroke(1.5f));
        } else {
            XYSeriesCollection newCollection = new XYSeriesCollection(newSerie);
            // final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
            final XYItemRenderer renderer = new SamplingXYLineRenderer();
            renderer.setSeriesStroke(0, new BasicStroke(1.5f));
            plot.setRenderer(nbDataset, renderer);
            plot.setDataset(nbDataset, newCollection);
            plot.setRangeAxis(nbDataset, axis);
            plot.setRangeAxisLocation(nbDataset, AxisLocation.TOP_OR_LEFT);
            plot.mapDatasetToRangeAxis(nbDataset, nbDataset);
        }

        parentPlot.setNotify(true);
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

    private final JPopupMenu createChartMenu() {

        final String ICON_PROPERTIES = "/icon_editPlot_16.png";
        final String ICON_ZOOMAUTO = "/icon_autoZoom_16.png";
        final String ICON_REMOVE = "/icon_removePlot_16.png";
        final String ICON_SCALE_Z = "/icon_scaleZ_16.png";
        final String ICON_TRENDLINE = "/icon_trendline_16.png";

        JPopupMenu popUp = new JPopupMenu();

        JMenuItem menuItem = new JMenuItem("Propri\u00e9t\u00e9s des graphiques", new ImageIcon(getClass().getResource(ICON_PROPERTIES)));
        menuItem.setActionCommand("PROPERTIES");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        menuItem = new JMenuItem("Echelle Y auto", new ImageIcon(getClass().getResource(ICON_ZOOMAUTO)));
        menuItem.setActionCommand("Y_AUTO");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        JCheckBoxMenuItem checkItem = new JCheckBoxMenuItem("Axe Y auto-adaptatif");
        checkItem.setToolTipText("l'\u00e9chelle s'adapte en fonction du zoom sur l'axe X");
        checkItem.setActionCommand("Y_AUTO_ADAPT");
        checkItem.addActionListener(this);
        popUp.add(checkItem);

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

        menuItem = new JMenuItem("Courbe de tendance", new ImageIcon(getClass().getResource(ICON_TRENDLINE)));
        menuItem.setName("TrendLine");
        menuItem.setActionCommand("TRENDLINE");
        menuItem.addActionListener(this);
        popUp.add(menuItem);

        return popUp;
    }

    public final CombinedDomainXYPlot getPlot() {
        return this.parentPlot;
    }

    public final Crosshair getCrossair() {
        return this.crosshair;
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
                DialogProperties propertiesPanel = new DialogProperties(ChartView.this, plot);
                int res = JOptionPane.showConfirmDialog(this, propertiesPanel, "Propri\u00e9t\u00e9s", 2, -1);
                if (res == JOptionPane.OK_OPTION) {
                    propertiesPanel.updatePlot(this, plot);
                }
            }

            break;
        case "Y_AUTO":
            int nbAxis = plot.getRangeAxisCount();
            for (int i = 0; i < nbAxis; i++) {
                plot.getRangeAxis(i).setAutoRange(true);
            }
            break;
        case "Y_AUTO_ADAPT":
            int nbAxis2 = plot.getRangeAxisCount();
            for (int i = 0; i < nbAxis2; i++) {
                JCheckBoxMenuItem checkBoxMenuItem = (JCheckBoxMenuItem) event.getSource();
                yAxisAutoAdapt.put(plot.getRangeAxis(i).getLabel(), checkBoxMenuItem.isSelected());
                if (checkBoxMenuItem.isSelected()) {
                    plot.getRangeAxis(i).setAutoRange(true);
                }
            }
            break;
        case "X_AUTO":
            plot.getDomainAxis().setAutoRange(true);
            break;
        case "DELETE_PLOT":

            for (int nDataset = 0; nDataset < plot.getDatasetCount(); nDataset++) {
                for (int nSerie = 0; nSerie < plot.getDataset(nDataset).getSeriesCount(); nSerie++) {
                    updateObservateur("remove", plot.getDataset(nDataset).getSeriesKey(nSerie));
                }
            }

            parentPlot.remove(plot);

            if (parentPlot.getSubplots().isEmpty()) {
                crosshairOverlay.clearDomainCrosshairs();
            }

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
                            double zMin = Double.parseDouble(splitRange[0]);
                            double zMax = Double.parseDouble(splitRange[1]);
                            colorScale.setBounds(zMin, zMax);
                            paintScale.getAxis().setRange(zMin, zMax);
                        } catch (NumberFormatException nfe) {

                        }
                    }
                    break;
                }
            }
            break;
        case "TRENDLINE":
            new DialTrendLine(ChartView.this);
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

    public static final HashMap<String, Double> getTableValue() {
        return tableValue;
    }

    public DefaultBoundedRangeModel getScrollBarModel() {
        return (DefaultBoundedRangeModel) scrollBar.getModel();
    }

    public JScrollBar getScrollbar() {
        return this.scrollBar;
    }

    public final void setScrollBarProperties(JScrollBar refScrollBar) {
        scrollBar.setModel(refScrollBar.getModel());
        scrollBar.setUnitIncrement(refScrollBar.getUnitIncrement());
        scrollBar.setBlockIncrement(refScrollBar.getBlockIncrement());
    }

    public final void setScrollBarProperties(DefaultBoundedRangeModel model) {
        scrollBar.setModel(model);
    }

    public final void configureScrollbar() {
        if (getDatasetType() < 2) {
            parentPlot.getDomainAxis().setDefaultAutoRange(parentPlot.getDomainAxis().getRange());
            scrollBar.setMaximum((int) (parentPlot.getDomainAxis().getRange().getUpperBound() * 1000));
            scrollBar.getModel().setExtent(scrollBar.getMaximum());
            scrollBar.setUnitIncrement(1000);
            scrollBar.setBlockIncrement(10000);
            scrollBar.addAdjustmentListener(this);
        }

    }

    public final void removeAxisSynchro(Measure time) {
        DefaultBoundedRangeModel barModel = new DefaultBoundedRangeModel(this.getScrollBarModel().getValue(), this.getScrollBarModel().getExtent(),
                this.getScrollBarModel().getMinimum(), this.getScrollBarModel().getMaximum());
        setScrollBarProperties(barModel);
        Range xAxisRange = this.getPlot().getDomainAxis().getRange();
        Range defaultRange = this.getPlot().getDomainAxis().getDefaultAutoRange();

        this.parentPlot.setNotify(false);
        this.getPlot().getDomainAxis().removeChangeListener(this);
        this.getPlot().setDomainAxis(new NumberAxis(time.getName()));
        this.getPlot().getDomainAxis().setDefaultAutoRange(defaultRange);
        this.getPlot().getDomainAxis().setRange(xAxisRange);
        this.getPlot().getDomainAxis().addChangeListener(this);

        this.parentPlot.setNotify(true);
    }

    @Override
    public void adjustmentValueChanged(AdjustmentEvent e) {
        ValueAxis axis = parentPlot.getDomainAxis();
        Range range = axis.getRange();
        axis.setRange((scrollBar.getValue() / 1000.0F), (scrollBar.getValue() / 1000.0F) + range.getLength());
    }

    @Override
    public void axisChanged(AxisChangeEvent arg0) {
        NumberAxis timeAxis = (NumberAxis) arg0.getAxis();
        Range timeAxisRange = timeAxis.getRange();

        if (xMaxRef < 0) {
            xMaxRef = timeAxisRange.getUpperBound();
        }

        if (cursorAnchored) {
            xValue = timeAxis.java2DToValue(cursorPos2Dvalue, chartPanel.getScreenDataArea(), org.jfree.chart.ui.RectangleEdge.BOTTOM);
            crosshair.setValue(xValue);
            updateTableValue();
        }

        for (Object plot : parentPlot.getSubplots()) {
            XYPlot xyplot = ((XYPlot) plot);
            for (int i = 0; i < xyplot.getRangeAxisCount(); i++) {
                if (yAxisAutoAdapt.getOrDefault(xyplot.getRangeAxis(i).getLabel(), false)) {
                    if (xyplot.getRangeAxis(i).isAutoRange()) {
                        xyplot.getRangeAxis(i).configure();
                    }
                }
            }
        }

        if (!timeAxis.getDefaultAutoRange().equals(timeAxisRange)) {
            if (timeAxisRange.getLowerBound() < timeAxis.getDefaultAutoRange().getLowerBound()) {
                timeAxis.setLowerBound(timeAxis.getDefaultAutoRange().getLowerBound());
            }

            if (timeAxisRange.getUpperBound() > timeAxis.getDefaultAutoRange().getUpperBound() && timeAxisRange.getLowerBound() > 0) {
                timeAxis.setUpperBound(timeAxis.getDefaultAutoRange().getUpperBound());
            }
        }

        scrollBar.removeAdjustmentListener(this);

        int value = Math.max((int) (timeAxis.getRange().getLowerBound() * 1000), 0);
        int min = Math.max((scrollBar.getMinimum()), 0);

        scrollBar.getModel().setRangeProperties(value, (int) (timeAxisRange.getLength() * 1000), min, scrollBar.getMaximum(), false);

        int minAxisValue = (int) (timeAxisRange.getLowerBound() * 1000);
        int maxAxisValue = (int) (timeAxisRange.getUpperBound() * 1000);
        int delta = maxAxisValue - minAxisValue;

        scrollBar.setUnitIncrement(delta / 10);
        scrollBar.setBlockIncrement(delta / 5);

        scrollBar.addAdjustmentListener(this);

    }

    @Override
    public void addCursorObservateur(CursorObservateur obs) {
        this.listCursorObservateur.add(obs);
    }

    @Override
    public void updateCursorObservateur(int cursorIndex) {
        for (CursorObservateur cursorObservateur : listCursorObservateur) {
            cursorObservateur.updateCursorValue(cursorIndex);
        }
    }

    @Override
    public void delCursorObservateur() {
        this.listCursorObservateur = new ArrayList<CursorObservateur>();
    }
}

package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler.DropLocation;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
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
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.Layer;
import org.jfree.ui.RectangleEdge;

import log.Measure;
import observer.Observable;
import observer.Observateur;

public final class ChartView extends ChartPanel implements Observable {

    private static final long serialVersionUID = 1L;

    private final CombinedDomainXYPlot parentPlot;
    private Stroke oldStrokePlot;
    private Point2D popUpLocation;

    private ValueMarker marker;
    private static double xValue = Double.NaN;

    private List<Observateur> listObservateur = new ArrayList<Observateur>();

    public ChartView() {

        super(null, 680, 420, 300, 200, 1920, 1080, true, false, false, false, false, false);

        setPopupMenu(createChartMenu());

        parentPlot = new CombinedDomainXYPlot();

        parentPlot.setDomainPannable(true);
        parentPlot.setOrientation(PlotOrientation.VERTICAL);
        parentPlot.setGap(10);

        oldStrokePlot = parentPlot.getOutlineStroke();

        setChart(new JFreeChart(parentPlot));
        setRangeZoomable(false);
        setDomainZoomable(true);

        addMouseListener(new MyChartMouseListener());
        addMouseMotionListener(new MyChartMouseListener());
    }

    public ChartView(JFreeChart serializedChart) {

        super(serializedChart, 680, 420, 300, 200, 1920, 1080, true, false, false, false, true, false);

        setLayout(new BorderLayout());

        setPopupMenu(createChartMenu());

        this.parentPlot = (CombinedDomainXYPlot) serializedChart.getXYPlot();
        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = parentPlot.getSubplots();
        for (XYPlot plot : subPlots) {
            plot.addChangeListener(parentPlot);

            Collection listMarker = plot.getDomainMarkers(Layer.FOREGROUND);
            if (listMarker != null) {
                marker = (ValueMarker) listMarker.iterator().next();
                plot.clearDomainMarkers();
                plot.addDomainMarker(marker);
            }

        }

        oldStrokePlot = parentPlot.getOutlineStroke();

        setRangeZoomable(false);
        setDomainZoomable(true);

        addMouseListener(new MyChartMouseListener());
        addMouseMotionListener(new MyChartMouseListener());
    }

    private final class MyChartMouseListener extends MouseAdapter {

        @Override
        public void mouseMoved(MouseEvent e) {

            Rectangle2D dataArea = getScreenDataArea();

            Point2D p = translateScreenToJava2D(e.getPoint());

            ValueAxis xAxis = parentPlot.getDomainAxis();
            double xMin = xAxis.java2DToValue(p.getX() - 1, dataArea, RectangleEdge.BOTTOM);
            double xMax = xAxis.java2DToValue(p.getX() + 1, dataArea, RectangleEdge.BOTTOM);

            if (xValue >= xMin && xValue <= xMax) {
                setCursor(new Cursor(Cursor.E_RESIZE_CURSOR));
            } else {
                setCursor(Cursor.getDefaultCursor());
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {

            if (SwingUtilities.isLeftMouseButton(e)) {
                setDomainZoomable(false);
            }
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                updateTableValue(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            setDomainZoomable(true);

            popUpLocation = translateScreenToJava2D(e.getPoint());

            ChartEntity chartEntity = getEntityForPoint((int) popUpLocation.getX(), (int) popUpLocation.getY());

            if (!(chartEntity instanceof PlotEntity)) {
                getPopupMenu().setVisible(false);
            }

            if (getPopupMenu().isVisible()) {
                boolean visibleZScale = false;
                if (getDatasetType() > 2) {
                    visibleZScale = true;
                }
                for (Component c : getPopupMenu().getComponents()) {
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
        Rectangle2D dataArea = getScreenDataArea();

        Point2D p = translateScreenToJava2D(e.getPoint());

        if (getDatasetType() > 1) {
            return;
        }
        ValueAxis xAxis = parentPlot.getDomainAxis();
        xValue = xAxis.java2DToValue(p.getX(), dataArea, RectangleEdge.BOTTOM);

        if (!xAxis.getRange().contains(xValue)) {
            xValue = Double.NaN;
        }

        HashMap<String, Double> tableValue = new HashMap<String, Double>();

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();

        if (subplots.isEmpty()) {
            return;
        }

        for (XYPlot subplot : subplots) {
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtilities.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        marker.setValue(xValue);

        updateObservateur("values", tableValue);
    }

    public final void updateTableValue() {

        HashMap<String, Double> tableValue = new HashMap<String, Double>();

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();

        if (subplots.isEmpty()) {
            return;
        }

        for (XYPlot subplot : subplots) {
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtilities.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        if (getDatasetType() < 2) {
            marker.setValue(xValue);
        }

        updateObservateur("values", tableValue);
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
            subplot.clearDomainMarkers();
            if (marker != null) {
                subplot.addDomainMarker(marker);
            }

        }
    }

    public final void addPlot(Measure time, Measure measure) {

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
            }
            xValue = temps.get(nbPoint / 2).doubleValue();
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
            renderer.setBaseOutlinePaint(Color.BLACK);

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            getChart().removeLegend();

            parentPlot.setDomainAxis(xAxis);

            parentPlot.add(plot, 1);
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
            renderer.setBaseOutlinePaint(Color.BLACK);

            double delta = z.getMax() - z.getMin();
            double min;
            double max;

            if (delta == 0) {
                double offset = Math.abs(z.getMax() / 100);
                min = z.getMin() - offset;
                max = z.getMax() + offset;
            } else {
                min = z.getMin();
                max = z.getMax();
            }

            ColorPaintScale localLookupPaintScale = new ColorPaintScale(min, max);

            renderer.setPaintScale(localLookupPaintScale);

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);

            final NumberAxis zAxis = new NumberAxis(z.getName());

            PaintScaleLegend localPaintScaleLegend = new PaintScaleLegend(localLookupPaintScale, zAxis);
            localPaintScaleLegend.setPosition(RectangleEdge.RIGHT);
            localPaintScaleLegend.setMargin(4.0D, 4.0D, 40.0D, 4.0D);
            localPaintScaleLegend.setAxisLocation(AxisLocation.BOTTOM_OR_RIGHT);
            getChart().addSubtitle(localPaintScaleLegend);
            getChart().removeLegend();

            parentPlot.setDomainAxis(xAxis);

            parentPlot.add(plot, 1);
        } else {
            JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    public final void addMeasure(XYPlot plot, Measure time, Measure measure, String axisName) {

        final int nbDataset = plot.getDatasetCount();

        XYSeriesCollection selectedCollection = null;

        ValueAxis axis = null;
        boolean newAxis = true;

        for (int i = 0; i < plot.getRangeAxisCount(); i++) {
            if (plot.getRangeAxis(i).getLabel().equals(axisName)) {
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

    public final void highlightPlot(DropLocation dropLocation) {

        ChartEntity chartEntity = getEntityForPoint(dropLocation.getDropPoint().x, dropLocation.getDropPoint().y);
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
        menuItem.setActionCommand("DELETE");
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

    public final Set<String> getMeasures() {

        Set<String> listMeasure = new HashSet<String>();
        XYPlot xyPlot;
        XYSeries serie;
        Comparable<?> key;

        for (Object plot : parentPlot.getSubplots()) {
            xyPlot = (XYPlot) plot;

            if (!(xyPlot.getDataset() instanceof XYSeriesCollection)) {
                return listMeasure;
            }

            for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {
                int nbSerie = xyPlot.getDataset(nDataset).getSeriesCount();

                for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

                    serie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeries(nSerie);

                    key = serie.getKey();

                    listMeasure.add(key.toString());
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
                obs.updateData(object.toString());
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

        XYPlot plot = parentPlot.findSubplot(getChartRenderingInfo().getPlotInfo(), popUpLocation);

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
                    getChart().fireChartChanged();
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
        case "DELETE":

            for (int nDataset = 0; nDataset < plot.getDatasetCount(); nDataset++) {
                for (int nSerie = 0; nSerie < plot.getSeriesCount(); nSerie++) {
                    updateObservateur("data", plot.getDataset(nDataset).getSeriesKey(nSerie));
                }
            }

            parentPlot.remove(plot);
            if (plot.getRenderer() instanceof XYShapeRenderer) {
                parentPlot.getDomainAxis().setLabel(null);
                getChart().clearSubtitles();
            }
            break;
        case "ECHELLE_Z":
            Iterator iterator = getChart().getSubtitles().iterator();
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
                            getChart().fireChartChanged();
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
}

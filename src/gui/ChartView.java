package gui;

import java.awt.BasicStroke;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler.DropLocation;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PlotEntity;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;

import log.Measure;
import observer.Observable;
import observer.Observateur;

public final class ChartView extends ChartPanel implements Observable {

    private static final long serialVersionUID = 1L;

    private final CombinedDomainXYPlot combinedPlot;
    private Stroke oldStrokePlot;
    private final List<String> measuresName;

    private List<Observateur> listObservateur = new ArrayList<Observateur>();

    public ChartView() {

        super(null, 680, 420, 300, 200, 1920, 1080, true, true, false, false, true, false);

        measuresName = new ArrayList<String>();

        setPopupMenu(createPopupMenu());

        combinedPlot = new CombinedDomainXYPlot();

        combinedPlot.setDomainPannable(true);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);
        combinedPlot.setGap(10);

        oldStrokePlot = combinedPlot.getOutlineStroke();

        setChart(new JFreeChart(combinedPlot));
        setRangeZoomable(false);
        setDomainZoomable(true);

        addMouseListener(new MyChartMouseListener());
        addMouseMotionListener(new MyChartMouseListener());
    }

    public ChartView(JFreeChart serializedChart) {

        super(serializedChart, 680, 420, 300, 200, 1920, 1080, true, true, false, false, true, false);

        this.measuresName = new ArrayList<String>();

        setPopupMenu(createPopupMenu());

        this.combinedPlot = (CombinedDomainXYPlot) serializedChart.getXYPlot();
        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = combinedPlot.getSubplots();
        for (XYPlot plot : subPlots) {
            plot.addChangeListener(combinedPlot);
            int nbSerie = plot.getDataset().getSeriesCount();
            for (int i = 0; i < nbSerie; i++) {
                measuresName.add((String) plot.getDataset().getSeriesKey(i));
            }
        }

        oldStrokePlot = combinedPlot.getOutlineStroke();

        setRangeZoomable(false);
        setDomainZoomable(true);

        addMouseListener(new MyChartMouseListener());
        addMouseMotionListener(new MyChartMouseListener());

    }

    private final class MyChartMouseListener extends MouseAdapter {
        @Override
        public void mousePressed(MouseEvent e) {
            if (SwingUtilities.isLeftMouseButton(e)) {
                setDomainZoomable(false);
                updateTableValue(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            setDomainZoomable(true);
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (SwingUtilities.isRightMouseButton(e)) {
                return;
            }
            updateTableValue(e);
        }
    }

    private final void updateTableValue(MouseEvent e) {
        Rectangle2D dataArea = getScreenDataArea();

        Point2D p = translateScreenToJava2D(e.getPoint());
        XYPlot plot = combinedPlot.findSubplot(getChartRenderingInfo().getPlotInfo(), p);
        if (plot == null) {
            return;
        }
        ValueAxis xAxis = plot.getDomainAxis();
        double x = xAxis.java2DToValue(p.getX(), dataArea, RectangleEdge.BOTTOM);
        // make the crosshairs disappear if the mouse is out of range
        if (!xAxis.getRange().contains(x)) {
            x = Double.NaN;
        }

        HashMap<String, Double> tableValue = new HashMap<String, Double>();

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = combinedPlot.getSubplots();
        for (XYPlot subplot : subplots) {
            subplot.setDomainCrosshairValue(x);
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtilities.findYValue(subplot.getDataset(i), j, x));
                }
            }
        }

        updateObservateur("values", tableValue);
    }

    public final void addPlot(Measure time, Measure measure) {

        final XYSeries series = new XYSeries(measure.getName());
        final XYSeriesCollection collections = new XYSeriesCollection(series);
        final NumberAxis yAxis = new NumberAxis(measure.getName());
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
        renderer.setSeriesShape(0, ShapeUtilities.createRegularCross(2, 0.5f));
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        final XYPlot plot = new XYPlot(collections, null, yAxis, renderer);

        final List<Double> temps = time.getData();
        final int nbPoint = temps.size();
        final int sizeData = measure.getData().size();

        double crossHairValue = Double.NaN;

        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairStroke(new BasicStroke(1.5f));
        plot.setDomainCrosshairLockedOnData(false);
        if (combinedPlot.getSubplots().size() > 0) {
            crossHairValue = ((XYPlot) combinedPlot.getSubplots().get(0)).getDomainCrosshairValue();
        } else {
            crossHairValue = temps.get(nbPoint / 2);
        }
        plot.setDomainCrosshairValue(crossHairValue);

        for (int n = 0; n < nbPoint; n++) {

            if (n < sizeData) {
                series.add(temps.get(n), measure.getData().get(n));
            }
        }

        combinedPlot.add(plot, 1);

        measuresName.add(measure.getName());
    }

    public final void addMeasure(Point point, Measure measure) {

        final XYPlot plot = ((CombinedDomainXYPlot) getChart().getPlot()).findSubplot(getChartRenderingInfo().getPlotInfo(), point);
        final XYSeriesCollection collection = (XYSeriesCollection) plot.getDataset();
        final XYSeries serie = collection.getSeries(0);

        final XYSeries newSerie = new XYSeries(measure.getName());

        final int nbPoint = serie.getItemCount();
        final int sizeData = measure.getData().size();

        for (int n = 0; n < nbPoint; n++) {

            if (n < sizeData) {
                newSerie.add(serie.getX(n), measure.getData().get(n));
            }
        }

        collection.addSeries(newSerie);

        plot.getRenderer().setSeriesShape(collection.getSeriesCount() - 1, ShapeUtilities.createRegularCross(2, 0.5f));
        plot.getRenderer().setSeriesStroke(collection.getSeriesCount() - 1, new BasicStroke(1.5f));

        plot.setOutlineStroke(oldStrokePlot);

        measuresName.add(measure.getName());

    }

    public final void highlightPlot(DropLocation dropLocation) {

        ChartEntity chartEntity = getEntityForPoint(dropLocation.getDropPoint().x, dropLocation.getDropPoint().y);
        if (chartEntity instanceof PlotEntity) {
            PlotEntity plotEntity = (PlotEntity) chartEntity;
            plotEntity.getPlot().setOutlineStroke(new BasicStroke(2f));
        } else {
            @SuppressWarnings("unchecked")
            List<XYPlot> subPlots = combinedPlot.getSubplots();
            for (XYPlot plot : subPlots) {
                plot.setOutlineStroke(oldStrokePlot);
            }

        }
    }

    private final JPopupMenu createPopupMenu() {

        final String ICON_PROPERTIES = "/icon_editPlot_16.png";

        JPopupMenu popUp = new JPopupMenu("Graphique :");

        JMenuItem propertiesItem = new JMenuItem("Propiétes des graphiques", new ImageIcon(getClass().getResource(ICON_PROPERTIES)));
        propertiesItem.setActionCommand("PROPERTIES");
        propertiesItem.addActionListener(this);
        popUp.add(propertiesItem);

        return popUp;
    }

    public final CombinedDomainXYPlot getPlot() {
        return this.combinedPlot;
    }

    public final List<String> getMeasures() {
        return measuresName;
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

        switch (command) {
        case "PROPERTIES":
            DialogProperties propertiesPanel = new DialogProperties(combinedPlot);
            int res = JOptionPane.showConfirmDialog(this, propertiesPanel, "Propriétés", 2, -1);
            if (res == JOptionPane.OK_OPTION) {
                propertiesPanel.updatePlot(this);
            }
            break;

        default:
            break;
        }

    }

}

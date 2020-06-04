package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.general.DatasetUtilities;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleEdge;
import org.jfree.util.ShapeUtilities;

import log.Measure;
import observer.Observable;
import observer.Observateur;

public final class ChartView extends ChartPanel implements Observable {

    private static final long serialVersionUID = 1L;

    private final CombinedDomainXYPlot parentPlot;
    private Stroke oldStrokePlot;

    private static double xValue = Double.NaN;

    private List<Observateur> listObservateur = new ArrayList<Observateur>();

    public ChartView() {

        super(null, 680, 420, 300, 200, 1920, 1080, true, false, false, false, false, false);

        setPopupMenu(createPopupMenu());

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

        super(serializedChart, 680, 420, 300, 200, 1920, 1080, true, true, false, false, true, false);

        setLayout(new BorderLayout());

        setPopupMenu(createPopupMenu());

        this.parentPlot = (CombinedDomainXYPlot) serializedChart.getXYPlot();
        @SuppressWarnings("unchecked")
        List<XYPlot> subPlots = parentPlot.getSubplots();
        for (XYPlot plot : subPlots) {
            plot.addChangeListener(parentPlot);
        }

        oldStrokePlot = parentPlot.getOutlineStroke();

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
        }

        @Override
        public void mouseDragged(MouseEvent e) {
        	int onmask = MouseEvent.CTRL_DOWN_MASK | MouseEvent.BUTTON1_DOWN_MASK;

            if (SwingUtilities.isRightMouseButton(e) || ((e.getModifiersEx() & onmask) == onmask) ) {
                return;
            }
            updateTableValue(e);
        }
    }

    private final void updateTableValue(MouseEvent e) {
        Rectangle2D dataArea = getScreenDataArea();

        Point2D p = translateScreenToJava2D(e.getPoint());
        XYPlot plot = parentPlot.findSubplot(getChartRenderingInfo().getPlotInfo(), p);
        if (plot == null) {
            return;
        }
        ValueAxis xAxis = plot.getDomainAxis();
        xValue = xAxis.java2DToValue(p.getX(), dataArea, RectangleEdge.BOTTOM);
        // make the crosshairs disappear if the mouse is out of range
        if (!xAxis.getRange().contains(xValue)) {
            xValue = Double.NaN;
        }

        HashMap<String, Double> tableValue = new HashMap<String, Double>();

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();
        for (XYPlot subplot : subplots) {
            subplot.setDomainCrosshairValue(xValue);
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtilities.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        updateObservateur("values", tableValue);
    }

    public final void updateTableValue() {

        HashMap<String, Double> tableValue = new HashMap<String, Double>();

        @SuppressWarnings("unchecked")
        List<XYPlot> subplots = parentPlot.getSubplots();
        for (XYPlot subplot : subplots) {
            subplot.setDomainCrosshairValue(xValue);
            for (int i = 0; i < subplot.getDatasetCount(); i++) {
                for (int j = 0; j < subplot.getDataset(i).getSeriesCount(); j++) {
                    tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(), DatasetUtilities.findYValue(subplot.getDataset(i), j, xValue));
                }
            }
        }

        updateObservateur("values", tableValue);
    }

    public final void addPlot(Measure time, Measure measure) {

        final XYSeries series = new XYSeries(measure.getName());
        final XYSeriesCollection collections = new XYSeriesCollection(series);
        final NumberAxis yAxis = new NumberAxis(measure.getName());
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        final XYPlot plot = new XYPlot(collections, null, yAxis, renderer);

        final List<Double> temps = time.getData();
        final int nbPoint = temps.size();
        final int sizeData = measure.getData().size();

        double crossHairValue = Double.NaN;

        plot.setDomainCrosshairVisible(true);
        plot.setDomainCrosshairStroke(new BasicStroke(1.5f));
        plot.setDomainCrosshairLockedOnData(false);
        if (parentPlot.getSubplots().size() > 0) {
            crossHairValue = ((XYPlot) parentPlot.getSubplots().get(0)).getDomainCrosshairValue();
        } else {
        	parentPlot.setDomainAxis(new NumberAxis(time.getName()));
            crossHairValue = temps.get(nbPoint / 2);
        }
        plot.setDomainCrosshairValue(crossHairValue);

        for (int n = 0; n < nbPoint; n++) {

            if (n < sizeData) {
                series.add(temps.get(n), measure.getData().get(n), false);
            }
        }

        series.fireSeriesChanged();
        parentPlot.add(plot, 1);
    }
    
    public final void add2DScatterPlot(Measure x, Measure y) {

    	if(parentPlot.getSubplots().size() == 0)
    	{
    		final DefaultXYDataset dataset = new DefaultXYDataset();
            double[][] arrayOfDouble = {x.getDouleValue(), y.getDouleValue()};
            dataset.addSeries("Series 1", arrayOfDouble);
            final NumberAxis xAxis = new NumberAxis(x.getName());
            final NumberAxis yAxis = new NumberAxis(y.getName());
            final XYShapeRenderer renderer = new XYShapeRenderer();

            final XYPlot plot = new XYPlot(dataset, xAxis, yAxis, renderer);
            
            getChart().removeLegend();
            
            parentPlot.setDomainAxis(xAxis);
            
            parentPlot.add(plot, 1);
    	}else{
    		JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info", JOptionPane.INFORMATION_MESSAGE);
    	} 
    }
    
    public final void add3DScatterPlot(Measure x, Measure y, Measure z) {

    	if(parentPlot.getSubplots().size() == 0)
    	{
    		final DefaultXYZDataset dataset = new DefaultXYZDataset();
            double[][] arrayOfDouble = {x.getDouleValue(), y.getDouleValue(), z.getDouleValue()};
            dataset.addSeries("Series 1", arrayOfDouble);
            final NumberAxis xAxis = new NumberAxis(x.getName());
            final NumberAxis yAxis = new NumberAxis(y.getName());
            final XYShapeRenderer renderer = new XYShapeRenderer();
            
            double delta = z.getMax()-z.getMin();
            double min;
            double max;
            
            if(delta == 0)
            {
            	double offset = Math.abs(z.getMax()/100);
            	min = z.getMin()-offset;
            	max = z.getMax()+offset;
            }else{
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
    	}else{
    		JOptionPane.showMessageDialog(this, "Un seul graphique de ce type peut-etre pr\u00e9sent par fenetre", "Info", JOptionPane.INFORMATION_MESSAGE);
    	} 
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
                newSerie.add(serie.getX(n), measure.getData().get(n), false);
            }
        }
        

        collection.addSeries(newSerie);

        plot.getRenderer().setSeriesShape(collection.getSeriesCount() - 1, ShapeUtilities.createRegularCross(2, 0.5f));
        plot.getRenderer().setSeriesStroke(collection.getSeriesCount() - 1, new BasicStroke(1.5f));

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

    private final JPopupMenu createPopupMenu() {

        final String ICON_PROPERTIES = "/icon_editPlot_16.png";

        JPopupMenu popUp = new JPopupMenu("Graphique :");

        JMenuItem propertiesItem = new JMenuItem("Propri\u00e9t\u00e9s des graphiques", new ImageIcon(getClass().getResource(ICON_PROPERTIES)));
        propertiesItem.setActionCommand("PROPERTIES");
        propertiesItem.addActionListener(this);
        popUp.add(propertiesItem);

        return popUp;
    }

    public final CombinedDomainXYPlot getPlot() {
        return this.parentPlot;
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
            
            if(!(xyPlot.getDataset() instanceof XYSeriesCollection))
            {
            	return listMeasure;
            }
            
            int nbSerie = xyPlot.getSeriesCount();

            for (int nSerie = 0; nSerie < nbSerie; nSerie++) {
            	
                serie = ((XYSeriesCollection) xyPlot.getDataset()).getSeries(nSerie);

                key = serie.getKey();

                listMeasure.add(key.toString());
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

        switch (command) {
        case "PROPERTIES":
            DialogProperties propertiesPanel = new DialogProperties(parentPlot);
            int res = JOptionPane.showConfirmDialog(this, propertiesPanel, "Propri\u00e9t\u00e9s", 2, -1);
            if (res == JOptionPane.OK_OPTION) {
                propertiesPanel.updatePlot(this);
            }
            break;

        default:
            break;
        }

    }
}

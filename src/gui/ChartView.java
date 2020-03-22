package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
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

import log.Measure;
import observer.Observable;
import observer.Observateur;

public final class ChartView extends JPanel implements Observable {

    private static final long serialVersionUID = 1L;

    private final ChartPanel chartPanel;
    private JFreeChart chart = null;
    final CombinedDomainXYPlot combinedPlot;
    final Stroke oldStrokePlot;
    private final List<String> measuresName;

    private transient List<Observateur> listObservateur = new ArrayList<Observateur>();

    public ChartView() {

        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.GRAY));

        measuresName = new ArrayList<String>();

        chartPanel = new ChartPanel(null, 680, 420, 300, 200, 1920, 1080, true, true, false, false, true, false);

        chartPanel.setPopupMenu(null);

        combinedPlot = new CombinedDomainXYPlot();

        combinedPlot.setDomainPannable(true);
        combinedPlot.setOrientation(PlotOrientation.VERTICAL);
        combinedPlot.setGap(10);

        chart = new JFreeChart(combinedPlot);

        oldStrokePlot = chart.getXYPlot().getOutlineStroke();

        chartPanel.setChart(chart);
        chartPanel.setRangeZoomable(false);
        chartPanel.setDomainZoomable(true);

        chartPanel.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
                chartPanel.setDomainZoomable(true);

            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    chartPanel.setDomainZoomable(false);

                    Rectangle2D dataArea = chartPanel.getScreenDataArea();

                    Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());
                    XYPlot plot = combinedPlot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), p);
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
                                tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(),
                                        DatasetUtilities.findYValue(subplot.getDataset(i), j, x));
                            }
                        }
                    }

                    updateObservateur(tableValue);
                }

            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
            }
        });

        chartPanel.addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseMoved(MouseEvent e) {
            }

            @Override
            public void mouseDragged(MouseEvent e) {

                if (SwingUtilities.isRightMouseButton(e)) {
                    return;
                }

                Rectangle2D dataArea = chartPanel.getScreenDataArea();

                Point2D p = chartPanel.translateScreenToJava2D(e.getPoint());
                XYPlot plot = combinedPlot.findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), p);
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
                            tableValue.put(subplot.getDataset(i).getSeriesKey(j).toString(),
                                    DatasetUtilities.findYValue(subplot.getDataset(i), j, x));
                        }
                    }
                }

                updateObservateur(tableValue);
            }
        });

        add(chartPanel, BorderLayout.CENTER);
    }

    public final void addPlot(Measure time, Measure measure) {

        final XYSeries series = new XYSeries(measure.getName());
        final XYSeriesCollection collections = new XYSeriesCollection(series);
        final NumberAxis yAxis = new NumberAxis(measure.getName());
        final XYItemRenderer renderer = new XYLineAndShapeRenderer(true, true);
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

        plot.setDataset(collections);

        measuresName.add(measure.getName());
    }

    public final void addMeasure(Point point, Measure measure) {

        final XYPlot plot = ((CombinedDomainXYPlot) chart.getPlot()).findSubplot(chartPanel.getChartRenderingInfo().getPlotInfo(), point);
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
        plot.setOutlineStroke(oldStrokePlot);

        measuresName.add(measure.getName());

    }

    public final void highlightPlot(DropLocation dropLocation) {

        ChartEntity chartEntity = chartPanel.getEntityForPoint(dropLocation.getDropPoint().x, dropLocation.getDropPoint().y);
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

    public final ChartPanel getChartPanel() {
        return chartPanel;
    }

    public final List<String> getMeasures() {
        return measuresName;
    }

    public final void serialize(File file) {

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(this);
            oos.flush();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static ChartView readObject(File file) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            ChartView chartView = (ChartView) ois.readObject();
            return chartView;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void addObservateur(Observateur obs) {
        this.listObservateur.add(obs);
    }

    @Override
    public void updateObservateur(HashMap<String, Double> tableValue) {
        for (Observateur obs : this.listObservateur) {
            obs.update(tableValue);
        }
    }

    @Override
    public void delObservateur() {
        this.listObservateur = new ArrayList<Observateur>();
    }

}

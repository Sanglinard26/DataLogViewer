/*
 * Creation : 4 juin 2021
 */
package utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.Range;
import org.jfree.data.xy.DefaultXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.data.xy.XYZDataset;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import gui.ColorPaintScale;
import gui.Condition;
import log.Formula;
import log.Measure;

public abstract class ExportUtils {

    public static final boolean ConfigToXml(File file, Map<String, JFreeChart> listChart, Set<Formula> listFormula, List<Condition> listConditions) {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element racine = doc.createElement("Configuration");
            doc.appendChild(racine);

            Element noeudObject;
            Element attribut;

            if (!listChart.isEmpty()) {
                Element windows = doc.createElement("Windows");
                racine.appendChild(windows);

                for (Entry<String, JFreeChart> entry : listChart.entrySet()) {
                    // Nom fenêtre
                    // Type graph
                    // Couleur de fond
                    // Serie
                    // Couleur série
                    // Epaisseur série

                    noeudObject = doc.createElement("Window");
                    windows.appendChild(noeudObject);

                    attribut = doc.createElement("Name");
                    attribut.appendChild(doc.createTextNode(entry.getKey()));
                    noeudObject.appendChild(attribut);

                    JFreeChart chart = entry.getValue();

                    CombinedDomainXYPlot parentPlot = (CombinedDomainXYPlot) chart.getXYPlot();

                    int datasetType = 0;
                    XYDataset dataset;

                    @SuppressWarnings("unchecked")
                    List<XYPlot> subPlots = parentPlot.getSubplots();

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

                    datasetType = datasetType / subPlots.size();

                    Element type = doc.createElement("Type");
                    type.appendChild(doc.createTextNode(String.valueOf(datasetType)));
                    noeudObject.appendChild(type);

                    Element plots = doc.createElement("Plots");
                    noeudObject.appendChild(plots);

                    for (XYPlot plot : subPlots) {

                        Element plotNode = doc.createElement("Plot");
                        plots.appendChild(plotNode);

                        Element plotBackgroundNode = doc.createElement("Background");
                        String stringColor = ((Color) plot.getBackgroundPaint()).toString();
                        int idx = stringColor.indexOf('[');
                        if (idx > -1) {
                            stringColor = stringColor.substring(idx);
                        }
                        plotBackgroundNode.appendChild(doc.createTextNode(stringColor));
                        plotNode.appendChild(plotBackgroundNode);

                        XYSeriesCollection selectedCollection = null;
                        ValueAxis axis = null;

                        XYItemRenderer renderer = null;

                        switch (datasetType) {
                        case 1:

                            Element plotTimeBase = doc.createElement("TimeBase");
                            String timeBase = parentPlot.getDomainAxis().getLabel();
                            plotTimeBase.appendChild(doc.createTextNode(timeBase));
                            plotNode.appendChild(plotTimeBase);

                            for (int i = 0; i < plot.getRangeAxisCount(); i++) {

                                axis = plot.getRangeAxis(i);

                                Element axisNode = doc.createElement("Axis");
                                plotNode.appendChild(axisNode);

                                Element axisNameNode = doc.createElement("Name");
                                axisNameNode.appendChild(doc.createTextNode(axis.getLabel()));
                                axisNode.appendChild(axisNameNode);

                                Element axisRangeNode = doc.createElement("Range");
                                Range yRange = axis.getRange();
                                String txtYRange = yRange.getLowerBound() + ";" + yRange.getUpperBound();
                                axisRangeNode.appendChild(doc.createTextNode(txtYRange));
                                axisNode.appendChild(axisRangeNode);

                                int idxAxis = plot.getRangeAxisIndex(axis);
                                selectedCollection = (XYSeriesCollection) plot.getDataset(idxAxis);

                                renderer = plot.getRendererForDataset(selectedCollection);

                                for (int idxSerie = 0; idxSerie < selectedCollection.getSeriesCount(); idxSerie++) {
                                    XYSeries serie = selectedCollection.getSeries(idxSerie);

                                    Element serieNode = doc.createElement("Serie");
                                    axisNode.appendChild(serieNode);

                                    Element serieNameNode = doc.createElement("Name");
                                    serieNameNode.appendChild(doc.createTextNode(serie.getKey().toString()));
                                    serieNode.appendChild(serieNameNode);

                                    Element serieColorNode = doc.createElement("Color");
                                    stringColor = ((Color) renderer.getSeriesPaint(idxSerie)).toString();
                                    idx = stringColor.indexOf('[');
                                    if (idx > -1) {
                                        stringColor = stringColor.substring(idx);
                                    }
                                    serieColorNode.appendChild(doc.createTextNode(stringColor));
                                    serieNode.appendChild(serieColorNode);

                                    Element serieStrokeNode = doc.createElement("Width");
                                    serieStrokeNode.appendChild(
                                            doc.createTextNode(String.valueOf(((BasicStroke) renderer.getSeriesStroke(idxSerie)).getLineWidth())));
                                    serieNode.appendChild(serieStrokeNode);
                                }

                            }
                            break;
                        case 2:

                            DefaultXYDataset xyDataset = (DefaultXYDataset) plot.getDataset();

                            XYShapeRenderer shapeRenderer = (XYShapeRenderer) plot.getRendererForDataset(xyDataset);

                            Element axisXRangeNode = doc.createElement("X_Range");
                            Range xRange = plot.getDomainAxis().getRange();
                            String txtXRange = xRange.getLowerBound() + ";" + xRange.getUpperBound();
                            axisXRangeNode.appendChild(doc.createTextNode(txtXRange));
                            plotNode.appendChild(axisXRangeNode);

                            Element axisYRangeNode = doc.createElement("Y_Range");
                            Range yRange = plot.getRangeAxis().getRange();
                            String txtYRange = yRange.getLowerBound() + ";" + yRange.getUpperBound();
                            axisYRangeNode.appendChild(doc.createTextNode(txtYRange));
                            plotNode.appendChild(axisYRangeNode);

                            Element serieNode = doc.createElement("Serie");
                            plotNode.appendChild(serieNode);

                            Element xNode = doc.createElement("X");
                            String xLabel = plot.getDomainAxis().getLabel();
                            xNode.appendChild(doc.createTextNode(xLabel));
                            serieNode.appendChild(xNode);

                            Element yNode = doc.createElement("Y");
                            String yLabel = plot.getRangeAxis().getLabel();
                            yNode.appendChild(doc.createTextNode(yLabel));
                            serieNode.appendChild(yNode);

                            Element serieColorNode = doc.createElement("Color");
                            stringColor = ((Color) shapeRenderer.getSeriesPaint(0)).toString();
                            idx = stringColor.indexOf('[');
                            if (idx > -1) {
                                stringColor = stringColor.substring(idx);
                            }
                            serieColorNode.appendChild(doc.createTextNode(stringColor));
                            serieNode.appendChild(serieColorNode);

                            Element serieShapeNode = doc.createElement("Shape_size");
                            serieShapeNode.appendChild(doc.createTextNode(String.valueOf(((Ellipse2D) shapeRenderer.getDefaultShape()).getHeight())));
                            serieNode.appendChild(serieShapeNode);

                            break;
                        case 3:

                            DefaultXYZDataset xyzDataset = (DefaultXYZDataset) plot.getDataset();

                            XYShapeRenderer shapeZRenderer = (XYShapeRenderer) plot.getRendererForDataset(xyzDataset);

                            Element axisXRangeNode_ = doc.createElement("X_Range");
                            Range xRange_ = plot.getDomainAxis().getRange();
                            String txtXRange_ = xRange_.getLowerBound() + ";" + xRange_.getUpperBound();
                            axisXRangeNode_.appendChild(doc.createTextNode(txtXRange_));
                            plotNode.appendChild(axisXRangeNode_);

                            Element axisYRangeNode_ = doc.createElement("Y_Range");
                            Range yRange_ = plot.getRangeAxis().getRange();
                            String txtYRange_ = yRange_.getLowerBound() + ";" + yRange_.getUpperBound();
                            axisYRangeNode_.appendChild(doc.createTextNode(txtYRange_));
                            plotNode.appendChild(axisYRangeNode_);

                            for (Object o : chart.getSubtitles()) {
                                if (o instanceof PaintScaleLegend) {

                                    PaintScaleLegend paintScale = (PaintScaleLegend) o;
                                    ColorPaintScale colorScale = (ColorPaintScale) paintScale.getScale();

                                    Element axisZRangeNode_ = doc.createElement("Z_Range");
                                    axisZRangeNode_.appendChild(doc.createTextNode(colorScale.toString()));
                                    plotNode.appendChild(axisZRangeNode_);

                                    break;
                                }
                            }

                            Element serieNode_ = doc.createElement("Serie");
                            plotNode.appendChild(serieNode_);

                            Element xNode_ = doc.createElement("X");
                            String xLabel_ = plot.getDomainAxis().getLabel();
                            xNode_.appendChild(doc.createTextNode(xLabel_));
                            serieNode_.appendChild(xNode_);

                            Element yNode_ = doc.createElement("Y");
                            String yLabel_ = plot.getRangeAxis().getLabel();
                            yNode_.appendChild(doc.createTextNode(yLabel_));
                            serieNode_.appendChild(yNode_);

                            Element zNode_ = doc.createElement("Z");
                            String zLabel = ((PaintScaleLegend) chart.getSubtitle(0)).getAxis().getLabel();
                            zNode_.appendChild(doc.createTextNode(zLabel));
                            serieNode_.appendChild(zNode_);

                            Element serieZShapeNode = doc.createElement("Shape_size");
                            serieZShapeNode
                                    .appendChild(doc.createTextNode(String.valueOf(((Ellipse2D) shapeZRenderer.getDefaultShape()).getHeight())));
                            serieNode_.appendChild(serieZShapeNode);

                            break;
                        }

                    }
                }
            }

            if (!listFormula.isEmpty()) {
                Element formulas = doc.createElement("Formulas");
                racine.appendChild(formulas);

                Formula formula;

                for (Measure aFormula : listFormula) {

                    formula = (Formula) aFormula;

                    noeudObject = doc.createElement("Formula");
                    formulas.appendChild(noeudObject);

                    attribut = doc.createElement("Name");
                    attribut.appendChild(doc.createTextNode(formula.getName()));
                    noeudObject.appendChild(attribut);

                    attribut = doc.createElement("Unit");
                    attribut.appendChild(doc.createTextNode(formula.getUnit()));
                    noeudObject.appendChild(attribut);

                    attribut = doc.createElement("Expression");
                    attribut.appendChild(doc.createCDATASection(formula.getExpression()));
                    noeudObject.appendChild(attribut);
                }

            }

            if (!listConditions.isEmpty()) {
                Element conditions = doc.createElement("Conditions");
                racine.appendChild(conditions);

                for (Condition condition : listConditions) {
                    noeudObject = doc.createElement("Condition");
                    conditions.appendChild(noeudObject);

                    attribut = doc.createElement("Name");
                    attribut.appendChild(doc.createTextNode(condition.getName()));
                    noeudObject.appendChild(attribut);

                    attribut = doc.createElement("Expression");
                    attribut.appendChild(doc.createCDATASection(condition.getExpression()));
                    noeudObject.appendChild(attribut);

                    attribut = doc.createElement("Color");
                    String stringColor = condition.getColor().toString();
                    int idx = stringColor.indexOf('[');
                    if (idx > -1) {
                        stringColor = stringColor.substring(idx);
                    }
                    attribut.appendChild(doc.createTextNode(stringColor));
                    noeudObject.appendChild(attribut);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1"); // ISO-8859-1
            DOMSource source = new DOMSource(doc);
            StreamResult resultat = new StreamResult(file);

            transformer.transform(source, resultat);

            return true;

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }

        return false;
    }

}

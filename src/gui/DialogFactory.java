/*
 * Creation : 23 mars 2020
 */
package gui;

import java.awt.BasicStroke;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

abstract class DialogFactory {

}

final class DialogProperties extends JPanel

{
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel model;

    public DialogProperties(CombinedDomainXYPlot combinedplot) {

        model = new DefaultTableModel(new String[] { "Série", "Couleur", "Epaisseur", "Supprimer?" }, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                case 0:
                    return String.class;
                case 1:
                    return Color.class;
                case 2:
                    return Float.class;
                case 3:
                    return Boolean.class;
                default:
                    return String.class;
                }
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column > 0 ? true : false;
            }
        };

        JTable table = new JTable(model);
        table.setRowSelectionAllowed(false);

        table.setDefaultRenderer(Color.class, new ColorRenderer(true));

        table.setDefaultEditor(Color.class, new ColorEditor());

        add(new JScrollPane(table));

        XYPlot xyPlot;
        XYSeries serie;
        Comparable<?> key;

        for (Object plot : combinedplot.getSubplots()) {
            xyPlot = (XYPlot) plot;
            int nbSerie = xyPlot.getSeriesCount();

            for (int nSerie = 0; nSerie < nbSerie; nSerie++) {
                serie = ((XYSeriesCollection) xyPlot.getDataset()).getSeries(nSerie);

                key = serie.getKey();

                model.addRow(new Object[] { key, (Color) xyPlot.getRenderer().getSeriesPaint(nSerie),
                        ((BasicStroke) xyPlot.getRenderer().getSeriesStroke(nSerie)).getLineWidth(), Boolean.FALSE });

            }
        }
    }

    public final void updatePlot(ChartView chartView) {

        CombinedDomainXYPlot combinedplot = chartView.getPlot();
        XYPlot xyPlot;
        String serieName;

        for (int i = 0; i < model.getRowCount(); i++) {
            serieName = model.getValueAt(i, 0).toString();

            boolean delete = (boolean) model.getValueAt(i, 3);
            Color color = (Color) model.getValueAt(i, 1);
            float widthLine = (float) model.getValueAt(i, 2);

            for (Object plot : combinedplot.getSubplots()) {
                xyPlot = (XYPlot) plot;

                int idxSerie = ((XYSeriesCollection) xyPlot.getDataset()).getSeriesIndex(serieName);

                if (idxSerie > -1) {
                    if (delete) {
                        if (((XYSeriesCollection) xyPlot.getDataset()).getSeriesCount() == 1) {
                            combinedplot.remove(xyPlot);
                        } else {
                            ((XYSeriesCollection) xyPlot.getDataset()).removeSeries(idxSerie);
                        }
                        chartView.updateObservateur("data", serieName);
                        break;
                    }
                    xyPlot.getRenderer().setSeriesPaint(idxSerie, color);
                    xyPlot.getRenderer().setSeriesStroke(idxSerie, new BasicStroke(widthLine));
                    break;
                }

            }
        }
    }

}

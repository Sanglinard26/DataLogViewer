/*
 * Creation : 23 mars 2020
 */
package gui;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Ellipse2D;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeriesCollection;

abstract class DialogFactory {

}

final class DialogProperties extends JPanel implements ActionListener

{
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel model;
    JTextField rangeTxt;
    JButton btBackgroundColor;
    JColorChooser colorChooser;
    JDialog dialog;

    public DialogProperties(XYPlot xyPlot) {

        super(new BorderLayout());
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

        model = new DefaultTableModel(new String[] { "Serie", "Couleur", "Taille", "Supprimer?" }, 0) {
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

        btBackgroundColor = new JButton("                               ");
        btBackgroundColor.setActionCommand("edit");
        colorChooser = new JColorChooser();
        dialog = JColorChooser.createDialog(btBackgroundColor, "Pick a Color", true, colorChooser, this, null);
        btBackgroundColor.addActionListener(this);

        btBackgroundColor.setBackground((Color) xyPlot.getBackgroundPaint());
        btBackgroundColor.setContentAreaFilled(false);
        btBackgroundColor.setOpaque(true);
        btBackgroundColor.setBorder(BorderFactory.createMatteBorder(2, 5, 2, 5, panel.getBackground()));
        panel.add(btBackgroundColor);

        rangeTxt = new JTextField();
        rangeTxt.setBorder(BorderFactory.createTitledBorder("Plage Y"));
        panel.add(rangeTxt);

        JTable table = new JTable(model);
        table.setRowSelectionAllowed(false);

        table.setDefaultRenderer(Color.class, new ColorRenderer(true));

        table.setDefaultEditor(Color.class, new ColorEditor());

        panel.add(new JScrollPane(table));

        add(panel, BorderLayout.CENTER);

        Comparable<?> key;
        XYItemRenderer renderer;
        Range yRange;

        int nbSerie = xyPlot.getSeriesCount();

        yRange = xyPlot.getRangeAxis().getRange();
        String txtYRange = yRange.getLowerBound() + ";" + yRange.getUpperBound();

        rangeTxt.setText(txtYRange);

        renderer = xyPlot.getRenderer();

        if (renderer instanceof XYLineAndShapeRenderer) {
            for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

                key = xyPlot.getDataset().getSeriesKey(nSerie);

                model.addRow(new Object[] { key, (Color) renderer.getSeriesPaint(nSerie),
                        ((BasicStroke) renderer.getSeriesStroke(nSerie)).getLineWidth(), Boolean.FALSE });

            }
        } else {

            key = xyPlot.getDataset().getSeriesKey(0);

            XYShapeRenderer shapeRenderer = (XYShapeRenderer) renderer;

            model.addRow(
                    new Object[] { key, (Color) renderer.getSeriesPaint(0), ((Ellipse2D) shapeRenderer.getBaseShape()).getHeight(), Boolean.FALSE });
        }

    }

    public final void updatePlot(ChartView chartView, XYPlot xyPlot) {

        CombinedDomainXYPlot combinedplot = chartView.getPlot();
        String serieName;

        String[] splitRange = rangeTxt.getText().split(";");
        if (splitRange.length == 2) {
            try {
                double lowerBound = Double.parseDouble(splitRange[0]);
                double upperBound = Double.parseDouble(splitRange[1]);
                Range newRange = new Range(lowerBound, upperBound);
                if (!xyPlot.getRangeAxis().getRange().equals(newRange)) {
                    xyPlot.getRangeAxis().setRange(newRange);
                }

            } catch (NumberFormatException e) {

            }
        }

        xyPlot.setBackgroundPaint(btBackgroundColor.getBackground());

        for (int i = 0; i < model.getRowCount(); i++) {
            serieName = model.getValueAt(i, 0).toString();

            boolean delete = (boolean) model.getValueAt(i, 3);
            Color color;

            if (xyPlot.getRenderer() instanceof XYShapeRenderer) {

                color = (Color) model.getValueAt(i, 1);
                xyPlot.getRenderer().setSeriesPaint(0, color);
                double size = Double.parseDouble(model.getValueAt(i, 2).toString());
                Shape shape = new Ellipse2D.Double(-size / 2, -size / 2, size, size);
                xyPlot.getRenderer().setBaseShape(shape);

                if (delete) {
                    combinedplot.remove(xyPlot);
                }

            }

            int idxSerie = ((XYSeriesCollection) xyPlot.getDataset()).getSeriesIndex(serieName);

            if (idxSerie > -1) {

                color = (Color) model.getValueAt(i, 1);
                float widthLine = (float) model.getValueAt(i, 2);
                xyPlot.getRenderer().setSeriesPaint(idxSerie, color);
                xyPlot.getRenderer().setSeriesStroke(idxSerie, new BasicStroke(widthLine));

                if (delete) {
                    if (((XYSeriesCollection) xyPlot.getDataset()).getSeriesCount() == 1) {
                        combinedplot.remove(xyPlot);
                    } else {
                        ((XYSeriesCollection) xyPlot.getDataset()).removeSeries(idxSerie);
                    }
                    chartView.updateObservateur("data", serieName);
                }

            }
        }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if ("edit".equals(e.getActionCommand())) {

            colorChooser.setColor(btBackgroundColor.getBackground());
            dialog.setVisible(true);

        } else { // User pressed dialog's "OK" button.
            btBackgroundColor.setBackground(colorChooser.getColor());
        }

    }

}

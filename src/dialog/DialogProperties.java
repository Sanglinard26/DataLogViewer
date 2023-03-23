/*
 * Creation : 23 mars 2020
 */
package dialog;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeriesCollection;

import gui.ChartView;
import gui.ColorEditor;
import gui.ColorRenderer;

public final class DialogProperties extends JPanel implements ActionListener

{
    private static final long serialVersionUID = 1L;

    private final DefaultTableModel model;
    private JTextField rangeX;
    private JTextField rangeY;
    private JButton btBackgroundColor;
    private JColorChooser colorChooser;
    private JDialog dialog;
    private final JComboBox<String> axisList;
    private Map<String, String> axisRange;

    public DialogProperties(final XYPlot xyPlot) {

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

        JPanel panelBackground = new JPanel(new BorderLayout());
        panelBackground.setBorder(BorderFactory.createTitledBorder("Couleur de fond"));
        btBackgroundColor = new JButton(" ");
        btBackgroundColor.setActionCommand("edit");
        colorChooser = new JColorChooser();
        dialog = JColorChooser.createDialog(btBackgroundColor, "Choix de couleur", true, colorChooser, this, null);
        btBackgroundColor.addActionListener(this);

        btBackgroundColor.setBackground((Color) xyPlot.getBackgroundPaint());
        btBackgroundColor.setContentAreaFilled(false);
        btBackgroundColor.setOpaque(true);
        btBackgroundColor.setBorder(BorderFactory.createEtchedBorder());
        panelBackground.add(btBackgroundColor, BorderLayout.CENTER);
        panel.add(panelBackground);

        JPanel panelRangeX = new JPanel(new BorderLayout());
        panelRangeX.setBorder(BorderFactory.createTitledBorder("Plage X"));
        rangeX = new JTextField();
        panelRangeX.add(rangeX, BorderLayout.CENTER);
        panel.add(panelRangeX);

        JPanel panelRangeY = new JPanel(new BorderLayout());
        panelRangeY.setBorder(BorderFactory.createTitledBorder("Plage Y"));

        axisList = new JComboBox<String>();
        ValueAxis axis;
        axisRange = new HashMap<String, String>(xyPlot.getRangeAxisCount());
        for (int i = 0; i < xyPlot.getRangeAxisCount(); i++) {
            axis = xyPlot.getRangeAxis(i);
            axisList.addItem(axis.getLabel());
            Range yRange = axis.getRange();
            String txtYRange = yRange.getLowerBound() + ";" + yRange.getUpperBound();
            axisRange.put(axis.getLabel(), txtYRange);
        }
        axisList.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                    axisRange.put(e.getItem().toString(), rangeY.getText());
                } else {
                    rangeY.setText(axisRange.get(axisList.getSelectedItem()));
                }
            }
        });
        panelRangeY.add(axisList, BorderLayout.NORTH);
        rangeY = new JTextField();
        panelRangeY.add(rangeY, BorderLayout.CENTER);
        panel.add(panelRangeY);

        JTable table = new JTable(model) {
            private static final long serialVersionUID = 1L;

            @Override
            public Dimension getPreferredScrollableViewportSize() {
                return new Dimension(super.getPreferredSize().width, getRowHeight() * getRowCount());
            }
        };
        table.setRowSelectionAllowed(false);
        table.setDefaultRenderer(Color.class, new ColorRenderer(true));
        table.setDefaultEditor(Color.class, new ColorEditor());

        panel.add(new JScrollPane(table));
        add(panel, BorderLayout.CENTER);

        Comparable<?> key;
        XYItemRenderer renderer;

        final DecimalFormat formatter = new DecimalFormat();
        formatter.setGroupingUsed(false);
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        formatter.setDecimalFormatSymbols(decimalFormatSymbols);

        Range xRange = xyPlot.getDomainAxis().getRange();
        String txtXRange = formatter.format(xRange.getLowerBound()) + ";" + formatter.format(xRange.getUpperBound());
        rangeX.setText(txtXRange);

        Range yRange = xyPlot.getRangeAxis().getRange();
        String txtYRange = formatter.format(yRange.getLowerBound()) + ";" + formatter.format(yRange.getUpperBound());
        rangeY.setText(txtYRange);

        renderer = xyPlot.getRenderer();

        if (renderer instanceof XYLineAndShapeRenderer) {

            for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {

                int nbSerie = xyPlot.getDataset(nDataset).getSeriesCount();
                renderer = xyPlot.getRenderer(nDataset);

                for (int nSerie = 0; nSerie < nbSerie; nSerie++) {

                    key = xyPlot.getDataset(nDataset).getSeriesKey(nSerie);

                    model.addRow(new Object[] { key, (Color) renderer.getSeriesPaint(nSerie),
                            ((BasicStroke) renderer.getSeriesStroke(nSerie)).getLineWidth(), Boolean.FALSE });

                }
            }

        } else {

            key = xyPlot.getDataset().getSeriesKey(0);

            model.addRow(
                    new Object[] { key, (Color) renderer.getSeriesPaint(0), ((Ellipse2D) renderer.getDefaultShape()).getHeight(), Boolean.FALSE });

        }

    }

    public final void updatePlot(ChartView chartView, XYPlot xyPlot) {

        chartView.getChart().setNotify(false);

        CombinedDomainXYPlot combinedplot = chartView.getPlot();
        String serieName;

        String[] splitRange = rangeX.getText().replace(',', '.').split(";");
        if (splitRange.length == 2) {
            try {
                double lowerBound = Double.parseDouble(splitRange[0]);
                double upperBound = Double.parseDouble(splitRange[1]);
                Range newRange = new Range(lowerBound, upperBound);
                if (!xyPlot.getDomainAxis().getRange().equals(newRange) && upperBound - lowerBound > 0) {
                    xyPlot.getDomainAxis().setRange(newRange);
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Il y a un probl\u00e8me de synthaxe sur les valeurs min/max de l'axe X.", "Erreur",
                        JOptionPane.ERROR_MESSAGE);
            }
        }

        axisList.setSelectedIndex(-1);

        for (int i = 0; i < xyPlot.getRangeAxisCount(); i++) {
            ValueAxis axis = xyPlot.getRangeAxis(i);
            splitRange = axisRange.get(axis.getLabel()).replace(',', '.').split(";");
            if (splitRange.length == 2) {
                try {
                    double lowerBound = Double.parseDouble(splitRange[0]);
                    double upperBound = Double.parseDouble(splitRange[1]);
                    Range newRange = new Range(lowerBound, upperBound);
                    if (!axis.getRange().equals(newRange) && upperBound - lowerBound > 0) {
                        axis.setRange(newRange);
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Il y a un probl\u00e8me de synthaxe sur les valeurs min/max de l'axe Y.", "Erreur",
                            JOptionPane.ERROR_MESSAGE);
                }
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
                xyPlot.getRenderer().setDefaultShape(shape);

                if (delete) {
                    combinedplot.remove(xyPlot);
                    combinedplot.getDomainAxis().setLabel(null);
                    chartView.getChart().clearSubtitles();
                }
                chartView.getChart().setNotify(true);
                return;
            }

            int idxSerie = -1;
            XYItemRenderer renderer = null;
            XYSeriesCollection dataset = null;

            for (int nDataset = 0; nDataset < xyPlot.getDatasetCount(); nDataset++) {
                idxSerie = ((XYSeriesCollection) xyPlot.getDataset(nDataset)).getSeriesIndex(serieName);
                if (idxSerie > -1) {
                    renderer = xyPlot.getRenderer(nDataset);
                    dataset = (XYSeriesCollection) xyPlot.getDataset(nDataset);
                    break;
                }
            }

            if (idxSerie > -1 && renderer != null) {

                color = (Color) model.getValueAt(i, 1);
                float widthLine = (float) model.getValueAt(i, 2);
                renderer.setSeriesPaint(idxSerie, color);
                renderer.setSeriesStroke(idxSerie, new BasicStroke(widthLine));

                chartView.updateObservateur("update", new Object[] { serieName, color });

                if (delete) {
                    if (xyPlot.getDatasetCount() == 1 && dataset.getSeriesCount() == 1) {
                        combinedplot.remove(xyPlot);
                    } else {
                        dataset.removeSeries(idxSerie);
                    }
                    chartView.updateObservateur("remove", serieName);
                }

            }
        }

        chartView.getChart().setNotify(true);

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

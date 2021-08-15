package dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import gui.Ihm;
import log.Formula;
import log.Measure;

public final class DialNewFormula extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtName;
    private final JTextField txtUnit;
    private final JTextArea formulaText;
    private ChartPanel chartPanel;

    public DialNewFormula(final Ihm ihm) {
        this(ihm, null);
    }

    public DialNewFormula(final Ihm ihm, final Formula formula) {

        super(ihm, "Edition de formule", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        final GridBagConstraints gbc = new GridBagConstraints();

        setLayout(new GridBagLayout());

        JPanel panelHeader = new JPanel();

        JPanel panTxtName = new JPanel(new BorderLayout());
        panTxtName.setBorder(BorderFactory.createTitledBorder("Nom :"));
        txtName = new JTextField(50);
        txtName.setMinimumSize(txtName.getPreferredSize());
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 4;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.WEST;
        panTxtName.add(txtName, BorderLayout.CENTER);
        panelHeader.add(panTxtName);

        add(panelHeader, gbc);

        JPanel panTxtUnit = new JPanel(new BorderLayout());
        panTxtUnit.setBorder(BorderFactory.createTitledBorder("Unit\u00e9 :"));
        txtUnit = new JTextField(10);
        txtUnit.setMinimumSize(txtUnit.getPreferredSize());
        panTxtUnit.add(txtUnit, BorderLayout.CENTER);
        panelHeader.add(panTxtUnit);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        formulaText = new JTextArea(5, 60);
        formulaText.setBorder(BorderFactory.createTitledBorder("Expression :"));
        formulaText.setFont(formulaText.getFont().deriveFont(14f));
        formulaText.setLineWrap(true);
        formulaText.setWrapStyleWord(true);
        formulaText.setTransferHandler(new TransferHandler("measure") {
            private static final long serialVersionUID = 1L;

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
                StringSelection selection = new StringSelection(formulaText.getSelectedText());
                clip.setContents(selection, selection);

                if (action == TransferHandler.MOVE) {
                    formulaText.setText(null);
                }
            }

            @Override
            public boolean canImport(TransferSupport support) {
                return true;
            }

            @Override
            public boolean importData(TransferSupport supp) {
                Transferable t = supp.getTransferable();
                String data = "";
                try {
                    data = (String) t.getTransferData(DataFlavor.stringFlavor);

                    if (!data.equals(Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor))) {
                        data = "#" + data + "#";
                    }

                } catch (UnsupportedFlavorException e) {
                    if (!"".equals(data)) {
                        data = "#" + data + "#";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                formulaText.insert(data, formulaText.getCaretPosition());

                return true;
            }
        });
        add(formulaText, gbc);

        if (formula != null) {
            txtName.setText(formula.getName());
            txtUnit.setText(formula.getUnit());
            formulaText.setText(formula.getExpression());
        }

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        add(createPad(), gbc);

        JSeparator separator = new JSeparator(SwingConstants.VERTICAL);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        add(separator, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 2;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        add(new JButton(new AbstractAction("Pr\u00e9-visualisation") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                Formula newFormula = new Formula(txtName.getText(), formulaText.getText(), ihm.getLog());
                if (ihm.getLog() != null && newFormula.isValid()) {
                    XYSeriesCollection dataset = (XYSeriesCollection) chartPanel.getChart().getXYPlot().getDataset();
                    XYSeries serie = dataset.getSeries(0);
                    serie.clear();
                    Measure time = ihm.getLog().getTime();
                    for (int i = 0; i < time.getData().size(); i++) {
                        serie.add(time.getData().get(i), newFormula.getData().get(i), false);
                    }
                    serie.fireSeriesChanged();
                    chartPanel.restoreAutoBounds();
                }

            }
        }), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 3;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        add(new JButton(new AbstractAction("OK") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {

                if (formula == null) {
                    Formula newFormula = new Formula(txtName.getText(), formulaText.getText(), ihm.getLog());
                    if (!ihm.getListFormula().contains(newFormula)) {
                        if (newFormula.isValid()) {
                            ihm.addMeasure(newFormula);
                            dispose();
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Une voie existe d\u00e9jà sous ce nom là", "Info", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    formula.setName(txtName.getText());
                    formula.setUnit(txtUnit.getText());
                    formula.setExpression(formulaText.getText());
                    formula.calculate(ihm.getLog());
                    dispose();
                }

            }
        }), gbc);

        chartPanel = new ChartPanel(null, 300, 150, 300, 150, 600, 300, true, false, false, false, false, false);
        chartPanel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries("");
        dataset.addSeries(series);
        JFreeChart chart = ChartFactory.createXYLineChart("", "", "", dataset);
        chart.getXYPlot().setBackgroundPaint(Color.WHITE);
        chart.getXYPlot().setDomainGridlinePaint(Color.GRAY);
        chart.getXYPlot().setRangeGridlinePaint(Color.GRAY);
        chart.removeLegend();
        chartPanel.setChart(chart);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        add(chartPanel, gbc);

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(ihm);
        setResizable(true);
        setVisible(true);
    }

    private final JPanel createPad() {
        String[] tab_string = new String[] { "^", "sqrt()", "cos()", "sin()", "tan()", "+", "-", "*", "/", ".", "0", "1", "2", "3", "4", "5", "6",
                "7", "8", "9" };
        JButton[] tab_button = new JButton[tab_string.length];

        JPanel pad = new JPanel(new GridLayout(4, 5));

        for (int i = 0; i < tab_string.length; i++) {
            tab_button[i] = new JButton(tab_string[i]);
            tab_button[i].addActionListener(new PadListener());
            pad.add(tab_button[i]);
        }

        return pad;
    }

    private final class PadListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String str = ((JButton) e.getSource()).getText();
            formulaText.insert(str, formulaText.getCaretPosition());

        }

    }

}

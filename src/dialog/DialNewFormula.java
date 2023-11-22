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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.TransferHandler;
import javax.swing.border.LineBorder;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import calib.MapCal;
import gui.Ihm;
import gui.MapView;
import log.Formula;
import log.Measure;
import sun.awt.datatransfer.ClipboardTransferable;

public final class DialNewFormula extends JDialog {

    private static final long serialVersionUID = 1L;

    private final JTextField txtName;
    private final JTextField txtUnit;
    private final JTextPane formulaTextPane;
    private ChartPanel chartPanel;

    public DialNewFormula(final Ihm ihm) {
        this(ihm, null, false);
    }

    public DialNewFormula(final Ihm ihm, final Formula formula, boolean isEdited) {

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
        gbc.gridwidth = 5;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.anchor = GridBagConstraints.CENTER;
        formulaTextPane = new JTextPane();
        formulaTextPane.setBorder(BorderFactory.createTitledBorder("Expression :"));
        formulaTextPane.setFont(formulaTextPane.getFont().deriveFont(14f));
        formulaTextPane.setTransferHandler(new TransferHandler("measure") {
            private static final long serialVersionUID = 1L;

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
                StringSelection selection = new StringSelection(iterateOverContent(formulaTextPane));
                clip.setContents(selection, selection);

                if (action == TransferHandler.MOVE) {
                    formulaTextPane.setText(null);
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

                    if (t instanceof ClipboardTransferable) {
                        parseFormula(data);
                        return true;
                    }

                    MapView mapView = ((Ihm) DialNewFormula.this.getParent()).getMapView();

                    if (mapView != null && mapView.isVisible()) {

                        MapCal calib = mapView.findSelectedCal();

                        if (calib != null && !calib.isUsedByFormula()) {

                            int res = JOptionPane.showConfirmDialog(null,
                                    "La fichier \"" + calib + "\"" + " n'a pas \u00e9t\u00e9 défini comme r\u00e9f\u00e9rence, le faire?", "INFO",
                                    JOptionPane.YES_NO_OPTION);
                            if (res == JOptionPane.YES_OPTION) {
                                mapView.setCalForFormula(calib.getName());
                            } else {
                                return false;
                            }
                        }
                    }
                } catch (UnsupportedFlavorException | IOException e) {
                    e.printStackTrace();
                }

                int caretPosition = formulaTextPane.getCaretPosition();

                if (checkAccolades(caretPosition)) {
                    formulaTextPane.insertComponent(createLabel(data, Color.BLUE));
                } else {
                    formulaTextPane.insertComponent(createLabel(data, Color.RED));
                }

                DialNewFormula.this.toFront();
                DialNewFormula.this.requestFocus();
                formulaTextPane.requestFocusInWindow();
                formulaTextPane.setCaretPosition(formulaTextPane.getText().length());

                return true;
            }
        });
        add(formulaTextPane, gbc);

        // AutoSuggestor autoSuggestor = new AutoSuggestor(formulaTextPane, this, words, Color.WHITE.brighter(), Color.BLUE, Color.RED, 0.75f);

        if (formula != null) {
            txtName.setText(formula.getName());
            txtUnit.setText(formula.getUnit());

            parseFormula(formula.getExpression());
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

                if (ihm.getLog() != null) {
                    Formula newFormula = new Formula(txtName.getText(), txtUnit.getText(), iterateOverContent(formulaTextPane), ihm.getLog(),
                            ihm.getSelectedCal());

                    XYSeriesCollection dataset = (XYSeriesCollection) chartPanel.getChart().getXYPlot().getDataset();
                    XYSeries serie = dataset.getSeries(0);
                    serie.clear();

                    if (newFormula.isSyntaxOK()) {

                        Measure time = ihm.getLog().getTime();
                        for (int i = 0; i < time.getDataLength(); i++) {
                            serie.add(time.get(i), newFormula.get(i), false);
                        }
                        serie.fireSeriesChanged();
                        chartPanel.restoreAutoBounds();
                    }

                } else {
                    JOptionPane.showMessageDialog(DialNewFormula.this.getParent(), "Il faut charger un log!", "Erreur", JOptionPane.ERROR_MESSAGE);
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
                    Formula newFormula = new Formula(txtName.getText(), txtUnit.getText(), iterateOverContent(formulaTextPane), ihm.getLog(),
                            ihm.getSelectedCal());
                    if (!ihm.getListFormula().contains(newFormula)) {
                        if (newFormula.isSyntaxOK()) {
                            ihm.addMeasure(newFormula);
                            dispose();
                        }
                    } else {
                        JOptionPane.showMessageDialog(null, "Une voie existe d\u00e9jà sous ce nom", "Info", JOptionPane.WARNING_MESSAGE);
                    }
                } else {
                    formula.setName(txtName.getText());
                    formula.setUnit(txtUnit.getText());
                    formula.setExpression(iterateOverContent(formulaTextPane));
                    if (isEdited) {
                        ihm.refresh(null);
                    }
                    dispose();
                }

            }
        }), gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 4;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        add(new JButton(new AbstractAction("Copier dans le presse papier") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent arg0) {
                String txtFormule = iterateOverContent(formulaTextPane);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                StringSelection stringSelection = new StringSelection(txtFormule);
                clipboard.setContents(stringSelection, null);
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
        gbc.gridwidth = 3;
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

    public final String iterateOverContent(JTextComponent textComp) {

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < textComp.getDocument().getLength(); i++) {
            Element elem = ((StyledDocument) textComp.getDocument()).getCharacterElement(i);
            AttributeSet as = elem.getAttributes();
            if (as.containsAttribute(AbstractDocument.ElementNameAttribute, StyleConstants.ComponentElementName)) {
                if (StyleConstants.getComponent(as) instanceof JLabel) {
                    JLabel myLabel = (JLabel) StyleConstants.getComponent(as);
                    Color type = ((LineBorder) myLabel.getBorder()).getLineColor();
                    if (type == Color.RED) {
                        sb.append("#" + myLabel.getText() + "#");
                    } else {
                        sb.append(myLabel.getText());
                    }
                    continue;
                }
            }
            try {
                String s = ((StyledDocument) textComp.getDocument()).getText(i, 1);
                if (!s.isEmpty()) {
                    sb.append(s);
                }
            } catch (BadLocationException e) {

            }
        }

        return sb.toString().trim();
    }

    public final void parseFormula(String txtformula) {

        Pattern pattern = Pattern.compile("\\#(.*?)\\#");
        Matcher regexMatcher = pattern.matcher(txtformula);

        String matchedMeasure;
        int idxLabel = -1;

        try {
            formulaTextPane.setText(txtformula);

            while (regexMatcher.find()) {
                matchedMeasure = regexMatcher.group(1);

                String measureText = "#" + matchedMeasure + "#";
                idxLabel = formulaTextPane.getText().indexOf(measureText);

                formulaTextPane.getDocument().remove(idxLabel, measureText.length());
                formulaTextPane.setCaretPosition(idxLabel);
                formulaTextPane.insertComponent(createLabel(matchedMeasure, Color.RED));
            }
            // *****

            // Traitement des paramètres de calibration
            String matchedParam;
            String[] splitParam = null;
            Set<Entry<String, String>> entries = Formula.mapRegexCal.entrySet();

            for (Entry<String, String> entryRegex : entries) {

                pattern = Pattern.compile(entryRegex.getValue());
                regexMatcher = pattern.matcher(txtformula);

                while (regexMatcher.find()) {
                    matchedParam = regexMatcher.group(0);

                    int idxAccolade = matchedParam.indexOf('{');
                    if (idxAccolade > 6) {
                        splitParam = matchedParam.substring(idxAccolade + 1).replace("}", "").split(",");
                    }

                    for (String sParam : splitParam) {
                        idxLabel = formulaTextPane.getText().indexOf(sParam);

                        formulaTextPane.getDocument().remove(idxLabel, sParam.length());
                        formulaTextPane.setCaretPosition(idxLabel);
                        formulaTextPane.insertComponent(createLabel(sParam, Color.BLUE));
                    }

                }
            }
            // *****
        } catch (BadLocationException e) {
            e.printStackTrace();
        }

    }

    private boolean checkAccolades(int caretPosition) {

        final char accoladeOuvrante = '{';
        final char accoladeFermante = '}';

        final String text = formulaTextPane.getText();
        final int textLength = text.length();

        if (text.isEmpty() || textLength == caretPosition || caretPosition == 0) {
            return false;
        }

        int idx1 = caretPosition;
        int idx2 = caretPosition;

        do {
            if (text.charAt(idx1--) == accoladeOuvrante) {
                break;
            }
        } while (idx1 > 0);

        do {
            if (text.charAt(idx2++) == accoladeFermante) {
                break;
            }
        } while (idx2 == textLength - 1);

        return idx2 > idx1 && idx1 != 0 && idx2 != textLength - 1;
    }

    private final JPanel createPad() {
        String[] tab_string = new String[] { "SCALAIRE{}", "TABLE1D{,}", "TABLE2D{,,}", "abs()", "^", "sqrt()", "delta(,)", "passeBasTypeK(,)",
                "bitactif(,)", "saturation(,,)", "+", "-", "*", "/", ".", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };
        String[] tabDescription = new String[] {
                "<html><u>Description:</u> Permet d'intégrer une valeur scalaire de calibration dans la formule"
                        + "<br><u>Exemple:</u> SCALAIRE{Seuil papillon activ. régul ralenti}",
                "<html><u>Description:</u> Permet d'intégrer une courbe de calibration dans la formule"
                        + "<br><u>Exemple:</u> TABLE1D{Avance Temp. Air, Air_T(°C)}",
                "<html><u>Description:</u> Permet d'intégrer une map de calibration dans la formule"
                        + "<br><u>Exemple:</u> TABLE2D{Rendement volumétrique, RPM(tr/min),Throttle_angle()}",
                "<html><u>Description:</u> Calcule la valeur absolue", "<html><u>Description:</u> Elève à la puissance renseignée après l'opérateur",
                "<html><u>Description:</u> Calcule la racine carrée",
                "<html><u>Description:</u> Calcule la différence de deux valeurs à n points d'interval" + "<br><u>Exemple:</u> delta(Richness(), 5)"
                        + "<br>Le calcul réalisé sera : Richness[n]-Richness[n-5]",
                "<html><u>Description:</u> Filtre passe bas du 1er ordre" + "<br><u>Exemple:</u> passeBasTypeK(Richness(), 0.1)"
                        + "<br>Le coefficient de filtrage doit être entre 0 et 1, à 1 pas de filtrage",
                "<html><u>Description:</u> Renvoie 1 si le bit sélectionné est actif" + "<br><u>Exemple:</u> bitactif(Status_transient_enrichm(), 0)",
                "<html><u>Description:</u> Sature un signal entre deux bornes" + "<br><u>Exemple:</u> saturation(RPM(tr/min), 2000,3000", null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null };
        JButton[] tab_button = new JButton[tab_string.length];

        JPanel pad = new JPanel(new GridLayout(6, 5));

        for (int i = 0; i < tab_string.length; i++) {
            tab_button[i] = new JButton(tab_string[i]);
            tab_button[i].setToolTipText(tabDescription[i]);
            tab_button[i].addActionListener(new PadListener());
            pad.add(tab_button[i]);
        }

        return pad;
    }

    private final class PadListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            String str = ((JButton) e.getSource()).getText();
            try {
                formulaTextPane.getDocument().insertString(formulaTextPane.getCaretPosition(), str, null);
            } catch (BadLocationException e1) {
                e1.printStackTrace();
            }

            int caretPosition = formulaTextPane.getCaretPosition();

            if (str.indexOf('(') > -1 || str.indexOf('{') > -1) {
                caretPosition--;
            }
            formulaTextPane.requestFocusInWindow();
            formulaTextPane.setCaretPosition(caretPosition);
        }

    }

    private final JLabel createLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(formulaTextPane.getFont());
        label.setBorder(new LineBorder(color, 1));
        label.setAlignmentY(0.8f);
        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                label.setForeground(color);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                label.setForeground(Color.BLACK);
            }
        });
        return label;

    }

}

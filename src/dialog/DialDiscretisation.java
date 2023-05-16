/*
 * Creation : 23 janv. 2023
 */
package dialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import calib.Variable;
import gui.Ihm;
import log.Log;
import log.Measure;
import utils.CopyPasteAdapter;
import utils.Preference;
import utils.Utilitaire;

public final class DialDiscretisation extends JDialog {

    private static final long serialVersionUID = 1L;

    private static final String varToMap = "Variable à discrétiser:";
    private static final String varToTarget = "Variable pour la cible:";
    private JLabel labelVariable;
    private JButton btCopyToParameter;
    private JComboBox<String> cbMethod;
    private JComboBox<String> cbBrkPtType;
    private JComboBox<String> cbSigne;

    private JTextField xLabel;
    private JTextField yLabel;
    private JTextField outputVariable;

    private JTextField nbXbrkPt;
    private JTextField nbYbrkPt;

    private JTextField paramCalib;

    private DefaultTableModel modelTableAvg;
    private JTable tableAvg;
    private DefaultTableModel modelTableTarget;
    private JTable tableTarget;

    private JTable tableContraintes;

    public DialDiscretisation(JFrame owner) {
        super(owner, "Discretisation cartographique", false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        final GridBagConstraints gbc = new GridBagConstraints();
        setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(createConfigurationPanel(), gbc);

        JButton btCreateTable = new JButton(new AbstractAction("Créer la table") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                modelTableAvg.setColumnCount(0);
                modelTableAvg.setRowCount(0);

                modelTableTarget.setColumnCount(0);
                modelTableTarget.setRowCount(0);

                switch (cbBrkPtType.getSelectedIndex()) {
                case 0:

                    String[] splitXaxis = nbXbrkPt.getText().split(":");
                    String[] splitYaxis = nbYbrkPt.getText().split(":");

                    if (splitXaxis.length == 3 && splitYaxis.length == 3) {
                        float xMin = Float.parseFloat(splitXaxis[0]);
                        float xInterval = Float.parseFloat(splitXaxis[1]);
                        float xMax = Float.parseFloat(splitXaxis[2]);

                        int nbValX = (int) ((xMax - xMin) / xInterval) + 2;
                        modelTableAvg.setColumnCount(nbValX);
                        modelTableTarget.setColumnCount(nbValX);

                        float yMin = Float.parseFloat(splitYaxis[0]);
                        float yInterval = Float.parseFloat(splitYaxis[1]);
                        float yMax = Float.parseFloat(splitYaxis[2]);

                        int nbValY = (int) ((yMax - yMin) / yInterval) + 2;
                        modelTableAvg.setRowCount(nbValY);
                        modelTableTarget.setRowCount(nbValY);

                        // tableAvg.setValueAt("Y \\ X", 0, 0);
                        // tableTarget.setValueAt("Y \\ X", 0, 0);

                        float val = xMin;
                        for (int col = 1; col < nbValX; col++) {
                            tableAvg.setValueAt(val, 0, col);
                            tableTarget.setValueAt(val, 0, col);
                            val += xInterval;
                        }

                        val = yMin;
                        for (int row = 1; row < nbValY; row++) {
                            tableAvg.setValueAt(val, row, 0);
                            tableTarget.setValueAt(val, row, 0);
                            val += yInterval;
                        }

                    } else {
                        modelTableAvg.setColumnCount(Integer.parseInt(nbXbrkPt.getText()));
                        modelTableAvg.setRowCount(Integer.parseInt(nbYbrkPt.getText()));
                        // tableAvg.setValueAt("Y \\ X", 0, 0);

                        modelTableTarget.setColumnCount(Integer.parseInt(nbXbrkPt.getText()));
                        modelTableTarget.setRowCount(Integer.parseInt(nbYbrkPt.getText()));
                        // tableTarget.setValueAt("Y \\ X", 0, 0);
                    }

                    break;
                case 2: // Fichier Map
                    Ihm ihm = (Ihm) DialDiscretisation.this.getOwner();
                    Variable var = ihm.getMapView().findSelectedCal().getVariable(paramCalib.getText());
                    int dimX = var.getDimX();
                    int dimY = var.getDimY();

                    switch (var.getType()) {
                    case COURBE:
                        modelTableAvg.setColumnCount(dimX);
                        modelTableAvg.setRowCount(dimY);

                        modelTableTarget.setColumnCount(dimX);
                        modelTableTarget.setRowCount(dimY);

                        for (int col = 0; col < dimX; col++) {
                            tableAvg.setValueAt(Float.parseFloat(var.getValue(0, 0, col).toString()), 0, col);
                            tableTarget.setValueAt(Float.parseFloat(var.getValue(0, 0, col).toString()), 0, col);
                        }
                        break;
                    case MAP:
                        modelTableAvg.setColumnCount(dimX);
                        modelTableAvg.setRowCount(dimY);

                        modelTableTarget.setColumnCount(dimX);
                        modelTableTarget.setRowCount(dimY);

                        for (int col = 0; col < dimX; col++) {
                            for (int row = 0; row < dimY; row++) {
                                if (col + row != 0) {
                                    tableAvg.setValueAt(Float.parseFloat(var.getValue(0, row, col).toString()), row, col);
                                    tableTarget.setValueAt(Float.parseFloat(var.getValue(0, row, col).toString()), row, col);
                                }

                            }
                            // tableAvg.setValueAt(Float.parseFloat(var.getValue(0, 0, col).toString()), 0, col);
                            // tableTarget.setValueAt(Float.parseFloat(var.getValue(0, 0, col).toString()), 0, col);
                        }

                        break;
                    default:
                        break;
                    }
                    break;
                }

                Utilitaire.adjustTableCells(tableAvg, tableTarget);

                if (cbMethod.getSelectedIndex() == 0) {
                    modelTableTarget.setColumnCount(0);
                    modelTableTarget.setRowCount(0);
                }

            }
        });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 10, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(btCreateTable, gbc);

        JButton btClearTable = new JButton(new AbstractAction("Effacer la table") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                for (int col = 0; col < tableAvg.getColumnCount(); col++) {
                    for (int row = 0; row < tableAvg.getRowCount(); row++) {
                        tableAvg.setValueAt(null, row, col);
                    }
                }
            }
        });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 10, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(btClearTable, gbc);

        JButton btDiscretise = new JButton(new AbstractAction("Discrétisation") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {

                BitSet filter = getFilter();

                if (cbMethod.getSelectedIndex() == 0) {
                    extractData(filter);
                } else {
                    discretWithTarget(filter);
                }

            }
        });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 3;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 10, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        add(btDiscretise, gbc);

        btCopyToParameter = new JButton(new AbstractAction("Copier vers le fichier map") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                copyToParameter();
            }
        });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 4;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 10, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        btCopyToParameter.setEnabled(false);
        add(btCopyToParameter, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 4;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 10, 0, 0);
        gbc.anchor = GridBagConstraints.CENTER;
        JScrollPane sp = new JScrollPane(createTablePanel());
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        add(sp, gbc);

        setPreferredSize(new Dimension(1400, 800));
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(owner);
        setResizable(true);
        setVisible(true);
    }

    private final JPanel createConfigurationPanel() {
        JPanel panel = new JPanel();

        panel.setBorder(BorderFactory.createTitledBorder("Paramétrage:"));

        final JButton btCSV = new JButton(new AbstractAction("Ouvrir CSV") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                final JFileChooser fc = new JFileChooser(Preference.getPreference(Preference.KEY_LOG));
                fc.setMultiSelectionEnabled(false);
                fc.setFileSelectionMode(JFileChooser.OPEN_DIALOG);
                fc.setFileFilter(new FileFilter() {

                    @Override
                    public String getDescription() {
                        return "Fichier CSV (*.csv)";
                    }

                    @Override
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        return f.getName().toLowerCase().endsWith("csv");
                    }
                });
                final int reponse = fc.showOpenDialog(null);

                List<String> xAxis = null;
                List<String> yAxis = null;
                List<String> zValues = null;

                if (reponse == JFileChooser.APPROVE_OPTION) {
                    try (BufferedReader br = new BufferedReader(new FileReader(fc.getSelectedFile()))) {

                        String line;
                        String[] splitLine;

                        int cnt = 0;

                        xAxis = new ArrayList<>();
                        yAxis = new ArrayList<>();
                        zValues = new ArrayList<>();

                        while ((line = br.readLine()) != null) {

                            line = line.replace(",", ".");

                            splitLine = line.split(";");

                            switch (cnt) {
                            case 0: // Axe Y
                                for (String s : splitLine) {
                                    yAxis.add(s);
                                }
                                break;
                            case 1: // Axe X
                                for (String s : splitLine) {
                                    xAxis.add(s);
                                }
                                break;
                            default: // Valeurs Z
                                for (String s : splitLine) {
                                    zValues.add(s);
                                }
                                break;
                            }

                            cnt++;
                        }

                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    modelTableAvg.setColumnCount(xAxis.size() + 1);
                    modelTableAvg.setRowCount(yAxis.size() + 1);

                    modelTableTarget.setColumnCount(xAxis.size() + 1);
                    modelTableTarget.setRowCount(yAxis.size() + 1);

                    // tableAvg.setValueAt("Y \\ X", 0, 0);
                    // tableTarget.setValueAt("Y \\ X", 0, 0);

                    for (int col = 0; col < xAxis.size(); col++) {
                        tableAvg.setValueAt(Float.parseFloat(xAxis.get(col)), 0, col + 1);
                        tableTarget.setValueAt(Float.parseFloat(xAxis.get(col)), 0, col + 1);
                    }
                    for (int row = 0; row < yAxis.size(); row++) {
                        tableAvg.setValueAt(Float.parseFloat(yAxis.get(row)), row + 1, 0);
                        tableTarget.setValueAt(Float.parseFloat(yAxis.get(row)), row + 1, 0);
                    }

                    for (int zIdx = 0; zIdx < zValues.size(); zIdx++) {
                        tableAvg.setValueAt(Float.parseFloat(zValues.get(zIdx)), (zIdx / xAxis.size()) + 1, (zIdx % xAxis.size()) + 1);
                    }

                    Utilitaire.adjustTableCells(tableAvg, tableTarget);

                    if (cbMethod.getSelectedIndex() == 0) {
                        modelTableTarget.setColumnCount(0);
                        modelTableTarget.setRowCount(0);
                    }
                }

            }
        });

        paramCalib = new JTextField(20);

        JPanel manualConf = createManualConfig();

        final GridBagConstraints gbc = new GridBagConstraints();
        panel.setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Méthode:"), gbc);

        cbMethod = new JComboBox<String>(new String[] { "Extraction des points", "Discretisation via une cible" });
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(cbMethod, gbc);

        cbMethod.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                switch (cbMethod.getSelectedIndex()) {
                case 0:
                    labelVariable.setText(varToMap);
                    cbSigne.setEnabled(false);

                    modelTableTarget.setColumnCount(0);
                    modelTableTarget.setRowCount(0);

                    break;
                case 1:
                    labelVariable.setText(varToTarget);
                    cbSigne.setEnabled(true);

                    if (modelTableAvg.getColumnCount() * modelTableAvg.getRowCount() == 0) {
                        return;
                    }

                    modelTableTarget.setColumnCount(modelTableAvg.getColumnCount());
                    modelTableTarget.setRowCount(modelTableAvg.getRowCount());

                    // tableTarget.setValueAt("Y \\ X", 0, 0);

                    for (int col = 1; col < modelTableAvg.getColumnCount(); col++) {
                        tableTarget.setValueAt(tableAvg.getValueAt(0, col), 0, col);
                    }

                    for (int row = 1; row < modelTableAvg.getRowCount(); row++) {
                        tableTarget.setValueAt(tableAvg.getValueAt(row, 0), row, 0);
                    }

                    Utilitaire.adjustTableCells(tableAvg, tableTarget);

                    break;
                }

            }
        });

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Définition de la table:"), gbc);

        cbBrkPtType = new JComboBox<String>(new String[] { "Manuellement", "Fichier CSV", "Depuis un fichier Map" });
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(cbBrkPtType, gbc);

        cbBrkPtType.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {

                switch (cbBrkPtType.getSelectedItem().toString()) {
                case "Manuellement":
                    manualConf.setVisible(true);
                    btCSV.setVisible(false);
                    paramCalib.setVisible(false);
                    paramCalib.setText(null);
                    btCopyToParameter.setEnabled(false);
                    break;
                case "Fichier CSV":
                    manualConf.setVisible(false);
                    nbXbrkPt.setText(null);
                    nbYbrkPt.setText(null);
                    btCSV.setVisible(true);
                    paramCalib.setVisible(false);
                    paramCalib.setText(null);
                    btCopyToParameter.setEnabled(false);
                    break;
                case "Depuis un fichier Map":
                    manualConf.setVisible(false);
                    nbXbrkPt.setText(null);
                    nbYbrkPt.setText(null);
                    btCSV.setVisible(false);
                    paramCalib.setVisible(true);
                    btCopyToParameter.setEnabled(true);
                    break;
                }

                // Permet de ne pas garder un espace vide entre les différents composant
                panel.revalidate();
                panel.repaint();
                //

            }
        });

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        btCSV.setVisible(false);
        panel.add(btCSV, gbc);

        paramCalib.setVisible(false);
        panel.add(paramCalib, gbc);

        panel.add(manualConf, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Variables d'entrée:"), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Axe X:", SwingConstants.RIGHT), gbc);

        xLabel = new JTextField(20);
        // xLabel.setMinimumSize(new Dimension(150, 20));
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(xLabel, gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Axe Y:", SwingConstants.RIGHT), gbc);

        yLabel = new JTextField(20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(yLabel, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        labelVariable = new JLabel(varToMap);
        panel.add(labelVariable, gbc);

        outputVariable = new JTextField(20);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.5;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(outputVariable, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("<html>Signe pour la correction:<br>Correctif = variable/cible"), gbc);

        cbSigne = new JComboBox<>(new String[] { "Multiplication", "Division" });
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        cbSigne.setEnabled(false);
        panel.add(cbSigne, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 8;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Contraintes:"), gbc);

        DefaultTableModel modelContraintes = new DefaultTableModel(new String[] { "Variable", "Signe", "Valeur", "Actif" }, 5) {
            private static final long serialVersionUID = 1L;

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                switch (columnIndex) {
                case 0:
                    return Measure.class;
                case 1:
                    return String.class;
                case 2:
                    return Float.class;
                case 3:
                    return Boolean.class;
                default:
                    return Object.class;
                }
            }

        };

        tableContraintes = new JTable(modelContraintes);
        // tableContraintes.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableContraintes.setCellSelectionEnabled(true);
        tableContraintes.setPreferredScrollableViewportSize(new Dimension(200, 200));
        String[] operators = new String[] { null, "=", "!=", "<", "<=", ">", ">=" };
        JComboBox<String> comboOperator = new JComboBox<>(operators);
        tableContraintes.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(comboOperator));

        for (int i = 0; i < tableContraintes.getColumnCount(); i++) {
            int width = 50;
            switch (i) {
            case 0:
                width = 140;
                break;
            case 1:
                width = 40;
                break;
            case 2:
                width = 60;
                break;
            case 3:
                width = 40;
                break;
            }

            tableContraintes.getColumnModel().getColumn(i).setPreferredWidth(width);
        }

        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 0;
        gbc.gridy = 9;
        gbc.gridwidth = 2;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        JScrollPane spContraintes = new JScrollPane(tableContraintes);
        panel.add(spContraintes, gbc);

        return panel;
    }

    private final JPanel createTablePanel() {
        JPanel panelTable = new JPanel();

        final GridBagConstraints gbc = new GridBagConstraints();
        panelTable.setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panelTable.add(new JLabel("Resultante:"), gbc);

        modelTableAvg = new DefaultTableModel(0, 0) {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                // TODO Auto-generated method stub
                return row + column != 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Float.class;
            }
        };
        tableAvg = new JTable(modelTableAvg);
        tableAvg.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableAvg.setTableHeader(null);
        tableAvg.setCellSelectionEnabled(true);
        new CopyPasteAdapter(tableAvg);

        tableAvg.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                treatKeyBoardInput(tableAvg, e);
            }

        });

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panelTable.add(tableAvg, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 0;
        gbc.insets = new Insets(10, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panelTable.add(new JLabel("Cible:"), gbc);

        modelTableTarget = new DefaultTableModel(0, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int row, int column) {
                // TODO Auto-generated method stub
                return row + column != 0;
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return Float.class;
            }
        };

        tableTarget = new JTable(modelTableTarget);
        tableTarget.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        tableTarget.setTableHeader(null);
        tableTarget.setCellSelectionEnabled(true);
        new CopyPasteAdapter(tableTarget);

        tableTarget.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {
                treatKeyBoardInput(tableTarget, e);
            }

        });

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 1;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panelTable.add(tableTarget, gbc);

        return panelTable;
    }

    private final void treatKeyBoardInput(JTable table, KeyEvent e) {
        if (table.getCellEditor() != null) {
            String res = null;
            double val;

            int[] cols = table.getSelectedColumns();
            int[] rows = table.getSelectedRows();

            switch (e.getKeyChar()) {
            case '=':
                table.getCellEditor().stopCellEditing(); // On arrête l'édition suite au caractère tapé pour keyTyped
                res = JOptionPane.showInputDialog(DialDiscretisation.this, "Valeur :");
                if (res == null || res.isEmpty()) {
                    return;
                }
                val = Utilitaire.getNumberObject(res).doubleValue();

                for (int col : cols) {
                    for (int row : rows) {
                        table.setValueAt(val, row, col);
                        table.editCellAt(row, col);
                    }
                }
                table.getCellEditor().stopCellEditing();
                break;
            case '\u007F':
                table.getCellEditor().stopCellEditing(); // On arrête l'édition suite au caractère tapé pour keyTyped
                for (int col : cols) {
                    for (int row : rows) {
                        table.setValueAt(null, row, col);
                        table.editCellAt(row, col);
                    }
                }
                table.getCellEditor().stopCellEditing();
                break;
            }
        }
    }

    private final JPanel createManualConfig() {
        JPanel panel = new JPanel();

        final GridBagConstraints gbc = new GridBagConstraints();
        panel.setLayout(new GridBagLayout());

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Point d'appui en X (nombre ou min:interval:max):"), gbc);

        nbXbrkPt = new JTextField(10);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(nbXbrkPt, gbc);

        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(new JLabel("Point d'appui en Y (nombre ou min:interval:max):"), gbc);

        nbYbrkPt = new JTextField(10);
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        panel.add(nbYbrkPt, gbc);

        return panel;
    }

    private final void extractData(BitSet filter) {

        LinkedHashMap<BrkPt, WeightedValue> collectedValues = new LinkedHashMap<BrkPt, WeightedValue>();
        BrkPt brkPt = null;

        for (int row = 1; row < tableAvg.getRowCount(); row++) {
            for (int col = 1; col < tableAvg.getColumnCount(); col++) {

                brkPt = new BrkPt(Float.parseFloat(tableAvg.getValueAt(0, col).toString()), Float.parseFloat(tableAvg.getValueAt(row, 0).toString()));

                // Ecart point support en X
                if (col > 1 && col < tableAvg.getColumnCount() - 1) {
                    float prevX = Float.parseFloat(tableAvg.getValueAt(0, col - 1).toString());
                    float nextX = Float.parseFloat(tableAvg.getValueAt(0, col + 1).toString());

                    brkPt.setPrevXDelta((brkPt.getX() - prevX) / 2);
                    brkPt.setNextXDelta((nextX - brkPt.getX()) / 2);
                } else if (col == 1) {
                    float nextX = Float.parseFloat(tableAvg.getValueAt(0, col + 1).toString());

                    brkPt.setPrevXDelta(0);
                    brkPt.setNextXDelta((nextX - brkPt.getX()) / 2);
                } else if (col == tableAvg.getColumnCount() - 1) {
                    float prevX = Float.parseFloat(tableAvg.getValueAt(0, col - 1).toString());

                    brkPt.setPrevXDelta((brkPt.getX() - prevX) / 2);
                    brkPt.setNextXDelta(0);
                }
                //

                // Ecart point support en Y
                if (row > 1 && row < tableAvg.getRowCount() - 1) {
                    float prevY = Float.parseFloat(tableAvg.getValueAt(row - 1, 0).toString());
                    float nextY = Float.parseFloat(tableAvg.getValueAt(row + 1, 0).toString());

                    brkPt.setPrevYDelta((brkPt.getY() - prevY) / 2);
                    brkPt.setNextYDelta((nextY - brkPt.getY()) / 2);
                } else if (row == 1) {
                    float nextY = Float.parseFloat(tableAvg.getValueAt(row + 1, 0).toString());

                    brkPt.setPrevYDelta(0);
                    brkPt.setNextYDelta((nextY - brkPt.getY()) / 2);
                } else if (row == tableAvg.getRowCount() - 1) {
                    float prevY = Float.parseFloat(tableAvg.getValueAt(row - 1, 0).toString());

                    brkPt.setPrevYDelta((brkPt.getY() - prevY) / 2);
                    brkPt.setNextYDelta(0);
                }
                //

                collectedValues.put(brkPt, new WeightedValue());
            }
        }

        Log log = ((Ihm) this.getOwner()).getLog();

        Measure xMeasure = log.getMeasure(xLabel.getText());
        Measure yMeasure = log.getMeasure(yLabel.getText());
        Measure zMeasure = log.getMeasure(outputVariable.getText());

        Set<BrkPt> brkPts = collectedValues.keySet();

        for (int i = 0; i < xMeasure.getDataLength(); i++) {

            if (filter.get(i)) {
                double xVal = xMeasure.get(i);
                double yVal = yMeasure.get(i);
                double zVal = zMeasure.get(i);

                for (BrkPt _brkPt : brkPts) {

                    if (_brkPt.checkThreshold(xVal, yVal)) {
                        float weight = _brkPt.getWeightFactor(xVal, yVal);
                        collectedValues.get(_brkPt).add(weight, (float) zVal);
                        break;
                    }
                }
            }

        }

        // Fill the table
        for (int row = 1; row < tableAvg.getRowCount(); row++) {
            for (int col = 1; col < tableAvg.getColumnCount(); col++) {
                WeightedValue value = collectedValues.get(new BrkPt(Float.parseFloat(tableAvg.getValueAt(0, col).toString()),
                        Float.parseFloat(tableAvg.getValueAt(row, 0).toString())));

                float meanValue = value.getMeanValue();
                if (!Float.isNaN(meanValue)) {
                    tableAvg.setValueAt(meanValue, row, col);
                }
            }
        }

        Utilitaire.adjustTableCells(tableAvg);
        // **************

    }

    private final class BrkPt {

        private double x;
        private double y;
        private int hashCode;

        private double prevXDelta;
        private double nextXDelta;
        private double prevYDelta;
        private double nextYDelta;

        public BrkPt(double xBrkPt, double yBrkPt) {
            this.x = xBrkPt;
            this.y = yBrkPt;
            this.hashCode = Objects.hash(x, y);
        }

        @Override
        public boolean equals(Object obj) {
            BrkPt brkPt = (BrkPt) obj;
            return this.x == brkPt.x && this.y == brkPt.y;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public boolean checkThreshold(double x, double y) {
            return (x > this.x - prevXDelta && x < this.x + nextXDelta) && (y > this.y - prevYDelta && y < this.y + nextYDelta);
        }

        public float getWeightFactor(double x, double y) {

            float xFactor = 0;
            double xDiff = this.x - x;

            if (xDiff > 0) {
                xFactor = (float) (1.0f - (xDiff / prevXDelta));
            } else if (xDiff < 0) {
                xFactor = (float) (1.0f - (-xDiff / nextXDelta));
            } else {
                xFactor = 1;
            }

            float yFactor = 0;
            double yDiff = this.y - y;

            if (yDiff > 0) {
                yFactor = (float) (1.0f - (yDiff / prevYDelta));
            } else if (yDiff < 0) {
                yFactor = (float) (1.0f - (-yDiff / nextYDelta));
            } else {
                yFactor = 1;
            }

            return xFactor * yFactor;
        }

        public void setPrevXDelta(double prevXDelta) {
            this.prevXDelta = prevXDelta;
        }

        public void setNextXDelta(double nextXDelta) {
            this.nextXDelta = nextXDelta;
        }

        public void setPrevYDelta(double prevYDelta) {
            this.prevYDelta = prevYDelta;
        }

        public void setNextYDelta(double nextYDelta) {
            this.nextYDelta = nextYDelta;
        }
    }

    private final class WeightedValue {

        private List<Float> nbValue;
        private List<Float> values;

        public WeightedValue() {
            this.nbValue = new ArrayList<Float>();
            this.values = new ArrayList<Float>();
        }

        public final void add(float weight, float value) {
            this.nbValue.add(weight);
            this.values.add(value * weight);
        }

        public final float getMeanValue() {
            float nbValue = 0;
            float sumValue = 0;

            for (int i = 0; i < this.values.size(); i++) {
                nbValue += this.nbValue.get(i);
                sumValue += this.values.get(i);
            }

            return nbValue > 0 ? sumValue / nbValue : Float.NaN;
        }

        @SuppressWarnings("unused")
        public final double getStdDev() {
            double standardDeviation = 0;
            float nbValue = 0;
            float sumValue = 0;

            if (this.values.size() > 0) {
                for (int i = 0; i < this.values.size(); i++) {
                    nbValue += this.nbValue.get(i);
                    sumValue += this.values.get(i);
                }

                float mean = sumValue / nbValue;

                for (int i = 0; i < this.values.size(); i++) {
                    float rawValue = this.values.get(i) / this.nbValue.get(i);
                    standardDeviation = standardDeviation + Math.pow((rawValue - mean) * this.nbValue.get(i), 2);
                }
                return Math.sqrt(standardDeviation / this.values.size());
            }
            return Float.NaN;
        }
    }

    private final void copyToParameter() {

        Ihm ihm = (Ihm) DialDiscretisation.this.getOwner();
        Variable var = ihm.getMapView().findSelectedCal().getVariable(paramCalib.getText());

        for (int row = 1; row < tableAvg.getRowCount(); row++) {
            for (int col = 1; col < tableAvg.getColumnCount(); col++) {

                Object value = tableAvg.getValueAt(row, col);
                if (value != null && !value.toString().isEmpty()) {
                    var.setValue(true, value, row, col);
                    // var.saveNewValue(row, col, value);
                }
            }
        }

        ihm.getMapView().refreshVariable(var);
        ihm.refresh(var);

    }

    private BitSet getFilter() {
        Log log = ((Ihm) this.getOwner()).getLog();
        BitSet filter = new BitSet(log.getTime().getDataLength());

        int cntContraintes = 0;
        List<String> literalExpressions = new ArrayList<String>(5);
        String exp;
        List<String> var = new ArrayList<String>(5);

        for (int i = 0; i < tableContraintes.getRowCount(); i++) {

            Object actif = tableContraintes.getValueAt(i, 3);
            if (actif != null && ((boolean) actif)) {

                Object variable = tableContraintes.getValueAt(i, 0);
                Object signe = tableContraintes.getValueAt(i, 1);
                Object valeur = tableContraintes.getValueAt(i, 2);

                if (variable != null && signe != null && valeur != null) {
                    var.add(variable.toString());
                    exp = variable.toString() + signe.toString() + valeur.toString();
                    literalExpressions.add(exp);
                    cntContraintes++;
                }

            }
        }

        if (cntContraintes > 0) {
            Argument[] args = new Argument[cntContraintes];

            int charDec = 102;

            String literalExpression = "";

            for (int j = 0; j < cntContraintes; j++) {
                args[j] = new Argument(String.valueOf((char) charDec++), Double.NaN);
                if (j != cntContraintes - 1) {
                    literalExpression = literalExpression + literalExpressions.get(j) + " && ";
                } else {
                    literalExpression = literalExpression + literalExpressions.get(j);
                }
                literalExpression = literalExpression.replace(var.get(j).toString(), args[j].getArgumentName());
            }

            Expression conditionExpression = new Expression(literalExpression, args);

            Measure[] measures = new Measure[conditionExpression.getArgumentsNumber()];
            Argument arg;

            for (int j = 0; j < conditionExpression.getArgumentsNumber(); j++) {
                measures[j] = log.getMeasure(var.get(j));
            }

            for (int i = 0; i < log.getTime().getDataLength(); i++) {
                for (int j = 0; j < conditionExpression.getArgumentsNumber(); j++) {
                    arg = conditionExpression.getArgument(j);
                    if (measures[j].getDataLength() == 0) {
                        break;
                    }
                    arg.setArgumentValue(measures[j].get(i));
                }
                double res = conditionExpression.calculate();
                if (res == 1) {
                    filter.set(i);
                }
            }
        } else {
            filter.flip(0, filter.size() - 1);
        }

        return filter;
    }

    private final void discretWithTarget(BitSet filter) {

        LinkedHashMap<BrkPt, WeightedValue> collectedValues = new LinkedHashMap<BrkPt, WeightedValue>();
        BrkPt brkPt = null;

        for (int row = 1; row < tableTarget.getRowCount(); row++) {
            for (int col = 1; col < tableTarget.getColumnCount(); col++) {

                brkPt = new BrkPt(Float.parseFloat(tableTarget.getValueAt(0, col).toString()),
                        Float.parseFloat(tableTarget.getValueAt(row, 0).toString()));

                // Ecart point support en X
                if (col > 1 && col < tableTarget.getColumnCount() - 1) {
                    float prevX = Float.parseFloat(tableTarget.getValueAt(0, col - 1).toString());
                    float nextX = Float.parseFloat(tableTarget.getValueAt(0, col + 1).toString());

                    brkPt.setPrevXDelta((brkPt.getX() - prevX) / 2);
                    brkPt.setNextXDelta((nextX - brkPt.getX()) / 2);
                } else if (col == 1) {
                    float nextX = Float.parseFloat(tableTarget.getValueAt(0, col + 1).toString());

                    brkPt.setPrevXDelta(0);
                    brkPt.setNextXDelta((nextX - brkPt.getX()) / 2);
                } else if (col == tableTarget.getColumnCount() - 1) {
                    float prevX = Float.parseFloat(tableTarget.getValueAt(0, col - 1).toString());

                    brkPt.setPrevXDelta((brkPt.getX() - prevX) / 2);
                    brkPt.setNextXDelta(0);
                }
                //

                // Ecart point support en Y
                if (row > 1 && row < tableTarget.getRowCount() - 1) {
                    float prevY = Float.parseFloat(tableTarget.getValueAt(row - 1, 0).toString());
                    float nextY = Float.parseFloat(tableTarget.getValueAt(row + 1, 0).toString());

                    brkPt.setPrevYDelta((brkPt.getY() - prevY) / 2);
                    brkPt.setNextYDelta((nextY - brkPt.getY()) / 2);
                } else if (row == 1) {
                    float nextY = Float.parseFloat(tableTarget.getValueAt(row + 1, 0).toString());

                    brkPt.setPrevYDelta(0);
                    brkPt.setNextYDelta((nextY - brkPt.getY()) / 2);
                } else if (row == tableTarget.getRowCount() - 1) {
                    float prevY = Float.parseFloat(tableTarget.getValueAt(row - 1, 0).toString());

                    brkPt.setPrevYDelta((brkPt.getY() - prevY) / 2);
                    brkPt.setNextYDelta(0);
                }
                //

                collectedValues.put(brkPt, new WeightedValue());
            }
        }

        Log log = ((Ihm) this.getOwner()).getLog();

        Measure xMeasure = log.getMeasure(xLabel.getText());
        Measure yMeasure = log.getMeasure(yLabel.getText());
        Measure targetMeasure = log.getMeasure(outputVariable.getText());

        Set<BrkPt> brkPts = collectedValues.keySet();

        for (int i = 0; i < xMeasure.getDataLength(); i++) {

            if (filter.get(i)) {
                double xVal = xMeasure.get(i);
                double yVal = yMeasure.get(i);
                double zVal = targetMeasure.get(i);

                for (BrkPt _brkPt : brkPts) {

                    if (_brkPt.checkThreshold(xVal, yVal)) {
                        float weight = _brkPt.getWeightFactor(xVal, yVal);
                        collectedValues.get(_brkPt).add(weight, (float) zVal);
                        break;
                    }
                }
            }

        }

        WeightedValue value;
        Object oTarget;
        Object oResult;

        // Fill the table
        for (int row = 1; row < tableAvg.getRowCount(); row++) {
            for (int col = 1; col < tableAvg.getColumnCount(); col++) {
                value = collectedValues.get(new BrkPt(Float.parseFloat(tableAvg.getValueAt(0, col).toString()),
                        Float.parseFloat(tableAvg.getValueAt(row, 0).toString())));

                oTarget = tableTarget.getValueAt(row, col);
                oResult = tableAvg.getValueAt(row, col);

                if (oTarget != null && oResult != null) {
                    float meanTargetValue = value.getMeanValue();
                    float targetValue = (float) oTarget;
                    float resultValue = (float) oResult;

                    if (cbSigne.getSelectedIndex() == 0) {
                        resultValue *= (meanTargetValue / targetValue);
                    } else {
                        resultValue /= (meanTargetValue / targetValue);
                    }

                    if (!Float.isNaN(resultValue)) {
                        tableAvg.setValueAt(resultValue, row, col);
                    }
                }

            }
        }

        Utilitaire.adjustTableCells(tableAvg);
        // **************

    }

}

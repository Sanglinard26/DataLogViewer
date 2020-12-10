/*
 * Creation : 17 nov. 2020
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;

import log.Log;
import log.Measure;

public final class TableCondition extends JTable {

    private static final long serialVersionUID = 1L;

    private JComboBox<String> cbMeasures;

    public TableCondition() {
        super(new ConditionModel());
        cbMeasures = new JComboBox<String>();
        cbMeasures.addItem("");
        setDefaultRenderer(Color.class, new ColorRenderer(true));
        setDefaultEditor(Color.class, new ColorEditor());
        setDropMode(DropMode.USE_SELECTION);

        JPopupMenu menu = new JPopupMenu();
        menu.add(new JMenuItem(new AbstractAction("Editer") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (getSelectedRow() > -1) {
                    Condition condition = (Condition) getValueAt(getSelectedRow(), 1);
                    new InputCondition(TableCondition.this, condition);
                }

            }
        }));

        setComponentPopupMenu(menu);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        Point p = e.getPoint();
        int rowIndex = rowAtPoint(p);
        int colIndex = columnAtPoint(p);

        if (colIndex == 1) {
            return ((Condition) getValueAt(rowIndex, colIndex)).getExpression();
        }
        return null;
    }

    public final void updateLog(Log log) {
        for (Measure measure : log.getMeasures()) {
            cbMeasures.addItem(measure.getName());
        }
    }

    public final int getActiveCondition() {
        for (int i = 0; i < getRowCount(); i++) {
            if ((boolean) getValueAt(i, 0)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public ConditionModel getModel() {
        return (ConditionModel) this.dataModel;
    }

}

final class InputCondition extends JDialog {

    private static final long serialVersionUID = 1L;
    private final JTextArea conditionText;

    public InputCondition(final TableCondition table, final Condition condition) {
        super();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(false);
        JPanel panel = new JPanel(new BorderLayout());
        conditionText = new JTextArea(5, 60);
        conditionText.setText(condition.getExpression());
        conditionText.setBorder(BorderFactory.createTitledBorder("Expression :"));
        conditionText.setFont(conditionText.getFont().deriveFont(14f));
        conditionText.setLineWrap(true);
        conditionText.setWrapStyleWord(true);
        conditionText.setTransferHandler(new TransferHandler("measure") {
            private static final long serialVersionUID = 1L;

            @Override
            public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
                StringSelection selection = new StringSelection(conditionText.getSelectedText());
                clip.setContents(selection, selection);

                if (action == TransferHandler.MOVE) {
                    conditionText.setText(null);
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

                conditionText.insert(data, conditionText.getCaretPosition());

                return true;
            }
        });
        panel.add(conditionText, BorderLayout.CENTER);

        panel.add(new JButton(new AbstractAction("Valider") {

            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                condition.setExpression(conditionText.getText());
                table.getModel().fireTableCellUpdated(table.getSelectedRow(), 1);
                dispose();

            }
        }), BorderLayout.SOUTH);

        add(panel);

        addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(WindowEvent e) {
                toFront();
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
            }
        });

        pack();
        setMinimumSize(getPreferredSize());
        setLocationRelativeTo(null);
        setResizable(true);
        setVisible(true);
    }
}

final class ConditionModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private final String[] HEADER = new String[] { "Activer", "Condition", "Couleur" };

    private List<Condition> conditions;

    public ConditionModel() {
        conditions = new ArrayList<Condition>(10);
        for (int i = 0; i < 10; i++) {
            Color hsbColor = Color.getHSBColor(0.1f * i, 1, 1);
            conditions.add(new Condition("", "", new Color(hsbColor.getRed(), hsbColor.getGreen(), hsbColor.getBlue(), 70)));
        }
    }

    @Override
    public String getColumnName(int column) {
        return HEADER[column];
    }

    @Override
    public int getRowCount() {
        return conditions.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {

        switch (columnIndex) {
        case 0:
            return Boolean.class;
        case 1:
            return Object.class;
        case 2:
            return Color.class;
        default:
            return String.class;
        }
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;

    }

    @Override
    public int getColumnCount() {
        return HEADER.length;
    }

    @Override
    public Object getValueAt(int row, int col) {
        switch (col) {
        case 0:
            return conditions.get(row).isActive();
        case 1:
            return conditions.get(row);
        case 2:
            return conditions.get(row).getColor();
        default:
            return null;
        }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {

        Condition condition = conditions.get(rowIndex);

        switch (columnIndex) {
        case 0:
            condition.setActive(!condition.isActive());
            break;
        case 1:
            condition.setName(aValue.toString());
            break;
        case 2:
            condition.setColor((Color) aValue);
            break;
        default:
            break;
        }
        fireTableCellUpdated(rowIndex, columnIndex);

    }

    public final List<Condition> getConditions() {
        return conditions;
    }

    public final void clearList() {
        this.conditions.clear();
        fireTableDataChanged();
    }

}

final class Condition {

    private String name;
    private String expression;
    private boolean active;
    private Color color;

    public Condition(String name, String expression, Color color) {
        this.name = name;
        this.expression = expression;
        this.color = color;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public final boolean isEmpty() {
        return "".equals(expression);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String operateur) {
        this.expression = operateur;
    }

    private String replaceVarName(Map<String, String> variables) {

        String internExpression = this.expression.replaceAll("#", "");

        for (Entry<String, String> entry : variables.entrySet()) {
            internExpression = internExpression.replace(entry.getValue(), entry.getKey().toString());
        }

        return internExpression;
    }

    private Map<String, String> findMeasure() {
        LinkedHashMap<String, String> variables = new LinkedHashMap<String, String>();

        Pattern pattern = Pattern.compile("\\#(.*?)\\#");
        final Matcher regexMatcher = pattern.matcher(this.expression);

        String matchedMeasure;

        int cnt = 1;

        while (regexMatcher.find()) {
            matchedMeasure = regexMatcher.group(1);

            variables.put("a" + cnt++, matchedMeasure);
        }

        return variables;
    }

    public BitSet apply(Log log) {

        BitSet bitCondition = new BitSet(log.getTime().getData().size());

        if ("".equals(expression) || !active) {
            return bitCondition;
        }

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        Map<String, String> variables = findMeasure();
        String renameExpression = replaceVarName(variables);

        List<Measure> measures = new ArrayList<Measure>(variables.size());
        for (String measureName : variables.values()) {
            measures.add(log.getMeasure(measureName));
        }

        try {

            String val;

            for (int i = 0; i < log.getTime().getData().size(); i++) {

                for (int j = 0; j < measures.size(); j++) {

                    val = "a" + (j + 1) + "=" + measures.get(j).getData().get(i);
                    engine.eval(val);
                }

                boolean result = (boolean) engine.eval(renameExpression);
                if (result) {
                    bitCondition.set(i);
                }

            }

        } catch (ScriptException se) {
            JOptionPane.showMessageDialog(null, "Problème de synthaxe !", "Erreur", JOptionPane.ERROR_MESSAGE);
            bitCondition.set(0, bitCondition.size(), false);
        }

        return bitCondition;
    }

}

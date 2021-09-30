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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.TransferHandler;
import javax.swing.table.AbstractTableModel;

public final class TableCondition extends JTable {

    private static final long serialVersionUID = 1L;

    public TableCondition() {
        super(new ConditionModel());
        setDefaultRenderer(Color.class, new ColorRenderer(true));
        setDefaultEditor(Color.class, new ColorEditor());
        setDropMode(DropMode.USE_SELECTION);

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Condition condition = (Condition) getValueAt(getSelectedRow(), 1);
                    new InputCondition(TableCondition.this, condition);
                }
            }

        });
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

    private final JTextArea conditionName;
    private final JTextArea conditionText;

    public InputCondition(final TableCondition table, final Condition condition) {
        super();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setModal(false);
        JPanel panel = new JPanel(new BorderLayout());

        conditionName = new JTextArea(1, 30);
        conditionName.setText(condition.getName());
        conditionName.setBorder(BorderFactory.createTitledBorder("Nom :"));
        panel.add(conditionName, BorderLayout.NORTH);

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
                condition.setName(conditionName.getText());
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
        return columnIndex != 1;

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

    public final void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
        fireTableDataChanged();
    }

    public final void clearList() {
        this.conditions.clear();
        fireTableDataChanged();
    }

}

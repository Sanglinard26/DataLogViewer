/*
 * Creation : 2 nov. 2020
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.MatteBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import calib.Type;
import calib.Variable;
import utils.Interpolation;
import utils.Utilitaire;

public class CalTable extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private RowNumberTable rowTable;
    private BarControl control;
    private JScrollPane scrollPane;
    private Variable selectedVariable;
    private JTableHeader header;
    private JPopupMenu renamePopup;
    private JTextField text;
    private TableColumn column;
    private int rowBrkPt;
    private int columnBrkPt;

    public CalTable(Variable variable) {
        super(new BorderLayout(0, 0));

        control = new BarControl();
        add(control, BorderLayout.NORTH);

        if (variable == null) {
            return;
        }

        this.selectedVariable = variable;

        int nbRow = variable.getDimY();
        int nbCol = variable.getDimX();

        switch (variable.getType()) {

        case COURBE:
            nbRow--;
            break;

        case MAP:
            nbRow--;
            nbCol--;
            break;

        default:

            break;
        }

        table = new JTable(nbRow, nbCol);

        table.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {

                String res = null;
                double val;
                double actualVal;

                int[] cols = table.getSelectedColumns();
                int[] rows = table.getSelectedRows();

                switch (e.getKeyChar()) {
                case '=':
                    res = JOptionPane.showInputDialog(CalTable.this, "Valeur :");
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            table.setValueAt(val, row, col);
                            table.editCellAt(row, col);
                        }
                    }
                    table.getCellEditor().stopCellEditing();
                    break;
                case '+':
                    res = JOptionPane.showInputDialog(CalTable.this, "Ajouter :");
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            actualVal = Utilitaire.getNumberObject(table.getValueAt(row, col)).doubleValue();
                            table.setValueAt(actualVal + val, row, col);
                            table.editCellAt(row, col);
                        }
                    }
                    table.getCellEditor().stopCellEditing();
                    break;
                case '*':
                    res = JOptionPane.showInputDialog(CalTable.this, "Multiplier par :");
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            actualVal = Utilitaire.getNumberObject(table.getValueAt(row, col)).doubleValue();
                            table.setValueAt(actualVal * val, row, col);
                            table.editCellAt(row, col);
                        }
                    }
                    table.getCellEditor().stopCellEditing();
                    break;
                case '/':
                    res = JOptionPane.showInputDialog(CalTable.this, "Diviser par :");
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            actualVal = Utilitaire.getNumberObject(table.getValueAt(row, col)).doubleValue();
                            table.setValueAt(actualVal / val, row, col);
                            table.editCellAt(row, col);
                        }
                    }
                    table.getCellEditor().stopCellEditing();
                    break;
                default:
                    break;
                }
            }

        });

        table.setDefaultEditor(Object.class, new SaturateValueEditor(variable.getMin(), variable.getMax(), variable.getResolution()));

        table.getModel().addTableModelListener(new TableModelListener() {

            @Override
            public void tableChanged(TableModelEvent e) {
                if (e.getType() == TableModelEvent.UPDATE) {

                    int row = e.getFirstRow();
                    int col = e.getColumn();

                    if (row < 0 || col < 0) {
                        return;
                    }

                    int offsetRow = 0;
                    int offsetCol = 0;

                    switch (CalTable.this.selectedVariable.getType()) {

                    case COURBE:
                        offsetRow = 1;
                        break;
                    case MAP:
                        offsetRow = 1;
                        offsetCol = 1;
                        break;
                    default:
                        break;
                    }

                    Object actValue = Utilitaire.getStorageObject(table.getValueAt(row, col));

                    if (actValue != null) {
                        CalTable.this.selectedVariable.saveNewValue(row + offsetRow, col + offsetCol, actValue);
                        // CalTable.this.selectedVariable.notifyObservers();
                    }
                }

            }
        });

        table.setTableHeader(new CustomTableHeader(table));
        header = table.getTableHeader();
        header.setDefaultRenderer(new SimpleHeaderRenderer());
        table.setCellSelectionEnabled(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);

        text = new JTextField();
        text.setBorder(null);
        text.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                changeBreakPoint();
            }
        });

        renamePopup = new JPopupMenu();
        renamePopup.setBorder(new MatteBorder(0, 1, 1, 1, Color.DARK_GRAY));
        renamePopup.add(text);

        scrollPane = new JScrollPane(table);
        rowTable = new RowNumberTable(table);

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setRowHeaderView(rowTable);

        populate(variable);

        adjustCells();

        add(scrollPane, BorderLayout.CENTER);

    }

    public final JTable getTable() {
        return table;
    }

    private void editColumnAt(Point p) {
        columnBrkPt = header.columnAtPoint(p);

        if (columnBrkPt != -1) {
            column = header.getColumnModel().getColumn(columnBrkPt);
            Rectangle columnRectangle = header.getHeaderRect(columnBrkPt);

            text.setText(column.getHeaderValue().toString());
            renamePopup.setPreferredSize(new Dimension(columnRectangle.width, columnRectangle.height - 1));
            renamePopup.show(header, columnRectangle.x, 0);

            text.requestFocusInWindow();
            text.selectAll();
        }
    }

    private void editRowAt(Point p) {
        rowBrkPt = rowTable.rowAtPoint(p);

        if (rowBrkPt != -1) {
            Rectangle rowRectangle = rowTable.getCellRect(rowBrkPt, 0, false);
            text.setText(rowTable.getValueAt(rowBrkPt, 0).toString());
            renamePopup.setPreferredSize(new Dimension(rowRectangle.width, rowRectangle.height - 1));
            renamePopup.show(rowTable, rowRectangle.x, rowRectangle.y);

            text.requestFocusInWindow();
            text.selectAll();
        }
    }

    private void changeBreakPoint() {

        int offsetRow = 0;
        int offsetCol = 0;

        if (rowTable.getRowCount() > 1) {
            offsetRow = 1;
            offsetCol = 1;
        }

        if (column != null) {
            CalTable.this.selectedVariable.saveNewValue(0, columnBrkPt + offsetCol, text.getText());
            column.setHeaderValue(text.getText());
            header.repaint();
        } else {
            if (rowBrkPt != -1) {
                CalTable.this.selectedVariable.saveNewValue(rowBrkPt + offsetRow, 0, text.getText());
                rowTable.setValueAt(text.getText(), rowBrkPt, 0);
            }
        }
        renamePopup.setVisible(false);
        calcZvalue();
    }

    public final void setValue(double newValue, int row, int col) {
        table.setValueAt(newValue, row, col);
    }

    public final void calcZvalue() {

        final double[][] datasTable = getTableDoubleValue();
        final double[][] datasRef = selectedVariable.toDouble2D(false);
        double result = Double.NaN;
        BigDecimal bd;
        BigDecimal bdOrigine;
        BigDecimal bdOldValTable;

        for (int row = 0; row < table.getRowCount(); row++) {

            if (selectedVariable.getDimY() > 2) {
                for (int x = 1; x < selectedVariable.getDimX(); x++) {
                    result = Interpolation.interpLinear2D(datasRef, datasTable[0][x], datasTable[row + 1][0]);

                    bd = BigDecimal.valueOf(result);
                    bdOrigine = BigDecimal.valueOf(datasRef[row + 1][x]);
                    bdOldValTable = BigDecimal.valueOf(datasTable[row + 1][x]);
                    int diff = bd.compareTo(bdOrigine);

                    TableCellRenderer cellRenderer = table.getCellRenderer(row, x - 1);
                    Component c = cellRenderer.getTableCellRendererComponent(table, bdOldValTable, false, false, row, x - 1);

                    if (bd.compareTo(bdOldValTable) != 0) {
                        table.setValueAt(result, row, x - 1);
                    }
                }
            }

            if (selectedVariable.getDimY() == 2) {
                for (int x = 0; x < selectedVariable.getDimX(); x++) {
                    result = Interpolation.interpLinear1D(datasRef, datasTable[0][x]);

                    bd = BigDecimal.valueOf(result);
                    bdOrigine = BigDecimal.valueOf(datasRef[1][x]);
                    bdOldValTable = BigDecimal.valueOf(datasTable[1][x]);
                    int diff = bd.compareTo(bdOrigine);

                    TableCellRenderer cellRenderer = table.getCellRenderer(0, x);
                    Component c = cellRenderer.getTableCellRendererComponent(table, bdOldValTable, false, false, 0, x);

                    if (bd.compareTo(bdOldValTable) != 0) {
                        table.setValueAt(result, 0, x);
                    }
                }
            }
        }
    }

    public final int getComponentHeight() {
        int hBarVisible = scrollPane.getHorizontalScrollBar().isVisible() ? 1 : 0;
        int dataHeight = (table.getRowCount() + 1 + hBarVisible * 1) * (table.getRowHeight() + table.getRowMargin() + 1);
        return dataHeight + control.getPreferredSize().height;
    }

    public class CustomTableHeader extends JTableHeader {

        private static final long serialVersionUID = 1L;

        public CustomTableHeader(JTable table) {
            super(table.getColumnModel());
            table.getColumnModel().getSelectionModel().addListSelectionListener(new ListSelectionListener() {
                @Override
                public void valueChanged(ListSelectionEvent e) {
                    repaint();
                }
            });
        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {
            repaint();
        }

    }

    public class SimpleHeaderRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        public SimpleHeaderRenderer() {
            super();
            setBorder(BorderFactory.createEtchedBorder());
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            setText(value.toString());

            if (table != null) {

                int[] selectedColumns = table.getSelectedColumns();

                Arrays.sort(selectedColumns);
                int idx = Arrays.binarySearch(selectedColumns, column);

                if (idx >= 0) {
                    setFont(getFont().deriveFont(Font.BOLD));
                    setForeground(Color.BLUE);
                } else {
                    setFont(getFont().deriveFont(Font.PLAIN));
                    setForeground(Color.BLACK);
                }
            }

            return this;
        }
    }

    public final void populate(Variable variable) {

        String value;
        Object oValue;

        boolean modifiedVar = variable.isModified();

        Vector<Vector> dataVector = ((DefaultTableModel) table.getModel()).getDataVector();

        if (variable.getDimX() * variable.getDimY() == 1) {
            table.setTableHeader(null);
            scrollPane.setRowHeaderView(null);

            oValue = variable.getValue(modifiedVar, 0, 0);

            value = oValue != null ? oValue.toString() : "";

            // table.setValueAt(value, 0, 0);
            dataVector.get(0).set(0, value);

        } else if (variable.getDimY() == 2) {

            scrollPane.setRowHeaderView(null);

            String xValue;

            for (int col = 0; col < variable.getDimX(); col++) {
                xValue = variable.getValue(modifiedVar, 0, col).toString();
                value = variable.getValue(modifiedVar, 1, col).toString();

                table.getColumnModel().getColumn(col).setHeaderValue(xValue);
                // table.setValueAt(value, 0, col);
                dataVector.get(0).set(col, value);
            }
        } else if (variable.getDimX() * variable.getDimY() == variable.getDimX()) {

            table.setTableHeader(null);
            scrollPane.setRowHeaderView(null);

            for (int col = 0; col < variable.getDimX(); col++) {
                oValue = variable.getValue(modifiedVar, 0, col);

                value = oValue != null ? oValue.toString() : "";

                // table.setValueAt(value, 0, col);
                dataVector.get(0).set(col, value);
            }
        } else {

            if ("Y \\ X".equals(variable.getValue(modifiedVar, 0, 0).toString())) {
                String xValue;
                String yValue;

                for (int row = 1; row < variable.getDimY(); row++) {
                    for (int col = 1; col < variable.getDimX(); col++) {
                        xValue = variable.getValue(modifiedVar, 0, col).toString();
                        yValue = variable.getValue(modifiedVar, row, 0).toString();
                        value = variable.getValue(modifiedVar, row, col).toString();

                        table.getColumnModel().getColumn(col - 1).setHeaderValue(xValue);
                        rowTable.setValueAt(yValue, row - 1, 0);
                        // table.setValueAt(value, row - 1, col - 1);
                        dataVector.get(row - 1).set(col - 1, value);
                    }
                }
            } else {

                table.setTableHeader(null);
                scrollPane.setRowHeaderView(null);

                for (int row = 0; row < variable.getDimY() - 1; row++) {
                    for (int col = 0; col < variable.getDimX() - 1; col++) {

                        oValue = variable.getValue(modifiedVar, row, col);

                        value = oValue != null ? oValue.toString() : "";

                        // table.setValueAt(value, row, col);
                        dataVector.get(row).set(col, value);
                    }
                }
            }

        }
        ((DefaultTableModel) table.getModel()).fireTableDataChanged();
    }

    private final void adjustCells() {

        final TableColumnModel columnModel = table.getColumnModel();
        final int nbCol = columnModel.getColumnCount();
        final int nbRow = table.getRowCount();
        int maxWidth;
        TableCellRenderer cellRenderer;
        Object value;
        Component component;
        TableColumn column;

        for (short col = 0; col < nbCol; col++) {
            maxWidth = 0;
            for (short row = 0; row < nbRow; row++) {
                cellRenderer = table.getCellRenderer(row, col);
                value = table.getValueAt(row, col);
                component = cellRenderer.getTableCellRendererComponent(table, value, false, false, row, col);
                ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);
                maxWidth = Math.max(((JLabel) component).getPreferredSize().width, maxWidth);
                maxWidth = Math.max(columnModel.getColumn(col).getPreferredWidth(), maxWidth);
            }
            column = columnModel.getColumn(col);
            column.setPreferredWidth(maxWidth + 5);
        }

    }

    private class MyMouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent event) {
            if (event.getClickCount() == 2) {
                if (event.getComponent() instanceof CustomTableHeader) {
                    editColumnAt(event.getPoint());
                } else {
                    column = null;
                    editRowAt(event.getPoint());
                }

            }
        }
    }

    private final double[][] getTableDoubleValue() {

        final TableColumnModel columnModel = table.getColumnModel();
        final int nbCol = columnModel.getColumnCount();
        final int nbRow = table.getRowCount();

        if (nbCol * nbRow <= 1) {
            return new double[][] { { Double.parseDouble(table.getValueAt(0, 0).toString()) } };
        }

        double[][] doubleValues = new double[selectedVariable.getDimY()][selectedVariable.getDimX()];

        doubleValues[0][0] = Double.NaN;

        if (selectedVariable.getDimY() > 2) {
            for (int col = 0; col < nbCol; col++) {
                doubleValues[0][col + 1] = Double.parseDouble(columnModel.getColumn(col).getHeaderValue().toString());
            }
            for (int row = 0; row < nbRow; row++) {
                doubleValues[row + 1][0] = Double.parseDouble(rowTable.getValueAt(row, 0).toString());
            }
            for (int row = 0; row < nbRow; row++) {
                for (int col = 0; col < nbCol; col++) {
                    doubleValues[row + 1][col + 1] = Double.parseDouble(table.getValueAt(row, col).toString());
                }
            }
        } else {
            for (int col = 0; col < nbCol; col++) {
                doubleValues[0][col] = Double.parseDouble(columnModel.getColumn(col).getHeaderValue().toString());
                doubleValues[1][col] = Double.parseDouble(table.getValueAt(0, col).toString());
            }
        }

        return doubleValues;

    }

    private final String getTableValue() {
        final TableColumnModel columnModel = table.getColumnModel();
        final int nbCol = columnModel.getColumnCount();
        final int nbRow = table.getRowCount();

        int startCol;
        int startRow;

        Type typeVar = selectedVariable.getType();

        if (typeVar.compareTo(Type.COURBE) == 0) {
            startCol = 0;
            startRow = -1;
        } else if (typeVar.compareTo(Type.MAP) == 0) {
            startCol = -1;
            startRow = -1;
        } else {
            startCol = 0;
            startRow = 0;
        }

        StringBuilder sb = new StringBuilder();

        if (nbCol * nbRow <= 1) {
            sb.append(table.getValueAt(0, 0));
            return sb.toString();
        }

        for (int row = startRow; row < nbRow; row++) {
            for (int col = startCol; col < nbCol; col++) {

                if (row == -1 && col == -1) {
                    sb.append("Y \\ X" + "\t");
                }
                if (row == -1 && col > -1) {
                    sb.append(columnModel.getColumn(col).getHeaderValue() + "\t");
                }
                if (col == -1 && row > -1) {
                    sb.append(rowTable.getValueAt(row, 0) + "\t");
                }
                if (row > -1 && col > -1) {
                    sb.append(table.getValueAt(row, col) + "\t");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private final class BarControl extends JToolBar {

        private static final long serialVersionUID = 1L;

        final String ICON_COPY = "/icon_copy_24.png";
        final String ICON_MATH = "/icon_math_24.png";
        final String ICON_RESET = "/icon_backRef_24.png";

        public BarControl() {
            super();
            setFloatable(false);
            setBorder(BorderFactory.createEtchedBorder());

            JButton btCopy = new JButton(null, new ImageIcon(getClass().getResource(ICON_COPY)));
            btCopy.setToolTipText("Copier dans le presse-papier");
            btCopy.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    if (selectedVariable == null) {
                        JOptionPane.showMessageDialog(null, "Il faut qu'une variable soit sélectionnée !", "Info", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }

                    Clipboard clipboard = getToolkit().getSystemClipboard();
                    StringSelection data = new StringSelection(getTableValue());
                    clipboard.setContents(data, data);
                }
            });
            add(btCopy);

            final JToggleButton btInterpolation = new JToggleButton(null, new ImageIcon(getClass().getResource(ICON_MATH)));
            btInterpolation.setToolTipText("Interpolation");
            btInterpolation.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (table == null) {
                        return;
                    }
                    if (btInterpolation.isSelected()) {
                        header.addMouseListener(new MyMouseListener());
                        rowTable.addMouseListener(new MyMouseListener());
                    } else {
                        for (MouseListener listener : header.getMouseListeners()) {
                            header.removeMouseListener(listener);
                        }

                        for (MouseListener listener : rowTable.getMouseListeners()) {
                            rowTable.removeMouseListener(listener);
                        }
                    }
                }
            });
            add(btInterpolation);

            final JButton btReset = new JButton(new ImageIcon(getClass().getResource(ICON_RESET)));
            btReset.setToolTipText("Retour aux valeurs de base");
            btReset.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (table == null) {
                        return;
                    }
                    selectedVariable.backToRefValue();
                    populate(selectedVariable);

                }
            });
            add(btReset);
        }
    }
}

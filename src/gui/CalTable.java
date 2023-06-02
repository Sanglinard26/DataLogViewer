/*
 * Creation : 2 nov. 2020
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
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
import javax.swing.text.NumberFormatter;

import calib.Type;
import calib.Variable;
import log.Log;
import log.Measure;
import net.ericaro.surfaceplotter.surface.ColorModel;
import utils.CopyPasteAdapter;
import utils.Interpolation;
import utils.Utilitaire;

public final class CalTable extends JPanel {

    private static final long serialVersionUID = 1L;

    private final MapView mapView;
    private JTable table;
    private RowNumberTable rowTable;
    private BarControl control;
    private JScrollPane scrollPane;
    private Variable selectedVariable;
    private final JTableHeader header;
    private JPopupMenu renamePopup;
    private JFormattedTextField text;
    private TableColumn column;
    private int rowBrkPt;
    private int columnBrkPt;
    private int idxCalPage = 0;

    private JToggleButton btTrace;
    private boolean[][] flagInLog;
    private boolean[][] cursorValues;

    float xFraction = Float.NaN;
    float yFraction = Float.NaN;
    int[] coordFraction;

    boolean colorTable = false;

    final String ICON_MARKER = "/icon_marker_12.png";
    private final ImageIcon iconMarker = new ImageIcon(getClass().getResource(ICON_MARKER));

    public CalTable(MapView mapView) {
        super(new BorderLayout(0, 0));

        this.mapView = mapView;

        control = new BarControl();
        add(control, BorderLayout.NORTH);

        DefaultTableModel model = new DefaultTableModel(0, 0) {
            private static final long serialVersionUID = 1L;

            public boolean isCellEditable(int row, int column) {
                return idxCalPage == 0 ? true : false;
            };
        };

        table = new JTable(model);
        new CopyPasteAdapter(table);
        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {

                    final String ICON_RESET = "/icon_backRef_16.png";

                    JPopupMenu menu = new JPopupMenu();
                    JMenuItem menuItem = new JMenuItem(new AbstractAction("Revenir aux valeurs de base sur la zone s\u00e9lectionn\u00e9e",
                            new ImageIcon(getClass().getResource(ICON_RESET))) {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public void actionPerformed(ActionEvent e) {

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

                            int[] cols = table.getSelectedColumns();
                            int[] rows = table.getSelectedRows();

                            Object refValue;

                            for (int col : cols) {
                                for (int row : rows) {
                                    refValue = selectedVariable.getValue(1, row + offsetRow, col + offsetCol);
                                    table.setValueAt(refValue, row, col);
                                }
                            }

                            // populate(selectedVariable); // Utilité à revoir

                        }
                    });
                    menu.add(menuItem);
                    menu.show(e.getComponent(), e.getX(), e.getY());

                }
            }
        });

        table.setDefaultRenderer(Object.class, new MyTableRenderer());

        table.addKeyListener(new KeyAdapter() {

            @Override
            public void keyTyped(KeyEvent e) {

                if (table.getCellEditor() == null) {
                    return;
                }

                String res = null;
                double val;
                double actualVal;

                int[] cols = table.getSelectedColumns();
                int[] rows = table.getSelectedRows();

                switch (e.getKeyChar()) {
                case '=':
                    table.getCellEditor().stopCellEditing(); // On arrête l'édition suite au caractère tapé pour keyTyped
                    res = JOptionPane.showInputDialog(CalTable.this, "Valeur :");
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
                case '+':
                    table.getCellEditor().stopCellEditing(); // On arrête l'édition suite au caractère tapé pour keyTyped
                    res = JOptionPane.showInputDialog(CalTable.this, "Ajouter :");
                    if (res == null || res.isEmpty()) {
                        return;
                    }
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            actualVal = Utilitaire.getNumberObject(table.getValueAt(row, col).toString()).doubleValue();
                            table.setValueAt(actualVal + val, row, col);
                            table.editCellAt(row, col);
                        }
                    }
                    table.getCellEditor().stopCellEditing();
                    break;
                case '*':
                    table.getCellEditor().stopCellEditing(); // On arrête l'édition suite au caractère tapé pour keyTyped
                    res = JOptionPane.showInputDialog(CalTable.this, "Multiplier par :");
                    if (res == null || res.isEmpty()) {
                        return;
                    }
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            actualVal = Utilitaire.getNumberObject(table.getValueAt(row, col).toString()).doubleValue();
                            table.setValueAt(actualVal * val, row, col);
                            table.editCellAt(row, col);
                        }
                    }
                    table.getCellEditor().stopCellEditing();
                    break;
                case '/':
                    table.getCellEditor().stopCellEditing(); // On arrête l'édition suite au caractère tapé pour keyTyped
                    res = JOptionPane.showInputDialog(CalTable.this, "Diviser par :");
                    if (res == null || res.isEmpty()) {
                        return;
                    }
                    val = Utilitaire.getNumberObject(res).doubleValue();

                    for (int col : cols) {
                        for (int row : rows) {
                            actualVal = Utilitaire.getNumberObject(table.getValueAt(row, col).toString()).doubleValue();
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
                    }

                    int[] cols = table.getSelectedColumns();
                    int[] rows = table.getSelectedRows();

                    if ((cols.length > 0 && rows.length > 0) && row == rows[rows.length - 1] && col == cols[cols.length - 1]) {
                        mapView.fireMapCalChange();
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

        text = new JFormattedTextField(new NumberFormatter());
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

        add(scrollPane, BorderLayout.CENTER);

    }

    public final JTable getTable() {
        return table;
    }

    public final int getIdxCalPage() {
        return idxCalPage;
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

    private final void changeBreakPoint() {

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
        final double[][] datasRef = selectedVariable.toDouble2D(1);
        double result = Double.NaN;

        for (int row = 0; row < table.getRowCount(); row++) {

            if (selectedVariable.getDimY() > 2) {
                for (int x = 1; x < selectedVariable.getDimX(); x++) {
                    result = Interpolation.interpLinear2D(datasRef, datasTable[0][x], datasTable[row + 1][0]);
                    table.setValueAt(result, row, x - 1);
                }
            }

            if (selectedVariable.getDimY() == 2) {
                for (int x = 0; x < selectedVariable.getDimX(); x++) {
                    result = Interpolation.interpLinear1D(datasRef, datasTable[0][x]);
                    table.setValueAt(result, 0, x);
                }
            }
        }
    }

    public final int getComponentHeight() {
        int hBarVisible = scrollPane.getHorizontalScrollBar().isVisible() ? 1 : 0;
        int dataHeight = (table.getRowCount() + 1 + hBarVisible * 1) * (table.getRowHeight() + table.getRowMargin() + 1);
        return dataHeight + control.getPreferredSize().height;
    }

    public final class CustomTableHeader extends JTableHeader {

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

    public final class SimpleHeaderRenderer extends JLabel implements TableCellRenderer {

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

    @SuppressWarnings("unchecked")
    public final void populate(Variable variable) {

        String value;
        Object oValue;

        this.selectedVariable = variable;

        if (this.selectedVariable != null) {
            int nbRow = variable.getDimY();
            int nbCol = variable.getDimX();

            table.setDefaultEditor(Object.class, new SaturateValueEditor(variable.getMin(), variable.getMax()));

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

            int nbRowTable = ((DefaultTableModel) table.getModel()).getRowCount();
            int nbColTable = ((DefaultTableModel) table.getModel()).getColumnCount();

            if (nbRowTable != nbRow) {
                ((DefaultTableModel) table.getModel()).setRowCount(nbRow);
                ((DefaultTableModel) rowTable.getModel()).setRowCount(nbRow);
            }

            if (nbColTable != nbCol) {
                ((DefaultTableModel) table.getModel()).setColumnCount(nbCol);
            }
        } else {
            ((DefaultTableModel) table.getModel()).setRowCount(0);
            ((DefaultTableModel) rowTable.getModel()).setRowCount(0);
            ((DefaultTableModel) table.getModel()).setColumnCount(0);
            return;
        }

        @SuppressWarnings({ "rawtypes" })
        Vector<Vector> dataVector = ((DefaultTableModel) table.getModel()).getDataVector();

        if (variable.getDimX() * variable.getDimY() == 1) {

            table.setTableHeader(null);
            scrollPane.setRowHeaderView(null);
            scrollPane.getColumnHeader().setVisible(false);

            oValue = variable.getValue(idxCalPage, 0, 0);

            value = oValue != null ? oValue.toString() : "";

            dataVector.get(0).set(0, value);

        } else if (variable.getDimY() == 2) {

            table.setTableHeader(header);
            scrollPane.setRowHeaderView(null);
            scrollPane.getColumnHeader().setVisible(true);

            String xValue;

            for (int col = 0; col < variable.getDimX(); col++) {

                if (idxCalPage == 2) {
                    xValue = variable.getValue(0, 0, col).toString();
                } else {
                    xValue = variable.getValue(idxCalPage, 0, col).toString();
                }
                value = variable.getValue(idxCalPage, 1, col).toString();

                table.getColumnModel().getColumn(col).setHeaderValue(xValue);
                dataVector.get(0).set(col, value);
            }
        } else if (variable.getDimX() * variable.getDimY() == variable.getDimX()) {

            table.setTableHeader(null);
            scrollPane.setRowHeaderView(null);
            scrollPane.getColumnHeader().setVisible(false);

            for (int col = 0; col < variable.getDimX(); col++) {
                oValue = variable.getValue(idxCalPage, 0, col);

                value = oValue != null ? oValue.toString() : "";

                dataVector.get(0).set(col, value);
            }
        } else {

            if ("Y \\ X".equals(variable.getValue(idxCalPage, 0, 0).toString())) {
                String xValue;
                String yValue;

                table.setTableHeader(header);
                scrollPane.setRowHeaderView(rowTable);
                scrollPane.getColumnHeader().setVisible(true);

                for (int row = 1; row < variable.getDimY(); row++) {
                    for (int col = 1; col < variable.getDimX(); col++) {
                        if (idxCalPage == 2) {
                            xValue = variable.getValue(0, 0, col).toString();
                            yValue = variable.getValue(0, row, 0).toString();
                        } else {
                            xValue = variable.getValue(idxCalPage, 0, col).toString();
                            yValue = variable.getValue(idxCalPage, row, 0).toString();
                        }

                        value = variable.getValue(idxCalPage, row, col).toString();

                        table.getColumnModel().getColumn(col - 1).setHeaderValue(xValue);
                        rowTable.setValueAt(yValue, row - 1, 0);
                        dataVector.get(row - 1).set(col - 1, value);
                    }
                }
            } else {

                table.setTableHeader(null);
                scrollPane.setRowHeaderView(null);
                scrollPane.getColumnHeader().setVisible(false);

                for (int row = 0; row < variable.getDimY() - 1; row++) {
                    for (int col = 0; col < variable.getDimX() - 1; col++) {

                        oValue = variable.getValue(idxCalPage, row, col);

                        value = oValue != null ? oValue.toString() : "";

                        dataVector.get(row).set(col, value);
                    }
                }
            }

        }
        ((DefaultTableModel) table.getModel()).fireTableDataChanged();
        header.repaint();

        if (btTrace.isSelected()) {
            setTrackFlag();
        }

        cursorValues = null;

        Utilitaire.adjustTableCells(table);
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
        final String ICON_TRACE = "/icon_traceMap_24.png";
        final String ICON_CURSOR_TRACK = "/icon_cursorTrack_24.png";
        final String ICON_COLOR = "/icon_color_24.png";
        final String ICON_COLOR_SELECTED = "/icon_color_selected_24.png";

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
                    if (table == null || selectedVariable == null) {
                        return;
                    }
                    selectedVariable.backToRefValue();
                    populate(selectedVariable);
                    mapView.fireMapCalChange();
                }
            });
            add(btReset);

            btTrace = new JToggleButton(new ImageIcon(getClass().getResource(ICON_TRACE)));
            btTrace.setToolTipText("Afficher les points du log");
            btTrace.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    if (!btTrace.isSelected() && table != null) {
                        flagInLog = null;
                        table.repaint();
                        return;
                    }

                    String logOK = mapView.getLog() != null ? "OK" : "nOK";
                    String workspaceOK = mapView.findSelectedCal() != null && mapView.findSelectedCal().hasWorkspaceLinked() ? "OK" : "nOK";

                    if ("OK".equals(logOK) && "OK".equals(workspaceOK)) {
                        setTrackFlag();
                    } else {
                        btTrace.setSelected(false);
                        JOptionPane.showMessageDialog(null, "Cette fonction a besoin :\n -d'un workspace associ\u00e9 au fichier *.map => "
                                + workspaceOK + "\n -d'un log => " + logOK, "INFO", JOptionPane.WARNING_MESSAGE);
                    }

                }
            });
            add(btTrace);

            JToggleButton btShowCursorValue = new JToggleButton(new ImageIcon(getClass().getResource(ICON_CURSOR_TRACK)));
            btShowCursorValue.setToolTipText("Affichage du point courant du curseur dans le log");
            btShowCursorValue.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                }
            });

            add(btShowCursorValue);
            btShowCursorValue.setVisible(false);

            String[] choixCal = new String[] { "Travail", "Référence", "Différences" };
            JComboBox<String> cbPage = new JComboBox<String>(choixCal);
            cbPage.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    int idx = cbPage.getSelectedIndex();
                    if (idx > -1 && idx != idxCalPage) {
                        idxCalPage = idx;
                        populate(selectedVariable);
                        mapView.updateChart(selectedVariable);
                    }
                }
            });
            cbPage.setMaximumSize(new Dimension(80, 24));
            add(cbPage);

            JCheckBox chkColor = new JCheckBox(new ImageIcon(getClass().getResource(ICON_COLOR)));
            chkColor.setSelectedIcon(new ImageIcon(getClass().getResource(ICON_COLOR_SELECTED)));
            chkColor.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    colorTable = chkColor.isSelected();
                    table.repaint();
                }
            });
            add(chkColor);
        }
    }

    public final void showCursorValues(int cursorIndex) {

        String[] args = selectedVariable.getInputsVar();

        List<Measure> measures = new ArrayList<>();
        List<String> missingMeasures = new ArrayList<>();

        for (String s : args) {
            if (!s.isEmpty()) {
                Measure measure = mapView.getLog().getMeasureWoutUnit(s);
                if (!measure.isEmpty()) {
                    measures.add(measure);
                } else {
                    missingMeasures.add(s);
                }
            }

        }

        if (!missingMeasures.isEmpty()) {
            // JOptionPane.showMessageDialog(null, "Il manque les grandeurs suivantes dans le log : " + missingMeasures);
        }

        Measure xMeasure = null;
        Measure yMeasure = null;
        float xValue = 0;
        float yValue = 0;

        if (measures.size() == 1) {
            xMeasure = measures.get(0);
            if (xMeasure != null) {
                xValue = (float) measures.get(0).get(cursorIndex);
            }

        } else if (measures.size() == 2) {
            xMeasure = measures.get(0);
            yMeasure = measures.get(1);
            if (xMeasure != null && yMeasure != null) {
                xValue = (float) measures.get(0).get(cursorIndex);
                yValue = (float) measures.get(1).get(cursorIndex);
            }
        } else {
            return;
        }

        float[] xBrkPt;
        float[] yBrkPt;

        int[] xIndex;
        int[] yIndex;

        float x1 = Float.NaN;
        float x2 = Float.NaN;
        float y1 = Float.NaN;
        float y2 = Float.NaN;

        switch (selectedVariable.getType()) {
        case COURBE:

            xBrkPt = selectedVariable.getXAxis(0);

            cursorValues = new boolean[1][xBrkPt.length];

            xIndex = new int[2];

            int xIdx = Arrays.binarySearch(xBrkPt, xValue);

            if (xIdx < 0) {
                xIndex[0] = Math.max(-(xIdx + 1) - 1, 0);
                xIndex[1] = Math.min(-(xIdx + 1), xBrkPt.length - 1);
            } else {
                Arrays.fill(xIndex, xIdx);
            }

            cursorValues[0][xIndex[0]] = true;
            cursorValues[0][xIndex[1]] = true;

            x1 = xBrkPt[xIndex[0]];
            x2 = xBrkPt[xIndex[1]];

            xFraction = (xValue - x1) / (x2 - x1);
            coordFraction = new int[1];
            if (xFraction <= 0.5) {
                coordFraction[0] = xIndex[0];
            } else {
                coordFraction[0] = xIndex[1];
            }

            break;
        case MAP:

            xBrkPt = selectedVariable.getXAxis(0);
            yBrkPt = selectedVariable.getYAxis(0);

            cursorValues = new boolean[yBrkPt.length][xBrkPt.length];

            xIndex = new int[2];
            yIndex = new int[2];

            int xIdx2 = Arrays.binarySearch(xBrkPt, xValue);
            int yIdx = Arrays.binarySearch(yBrkPt, yValue);

            if (xIdx2 < 0) {
                xIndex[0] = Math.max(-(xIdx2 + 1) - 1, 0);
                xIndex[1] = Math.min(-(xIdx2 + 1), xBrkPt.length - 1);
            } else {
                Arrays.fill(xIndex, xIdx2);
            }

            if (yIdx < 0) {
                yIndex[0] = Math.max(-(yIdx + 1) - 1, 0);
                yIndex[1] = Math.min(-(yIdx + 1), yBrkPt.length - 1);
            } else {
                Arrays.fill(yIndex, yIdx);
            }

            cursorValues[yIndex[0]][xIndex[0]] = true;
            cursorValues[yIndex[0]][xIndex[1]] = true;
            cursorValues[yIndex[1]][xIndex[0]] = true;
            cursorValues[yIndex[1]][xIndex[1]] = true;

            x1 = xBrkPt[xIndex[0]];
            x2 = xBrkPt[xIndex[1]];
            y1 = yBrkPt[yIndex[0]];
            y2 = yBrkPt[yIndex[1]];

            xFraction = (xValue - x1) / (x2 - x1);
            yFraction = (yValue - y1) / (y2 - y1);

            coordFraction = new int[2];
            if (xFraction <= 0.5) {
                coordFraction[0] = xIndex[0];
            } else {
                coordFraction[0] = xIndex[1];
            }
            if (yFraction <= 0.5) {
                coordFraction[1] = yIndex[0];
            } else {
                coordFraction[1] = yIndex[1];
            }

            break;
        default:
            return;
        }

        if (Float.isNaN(xFraction) || Float.isInfinite(xFraction)) {
            xFraction = 0;
        }

        if (Float.isNaN(yFraction) || Float.isInfinite(yFraction)) {
            yFraction = 0f;
        }

        table.repaint();
    }

    public final void setTrackFlag() {

        if (this.selectedVariable == null || !btTrace.isSelected()) {
            flagInLog = null;
            return;
        }

        Log log = mapView.getLog();

        String[] args = this.selectedVariable.getInputsVar();

        Measure xMeasure;
        Measure yMeasure;

        float[] xBrkPt;
        float[] yBrkPt;

        int[] xIndex;
        int[] yIndex;

        switch (this.selectedVariable.getType()) {
        case COURBE:
            xMeasure = log.getMeasureWoutUnit(args[0]);

            if (xMeasure.isEmpty()) {
                return;
            }

            xBrkPt = this.selectedVariable.getXAxis(0);

            flagInLog = new boolean[1][xBrkPt.length];

            xIndex = new int[2];

            for (int i = 0; i < log.getTime().getDataLength(); i++) {

                float x = (float) xMeasure.get(i);

                int xIdx = Arrays.binarySearch(xBrkPt, x);

                if (xIdx < 0) {
                    xIndex[0] = Math.max(-(xIdx + 1) - 1, 0);
                    xIndex[1] = Math.min(-(xIdx + 1), xBrkPt.length - 1);
                } else {
                    Arrays.fill(xIndex, xIdx);
                }

                flagInLog[0][xIndex[0]] = true;
                flagInLog[0][xIndex[1]] = true;
            }
            break;
        case MAP:
            xMeasure = log.getMeasureWoutUnit(args[0]);
            yMeasure = log.getMeasureWoutUnit(args[1]);

            if (xMeasure.isEmpty() || yMeasure.isEmpty()) {
                return;
            }

            xBrkPt = this.selectedVariable.getXAxis(0);
            yBrkPt = this.selectedVariable.getYAxis(0);

            flagInLog = new boolean[yBrkPt.length][xBrkPt.length];

            xIndex = new int[2];
            yIndex = new int[2];

            for (int i = 0; i < log.getTime().getDataLength(); i++) {

                float x = (float) xMeasure.get(i);
                float y = (float) yMeasure.get(i);

                int xIdx = Arrays.binarySearch(xBrkPt, x);
                int yIdx = Arrays.binarySearch(yBrkPt, y);

                if (xIdx < 0) {
                    xIndex[0] = Math.max(-(xIdx + 1) - 1, 0);
                    xIndex[1] = Math.min(-(xIdx + 1), xBrkPt.length - 1);
                } else {
                    Arrays.fill(xIndex, xIdx);
                }

                if (yIdx < 0) {
                    yIndex[0] = Math.max(-(yIdx + 1) - 1, 0);
                    yIndex[1] = Math.min(-(yIdx + 1), yBrkPt.length - 1);
                } else {
                    Arrays.fill(yIndex, yIdx);
                }

                flagInLog[yIndex[0]][xIndex[0]] = true;
                flagInLog[yIndex[0]][xIndex[1]] = true;
                flagInLog[yIndex[1]][xIndex[0]] = true;
                flagInLog[yIndex[1]][xIndex[1]] = true;
            }
            break;
        default:
            return;
        }

        table.repaint();
    }

    private final class MyTableRenderer extends JLabel implements TableCellRenderer {

        private static final long serialVersionUID = 1L;

        private Color unselectedBackground;
        private boolean flagPaint = false;
        private ColorModel spectrum;

        public MyTableRenderer() {
            setOpaque(true);
            this.spectrum = new ColorModel((byte) 1, 0.0F, 1.0F, 1.0F, 0.0F, 0.6666F);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (flagPaint) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(Color.RED);

                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int x = (int) (getWidth() * xFraction) - 3;

                switch (coordFraction.length) {
                case 1:
                    if (xFraction <= 0.5) {
                        x += (getWidth() / 2);
                    } else {
                        x -= (getWidth() / 2);
                    }
                    g2d.fillOval(x, getHeight() / 2 - 3, 6, 6);
                    break;
                case 2:

                    int y = (int) (getHeight() * yFraction) - 3;

                    if (xFraction <= 0.5) {
                        x += (getWidth() / 2);
                    } else {
                        x -= (getWidth() / 2);
                    }

                    if (yFraction <= 0.5) {
                        y += (getHeight() / 2);
                    } else {
                        y -= (getHeight() / 2);
                    }

                    g2d.fillOval(x, y, 6, 6);
                    break;
                default:
                    break;
                }

            }

        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (flagInLog != null) {
                if (flagInLog[row][column]) {
                    setIcon(iconMarker);
                } else {
                    setIcon(null);
                }
            } else {
                setIcon(null);
            }

            if (cursorValues != null) {
                if (cursorValues[row][column]) {
                    setBorder(BorderFactory.createLineBorder(Color.RED));

                    switch (coordFraction.length) {
                    case 1:
                        if (column == coordFraction[0]) {
                            flagPaint = true;
                        } else {
                            flagPaint = false;
                        }
                        break;
                    case 2:
                        if (column == coordFraction[0] && row == coordFraction[1]) {
                            flagPaint = true;
                        } else {
                            flagPaint = false;
                        }
                        break;
                    default:
                        break;
                    }
                } else {
                    setBorder(null);
                    flagPaint = false;
                }
            } else {
                setBorder(null);
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

            Object oValue1 = selectedVariable.getValue(1, row + offsetRow, column + offsetCol);
            Object oValue2 = selectedVariable.getValue(0, row + offsetRow, column + offsetCol);

            double oldValue = 0;
            double newValue = 0;
            double diff = 0;

            if (oValue1 instanceof Number && oValue2 instanceof Number) {
                oldValue = ((Number) oValue1).doubleValue();
                newValue = ((Number) oValue2).doubleValue();
                diff = newValue - oldValue;
            }

            if (isSelected) {
                super.setBackground(Color.LIGHT_GRAY);
            } else {
                if (colorTable) {
                    float[] minMax = selectedVariable.getMinMaxZ(idxCalPage);
                    float z = (Utilitaire.getNumberObject(value.toString()).floatValue() - minMax[0]) / (minMax[1] - minMax[0]);
                    if (Float.isNaN(z)) {
                        z = 0.5f;
                    }
                    setBackground(this.spectrum.getPolygonColor(z));
                } else {
                    Color background = unselectedBackground != null ? unselectedBackground : table.getBackground();
                    super.setBackground(background);
                }

            }

            if (diff > 0 && idxCalPage != 1 && !colorTable) {
                setForeground(Color.RED);
            } else if (diff < 0 && idxCalPage != 1 && !colorTable) {
                setForeground(Color.BLUE);
            } else {
                setForeground(Color.BLACK);
            }

            setText((value == null) ? "" : value.toString());

            return this;
        }

    }
}

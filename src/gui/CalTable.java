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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Arrays;

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
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import calib.Variable;
import utils.Interpolation;

public class CalTable extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private RowNumberTable rowTable;
    private BarControl control;
    private JScrollPane scrollPane;
    private Variable variable;
    private JTableHeader header;
    private JPopupMenu renamePopup;
    private JTextField text;
    private TableColumn column;
    private int rowBrkPt;

    public CalTable(Variable variable) {
        super(new BorderLayout(0, 0));

        control = new BarControl();
        add(control, BorderLayout.NORTH);

        if (variable == null) {
            return;
        }

        this.variable = variable;

        int nbRow = variable.getDimY();
        int nbCol = variable.getDimX();
        switch (nbRow) {
        case 1:
            // nothin
            break;
        case 2:
            nbRow--;
            break;
        default:
            nbRow--;
            nbCol--;
            break;
        }

        table = new JTable(nbRow, nbCol);

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

        add(scrollPane, BorderLayout.CENTER);

    }

    private void editColumnAt(Point p) {
        int columnIndex = header.columnAtPoint(p);

        if (columnIndex != -1) {
            column = header.getColumnModel().getColumn(columnIndex);
            Rectangle columnRectangle = header.getHeaderRect(columnIndex);

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
        if (column != null) {
            column.setHeaderValue(text.getText());
            header.repaint();
        } else {
            if (rowBrkPt != -1) {
                rowTable.setValueAt(text.getText(), rowBrkPt, 0);
            }
        }
        renamePopup.setVisible(false);
        calcZvalue();
    }

    public final void calcZvalue() {

        final double[][] datasTable = getTableDoubleValue();
        double result = Double.NaN;

        for (int row = 0; row < table.getRowCount(); row++) {

            if (variable.getDimY() > 2) {
                for (int x = 1; x < variable.getDimX(); x++) {
                    result = Interpolation.interpLinear2D(variable.toDouble2D(), datasTable[0][x], datasTable[row + 1][0]);
                    int diff = Double.compare(result, Double.parseDouble(table.getValueAt(row, x - 1).toString()));
                    if (diff > 0) {
                        System.out.println("augmente");
                    } else if (diff < 0) {
                        System.out.println("diminue");
                    } else {
                        System.out.println("égale");
                    }
                    table.setValueAt(result, row, x - 1);
                }
            }

            if (variable.getDimY() == 2) {
                for (int x = 0; x < variable.getDimX(); x++) {
                    result = Interpolation.interpLinear1D(variable.toDouble2D(), datasTable[0][x]);
                    int diff = Double.compare(result, Double.parseDouble(table.getValueAt(0, x).toString()));
                    if (diff > 0) {
                        System.out.println("augmente");
                    } else if (diff < 0) {
                        System.out.println("diminue");
                    } else {
                        System.out.println("égale");
                    }
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
        if (variable.getDimX() * variable.getDimY() == 1) {
            table.setTableHeader(null);
            scrollPane.setRowHeaderView(null);

            value = variable.getValue(0, 0).toString();

            table.setValueAt(value, 0, 0);

        } else if (variable.getDimY() == 2) {

            scrollPane.setRowHeaderView(null);

            String xValue;

            for (int col = 0; col < variable.getDimX(); col++) {
                xValue = variable.getValue(0, col).toString();
                value = variable.getValue(1, col).toString();

                table.getColumnModel().getColumn(col).setHeaderValue(xValue);
                table.setValueAt(value, 0, col);
            }
        } else {

            String xValue;
            String yValue;

            for (int row = 1; row < variable.getDimY(); row++) {
                for (int col = 1; col < variable.getDimX(); col++) {
                    xValue = variable.getValue(0, col).toString();
                    yValue = variable.getValue(row, 0).toString();
                    value = variable.getValue(row, col).toString();

                    table.getColumnModel().getColumn(col - 1).setHeaderValue(xValue);
                    rowTable.setValueAt(yValue, row - 1, 0);
                    table.setValueAt(value, row - 1, col - 1);
                }
            }
        }
        adjustCells();

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

        double[][] doubleValues = new double[variable.getDimY()][variable.getDimX()];

        doubleValues[0][0] = Double.NaN;

        if (variable.getDimY() > 2) {
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

        final int startCol = nbRow > 1 ? -1 : 0;
        final int startRow = nbCol > 1 ? -1 : 0;

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

        public BarControl() {
            super();
            setFloatable(false);
            setBorder(BorderFactory.createEtchedBorder());

            JButton btCopy = new JButton(null, new ImageIcon(getClass().getResource(ICON_COPY)));
            btCopy.setToolTipText("Copier dans le presse-papier");
            btCopy.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    System.out.println(getTableValue());

                    if (variable == null) {
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
        }
    }

}

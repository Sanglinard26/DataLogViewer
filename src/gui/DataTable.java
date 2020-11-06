/*
 * Creation : 2 nov. 2020
 */
package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import calib.Variable;

public class DataTable extends JPanel {

    private static final long serialVersionUID = 1L;

    private JTable table;
    private RowNumberTable rowTable;
    JScrollPane scrollPane;

    public DataTable(Variable variable) {
        super();

        if (variable == null) {
            return;
        }

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

        setLayout(new BorderLayout());

        table = new JTable(nbRow, nbCol);
        table.setTableHeader(new CustomTableHeader(table));
        table.getTableHeader().setDefaultRenderer(new SimpleHeaderRenderer());
        table.setCellSelectionEnabled(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.getTableHeader().setReorderingAllowed(false);

        scrollPane = new JScrollPane(table);
        rowTable = new RowNumberTable(table);

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setRowHeaderView(rowTable);
        // scrollPane.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, new JButton("bt"));

        populate(variable);

        add(scrollPane, BorderLayout.CENTER);

    }

    public final int getTableHeight() {
        int hBarVisible = scrollPane.getHorizontalScrollBar().isVisible() ? 1 : 0;
        int dataHeight = (table.getRowCount() + 1 + hBarVisible * 1) * (table.getRowHeight() + table.getRowMargin() + 1);
        return dataHeight;
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

}

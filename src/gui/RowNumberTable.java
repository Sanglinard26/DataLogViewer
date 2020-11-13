/*
 * Creation : 2 nov. 2020
 */
package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/*
 *  Use a JTable as a renderer for row numbers of a given main table.
 *  This table must be added to the row header of the scrollpane that
 *  contains the main table.
 */
public class RowNumberTable extends JTable implements ChangeListener, PropertyChangeListener {
    private static final long serialVersionUID = 1L;
    private JTable main;
    private DefaultTableModel model;

    public RowNumberTable(JTable table) {
        main = table;
        main.addPropertyChangeListener(this);
        main.getModel().addTableModelListener(this);

        model = new DefaultTableModel(main.getRowCount(), 1);
        setModel(model);

        setShowGrid(false);
        setFocusable(false);
        setAutoCreateColumnsFromModel(false);
        setSelectionModel(main.getSelectionModel());
        setIntercellSpacing(new Dimension(0, 0));

        TableColumn column = new TableColumn();
        column.setHeaderValue("");
        addColumn(column);
        column.setCellRenderer(new RowNumberRenderer());

        getColumnModel().getColumn(0).setPreferredWidth(50);
        setPreferredScrollableViewportSize(getPreferredSize());
    }

    @Override
    public void addNotify() {
        super.addNotify();

        Component c = getParent();

        // Keep scrolling of the row table in sync with the main table.

        if (c instanceof JViewport) {
            JViewport viewport = (JViewport) c;
            viewport.addChangeListener(this);
        }
    }

    /*
     * Delegate method to main table
     */
    @Override
    public int getRowCount() {
        return main.getRowCount();
    }

    @Override
    public int getRowHeight(int row) {
        int rowHeight = main.getRowHeight(row);

        if (rowHeight != super.getRowHeight(row)) {
            super.setRowHeight(row, rowHeight);
        }

        return rowHeight;
    }

    /*
     * No model is being used for this table so just use the row number as the value of the cell.
     */
    // @Override
    public Object getValueAt(int row, int column) {
        return model.getValueAt(row, column);
    }

    /*
     * Don't edit data in the main TableModel by mistake
     */
    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }

    /*
     * Do nothing since the table ignores the model
     */
    @Override
    public void setValueAt(Object value, int row, int column) {
        model.setValueAt(value, row, column);
    }

    //
    // Implement the ChangeListener
    //
    public void stateChanged(ChangeEvent e) {
        // Keep the scrolling of the row table in sync with main table

        JViewport viewport = (JViewport) e.getSource();
        JScrollPane scrollPane = (JScrollPane) viewport.getParent();
        scrollPane.getVerticalScrollBar().setValue(viewport.getViewPosition().y);
    }

    //
    // Implement the PropertyChangeListener
    //
    public void propertyChange(PropertyChangeEvent e) {
        // Keep the row table in sync with the main table

        if ("selectionModel".equals(e.getPropertyName())) {
            setSelectionModel(main.getSelectionModel());
        }

        if ("rowHeight".equals(e.getPropertyName())) {
            repaint();
        }

        if ("model".equals(e.getPropertyName())) {
            main.getModel().addTableModelListener(this);
            revalidate();
        }
    }

    //
    // Implement the TableModelListener
    //
    @Override
    public void tableChanged(TableModelEvent e) {
        revalidate();
    }

    /*
     * Attempt to mimic the table header renderer
     */
    private static class RowNumberRenderer extends JLabel implements TableCellRenderer {
        private static final long serialVersionUID = 1L;

        public RowNumberRenderer() {
            setHorizontalAlignment(JLabel.CENTER);
            setOpaque(true);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table != null) {
                JTableHeader header = table.getTableHeader();

                if (header != null) {
                    setForeground(header.getForeground());
                    setBackground(header.getBackground());
                    setFont(header.getFont());
                    setBorder(BorderFactory.createEtchedBorder());
                }
            }

            if (isSelected) {
                setFont(getFont().deriveFont(Font.BOLD));
                setForeground(Color.BLUE);
            }

            setText((value == null) ? "" : value.toString());
            // setBorder(UIManager.getBorder("TableHeader.cellBorder"));

            return this;
        }
    }
}
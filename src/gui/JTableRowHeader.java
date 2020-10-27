/*
 * Creation : 27 oct. 2020
 */
package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

public class JTableRowHeader {

    private JFrame frame = new JFrame("JTable RowHeader");
    private JScrollPane scrollPane;
    private JTable table;
    private DefaultTableModel model;
    private JTable headerTable;

    public JTableRowHeader() {
        table = new JTable(16, 16);
        for (int i = 0; i < table.getRowCount(); i++) {
            table.setValueAt(i, i, 0);
        }

        model = new DefaultTableModel() {

            private static final long serialVersionUID = 1L;

            @Override
            public int getColumnCount() {
                return 1;
            }

            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }

            @Override
            public int getRowCount() {
                return table.getRowCount();
            }

            @Override
            public Class<?> getColumnClass(int colNum) {
                switch (colNum) {
                case 0:
                    return String.class;
                default:
                    return super.getColumnClass(colNum);
                }
            }
        };

        headerTable = new JTable(model);
        for (int i = 0; i < table.getRowCount(); i++) {
            headerTable.setValueAt("Row " + (i + 1), i, 0);
        }
        headerTable.setShowGrid(false);
        headerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        headerTable.setPreferredScrollableViewportSize(new Dimension(50, 0));
        headerTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        headerTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable x, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

                boolean selected = table.getSelectionModel().isSelectedIndex(row);
                Component component = table.getTableHeader().getDefaultRenderer().getTableCellRendererComponent(table, value, false, false, -1, -2);
                ((JLabel) component).setHorizontalAlignment(SwingConstants.CENTER);
                if (selected) {
                    component.setFont(component.getFont().deriveFont(Font.BOLD));
                    component.setForeground(Color.red);
                } else {
                    component.setFont(component.getFont().deriveFont(Font.PLAIN));
                }
                return component;
            }
        });

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                model.fireTableRowsUpdated(0, model.getRowCount() - 1);
            }
        });
        scrollPane = new JScrollPane(table);
        scrollPane.setRowHeaderView(headerTable);
        table.setPreferredScrollableViewportSize(table.getPreferredSize());
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(scrollPane);

        frame.pack();
        frame.setLocation(150, 150);
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (info.getName().equals("Windows")) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // e.printStackTrace();
        }
        EventQueue.invokeLater(new Runnable() {

            @Override
            public void run() {
                JTableRowHeader TestTableRowHeader = new JTableRowHeader();
            }
        });
    }
}

package gui;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import observer.Observateur;

public final class TableCursorValue extends JTable implements Observateur {

    private static final long serialVersionUID = 1L;

    public TableCursorValue() {
        super(new DataValueModel());
        getColumnModel().getColumn(0).setPreferredWidth(10);
        getColumnModel().getColumn(1).setPreferredWidth(160);
        getColumnModel().getColumn(2).setPreferredWidth(80);
        setDefaultRenderer(Color.class, new ColorRenderer(true));
    }

    @Override
    public TableModel getModel() {
        return super.getModel();
    }

    @Override
    public void updateValues(HashMap<String, Double> tableValue) {

        Set<Entry<String, Double>> set = tableValue.entrySet();
        for (Entry<String, Double> entry : set) {
            int idx = ((DataValueModel) getModel()).labels.indexOf(entry.getKey());
            if (idx > -1) {
                ((DataValueModel) getModel()).setValueAt(entry.getValue(), idx, 2);
            }
        }

    }

    @Override
    public void updateData(String type, Object object) {

        switch (type) {
        case "remove":
            ((DataValueModel) getModel()).removeElement(object.toString());
            break;
        case "update":
            Object[] objects = (Object[]) object;
            int idx = ((DataValueModel) getModel()).labels.indexOf(objects[0]);
            if (idx > -1) {
                ((DataValueModel) getModel()).setValueAt(objects[1], idx, 0);
            }
            break;
        default:
            break;
        }
    }

}

final class DataValueModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] ENTETES = new String[] { "", "Label", "Valeur" };

    final List<Color> colors;
    final List<String> labels;
    final List<Double> values;

    public DataValueModel() {
        colors = new ArrayList<Color>();
        labels = new ArrayList<String>();
        values = new ArrayList<Double>();
    }

    @Override
    public String getColumnName(int column) {
        return ENTETES[column];
    }

    @Override
    public int getColumnCount() {
        return ENTETES.length;
    }

    @Override
    public int getRowCount() {
        return labels.size();
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        switch (columnIndex) {
        case 0:
            return Color.class;
        case 1:
            return String.class;
        case 2:
            return Double.class;
        default:
            return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int col) {

        switch (col) {
        case 0:
            return colors.get(row);
        case 1:
            return labels.get(row);
        case 2:
            return values.get(row);
        default:
            return null;
        }

    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        switch (col) {
        case 0:
            colors.set(row, (Color) aValue);
            break;
        case 1:
            labels.set(row, aValue.toString());
            break;
        case 2:
            values.set(row, (Double) aValue);
            break;
        default:
            break;
        }
        fireTableDataChanged();
    }

    public final void clearList() {
        this.colors.clear();
        this.labels.clear();
        this.values.clear();
        fireTableDataChanged();
    }

    public final void changeList(Map<String, Color> newLabels) {
        this.colors.clear();
        this.labels.clear();
        this.values.clear();
        Set<Entry<String, Color>> entries = newLabels.entrySet();
        for (Entry<String, Color> entry : entries) {
            this.colors.add(entry.getValue());
            this.labels.add(entry.getKey());
            this.values.add(Double.NaN);
        }
        fireTableDataChanged();
    }

    public final void addElement(String label, Color color) {
        if (!this.labels.contains(label)) {
            this.colors.add(color);
            this.labels.add(label);
            this.values.add(Double.NaN);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
        }
    }

    public final void removeElement(String label) {
        int idx = this.labels.indexOf(label);
        if (idx > -1) {
            this.colors.remove(idx);
            this.labels.remove(idx);
            this.values.remove(idx);
            fireTableRowsDeleted(idx, idx);
        }

    }

}

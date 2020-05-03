package gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
        getColumnModel().getColumn(0).setPreferredWidth(160);
        getColumnModel().getColumn(1).setPreferredWidth(80);
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
                ((DataValueModel) getModel()).setValueAt(entry.getValue(), idx, 1);
            }
        }

    }

    @Override
    public void updateData(String key) {
        ((DataValueModel) getModel()).removeElement(key);
    }

}

final class DataValueModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    private static final String[] ENTETES = new String[] { "Label", "Valeur" };

    final List<String> labels;
    final List<Double> values;

    public DataValueModel() {
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
            return String.class;
        case 1:
            return Double.class;
        default:
            return String.class;
        }
    }

    @Override
    public Object getValueAt(int row, int col) {

        switch (col) {
        case 0:
            return labels.get(row);
        case 1:
            return values.get(row);
        default:
            return null;
        }

    }

    @Override
    public void setValueAt(Object aValue, int row, int col) {
        switch (col) {
        case 0:
            labels.set(row, aValue.toString());
            fireTableDataChanged();
        case 1:
            double value;
            try {
                value = Double.parseDouble(aValue.toString());
            } catch (NumberFormatException nfe) {
                value = Double.NaN;
            }
            values.set(row, value);
            fireTableDataChanged();
        default:
            return;
        }
    }

    public final void clearList() {
        this.labels.clear();
        this.values.clear();
        fireTableDataChanged();
    }

    public final void changeList(List<String> newLabels) {
        this.labels.clear();
        this.values.clear();
        for (int i = 0; i < newLabels.size(); i++) {
            this.labels.add(newLabels.get(i));
            this.values.add(Double.NaN);
        }
        fireTableDataChanged();
    }

    public final void addElement(String label) {
        if (!this.labels.contains(label)) {
            this.labels.add(label);
            this.values.add(Double.NaN);
            fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
        }
    }

    public final void removeElement(String label) {
        int idx = this.labels.indexOf(label);
        if (idx > -1) {
            this.labels.remove(idx);
            this.values.remove(idx);
            fireTableRowsDeleted(idx, idx);
        }

    }

}

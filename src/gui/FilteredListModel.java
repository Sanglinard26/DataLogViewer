/*
 * Creation : 10 oct. 2021
 */
package gui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractListModel;

import log.Measure;

public final class FilteredListModel extends AbstractListModel<Measure> {

    private static final long serialVersionUID = 1L;

    private final List<Measure> listLabel = new ArrayList<Measure>();
    private final List<Measure> listLabelFiltre = new ArrayList<Measure>();

    public FilteredListModel() {
        super();
    }

    public final void setFilter(String filter) {

        final Set<Measure> tmpList = new LinkedHashSet<Measure>();

        listLabelFiltre.clear();

        final int nbMeasure = listLabel.size();
        Measure measure;

        for (int i = 0; i < nbMeasure; i++) {
            measure = listLabel.get(i);

            if (measure.getName().toLowerCase().indexOf(filter) > -1) {
                tmpList.add(measure);
            }
        }

        listLabelFiltre.addAll(tmpList);

        this.fireContentsChanged(this, 0, getSize());
    }

    public final void addElement(Measure measure) {
        int index = listLabel.size();
        listLabel.add(measure);
        listLabelFiltre.add(measure);
        fireIntervalAdded(this, index, index);
    }

    public int indexOf(Object elem) {
        return listLabelFiltre.indexOf(elem);
    }

    public final void clear() {
        listLabel.clear();
        setFilter("");
    }

    @Override
    public int getSize() {
        return listLabelFiltre.size();
    }

    @Override
    public Measure getElementAt(int index) {
        return listLabelFiltre.get(index);
    }

    public boolean contains(Object elem) {
        return listLabelFiltre.contains(elem);
    }

    public boolean removeElement(Object obj) {
        int index = indexOf(obj);
        listLabel.remove(obj);
        boolean rv = listLabelFiltre.remove(obj);
        if (index >= 0) {
            fireIntervalRemoved(this, index, index);
        }
        return rv;
    }

    public final List<String> getStringList() {
        List<String> list = new ArrayList<>();

        for (Measure measure : listLabel) {
            list.add(measure.toString());
        }
        return list;
    }

}

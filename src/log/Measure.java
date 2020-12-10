/*
 * Creation : 15 mars 2018
 */
package log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Measure implements Comparable<Measure>, Serializable {

    private static final long serialVersionUID = 1L;
    protected String name;
    protected String unit;
    protected List<Number> data;
    protected double min = Double.POSITIVE_INFINITY;
    protected double max = Double.NEGATIVE_INFINITY;

    public Measure(String name) {
        this.name = name;
        this.unit = "";
        this.data = new ArrayList<Number>();
    }

    public final String getName() {
        return this.name;
    }

    public final void setName(String name) {
        this.name = name;
    }

    public final String getUnit() {
        return this.unit;
    }

    public final void setUnit(String unit) {
        this.unit = unit;
    }

    public final double getMin() {
        return min;
    }

    public final double getMax() {
        return max;
    }

    public void setMax(Number value) {
        if (Double.isNaN(value.doubleValue())) {
            return;
        }
        this.max = Math.max(max, value.doubleValue());
    }

    public void setMin(Number value) {
        if (Double.isNaN(value.doubleValue())) {
            return;
        }
        this.min = Math.min(min, value.doubleValue());
    }

    public final List<Number> getData() {
        return this.data;
    }

    public final double[] getDoubleValue() {
        final double[] result = new double[data.size()];
        for (int i = 0; i < data.size(); i++) {
            result[i] = data.get(i).doubleValue();
        }
        return result;
    }

    public final void clearData() {
        data.clear();
        min = Double.POSITIVE_INFINITY;
        max = Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public boolean equals(Object name) {
        if (name == null) {
            return false;
        }
        return this.name.equals(name.toString());
    }

    @Override
    public int compareTo(Measure measure) {
        return this.name.compareToIgnoreCase(measure.getName());
    }

}

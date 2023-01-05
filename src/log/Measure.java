/*
 * Creation : 15 mars 2018
 */
package log;

import java.util.BitSet;

public class Measure implements Comparable<Measure> {

    protected String name;
    protected String unit;
    protected double[] data;
    protected double min = Double.POSITIVE_INFINITY;
    protected double max = Double.NEGATIVE_INFINITY;
    protected int idx;

    public Measure(String name) {
        this(name, 0);
    }

    public Measure(String name, int dataSize) {
        this.name = name;
        this.unit = "";
        this.idx = 0;
        this.data = new double[dataSize];
        // Arrays.fill(data, Double.NaN);
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

    public final void addPoint(double value) {
        this.data[idx++] = value;
        if (Double.isNaN(value)) {
            return;
        }
        this.min = Math.min(min, value);
        this.max = Math.max(max, value);
    }

    public final double[] getData() {
        return this.data;
    }

    public final double get(int index) {
        return this.data[index];
    }

    public final int getDataLength() {
        return this.data.length;
    }

    public final boolean isEmpty() {
        return this.idx == 0;
    }

    public final double[] getDoubleValue(BitSet bitCondition) {

        final double[] result = new double[bitCondition.cardinality()];
        int cnt = 0;
        for (int i = 0; i < bitCondition.size(); i++) {
            if (bitCondition.get(i)) {
                result[cnt++] = data[i];
            }
        }
        return result;
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

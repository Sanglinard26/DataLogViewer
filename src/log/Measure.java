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
    protected List<Double> data;
    protected double min = Double.POSITIVE_INFINITY;
    protected double max = Double.NEGATIVE_INFINITY;

    public Measure(String name) {
        this.name = name;
        this.unit = "";
        this.data = new ArrayList<Double>();
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
    
    public void setMax(double value) {
		this.max = Math.max(max, value);
	}
    
    public void setMin(double value) {
		this.min = Math.min(min, value);
	}

    public final List<Double> getData() {
        return this.data;
    }
    
    public final double[] getDouleValue()
    {
    	final double[] result = new double[data.size()];
    	  for (int i = 0; i < data.size(); i++) {
    	    result[i] = data.get(i).doubleValue();
    	  }
    	  return result;
    }

    public final void setData(List<Double> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public boolean equals(Object name) {
        return this.name.equals(name.toString());
    }

    @Override
    public int compareTo(Measure measure) {
        return this.name.compareToIgnoreCase(measure.getName());
    }

}

/*
 * Creation : 28 avr. 2023
 */
package log;

import java.util.ArrayList;
import java.util.List;

import org.mariuszgromada.math.mxparser.FunctionExtension;

public class DeltaFunction implements FunctionExtension {

    private int nbPoint;
    private int count = 0;
    private final List<Double> values;

    public DeltaFunction() {
        this.values = new ArrayList<Double>();
    }

    @Override
    public double calculate() {
        if (this.count > this.nbPoint) {
            return this.values.get(this.count - 1) - this.values.get(this.count - 1 - this.nbPoint);
        }
        return Double.NaN;
    }

    @Override
    public FunctionExtension clone() {
        return null;
    }

    @Override
    public String getParameterName(int argumentIndex) {
        switch (argumentIndex) {
        case 0:
            return "valeur";
        case 1:
            return "nombre de point";
        default:
            return "";
        }
    }

    @Override
    public int getParametersNumber() {
        return 2;
    }

    @Override
    public void setParameterValue(int argumentIndex, double arg1) {
        if (argumentIndex == 0) {
            this.values.add(arg1);
            count++;
        }

        if (argumentIndex == 1) {
            this.nbPoint = (int) arg1;
        }
    }

}

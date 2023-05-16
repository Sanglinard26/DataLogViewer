/*
 * Creation : 28 avr. 2023
 */
package log;

import org.mariuszgromada.math.mxparser.FunctionExtension;

public class Limit implements FunctionExtension {

    private double min;
    private double max;
    private double value;

    public Limit() {
    }

    @Override
    public double calculate() {
        return Math.max(this.min, Math.min(this.max, this.value));
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
            return "valeur mini";
        case 2:
            return "valeur maxi";
        default:
            return "";
        }
    }

    @Override
    public int getParametersNumber() {
        return 3;
    }

    @Override
    public void setParameterValue(int argumentIndex, double arg1) {
        if (argumentIndex == 0) {
            this.value = arg1;
        }

        if (argumentIndex == 1) {
            this.min = arg1;
        }

        if (argumentIndex == 2) {
            this.max = arg1;
        }
    }

}

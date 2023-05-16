/*
 * Creation : 28 avr. 2023
 */
package log;

import org.mariuszgromada.math.mxparser.FunctionExtension;

public class LowPassFilterK implements FunctionExtension {

    private double prevOut = Double.NaN;
    private double in;
    private double k;

    public LowPassFilterK() {

    }

    @Override
    public double calculate() {
        if (!Double.isNaN(prevOut)) {
            double out = k * in + (1 - k) * prevOut;
            prevOut = out;
            return out;
        }
        prevOut = this.in;
        return this.in;
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
            return "coefficient";
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
            this.in = arg1;
        }

        if (argumentIndex == 1) {
            this.k = arg1;
        }
    }

}

/*
 * Creation : 28 avr. 2023
 */
package log;

import org.mariuszgromada.math.mxparser.FunctionExtension;

public class GetBit implements FunctionExtension {

    private int value;
    private int bitNum;

    public GetBit() {

    }

    @Override
    public double calculate() {
        int bitVal = (int) Math.pow(2, bitNum);
        return (value & bitVal) / bitVal;
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
            return "bitnumber";
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
            this.value = (int) arg1;
        }

        if (argumentIndex == 1) {
            this.bitNum = (int) arg1;
        }
    }

}

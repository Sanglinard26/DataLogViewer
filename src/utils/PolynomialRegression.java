/*
 * Creation : 7 févr. 2023
 */
package utils;

import java.util.Arrays;
import java.util.function.DoubleFunction;

public class PolynomialRegression {
    public static double[] polyRegression(double[] x, double[] y) {
        int n = x.length;
        double xm = Arrays.stream(x).average().orElse(Double.NaN);
        double ym = Arrays.stream(y).average().orElse(Double.NaN);
        double x2m = Arrays.stream(x).map(a -> a * a).average().orElse(Double.NaN);
        double x3m = Arrays.stream(x).map(a -> a * a * a).average().orElse(Double.NaN);
        double x4m = Arrays.stream(x).map(a -> a * a * a * a).average().orElse(Double.NaN);
        double xym = 0.0;
        for (int i = 0; i < x.length && i < y.length; ++i) {
            xym += x[i] * y[i];
        }
        xym /= Math.min(x.length, y.length);
        double x2ym = 0.0;
        for (int i = 0; i < x.length && i < y.length; ++i) {
            x2ym += x[i] * x[i] * y[i];
        }
        x2ym /= Math.min(x.length, y.length);

        double sxx = x2m - xm * xm;
        double sxy = xym - xm * ym;
        double sxx2 = x3m - xm * x2m;
        double sx2x2 = x4m - x2m * x2m;
        double sx2y = x2ym - x2m * ym;

        double b = (sxy * sx2x2 - sx2y * sxx2) / (sxx * sx2x2 - sxx2 * sxx2);
        double c = (sx2y * sxx - sxy * sxx2) / (sxx * sx2x2 - sxx2 * sxx2);
        double a = ym - b * xm - c * x2m;

        DoubleFunction<Double> abc = (double xx) -> a + b * xx + c * xx * xx;

        double[] result = new double[n];

        for (int i = 0; i < n; ++i) {
            result[i] = abc.apply(x[i]);
        }
        return result;
    }

}

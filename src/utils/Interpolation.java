/*
 * Creation : 25 juin 2018
 */
package utils;

import java.util.Arrays;

public final class Interpolation {

    public static final double[] interpLinear(double[] x, double[] y, double[] xi) throws IllegalArgumentException {

        if (x.length != y.length) {
            throw new IllegalArgumentException("X and Y must be the same length");
        }
        if (x.length == 1) {
            throw new IllegalArgumentException("X must contain more than one value");
        }
        double[] dx = new double[x.length - 1];
        double[] dy = new double[x.length - 1];
        double[] slope = new double[x.length - 1];
        double[] intercept = new double[x.length - 1];

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < x.length - 1; i++) {
            dx[i] = x[i + 1] - x[i];
            if (dx[i] == 0) {
                throw new IllegalArgumentException("X must be montotonic. A duplicate " + "x-value was found");
            }
            if (dx[i] < 0) {
                throw new IllegalArgumentException("X must be sorted");
            }
            dy[i] = y[i + 1] - y[i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = y[i] - x[i] * slope[i];
        }

        // Perform the interpolation here
        double[] yi = new double[xi.length];
        for (int i = 0; i < xi.length; i++) {
            if ((xi[i] > x[x.length - 1]) || (xi[i] < x[0])) {
                yi[i] = Double.NaN;
            } else {
                int loc = Arrays.binarySearch(x, xi[i]);
                if (loc < -1) {
                    loc = -loc - 2;
                    yi[i] = slope[loc] * xi[i] + intercept[loc];
                } else {
                    yi[i] = y[loc];
                }
            }
        }

        return yi;
    }

    public static final double interpLinear1D(double[][] curve, double xDes) throws IllegalArgumentException {

        int nbInterval = curve[0].length - 1;

        double xMin = curve[0][0];
        double xMax = curve[0][curve[0].length - 1];

        double[] x = new double[curve[0].length];

        double[] dx = new double[nbInterval];
        double[] dy = new double[nbInterval];
        double[] slope = new double[nbInterval];
        double[] intercept = new double[nbInterval];

        for (int i = 0; i < nbInterval + 1; i++) {
            x[i] = curve[0][i];
        }

        // Calculate the line equation (i.e. slope and intercept) between each point
        for (int i = 0; i < nbInterval; i++) {
            x[i] = curve[0][i];
            dx[i] = curve[0][i + 1] - curve[0][i];
            dy[i] = curve[1][i + 1] - curve[1][i];
            slope[i] = dy[i] / dx[i];
            intercept[i] = curve[1][i] - curve[0][i] * slope[i];
        }

        // Perform the interpolation here
        double yi = Double.NaN;
        double xi = Math.min(Math.max(xDes, xMin), xMax);

        int loc = Arrays.binarySearch(x, xi);
        if (loc < -1) {
            loc = -loc - 2;
            yi = slope[loc] * xi + intercept[loc];
        } else {
            yi = curve[1][loc];
        }

        return yi;
    }

    public static final double interpLinear2D(double[][] map, double xDes, double yDes) {

        double xMin = map[0][1];
        double xMax = map[0][map[0].length - 1];
        double yMin = map[1][0];
        double yMax = map[map.length - 1][0];

        double x0;
        double x1;
        double y0;
        double y1;
        double z00;
        double z01;
        double z10;
        double z11;
        double z_y0;
        double z_y1;
        
        double xi = Math.min(Math.max(xDes, xMin), xMax);
        double yi = Math.min(Math.max(yDes, yMin), yMax);

        int idxX = 0;

        do {
            idxX++;
            x0 = map[0][idxX];
            x1 = map[0][idxX + 1];
        } while (!(xi >= x0 && xi <= x1));

        int idxY = 0;

        do {
            idxY++;
            y0 = map[idxY][0];
            y1 = map[idxY + 1][0];
        } while (!(yi >= y0 && yi <= y1));

        z00 = map[idxY][idxX];
        z01 = map[idxY][idxX + 1];
        z10 = map[idxY + 1][idxX];
        z11 = map[idxY + 1][idxX + 1];

        z_y0 = z00 + (z01 - z00) / (x1 - x0) * (xi - x0);
        z_y1 = z10 + (z11 - z10) / (x1 - x0) * (xi - x0);

        return z_y0 + (z_y1 - z_y0) / (y1 - y0) * (yi - y0);

    }

}

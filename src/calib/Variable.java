/*
 * Creation : 4 nov. 2020
 */
package calib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import calib.MdbData.VariableInfo;

public final class Variable implements Comparable<Variable> {

    private final String name;
    private int dimX;
    private int dimY;
    private Object[] values;
    private VariableInfo infos;

    public Variable(List<String> data, MdbData mdbData) {
        this.name = data.get(0).substring(1, data.get(0).length() - 1);
        this.infos = mdbData.getInfos().get(this.name);
        build(data);
    }

    public String getName() {
        return name;
    }

    public String getType() {
        if (dimX * dimY == 1) {
            return "scalaire";
        }
        if (dimY == 2) {
            return "courbe";
        }
        if (dimY > 2) {
            return "carto";
        }
        return "inconnu";
    }

    @Override
    public String toString() {
        return this.name;
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public Object getValue(int... coord) {
        int idx = coord[1] + dimX * coord[0];
        return this.values[idx];
    }

    public void setValue(Object value, int... coord) {
        int idx = coord[1] + dimX * coord[0];
        this.values[idx] = value;
    }

    private final void build(List<String> data) {

        final String COLONNES = "colonnes";
        final String BKPTCOL = "bkptcol";
        final String LIGNE = "ligne";
        final String BKPTLIGN = "bkptlign";
        final String SEMICOLON = ";";
        final String EGALE = "=";

        String[] splitEgale;
        String[] splitSemiColon;

        List<Object> bkptcol = null;
        List<Object> bkptlign = null;
        List<Object> line = null;
        Map<Object, Object> mapValues = null;

        for (int i = 1; i < data.size(); i++) {
            splitEgale = data.get(i).split(EGALE);
            if (splitEgale.length > 1) {
                switch (splitEgale[0]) {
                case COLONNES:
                    dimX = 1;
                    dimY = 1;
                    values = new Object[] { getStorageObject(splitEgale[1].replace(SEMICOLON, "")) };
                    break;
                case BKPTCOL:
                    if (bkptcol != null) {
                        break;
                    }
                    bkptcol = new ArrayList<Object>();
                    for (String s : splitEgale[1].split(SEMICOLON)) {
                        if (!s.isEmpty()) {
                            bkptcol.add(getStorageObject(s));
                        }
                    }

                    break;
                case LIGNE:
                    line = new ArrayList<Object>();
                    for (String s : splitEgale[1].split(SEMICOLON)) {
                        if (!s.isEmpty()) {
                            line.add(getStorageObject(s));
                        }
                    }
                    break;
                case BKPTLIGN:
                    if (bkptlign != null) {
                        break;
                    }
                    bkptlign = new ArrayList<Object>();
                    for (String s : splitEgale[1].split(SEMICOLON)) {
                        if (!s.isEmpty()) {
                            bkptlign.add(getStorageObject(s));
                        }
                    }
                    break;
                default:

                    if (mapValues == null) {
                        mapValues = new LinkedHashMap<Object, Object>();
                    }
                    mapValues.put(getStorageObject(splitEgale[0].replace("ligne", "")), splitEgale[1]);

                    break;
                }
            }

        }

        if (bkptcol != null && line != null && mapValues == null) {
            dimX = bkptcol.size();
            dimY = 2;
            values = new Object[dimX * dimY];
            for (int i = 0; i < dimX; i++) {
                setValue(bkptcol.get(i), 0, i);
                setValue(line.get(i), 1, i);
            }
        } else if (mapValues != null && bkptcol != null && bkptlign != null) {
            dimX = bkptcol.size() + 1;
            dimY = bkptlign.size() + 1;
            values = new Object[dimX * dimY];

            setValue("Y \\ X", 0, 0);

            for (int i = 1; i < dimX; i++) {
                setValue(bkptcol.get(i - 1), 0, i);
            }

            Object keyLigne;
            Object ligne;
            for (int i = 0; i < dimY - 1; i++) {
                keyLigne = bkptlign.get(i);
                setValue(keyLigne, i + 1, 0);

                ligne = mapValues.get(keyLigne);
                if (ligne != null) {
                    splitSemiColon = ligne.toString().split(SEMICOLON);
                    for (int j = 0; j < dimX - 1; j++) {
                        setValue(getStorageObject(splitSemiColon[j]), i + 1, j + 1);
                    }
                }
            }

        } else if (mapValues != null && bkptcol == null && bkptlign == null) {
            StringBuilder sb = new StringBuilder();

            dimX = 1;
            dimY = 1;

            for (Object s : mapValues.values()) {
                sb.append(s.toString().replace(";", ""));
            }
            values = new Object[] { sb.toString() };

        }

    }

    public final String toTxtTab() {
        StringBuilder sb = new StringBuilder();
        sb.append((this.name));
        if (dimX * dimY == 1) {
            sb.append("\n" + values[0].toString());
            return sb.toString();
        }
        if (dimY == 2) {
            sb.append("\n");
            for (int i = 0; i < dimX; i++) {
                sb.append(getValue(0, i) + "\t");
            }
            sb.append("\n");
            for (int i = 0; i < dimX; i++) {
                sb.append(getValue(1, i) + "\t");
            }
            return sb.toString();
        }

        if (dimY > 2) {
            sb.append("\n");

            for (int row = 0; row < dimY; row++) {
                for (int col = 0; col < dimX; col++) {
                    sb.append(getValue(row, col) + "\t");
                }
                sb.append("\n");
            }
            return sb.toString();
        }
        return "";
    }

    public final void printInfo() {
        System.out.println(infos.toString());
    }

    @Override
    public int compareTo(Variable var) {
        return this.name.compareToIgnoreCase(var.getName());
    }

    private final Object getStorageObject(Object o) {

        Double doubleValue;

        try {
            doubleValue = Double.parseDouble(o.toString().replace(",", "."));
            int i = doubleValue.intValue();
            if (doubleValue - i != 0) {
                return doubleValue;
            } else if (i <= Byte.MAX_VALUE && i >= Byte.MIN_VALUE) {
                return (byte) i;
            } else if (i <= Short.MAX_VALUE && i >= Short.MIN_VALUE) {
                return (short) i;
            } else {
                return i;
            }
        } catch (Exception e) {
            return o;
        }
    }

    public final float[] getXAxis() {

        float[] xAxis;

        if (dimY > 2) {
            xAxis = new float[dimX - 1];

            for (int x = 1; x < dimX; x++) {
                xAxis[x - 1] = Float.parseFloat(getValue(0, x).toString());
            }
        } else {
            xAxis = new float[dimX];

            for (int x = 0; x < dimX; x++) {
                xAxis[x] = Float.parseFloat(getValue(0, x).toString());
            }
        }

        return xAxis;
    }

    public final float[] getYAxis() {
        float[] yAxis = new float[dimY - 1];

        for (int y = 1; y < dimY; y++) {
            yAxis[y - 1] = Float.parseFloat(getValue(y, 0).toString());
        }
        return yAxis;
    }

    public final float[][] getZvalues() {

        float[][] floatValues;

        if (dimY > 2) {
            floatValues = new float[dimY - 1][dimX - 1];
            for (short y = 1; y < dimY; y++) {
                for (short x = 1; x < dimX; x++) {
                    floatValues[y - 1][x - 1] = Float.parseFloat(getValue(y, x).toString());
                }
            }
        } else {
            floatValues = new float[1][dimX];
            for (short x = 0; x < dimX; x++) {
                try {
                    floatValues[0][x] = Float.parseFloat(getValue(1, x).toString());
                } catch (NumberFormatException e) {
                    floatValues[0][x] = Float.NaN;
                }

            }
        }

        return floatValues;
    }

    public final double[][] toDouble2D() {

        double[][] doubleValues = new double[dimY][dimX];

        for (short y = 0; y < dimY; y++) {
            for (short x = 0; x < dimX; x++) {

                if (getValue(y, x) instanceof Number) {
                    doubleValues[y][x] = Double.parseDouble(getValue(y, x).toString());
                } else {
                    if (x * y != 0) {
                        doubleValues[y][x] = Double.NaN;
                    }
                }
            }
        }

        return doubleValues;
    }

}

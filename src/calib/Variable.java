/*
 * Creation : 4 nov. 2020
 */
package calib;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import calib.MdbData.VariableInfo;
import utils.Utilitaire;

public final class Variable implements Comparable<Variable> {

    private final String name;
    private Type type;
    private int dimX;
    private int dimY;
    private Object[] values;
    private VariableInfo infos;

    public Variable(List<String> data, MdbData mdbData) {
        this.name = data.get(0).substring(1, data.get(0).length() - 1);
        this.infos = mdbData.getInfos().get(this.name);

        // System.out.println(this.name);

        build(data);
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type != null ? type : Type.UNKNOWN;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public boolean isTextValue() {
        return this.infos != null ? this.infos.getTypeVar() == 1 : false;
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
                    int nbSemiColon = Utilitaire.countChar(splitEgale[1], SEMICOLON);
                    dimX = nbSemiColon;

                    if (dimX == 1) {
                        type = Type.SCALAIRE;
                    } else {
                        type = Type.ARRAY;
                    }

                    dimY = 1;
                    values = new Object[dimX];

                    int cnt = 0;
                    for (String s : splitEgale[1].split(SEMICOLON)) {
                        if (!s.isEmpty()) {
                            values[cnt] = Utilitaire.getStorageObject(s);
                        }
                        cnt++;
                    }

                    break;
                case BKPTCOL:
                    if (bkptcol != null) {
                        break;
                    }
                    bkptcol = new ArrayList<Object>();
                    for (String s : splitEgale[1].split(SEMICOLON)) {
                        if (!s.isEmpty()) {
                            bkptcol.add(Utilitaire.getStorageObject(s));
                        }
                    }
                    break;
                case LIGNE:
                    line = new ArrayList<Object>();
                    for (String s : splitEgale[1].split(SEMICOLON)) {
                        if (!s.isEmpty()) {
                            line.add(Utilitaire.getStorageObject(s));
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
                            bkptlign.add(Utilitaire.getStorageObject(s));
                        }
                    }
                    break;
                default:

                    if (mapValues == null) {
                        mapValues = new LinkedHashMap<Object, Object>();
                    }
                    mapValues.put(Utilitaire.getStorageObject(splitEgale[0].replace("ligne", "")), splitEgale[1]);

                    break;
                }
            }

        }

        if (bkptcol != null && line != null && mapValues == null) {
            dimX = bkptcol.size();
            dimY = 2;
            type = Type.COURBE;
            values = new Object[dimX * dimY];
            for (int i = 0; i < dimX; i++) {
                setValue(bkptcol.get(i), 0, i);
                setValue(line.get(i), 1, i);
            }
        } else if (mapValues != null && bkptcol != null && bkptlign != null) {
            dimX = bkptcol.size() + 1;
            dimY = bkptlign.size() + 1;

            type = Type.MAP;

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
                        if (splitSemiColon.length > 0) {
                            setValue(Utilitaire.getStorageObject(splitSemiColon[j]), i + 1, j + 1);
                        } else {
                            setValue(Utilitaire.getStorageObject(Float.NaN), i + 1, j + 1);
                        }
                    }
                }
            }

        } else if (mapValues != null && bkptcol == null && bkptlign == null) {

            dimX = Utilitaire.countChar(mapValues.get(new Byte("1")).toString(), SEMICOLON);
            dimY = mapValues.size();

            type = Type.TEXT;

            values = new Object[dimX * dimY];

            int y = 0;
            for (Object s : mapValues.values()) {
                int x = 0;
                splitSemiColon = s.toString().split(SEMICOLON);

                if (splitSemiColon.length > 0) {
                    for (String s_ : splitSemiColon) {
                        if (!s_.isEmpty()) {
                            setValue(Utilitaire.getStorageObject(s_), y, x);
                        }
                        x++;
                    }
                } else {
                    for (int j = 0; j < dimX; j++) {
                        setValue(Utilitaire.getStorageObject(""), y, j);
                    }
                }

                y++;
            }
        }

    }

    public final void printInfo() {
        System.out.println(infos.toString());
    }

    @Override
    public int compareTo(Variable var) {
        return this.name.compareToIgnoreCase(var.getName());
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

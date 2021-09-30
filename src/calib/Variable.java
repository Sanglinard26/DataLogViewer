/*
 * Creation : 4 nov. 2020
 */
package calib;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Observable;

import calib.MdbData.VariableInfo;
import utils.Utilitaire;

public final class Variable extends Observable implements Comparable<Variable> {

    private final String name;
    private Type type;
    private int dimX;
    private int dimY;
    private Object[] values;
    private Object[] newValues;
    private VariableInfo infos;

    private String[] inputsVar;

    public Variable(List<String> data, MdbData mdbData) {
        this.name = data.get(0).substring(1, data.get(0).length() - 1);
        this.infos = mdbData.getInfos().get(this.name);

        // System.out.println(this.name);
        if (this.name.equals("Consigne vanne fresh air")) {
            int zz = 0;
        }

        build(data);

        int nbInput;

        switch (type) {
        case COURBE:
            nbInput = 1;
            break;
        case MAP:
            nbInput = 2;
            break;
        default:
            nbInput = 0;
        }

        inputsVar = new String[nbInput];
    }

    public String getName() {
        return name;
    }

    public Type getType() {
        return type != null ? type : Type.UNKNOWN;
    }

    public final String[] getInputsVar() {
        return inputsVar;
    }

    public final void setInputX(String input) {
        inputsVar[0] = input;
    }

    public final void setInputY(String input) {
        if (inputsVar.length == 2) {
            inputsVar[1] = input;
        }
    }

    @Override
    public String toString() {
        return this.name;
    }

    public boolean isTextValue() {
        return this.infos != null ? this.infos.getTypeVar() == 1 : false;
    }

    public boolean isModified() {

        if (this.newValues != null) {
            for (short y = 0; y < dimY; y++) {
                for (short x = 0; x < dimX; x++) {
                    try {
                        if (Double.parseDouble(getValue(false, y, x).toString()) != Double.parseDouble(getValue(true, y, x).toString())) {
                            return true;
                        }
                    } catch (NumberFormatException nfe) {

                    }

                }
            }
        }
        return false;
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public final double getMax() {
        return this.infos != null ? this.infos.getMax() : Short.MAX_VALUE;
    }

    public final double getMin() {
        return this.infos != null ? this.infos.getMin() : Short.MIN_VALUE;
    }

    public final double getResolution() {
        return this.infos != null ? 1.0 / this.infos.getFactor() : 1.0 / 32768.0;
    }

    public final boolean checkDim() {

        if (infos != null) {
            switch (this.type) {
            case SCALAIRE:
                return this.dimX == infos.getNbBkPtCol();
            case ARRAY:
                return this.dimX == infos.getNbBkPtCol();
            case COURBE:
                return this.dimX == infos.getNbBkPtCol() && this.dimY == infos.getNbBkPtRow() + 2;
            case MAP:
                return this.dimX - 1 == infos.getNbBkPtCol() && this.dimY - 1 == infos.getNbBkPtRow();
            case TEXT:
                return this.dimX == infos.getNbBkPtCol() && this.dimY == infos.getNbBkPtRow();
            default:
                break;
            }
        }

        return true;
    }

    public final Object getValue(boolean modifiedVar, int... coord) {
        int idx = coord[1] + dimX * coord[0];
        if (!modifiedVar) {
            return this.values[idx];
        }

        if (newValues == null) {
            newValues = Arrays.copyOf(values, values.length);
        }

        return this.newValues[idx];

    }

    public final void setValue(boolean newVal, Object value, int... coord) {
        int idx = coord[1] + dimX * coord[0];
        if (!newVal) {
            this.values[idx] = value;
        } else {
            this.newValues[idx] = value;
        }

    }

    public final void backToRefValue() {
        this.newValues = null;
        setChanged();
        notifyObservers();
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

                    if (cnt == 0) {
                        Arrays.fill(values, Double.NaN);
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

                    if (bkptcol.isEmpty()) {
                        nbSemiColon = Utilitaire.countChar(splitEgale[1], SEMICOLON);
                        for (int a = 0; a < nbSemiColon; a++) {
                            bkptcol.add(Double.NaN);
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

                    if (line.isEmpty()) {
                        nbSemiColon = Utilitaire.countChar(splitEgale[1], SEMICOLON);
                        for (int a = 0; a < nbSemiColon; a++) {
                            line.add(Double.NaN);
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
            if (dimX > 0) {
                dimY = 2;
                type = Type.COURBE;
                values = new Object[dimX * dimY];
                for (int i = 0; i < dimX; i++) {
                    setValue(false, bkptcol.get(i), 0, i);
                    setValue(false, line.get(i), 1, i);
                }
            } else { // Cas d'une carto vide

                if (this.infos != null) {
                    dimX = this.infos.getNbBkPtCol() + 1;
                    dimY = this.infos.getNbBkPtRow() + 1;
                } else {
                    dimX = 3;
                    dimY = 3;
                }

                type = Type.MAP;
                values = new Object[dimX * dimY];
                Arrays.fill(values, Double.NaN);
            }

        } else if (mapValues != null && bkptcol != null && bkptlign != null) {
            dimX = bkptcol.size() + 1;
            dimY = bkptlign.size() + 1;

            type = Type.MAP;

            values = new Object[dimX * dimY];

            setValue(false, "Y \\ X", 0, 0);

            for (int i = 1; i < dimX; i++) {
                setValue(false, bkptcol.get(i - 1), 0, i);
            }

            Object keyLigne;
            Object ligne;
            for (int i = 0; i < dimY - 1; i++) {
                keyLigne = bkptlign.get(i);
                setValue(false, keyLigne, i + 1, 0);

                ligne = mapValues.get(keyLigne);
                if (ligne != null) {
                    splitSemiColon = ligne.toString().split(SEMICOLON);
                    for (int j = 0; j < dimX - 1; j++) {
                        if (splitSemiColon.length > 0) {
                            setValue(false, Utilitaire.getStorageObject(splitSemiColon[j]), i + 1, j + 1);
                        } else {
                            setValue(false, Utilitaire.getStorageObject(Float.NaN), i + 1, j + 1);
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
                            setValue(false, Utilitaire.getStorageObject(s_), y, x);
                        }
                        x++;
                    }
                } else {
                    for (int j = 0; j < dimX; j++) {
                        setValue(false, Utilitaire.getStorageObject(""), y, j);
                    }
                }

                y++;
            }
        }

    }

    public final VariableInfo getInfos() {
        return infos;
    }

    @Override
    public int compareTo(Variable var) {
        return this.name.compareToIgnoreCase(var.getName());
    }

    public final Object saturateValue(Object value) {
        if (value instanceof Number) {
            double val = ((Number) value).doubleValue();
            Double saturatedVal = Math.max(Math.min(val, getMax()), getMin());
            return saturatedVal;
        }
        return value;
    }

    public final void saveNewValue(int y, int x, Object newValue) {
        if (newValues == null) {
            newValues = Arrays.copyOf(values, values.length);
        }

        setValue(true, saturateValue(newValue), y, x);
        setChanged();
        notifyObservers();
    }

    public final float[] getXAxis(boolean modifiedVar) {

        float[] xAxis;

        if (dimY > 2) {
            xAxis = new float[dimX - 1];

            for (int x = 1; x < dimX; x++) {
                xAxis[x - 1] = Float.parseFloat(getValue(modifiedVar, 0, x).toString());
            }
        } else {
            xAxis = new float[dimX];

            for (int x = 0; x < dimX; x++) {
                xAxis[x] = Float.parseFloat(getValue(modifiedVar, 0, x).toString());
            }
        }

        return xAxis;
    }

    public final float[] getYAxis(boolean modifiedVar) {
        float[] yAxis = new float[dimY - 1];

        for (int y = 1; y < dimY; y++) {
            yAxis[y - 1] = Float.parseFloat(getValue(modifiedVar, y, 0).toString());
        }
        return yAxis;
    }

    public final float[][] getZvalues(boolean modifiedVar) {

        float[][] floatValues;

        if (dimY > 2) {
            floatValues = new float[dimY - 1][dimX - 1];
            for (short y = 1; y < dimY; y++) {
                for (short x = 1; x < dimX; x++) {
                    floatValues[y - 1][x - 1] = Float.parseFloat(getValue(modifiedVar, y, x).toString());
                }
            }
        } else {
            floatValues = new float[1][dimX];
            for (short x = 0; x < dimX; x++) {
                try {
                    floatValues[0][x] = Float.parseFloat(getValue(modifiedVar, 1, x).toString());
                } catch (NumberFormatException e) {
                    floatValues[0][x] = Float.NaN;
                }

            }
        }

        return floatValues;
    }

    public final double[][] toDouble2D(boolean modifiedVar) {

        double[][] doubleValues = new double[dimY][dimX];

        for (short y = 0; y < dimY; y++) {
            for (short x = 0; x < dimX; x++) {

                if (getValue(modifiedVar, y, x) instanceof Number) {
                    // doubleValues[y][x] = Double.parseDouble(getValue(modifiedVar, y, x).toString());
                    doubleValues[y][x] = ((Number) getValue(modifiedVar, y, x)).doubleValue();
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

/*
 * Creation : 3 nov. 2020
 */
package calib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.Vector;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

public final class MdbData {

    private static final String CARTOS = "Cartos";
    private static final String NOMCARTO = "NomCarto";
    private static final String TYPENAME = "Typename";
    private static final String SOUSTYPE = "Soustype";
    private static final String VARCOL = "Varcol";
    private static final String VARLIGNE = "Varligne";
    private static final String NBBKPTCOL = "NbBkptCol"; // float
    private static final String NBBKPTLGN = "NbBkptLgn"; // float
    private static final String TYPEVAR = "TypeVariable";
    private static final String COLBKPTFACTOR = "ColBkptFactor"; // int
    private static final String ROWBKPTFACTOR = "RowBkptFactor"; // int
    private static final String FACTOR = "Factor"; // int
    private static final String VAL_MAX = "Valeur_max"; //
    private static final String VAL_MIN = "Valeur_mini"; //
    private static final String DETAIL = "Détail"; //

    private final String name;
    private Map<String, VariableInfo> infos;
    private Hashtable<String, Vector<String>> category;

    public MdbData(File mdbFile) {
        this.name = mdbFile.getName().replace(".mdb", "");
        readDatabase(mdbFile);
    }

    public String getName() {
        return name;
    }

    public Map<String, VariableInfo> getInfos() {
        return infos != null ? infos : new HashMap<String, VariableInfo>();
    }

    private final void readDatabase(File mdbFile) {

        try {
            Database db = DatabaseBuilder.open(mdbFile);

            Table tableCartos = db.getTable(CARTOS);

            this.infos = getVariableInfo(tableCartos);
            db.close();
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                System.out.println("Fichier non trouve : " + mdbFile.getAbsolutePath());

            }
        }
    }

    public final Map<String, VariableInfo> getVariableInfo(Table tableCartos) {

        HashMap<String, VariableInfo> listInfos = new HashMap<String, VariableInfo>(tableCartos.getRowCount());

        category = new Hashtable<>();

        for (Row row : tableCartos) {

            String typeName = row.getString(TYPENAME);
            String sousType = row.getString(SOUSTYPE);

            Object v = category.get(typeName);

            if (v == null) {
                category.put(typeName, new Vector<String>());
                v = category.get(typeName);
            }

            if (sousType != null && !((Vector<String>) v).contains(sousType)) {
                ((Vector<String>) v).add(sousType);
            }

            listInfos.put(row.getString(NOMCARTO),
                    new VariableInfo(row.getString(TYPENAME), row.getString(SOUSTYPE), row.getString(VARCOL), row.getString(VARLIGNE),
                            row.getShort(NBBKPTCOL), row.getShort(NBBKPTLGN), row.getInt(COLBKPTFACTOR), row.getDouble(ROWBKPTFACTOR),
                            row.getDouble(FACTOR), row.getByte(TYPEVAR), row.getDouble(VAL_MAX), row.getDouble(VAL_MIN), row.getString(DETAIL)));
        }

        return listInfos;
    }

    public final Hashtable<String, Vector<String>> getCategory() {

        if (this.category != null) {
            for (Vector<String> v : this.category.values()) {
                Collections.sort(v);
            }
        }

        return this.category;
    }

    public class VariableInfo {

        private String typeName;
        private String sousType;
        private String varCol;
        private String varLigne;
        private short nbBkPtCol;
        private short nbBkPtRow;
        private int colBkPtFactor;
        private double rowBkPtFactor;
        private double factor;
        private byte typeVar;
        private double max;
        private double min;
        private String detail;

        public VariableInfo(String typeName, String sousType, String varCol, String varLigne, short nbBkPtCol, short nbBkPtRow, int colBkPtFactor,
                double rowBkPtFactor, double factor, byte typeVar, double max, double min, String detail) {
            this.typeName = typeName;
            this.sousType = sousType;
            this.varCol = varCol;
            this.varLigne = varLigne;
            this.nbBkPtCol = nbBkPtCol;
            this.nbBkPtRow = nbBkPtRow;
            this.colBkPtFactor = colBkPtFactor;
            this.rowBkPtFactor = rowBkPtFactor;
            this.factor = factor;
            this.typeVar = typeVar;
            this.max = max;
            this.min = min;
            this.detail = detail;
        }

        public String getTypeName() {
            return typeName != null ? typeName : "";
        }

        public String getSousType() {
            return sousType != null ? sousType : "";
        }

        public String getVarCol() {
            return varCol;
        }

        public String getVarLigne() {
            return varLigne;
        }

        public short getNbBkPtCol() {
            return nbBkPtCol;
        }

        public short getNbBkPtRow() {
            return nbBkPtRow;
        }

        public int getColBkPtFactor() {
            return colBkPtFactor;
        }

        public double getRowBkPtFactor() {
            return rowBkPtFactor;
        }

        public double getFactor() {
            return factor;
        }

        public byte getTypeVar() {
            return typeVar;
        }

        public double getMax() {
            return max;
        }

        public double getMin() {
            return min;
        }

        public String getDetail() {
            return detail;
        }

        @Override
        public String toString() {
            return "Var=f(" + varCol + "," + varLigne + ")" + "\n\t|_" + typeName + "\n\t\t|_" + sousType;
        }

    }

}

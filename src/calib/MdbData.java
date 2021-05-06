/*
 * Creation : 3 nov. 2020
 */
package calib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
    private static final String TYPEVAR = "TypeVariable";

    private final String name;
    private Map<String, VariableInfo> infos;

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

        for (Row row : tableCartos) {
            listInfos.put(row.getString(NOMCARTO), new VariableInfo(row.getString(TYPENAME), row.getString(SOUSTYPE), row.getString(VARCOL),
                    row.getString(VARLIGNE), row.getByte(TYPEVAR)));
        }

        return listInfos;
    }

    protected class VariableInfo {

        private String typeName;
        private String sousType;
        private String varCol;
        private String varLigne;
        private byte typeVar;

        public VariableInfo(String typeName, String sousType, String varCol, String varLigne, byte typeVar) {
            this.typeName = typeName;
            this.sousType = sousType;
            this.varCol = varCol;
            this.varLigne = varLigne;
            this.typeVar = typeVar;
        }

        public String getTypeName() {
            return typeName;
        }

        public String getSousType() {
            return sousType;
        }

        public String getVarCol() {
            return varCol;
        }

        public String getVarLigne() {
            return varLigne;
        }

        public byte getTypeVar() {
            return typeVar;
        }

        @Override
        public String toString() {
            return "Var=f(" + varCol + "," + varLigne + ")" + "\n\t|_" + typeName + "\n\t\t|_" + sousType;
        }

    }

}

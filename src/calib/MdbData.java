/*
 * Creation : 3 nov. 2020
 */
package calib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
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
    private static final String INTERP_TABLE = "InterpTable";
    private static final String VARCOL = "ColVarAdr";
    private static final String VARLIGNE = "LgnVarAdr";
    private static final String NBBKPTCOL = "NbBkptCol"; // float
    private static final String NBBKPTLGN = "NbBkptLgn"; // float
    private static final String TYPEVAR = "TypeVariable";
    private static final String FACTOR = "Factor"; // int
    private static final String VAL_MAX = "Valeur_max"; //
    private static final String VAL_MIN = "Valeur_mini"; //
    private static final String DETAIL = "Détail"; //

    private static final String DATALOGCFG = "datalogcfg";

    private final String name;
    private Map<String, VariableInfo> infos;
    private Hashtable<String, Vector<String>> category;

    private ConfigDatalogger configDatalogger;

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

    public ConfigDatalogger getConfigDatalogger() {
        return this.configDatalogger;
    }

    private final void readDatabase(File mdbFile) {

        try {
            Database db = DatabaseBuilder.open(mdbFile);

            Table tableCartos = db.getTable(CARTOS);
            this.infos = getVariableInfo(tableCartos);

            Table tableDatalogCfg = db.getTable(DATALOGCFG);
            this.configDatalogger = new ConfigDatalogger(tableDatalogCfg);

            db.close();
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                System.out.println("Fichier non trouve : " + mdbFile.getAbsolutePath());

            }
        }
    }

    @SuppressWarnings("unchecked")
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
                    new VariableInfo(row.getString(TYPENAME), row.getString(SOUSTYPE), row.getBoolean(INTERP_TABLE), row.getInt(VARCOL),
                            row.getInt(VARLIGNE), row.getShort(NBBKPTCOL), row.getShort(NBBKPTLGN), row.getDouble(FACTOR), row.getByte(TYPEVAR),
                            row.getDouble(VAL_MAX), row.getDouble(VAL_MIN), row.getString(DETAIL)));
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
        private boolean interpTable;
        private int varCol;
        private int varLigne;
        private short nbBkPtCol;
        private short nbBkPtRow;
        private double factor;
        private byte typeVar;
        private double max;
        private double min;
        private String detail;

        public VariableInfo(String typeName, String sousType, boolean interpTable, int varCol, int varLigne, short nbBkPtCol, short nbBkPtRow,
                double factor, byte typeVar, double max, double min, String detail) {
            this.typeName = typeName;
            this.sousType = sousType;
            this.interpTable = interpTable;
            this.varCol = varCol;
            this.varLigne = varLigne;
            this.nbBkPtCol = nbBkPtCol;
            this.nbBkPtRow = nbBkPtRow;
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

        public boolean isInterpTable() {
            return this.interpTable;
        }

        public int getVarCol() {
            return varCol;
        }

        public int getVarLigne() {
            return varLigne;
        }

        public short getNbBkPtCol() {
            return nbBkPtCol;
        }

        public short getNbBkPtRow() {
            return nbBkPtRow;
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

    public class ConfigDatalogger {

        private static final String NOM = "Nom";
        private static final String VALEUR = "Valeur";

        private boolean active;
        private boolean cyclage;
        private int freq;
        private int cible;
        private int nbCanaux;

        private int varBegin;
        private double varBeginValue;
        private int varEnd;
        private double varEndValue;

        private List<Integer> canaux;

        public ConfigDatalogger(Table table) {

            canaux = new ArrayList<>();

            for (Row row : table) {
                String nom = row.getString(NOM);
                // double adresse = row.getDouble(ADRESSE);
                double valeur = row.getDouble(VALEUR);

                switch (nom) {
                case "Flag activation datalogger":
                    active = valeur == 1 ? true : false;
                    break;
                case "Config data logger":
                    decodeConfig((int) valeur);
                    break;
                case "Variable déclenchement":
                    varBegin = (int) valeur;
                    break;
                case "valeur de déclenchement":
                    varBeginValue = valeur;
                    break;
                case "Variable d'arrêt":
                    varEnd = (int) valeur;
                    break;
                case "valeur d'arrêt":
                    varEndValue = valeur;
                default:
                    if (nom.startsWith("Canal")) {
                        canaux.add((int) valeur);
                    }
                    break;
                }
            }
        }

        private final void decodeConfig(int mot) {
            String binMot = String.format("%16s", Integer.toBinaryString(mot)).replaceAll(" ", "0");

            cyclage = (mot & 1) == 1 ? true : false;
            freq = (0b0000000000011110 & mot);

            nbCanaux = (0b11111111 & (Integer.parseInt(binMot.substring(0, 8), 2)));

            cible = Integer.parseInt(binMot.substring(8, 10));
        }

        public boolean isActive() {
            return active;
        }

        public boolean isCyclage() {
            return cyclage;
        }

        public int getFreq() {
            return freq;
        }

        public int getCible() {
            return cible;
        }

        public int getNbCanaux() {
            return nbCanaux;
        }

        public int getVarBegin() {
            return varBegin;
        }

        public double getVarBeginValue() {
            return varBeginValue;
        }

        public int getVarEnd() {
            return varEnd;
        }

        public double getVarEndValue() {
            return varEndValue;
        }

        public List<Integer> getCanaux() {
            return canaux;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            sb.append("Config datalogger:");
            sb.append("\n" + "Actif = " + isActive());
            sb.append("\n" + "Cyclage = " + isCyclage());
            sb.append("\n" + "Fréquence = " + getFreq());
            sb.append("\n" + "Cible = " + getCible());
            sb.append("\n" + "Nb canaux = " + nbCanaux);
            sb.append("\n" + "Variable de déclenchement = " + getVarBegin() + " à " + getVarBeginValue());
            sb.append("\n" + "Variable d'arrêt = " + getVarEnd() + " à " + getVarEndValue());
            sb.append("\n" + "Liste des canaux = " + canaux);

            return sb.toString();
        }

    }

}

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
    private static final String ALLOCADR = "Allocadr";
    private static final String COLBKPTADR = "colbkptadr";
    private static final String LGNBKPTADR = "lgnbkptadr";
    private static final String VAL_ADR = "Val_adr";
    private static final String VARCOL = "ColVarAdr";
    private static final String VARLIGNE = "LgnVarAdr";
    private static final String NBBKPTCOL = "NbBkptCol"; // float
    private static final String NBBKPTLGN = "NbBkptLgn"; // float
    private static final String TYPEVAR = "TypeVariable";
    private static final String COLBKPTFACTOR = "ColBkptFactor";
    private static final String ROWBKPTFACTOR = "RowBkptFactor";
    private static final String FACTOR = "Factor"; // int
    private static final String VAL_MAX = "Valeur_max"; //
    private static final String VAL_MIN = "Valeur_mini"; //
    private static final String DETAIL = "Détail"; //

    private static final String DATALOGCFG = "datalogcfg";

    private static final String CONFIG_ECU = "config_ecu";

    private final String name;
    private Map<String, VariableInfo> infos;
    private Hashtable<String, Vector<String>> category;

    private ConfigDatalogger configDatalogger;

    private ConfigEcu configEcu;

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

            Table tableConfigEcu = db.getTable(CONFIG_ECU);
            this.configEcu = new ConfigEcu(tableConfigEcu);

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
                    new VariableInfo(row.getString(TYPENAME), row.getString(SOUSTYPE), row.getBoolean(INTERP_TABLE), row.getInt(ALLOCADR),
                            row.getInt(COLBKPTADR), row.getInt(LGNBKPTADR), row.getInt(VAL_ADR), row.getInt(VARCOL), row.getInt(VARLIGNE),
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

    public static final String AdressDecToHex(int decimalValue) {
        return "0x" + Integer.toHexString(decimalValue).toUpperCase();
    }

    public ConfigEcu getConfigEcu() {
        return configEcu;
    }

    public class VariableInfo {

        private String typeName;
        private String sousType;
        private boolean interpTable;
        private int allocadr;
        private int colbkptadr;
        private int lgnbkptadr;
        private int val_adr;
        private int varCol;
        private int varLigne;
        private short nbBkPtCol;
        private short nbBkPtRow;
        private int colBkptFactor;
        private double rowBkptFactor;
        private double factor;
        private byte typeVar;
        private double max;
        private double min;
        private String detail;

        public VariableInfo(String typeName, String sousType, boolean interpTable, int allocadr, int colbkptadr, int lgnbkptadr, int val_adr,
                int varCol, int varLigne, short nbBkPtCol, short nbBkPtRow, int colBkptFactor, double rowBkptFactor, double factor, byte typeVar,
                double max, double min, String detail) {
            this.typeName = typeName;
            this.sousType = sousType;
            this.interpTable = interpTable;
            this.allocadr = allocadr;
            this.colbkptadr = colbkptadr;
            this.lgnbkptadr = lgnbkptadr;
            this.val_adr = val_adr;
            this.varCol = varCol;
            this.varLigne = varLigne;
            this.nbBkPtCol = nbBkPtCol;
            this.nbBkPtRow = nbBkPtRow;
            this.colBkptFactor = colBkptFactor;
            this.rowBkptFactor = rowBkptFactor;
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
            return factor != 0 ? factor : 1.0;
        }

        public String getFormat() {
            double facConv = 1 / getFactor();
            String sFacConv = String.valueOf(facConv);
            int idxDot = sFacConv.indexOf('.');

            if (idxDot == -1) {
                return "%.0";
            }

            int nbDigit = sFacConv.length() - (idxDot + 1);

            return "%." + nbDigit;
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
            return detail != null ? detail : "";
        }

        @Override
        public String toString() {
            return "Var=f(" + varCol + "," + varLigne + ")" + "\n\t|_" + typeName + "\n\t\t|_" + sousType;
        }

        public int getAllocadr() {
            return allocadr;
        }

        public int getColbkptadr() {
            return colbkptadr;
        }

        public int getLgnbkptadr() {
            return lgnbkptadr;
        }

        public int getVal_adr() {
            return val_adr;
        }

        public int getColBkptFactor() {
            return colBkptFactor;
        }

        public double getRowBkptFactor() {
            return rowBkptFactor;
        }

        public String getType() {

            int nbCol = getNbBkPtCol();
            int nbLigne = getNbBkPtRow();

            if (nbCol == 1) {
                return "VALUE";
            }

            if (nbCol > 1 && nbLigne == 0) {
                if (!interpTable) {
                    return "VAL_BLK";
                }
                return "CURVE";
            }

            if (nbCol > 1 && nbLigne > 1) {
                if (!interpTable) {
                    return "VAL_BLK";
                }
                return "MAP";
            }

            return "UNKNOW";
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

    public class ConfigEcu {

        private static final String NOM = "Nom";
        private static final String ADRESSE = "Adresse";
        private static final String VALEUR = "Valeur";
        private static final String COMMENTAIRE = "Commentaire";

        private List<ParamEcu> paramsEcu;

        public List<ParamEcu> getParamsEcu() {
            return paramsEcu;
        }

        public ConfigEcu(Table tableConfigEcu) {

            this.paramsEcu = new ArrayList<>();

            for (Row row : tableConfigEcu) {
                if (!row.getString(NOM).equals("-")) {
                    this.paramsEcu.add(new ParamEcu(row));
                }
            }
        }

        public class ParamEcu {

            private String nom;
            private int adresse;
            private int valeur;
            private String commentaire;

            public ParamEcu(Row row) {
                this.nom = row.getString(NOM);
                this.adresse = row.getDouble(ADRESSE).intValue();
                this.valeur = row.getDouble(VALEUR).intValue();
                this.commentaire = row.getString(COMMENTAIRE);
            }

            public String getNom() {
                return nom;
            }

            public int getAdresse() {
                return adresse;
            }

            public int getValeur() {
                return valeur;
            }

            public String getCommentaire() {
                return commentaire;
            }

        }
    }

}

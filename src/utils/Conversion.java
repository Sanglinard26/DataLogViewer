/*
 * Creation : 11 avr. 2021
 */
package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import calib.MdbData;
import calib.MdbData.ConfigEcu.ParamEcu;
import calib.MdbData.VariableInfo;

public class Conversion {

    private final static HashMap<String, String> dataType;
    static {
        dataType = new HashMap<String, String>();
        dataType.put("uint16", "UWORD");
        dataType.put("int16", "SWORD");
        dataType.put("sint16", "SWORD");
    }

    public enum DataBit {

        DB(8), DSB(8), DW(16), DSW(16);

        private int nbBits;

        private DataBit(int nbBits) {
            this.nbBits = nbBits;
        }

        public final int getNbBits() {
            return nbBits;
        }

        public final int getAdressOffset() {
            return this.nbBits / 8;
        }

        public static final DataBit getDataBit(String s) {
            switch (s) {
            case "DB":
                return DB;
            case "DSB":
                return DSB;
            case "DW":
                return DW;
            case "DSW":
                return DSW;
            default:
                return null;
            }
        }

    }

    public static final void AppIncToA2l(File[] appIncFile) {

        /*
         * /begin PROJECT
         * /begin HEADER
         * Project description
         * /end HEADER
         * /begin MODULE
         * /begin MOD_PAR
         * Control unit management data
         * /end MOD_PAR
         * /begin MOD_COMMON
         * Module-wide (ECU specific) definitions
         * /end MOD_COMMON
         */

        /*
         * /begin MEASUREMENT ident Name
         * string LongIdentifier
         * datatype Datatype
         * ident Conversion
         * uint Resolution ==> 1
         * float Accuracy ==> 0
         * float LowerLimit ==> Short.MIN
         * float UpperLimit ==> Short.MAX
         * [-> ECU_ADDRESS]
         * /end MEASUREMENT
         */

        /*
         * /begin COMPU_METHOD ident Name ==> CM_identMeasurement
         * string LongIdentifier ==> ""
         * enum ConversionType ==> LINEAR
         * string Format ==> %.5
         * string Unit
         * [-> COEFFS_LINEAR]
         * /end COMPU_METHOD
         */

        final String EQU = "EQU";
        final String DEFR = "DEFR";
        // final Pattern HEX_PATTERN = Pattern.compile("\\b(\\p{XDigit}+h)\\b"); // Pattern.compile("\\b(\\p{XDigit}+h)\\b");
        // final Pattern HEX_PATTERN_0x = Pattern.compile("(0x+\\p{XDigit}+)");
        final Pattern HEX_PATTERN_Final = Pattern.compile("\\b(\\p{XDigit}+h|(0x+\\p{XDigit}+))\\b");
        final Pattern WORD_PATTERN = Pattern.compile("\\b\\w+\\b");
        final String SECTION_DATA = "SECTION DATA";
        // final String AT = "AT";
        final String ENDS = "ENDS";
        final String RAM_EXT = ";ramsav SECTION HDAT AT 100002h";

        final int minAdress = 0;
        final int maxAdress = 65534;

        List<Variable> listVar = new ArrayList<Variable>();
        Map<Variable, String> mappingAdress = new HashMap<>();

        for (int nFile = 0; nFile < appIncFile.length - 1; nFile++) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(appIncFile[nFile]), Charset.forName("ISO-8859-1")))) { // ISO-8859-1

                String line;

                Variable variable;

                String variableName;
                String adress = null;
                String infos;
                HashMap<String, String> varInfos;

                while ((line = br.readLine()) != null && !line.contains(RAM_EXT)) {

                    int idxEQU = line.toUpperCase().lastIndexOf(EQU);
                    int idxSectionData = line.toUpperCase().lastIndexOf(SECTION_DATA);
                    int idxDEFR = line.toUpperCase().lastIndexOf(DEFR);

                    boolean condition;

                    if (nFile == 0) {
                        condition = !line.trim().startsWith("PUBLIC") && idxEQU > -1;
                    } else {
                        condition = idxDEFR > -1 || idxEQU > -1;
                    }

                    if (condition) {

                        if (line.trim().charAt(0) == ';' || line.contains("hidden")) {
                            continue;
                        }

                        if (nFile == 0) {
                            variableName = line.substring(0, idxEQU - 1).trim();
                        } else {
                            variableName = line.substring(0, Math.max(idxEQU, idxDEFR) - 1).trim();
                        }

                        // System.out.println(variableName);

                        if (!mappingAdress.containsKey(new Variable(variableName))) {
                            variable = new Variable(variableName);
                        } else {
                            System.out.println(variableName + " déjà présente!");
                            variable = new Variable(variableName + "_2");
                        }

                        varInfos = variable.getVarInfos();
                        listVar.add(variable);

                        Matcher matcher = HEX_PATTERN_Final.matcher(line);

                        int endAdress = -1;

                        if (matcher.find()) {
                            adress = matcher.group();
                            if (!adress.startsWith("0x")) {
                                adress = "0x" + adress.replace("h", "").toUpperCase();
                            }
                            variable.setAdress(adress);
                            endAdress = matcher.end();
                        } else {
                            matcher = WORD_PATTERN.matcher(line);

                            String prevWord = null;

                            while (matcher.find()) {
                                adress = matcher.group();
                                if (EQU.equals(prevWord)) {
                                    break;
                                }
                                prevWord = adress;
                            }

                            variable.setAdress(adress);
                            endAdress = matcher.end();
                        }

                        mappingAdress.put(variable, adress);

                        int idxStartInfo;
                        if (nFile == 0) {
                            idxStartInfo = endAdress > -1 ? endAdress : idxEQU + 3;
                        } else {
                            idxStartInfo = endAdress > -1 ? endAdress : Math.max(idxEQU, idxDEFR) + 3;
                        }

                        int idxSemiCol = line.indexOf(';', idxStartInfo);

                        if (idxSemiCol > -1) {
                            infos = line.substring(idxSemiCol);

                            for (String s : varInfos.keySet()) {
                                int idx = infos.indexOf(s);
                                if (idx > -1) {
                                    int idx2 = infos.indexOf(';', idx);
                                    int lastIdx = infos.lastIndexOf(';');
                                    if (idx2 > -1) {
                                        varInfos.put(s, infos.substring(idx + s.length(), idx2));
                                    } else if (idx == lastIdx + 1) {
                                        varInfos.put(s, infos.substring(idx + s.length(), infos.length()));
                                    }
                                }
                            }
                        }
                    } else if (idxSectionData > -1) {

                        final Matcher matcher = HEX_PATTERN_Final.matcher(line);

                        int decimalAdress = 0;

                        if (matcher.find()) {
                            adress = matcher.group();
                            adress = adress.substring(1, adress.length() - 1);

                            decimalAdress = Integer.parseInt(adress, 16);
                        }

                        line = br.readLine().trim();

                        int loop = 0;
                        do {
                            String[] splitSpace = line.split("\t");
                            variableName = splitSpace[0].trim();

                            if (!mappingAdress.containsKey(new Variable(variableName))) {
                                variable = new Variable(variableName);
                            } else {
                                System.out.println(variableName + " déjà présente!");
                                variable = new Variable(variableName + "_2");
                            }

                            varInfos = variable.getVarInfos();
                            listVar.add(variable);

                            int cnt = 0;

                            do {
                                cnt++;
                            } while (splitSpace[cnt].isEmpty());

                            int offset;

                            if (loop == 0) {
                                offset = 0;
                            } else {
                                offset = DataBit.getDataBit(splitSpace[cnt].trim()).getAdressOffset();
                            }

                            decimalAdress = decimalAdress + offset;
                            adress = "0x" + Integer.toHexString(decimalAdress).toUpperCase();

                            variable.setAdress(adress);

                            mappingAdress.put(variable, adress);

                            int idxSemiCol = line.indexOf(';');

                            if (idxSemiCol > -1) {
                                infos = line.substring(idxSemiCol);

                                for (String s : varInfos.keySet()) {
                                    int idx = infos.indexOf(s);
                                    if (idx > -1) {
                                        int idx2 = infos.indexOf(';', idx);
                                        int lastIdx = infos.lastIndexOf(';');
                                        if (idx2 > -1) {
                                            varInfos.put(s, infos.substring(idx + s.length(), idx2));
                                        } else if (idx == lastIdx + 1) {
                                            varInfos.put(s, infos.substring(idx + s.length(), infos.length()));
                                        }
                                    }
                                }
                            }
                            loop++;
                        } while (!(line = br.readLine().trim()).contains(ENDS));
                    }

                }

            } catch (FileNotFoundException e) {
            } catch (IOException e) {
            }
        }

        // Traitement des adresses (min/max) et suppression des variables en doublons pour une même adresse
        for (Entry<Variable, String> entry : mappingAdress.entrySet()) {
            if (!entry.getValue().startsWith("0x")) {
                String newAdress = mappingAdress.get(new Variable(entry.getValue()));
                entry.getKey().setAdress(newAdress);
                listVar.remove(new Variable(entry.getValue()));
            } else {
                int intAdress = Integer.parseInt(entry.getValue().replace("0x", ""), 16);
                if (intAdress < minAdress || intAdress > maxAdress) {
                    listVar.remove(entry.getKey());
                }
            }

        }

        boolean mdb = true;
        MdbData mdbData;
        Map<String, VariableInfo> varInfo;
        List<ParamEcu> paramsEcu;
        String fileNameMdb = "";
        // Mdb
        if (mdb) {
            mdbData = new MdbData(appIncFile[2]);
            fileNameMdb = mdbData.getName() + "_";
            varInfo = mdbData.getInfos();
            paramsEcu = mdbData.getConfigEcu().getParamsEcu();
        } else {
            varInfo = Collections.emptyMap();
            paramsEcu = Collections.emptyList();
        }

        //

        // Ecriture A2l
        // try (BufferedWriter bw = new BufferedWriter(new FileWriter(appIncFile[0].getAbsolutePath().replace(".inc", ".a2l")))) {
        // bw.write("ASAP2_VERSION 1 60\n");
        // bw.write(writeProject("BGM_Project", "\"Project description\"", listVar));

        // } catch (IOException e) {
        // e.printStackTrace();
        // }
        // ************

        // Ecriture A2l

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("ddMMyyyy");
        String dateStr = simpleDateFormat.format(new Date());
        String a2lFileName = appIncFile[0].getParent() + File.separator + dateStr + "_" + fileNameMdb
                + appIncFile[0].getName().replace(".inc", ".a2l");

        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(a2lFileName), StandardCharsets.ISO_8859_1));) {
            bw.write("ASAP2_VERSION 1 60\n");
            bw.write(writeProject("BGM_Project", "\"Project description\"", listVar, varInfo, paramsEcu));

        } catch (IOException e) {
            e.printStackTrace();
        }
        // ************

    }

    private static final String writeProject(String name, String description, List<Variable> listVar, Map<String, VariableInfo> varInfo,
            List<ParamEcu> paramsEcu) {

        StringBuilder sb = new StringBuilder("/begin PROJECT ");

        sb.append(name);
        sb.append("\n" + description + "\n\n");

        sb.append(writeHeader("\"Header description\""));

        sb.append(writeModule("ECU_Variables", "\"Module description\"", listVar, varInfo, paramsEcu));

        sb.append("/end PROJECT");

        return sb.toString();

    }

    private static final String writeHeader(String description) {

        StringBuilder sb = new StringBuilder("/begin HEADER ");

        sb.append(description + "\n");

        sb.append("/end HEADER\n\n");

        return sb.toString();

    }

    private static final String writeModule(String name, String description, List<Variable> listVar, Map<String, VariableInfo> varInfo,
            List<ParamEcu> paramsEcu) {

        final Set<String> listClasses = new HashSet<String>();

        StringBuilder sb = new StringBuilder("/begin MODULE ");

        sb.append(name);
        sb.append("\n" + description + "\n\n");

        sb.append(writeModPar("\"MOD_PAR description\""));

        for (Variable var : listVar) {
            sb.append(writeMeasurement(var.getName(), var.getComment(), var.getDataType(), "CM_" + var.getName(), var.getRangeMin(),
                    var.getRangeMax(), var.getDisplayName(), var.getAdress(), var.getDisplayBase()));
            sb.append("\n\n");

            sb.append(writeCompuMethod("CM_" + var.getName(), var.getDisplayFormat(), var.getUnit(), var.getConversion() + " 0"));
            sb.append("\n\n");

            listClasses.add(var.getGroup());
        }

        for (String classe : listClasses) {
            sb.append(writeGroup(classe, "\"Group description\"", listVar));
        }

        for (Entry<String, VariableInfo> entry : varInfo.entrySet()) {
            sb.append(writeCharacteristic(entry));

            sb.append(writeCompuMethod(entry));
            sb.append("\n\n");
        }

        for (ParamEcu param : paramsEcu) {
            sb.append(writeCharacteristic(param));

            sb.append(writeCompuMethod("CM_" + param.getNom(), "%.0", "-", "1.0 0"));
            sb.append("\n\n");
        }

        sb.append("/end MODULE\n\n");

        return sb.toString();

    }

    private static final String writeModPar(String description) {
        StringBuilder sb = new StringBuilder("/begin MOD_PAR ");

        sb.append(description + "\n");

        sb.append("EPK" + " " + "\"Cdfx_Name\"" + "\n");

        sb.append("USER" + " " + "\"Workspace_Name\"" + "\n");

        sb.append("/end MOD_PAR\n\n");

        return sb.toString();
    }

    private static final String writeMeasurement(String name, String comment, String dataType, String conversion, String min, String max,
            String displayIdentifier, String adress, String base) {

        StringBuilder sb = new StringBuilder("/begin MEASUREMENT ");

        sb.append(name + "\n");
        sb.append("\"" + comment + "\"" + "\n");
        sb.append(Conversion.dataType.get(dataType) + "\n");
        sb.append(conversion + "\n");
        sb.append("1" + "\n");
        sb.append("0" + "\n");
        sb.append(min + "\n");
        sb.append(max + "\n");
        sb.append("DISPLAY_IDENTIFIER " + displayIdentifier + "\n");
        sb.append("ECU_ADDRESS " + adress + "\n");
        sb.append(writeAnnotation(base) + "\n");

        sb.append("/end MEASUREMENT");

        return sb.toString();

    }

    private static final String writeAnnotation(String base) {

        StringBuilder sb = new StringBuilder("/begin ANNOTATION\n");

        sb.append("ANNOTATION_LABEL " + "\"" + base + "\"" + "\n");

        sb.append("/end ANNOTATION");

        return sb.toString();
    }

    private static String writeCompuMethod(String name, String format, String unit, String coeff) {

        StringBuilder sb = new StringBuilder("/begin COMPU_METHOD ");

        sb.append(name + "\n");
        sb.append("\"\"" + "\n");
        sb.append("LINEAR" + "\n");
        sb.append("\"" + format + "\"" + "\n");
        sb.append("\"" + unit + "\"" + "\n");
        sb.append("COEFFS_LINEAR " + coeff + "\n");

        sb.append("/end COMPU_METHOD");

        return sb.toString();

    }

    private static final String writeGroup(String name, String description, List<Variable> listVar) {

        StringBuilder sb = new StringBuilder("/begin GROUP ");

        sb.append(name);
        sb.append("\n" + description + "\n");
        sb.append("ROOT\n");

        sb.append("/begin REF_MEASUREMENT\n");
        for (Variable var : listVar) {
            if (var.getGroup().equals(name)) {
                sb.append(var.getName() + "\n");
            }
        }
        sb.append("/end REF_MEASUREMENT\n");
        sb.append("/end GROUP\n\n");

        return sb.toString();

    }

    private static final String writeCharacteristic(Entry<String, VariableInfo> varInfo) {
        /// begin CHARACTERISTIC ident Name (NomCarto)
        // string LongIdentifier
        // enum Type (ASCII, CURVE, MAP, VAL_BLK, VALUE)
        // ulong Address ()
        // ident Deposit ()
        // float MaxDiff (0)
        // ident Conversion ()
        // float LowerLimit (Valeur_mini)
        // float UpperLimit (Valeur_max)
        // [-> ANNOTATION]*
        // [-> AXIS_DESCR]*
        // [-> FORMAT]
        /// end CHARACTERISTIC

        StringBuilder sb = new StringBuilder("/begin CHARACTERISTIC ");

        sb.append(varInfo.getKey() + "\n");
        sb.append("\"" + varInfo.getValue().getDetail() + "\"" + "\n");
        sb.append(varInfo.getValue().getType() + "\n");
        sb.append(MdbData.AdressDecToHex(varInfo.getValue().getVal_adr()) + "\n");
        sb.append("RL_" + varInfo.getKey() + "\n");
        sb.append("0" + "\n");
        sb.append("CM_" + varInfo.getKey() + "\n");
        sb.append(varInfo.getValue().getMin() + "\n");
        sb.append(varInfo.getValue().getMax() + "\n");
        sb.append("ECU_ADDRESS_EXTENSION " + MdbData.AdressDecToHex(varInfo.getValue().getAllocadr()) + "\n");

        switch (varInfo.getValue().getType()) {
        case "VALUE":
            break;
        case "CURVE":
            sb.append("\n");
            sb.append(writeAxisDescr(varInfo, (byte) 1));
            sb.append("\n\n");
            break;
        case "MAP":
            sb.append("\n");
            sb.append(writeAxisDescr(varInfo, (byte) 1));
            sb.append("\n\n");
            sb.append(writeAxisDescr(varInfo, (byte) 2));
            sb.append("\n\n");
            break;
        }

        sb.append("/end CHARACTERISTIC\n\n");

        switch (varInfo.getValue().getType()) {
        case "VALUE":
            break;
        case "CURVE":
            sb.append(writeCompuMethod("CM_AXIS_1_" + varInfo.getKey(), "%.8", "-", String.valueOf(1.0F / varInfo.getValue().getColBkptFactor())));
            sb.append("\n\n");
            break;
        case "MAP":
            sb.append(writeCompuMethod("CM_AXIS_1_" + varInfo.getKey(), "%.8", "-", String.valueOf(1.0F / varInfo.getValue().getColBkptFactor())));
            sb.append("\n\n");
            sb.append(writeCompuMethod("CM_AXIS_2_" + varInfo.getKey(), "%.8", "-", String.valueOf(1.0F / varInfo.getValue().getRowBkptFactor())));
            sb.append("\n\n");
            break;
        }

        return sb.toString();
    }

    private static final String writeCharacteristic(ParamEcu param) {
        /// begin CHARACTERISTIC ident Name (NomCarto)
        // string LongIdentifier
        // enum Type (ASCII, CURVE, MAP, VAL_BLK, VALUE)
        // ulong Address ()
        // ident Deposit ()
        // float MaxDiff (0)
        // ident Conversion ()
        // float LowerLimit (Valeur_mini)
        // float UpperLimit (Valeur_max)
        // [-> ANNOTATION]*
        // [-> AXIS_DESCR]*
        // [-> FORMAT]
        /// end CHARACTERISTIC

        StringBuilder sb = new StringBuilder("/begin CHARACTERISTIC ");

        sb.append(param.getNom() + "\n");
        sb.append("\"" + param.getCommentaire() + "\"" + "\n");
        sb.append("VALUE" + "\n");
        sb.append(MdbData.AdressDecToHex(param.getAdresse()) + "\n");
        sb.append("RL_" + param.getNom() + "\n");
        sb.append("0" + "\n");
        sb.append("CM_" + param.getNom() + "\n");
        sb.append("0" + "\n");
        sb.append("65535" + "\n");
        sb.append("/end CHARACTERISTIC\n\n");

        return sb.toString();
    }

    private static final String writeAxisDescr(Entry<String, VariableInfo> varInfo, byte axisNum) {
        /// begin AXIS_DESCR enum Attribute ()
        // ident InputQuantity ()
        // ident Conversion ()
        // uint MaxAxisPoints ()
        // float LowerLimit ()
        // float UpperLimit ()
        // [-> ANNOTATION]*
        // [-> FORMAT]
        /// end AXIS_DESCR

        StringBuilder sb = new StringBuilder("/begin AXIS_DESCR ");
        sb.append("STD_AXIS\n");
        switch (axisNum) {
        case 1:
            sb.append(MdbData.AdressDecToHex(varInfo.getValue().getColbkptadr()) + "\n");
            sb.append("CM_AXIS_1_" + varInfo.getKey() + "\n");
            sb.append(varInfo.getValue().getNbBkPtCol() + "\n");
            sb.append("min\n");
            sb.append("max\n");
            break;
        case 2:
            sb.append(MdbData.AdressDecToHex(varInfo.getValue().getLgnbkptadr()) + "\n");
            sb.append("CM_AXIS_2_" + varInfo.getKey() + "\n");
            sb.append(varInfo.getValue().getNbBkPtRow() + "\n");
            sb.append("min\n");
            sb.append("max\n");
            break;
        }

        sb.append("/end AXIS_DESCR");

        return sb.toString();
    }

    private static String writeCompuMethod(Entry<String, VariableInfo> varInfo) {

        StringBuilder sb = new StringBuilder("/begin COMPU_METHOD ");

        sb.append("CM_" + varInfo.getKey() + "\n");
        sb.append("\"\"" + "\n");
        sb.append("LINEAR" + "\n");
        sb.append("\"" + varInfo.getValue().getFormat() + "\"" + "\n");
        sb.append("\"" + "-" + "\"" + "\n");
        sb.append("COEFFS_LINEAR " + (1.0F / varInfo.getValue().getFactor()) + " 0" + "\n");

        sb.append("/end COMPU_METHOD");

        return sb.toString();

    }

    protected static class Variable {

        private String name;
        private String adress;

        final String NOM_IHM = "[NOM_IHM]";
        final String COMMENT = "[COMMENT]";
        final String DATATYPE = "[FMT_RAW]";
        final String CONVERSION = "[CONVRAW2PHYS]";
        final String UNIT = "[UNIT_PHYS]";
        final String CLASS = "[CLASS]";
        final String RANGE_MIN = "[RANGE_MIN]";
        final String RANGE_MAX = "[RANGE_MAX]";
        final String DISPLAY_FORMAT = "[DISPFMT]";

        private final HashMap<String, String> varInfos = new HashMap<>(9);

        public Variable(String name) {
            this.name = name;
            varInfos.put(NOM_IHM, null);
            varInfos.put(COMMENT, null);
            varInfos.put(DATATYPE, null);
            varInfos.put(CONVERSION, null);
            varInfos.put(UNIT, null);
            varInfos.put(CLASS, null);
            varInfos.put(RANGE_MIN, null);
            varInfos.put(RANGE_MAX, null);
            varInfos.put(DISPLAY_FORMAT, null);
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            if (varInfos.get(NOM_IHM) != null && !varInfos.get(NOM_IHM).isEmpty()) {
                return varInfos.get(NOM_IHM);
            }
            return this.name;
        }

        public void setAdress(String adress) {
            this.adress = adress;
        }

        public String getAdress() {
            return adress != null ? adress : "0xFFFE";
        }

        public HashMap<String, String> getVarInfos() {
            return varInfos;
        }

        public String getComment() {
            return varInfos.get(COMMENT) != null ? varInfos.get(COMMENT) : "NON_DEFINI";
        }

        public String getDataType() {
            return varInfos.get(DATATYPE) != null ? varInfos.get(DATATYPE) : "sint16";
        }

        public String getUnit() {
            return varInfos.get(UNIT) != null ? varInfos.get(UNIT) : "su";
        }

        public String getConversion() {
            return varInfos.get(CONVERSION) != null ? varInfos.get(CONVERSION).replace(',', '.') : "1";
        }

        public String getGroup() {
            return varInfos.get(CLASS) != null ? varInfos.get(CLASS) : "NON_DEFINI";
        }

        public String getRangeMin() {
            if ("sint16".equals(getDataType()) || "int16".equals(getDataType())) {
                return varInfos.get(RANGE_MIN) != null ? varInfos.get(RANGE_MIN).replace(',', '.') : String.valueOf(Short.MIN_VALUE);
            }
            return varInfos.get(RANGE_MIN) != null ? varInfos.get(RANGE_MIN).replace(',', '.') : "0";

        }

        public String getRangeMax() {
            if ("sint16".equals(getDataType()) || "int16".equals(getDataType())) {
                return varInfos.get(RANGE_MAX) != null ? varInfos.get(RANGE_MAX).replace(',', '.') : String.valueOf(Short.MAX_VALUE);
            }
            return varInfos.get(RANGE_MAX) != null ? varInfos.get(RANGE_MAX).replace(',', '.') : "65535";
        }

        public String getDisplayFormat() {
            String dispFmt = varInfos.get(DISPLAY_FORMAT) != null ? varInfos.get(DISPLAY_FORMAT) : "%5.0";

            int idx = dispFmt.indexOf('%');

            if (idx == 1) {
                return dispFmt.substring(idx);
            }

            return "%5.0";
        }

        public String getDisplayBase() {
            String dispFmt = varInfos.get(DISPLAY_FORMAT) != null ? varInfos.get(DISPLAY_FORMAT) : "%10.10";

            int idx = dispFmt.indexOf('%');

            if (idx == 1) {
                switch (dispFmt.charAt(idx - 1)) {
                case 'd':
                    return "DEC";
                case 'h':
                    return "HEX";
                case 'b':
                    return "BIN";
                default:
                    return "DEC";
                }
            }

            return "DEC";
        }

        @Override
        public String toString() {
            return this.name;
        }

        @Override
        public int hashCode() {
            return this.name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            return this.name.equals(obj.toString());
        }

    }

    protected static class Characteristic {

    }

}

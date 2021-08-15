/*
 * Creation : 11 avr. 2021
 */
package utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Conversion {

    private final static HashMap<String, String> dataType;
    static {
        dataType = new HashMap<String, String>();
        dataType.put("uint16", "UWORD");
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

    public static final void AppIncToA2l(File appIncFile) {

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
        final Pattern HEX_PATTERN = Pattern.compile("\\b(\\p{XDigit}+h)\\b");
        final String SECTION_DATA = "SECTION DATA";
        final String AT = "AT";
        final String ENDS = "ENDS";

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(appIncFile), Charset.forName("ISO-8859-1")))) {

            String line;

            Variable variable;
            List<Variable> listVar = new ArrayList<Variable>();
            String variableName;
            String adress = null;
            String infos;
            HashMap<String, String> varInfos;

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(appIncFile.getAbsolutePath().replace(".inc", ".a2l")))) {

                while ((line = br.readLine()) != null) {

                    int idxEQU = line.toUpperCase().lastIndexOf(EQU);
                    int idxSectionData = line.toUpperCase().lastIndexOf(SECTION_DATA);

                    if (!line.trim().startsWith("PUBLIC") && idxEQU > -1) {

                        if (line.trim().charAt(0) == ';' || line.contains("hidden")) {
                            continue;
                        }

                        if (line.contains("adcres15")) {
                            int zz = 0;
                        }

                        variableName = line.substring(0, idxEQU - 1).trim();

                        variable = new Variable(variableName);
                        varInfos = variable.getVarInfos();
                        listVar.add(variable);

                        final Matcher matcher = HEX_PATTERN.matcher(line);

                        int endAdress = -1;

                        if (matcher.find()) {
                            adress = matcher.group();
                            adress = "0x" + adress.substring(1, adress.length() - 1);
                            variable.setAdress(adress);
                            endAdress = matcher.end();
                        }

                        int idxStartInfo = endAdress > -1 ? endAdress : idxEQU + 3;

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

                        final Matcher matcher = HEX_PATTERN.matcher(line);

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

                            variable = new Variable(variableName);
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

                bw.write("ASAP2_VERSION 1 51\n");
                bw.write(writeProject("BGM_Project", "\"Project description\"", listVar));

            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static final String writeProject(String name, String description, List<Variable> listVar) {

        StringBuilder sb = new StringBuilder("/begin PROJECT ");

        sb.append(name);
        sb.append("\n" + description + "\n\n");

        sb.append(writeHeader("\"Header description\""));

        sb.append(writeModule("ECU_Variables", "\"Module description\"", listVar));

        sb.append("/end PROJECT");

        return sb.toString();

    }

    private static final String writeHeader(String description) {

        StringBuilder sb = new StringBuilder("/begin HEADER ");

        sb.append(description + "\n");

        sb.append("/end HEADER\n\n");

        return sb.toString();

    }

    private static final String writeModule(String name, String description, List<Variable> listVar) {

        final Set<String> listClasses = new HashSet<String>();

        StringBuilder sb = new StringBuilder("/begin MODULE ");

        sb.append(name);
        sb.append("\n" + description + "\n\n");

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

        sb.append("/end MODULE\n\n");

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
            return adress != null ? adress : "0xFFFF";
        }

        public HashMap<String, String> getVarInfos() {
            return varInfos;
        }

        public String getComment() {
            return varInfos.get(COMMENT) != null ? varInfos.get(COMMENT) : "...";
        }

        public String getDataType() {
            return varInfos.get(DATATYPE) != null ? varInfos.get(DATATYPE) : "uint16";
        }

        public String getUnit() {
            return varInfos.get(UNIT) != null ? varInfos.get(UNIT) : "...";
        }

        public String getConversion() {
            return varInfos.get(CONVERSION) != null ? varInfos.get(CONVERSION).replace(',', '.') : "0";
        }

        public String getGroup() {
            return varInfos.get(CLASS) != null ? varInfos.get(CLASS) : "...";
        }

        public String getRangeMin() {
            return varInfos.get(RANGE_MIN) != null ? varInfos.get(RANGE_MIN).replace(',', '.') : String.valueOf(Short.MIN_VALUE);
        }

        public String getRangeMax() {
            return varInfos.get(RANGE_MAX) != null ? varInfos.get(RANGE_MAX).replace(',', '.') : String.valueOf(Short.MAX_VALUE);
        }

        public String getDisplayFormat() {
            String dispFmt = varInfos.get(DISPLAY_FORMAT) != null ? varInfos.get(DISPLAY_FORMAT) : "%10.10";

            int idx = dispFmt.indexOf('%');

            if (idx == 1) {
                return dispFmt.substring(idx);
            }

            return "%10.10";
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

    }

}

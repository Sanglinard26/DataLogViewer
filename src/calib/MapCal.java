/*
 * Creation : 30 oct. 2020
 */
package calib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import calib.MdbWorkspace.VariableECU;
import jxl.Workbook;
import jxl.format.Alignment;
import jxl.format.Border;
import jxl.format.BorderLineStyle;
import jxl.format.Colour;
import jxl.format.VerticalAlignment;
import jxl.write.Label;
import jxl.write.Number;
import jxl.write.WritableCellFormat;
import jxl.write.WritableFont;
import jxl.write.WritableSheet;
import jxl.write.WritableWorkbook;
import jxl.write.WriteException;
import jxl.write.biff.RowsExceededException;

public final class MapCal {

    private final String name;
    private final List<Variable> listVariable;
    private MdbData mdbData;
    private boolean usedByFormula = false;
    private boolean hasWorkspace = false;

    public static enum ComparResult {
        RESULT_OK, NO_DIFFERENCE, ERROR
    }

    public MapCal(File mapFile) {
        this.name = mapFile.getName().replace(".map", "");
        this.listVariable = new ArrayList<Variable>();
        mdbData = new MdbData(new File(mapFile.getAbsolutePath().replace(".map", ".mdb")));
        parseFile(mapFile);
    }

    private final void parseFile(File mapFile) {

        final char crochet = '[';

        try {
            List<String> fileToList = Files.readAllLines(mapFile.toPath(), Charset.forName("ISO-8859-1"));
            String line;

            Variable variable;

            for (int nLigne = 0; nLigne < fileToList.size(); nLigne++) {
                line = fileToList.get(nLigne);

                if (line.charAt(0) == crochet) {
                    final int begin = nLigne;

                    do {
                        nLigne++;
                        if (nLigne < fileToList.size() && !fileToList.get(nLigne).isEmpty() && fileToList.get(nLigne).charAt(0) == crochet) {
                            break;
                        }

                    } while (nLigne < fileToList.size());

                    variable = new Variable(fileToList.subList(begin, nLigne), mdbData);

                    if (mdbData.getInfos().isEmpty() || variable.getInfos() != null) {
                        listVariable.add(variable);
                    }

                    nLigne--;
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public MdbData getMdbData() {
        return mdbData;
    }

    public boolean isUsedByFormula() {
        return usedByFormula;
    }

    public void setUsedByFormula(boolean usedByFormula) {
        this.usedByFormula = usedByFormula;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public final boolean hasWorkspaceLinked() {
        return hasWorkspace;
    }

    public List<Variable> getListVariable() {
        Collections.sort(listVariable);
        return listVariable;
    }

    public final Variable getVariable(String name) {
        for (Variable var : listVariable) {
            if (name.equals(var.getName())) {
                return var;
            }
        }
        return null;
    }

    public final boolean associateWorksplace(Map<String, VariableECU> variablesECU) {

        boolean result = false;

        Set<Entry<String, VariableECU>> entries = variablesECU.entrySet();
        for (Variable var : listVariable) {
            if (var.getInfos() != null) {
                for (Entry<String, VariableECU> entry : entries) {
                    VariableECU variableECU = entry.getValue();
                    if (var.getInfos().getVarCol() == variableECU.getOffset()) {
                        var.setInputX(entry.getKey().toString());
                        result = true;
                    } else if (var.getInfos().getVarLigne() == variableECU.getOffset()) {
                        var.setInputY(entry.getKey().toString());
                        result = true;
                    }
                }
            }
        }

        if (result) {
            hasWorkspace = true;
        } else {
            hasWorkspace = false;
        }

        return result;
    }

    public static final boolean toCdfx(List<Variable> listVariable, final File file) {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            doc.setXmlStandalone(true);

            DOMImplementation domImpl = doc.getImplementation();
            DocumentType doctype = domImpl.createDocumentType("MSRSW", "-//ASAM//DTD CALIBRATION DATA FORMAT:V2.1:LAI:IAI:XML //EN",
                    "cdf_v2.1.0.sl.dtd");
            doc.appendChild(doctype);

            Element racine = doc.createElement("MSRSW");
            racine.setAttribute("CREATOR-VERSION", "V1.0");
            racine.setAttribute("CREATOR", "DataLogViewer");
            doc.appendChild(racine);

            Element shortName = doc.createElement("SHORT-NAME");
            shortName.appendChild(doc.createTextNode("TEST_CDFX"));
            racine.appendChild(shortName);

            Element category = doc.createElement("CATEGORY");
            category.appendChild(doc.createTextNode("CDF21"));
            racine.appendChild(category);

            // <SW-SYSTEMS>
            Element swSystems = doc.createElement("SW-SYSTEMS");
            racine.appendChild(swSystems);

            Element swSystem = doc.createElement("SW-SYSTEM");
            swSystems.appendChild(swSystem);

            shortName = doc.createElement("SHORT-NAME");
            shortName.appendChild(doc.createTextNode("BGM_ECU"));
            swSystem.appendChild(shortName);

            // <SW-INSTANCE-SPEC>
            Element swInstanceSpec = doc.createElement("SW-INSTANCE-SPEC");
            swSystem.appendChild(swInstanceSpec);

            // <SW-INSTANCE-TREE>
            Element swInstanceTree = doc.createElement("SW-INSTANCE-TREE");
            swInstanceSpec.appendChild(swInstanceTree);

            shortName = doc.createElement("SHORT-NAME");
            shortName.appendChild(doc.createTextNode("..."));
            swInstanceTree.appendChild(shortName);

            category = doc.createElement("CATEGORY");
            category.appendChild(doc.createTextNode("NO_VCD"));
            swInstanceTree.appendChild(category);

            // Input
            Date date = new Date(System.currentTimeMillis());

            // Conversion
            SimpleDateFormat sdf;
            sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            String text = sdf.format(date);

            for (Variable var : listVariable) {
                if (var.getType().compareTo(Type.SCALAIRE) == 0 || var.getType().compareTo(Type.COURBE) == 0 || var.getType().compareTo(Type.MAP) == 0
                        || var.getType().compareTo(Type.ARRAY) == 0 || var.getType().compareTo(Type.TEXT) == 0) {
                    createSwInstance(doc, swInstanceTree, var, text);
                }

            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
            transformer.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());
            DOMSource source = new DOMSource(doc);
            StreamResult resultat = new StreamResult(file);

            transformer.transform(source, resultat);

            return true;

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static final void createSwInstance(Document doc, Element parent, Variable var, String sDate) {
        /*
         * <SW-INSTANCE>
         * <SHORT-NAME>CDF20.ARRAY.ELEMENT[0]</SHORT-NAME>
         * <CATEGORY>VALUE</CATEGORY>
         * <SW-FEATURE-REF>Vectors</SW-FEATURE-REF>
         * <SW-VALUE-CONT>
         * <UNIT-DISPLAY-NAME xml:space="preserve"></UNIT-DISPLAY-NAME>
         * <SW-VALUES-PHYS>
         * <V>5.0000</V>
         * </SW-VALUES-PHYS>
         * </SW-VALUE-CONT>
         * </SW-INSTANCE>
         */

        Element swInstance = doc.createElement("SW-INSTANCE");
        parent.appendChild(swInstance);

        Element shortName = doc.createElement("SHORT-NAME");
        shortName.appendChild(doc.createTextNode(var.getName()));
        swInstance.appendChild(shortName);

        Element category = doc.createElement("CATEGORY");
        category.appendChild(doc.createTextNode(var.getType().getName()));
        swInstance.appendChild(category);

        Element swFeatureRef = doc.createElement("SW-FEATURE-REF");
        swFeatureRef.appendChild(doc.createTextNode("..."));
        swInstance.appendChild(swFeatureRef);

        Element swValueCont = doc.createElement("SW-VALUE-CONT");
        swInstance.appendChild(swValueCont);

        Element unitDisplayName = doc.createElement("UNIT-DISPLAY-NAME");
        unitDisplayName.setAttribute("xml:space", "preserve");
        unitDisplayName.appendChild(doc.createTextNode("-"));
        swValueCont.appendChild(unitDisplayName);

        if (var.getType().compareTo(Type.ARRAY) == 0 || var.getType().compareTo(Type.TEXT) == 0) {
            Element swArraySize = doc.createElement("SW-ARRAYSIZE");
            swValueCont.appendChild(swArraySize);
            Element v = doc.createElement("V");
            v.appendChild(doc.createTextNode(String.valueOf(var.getDimX())));
            swArraySize.appendChild(v);
            v = doc.createElement("V");
            v.appendChild(doc.createTextNode(String.valueOf(var.getDimY())));
            swArraySize.appendChild(v);
        }

        Element swValuePhys = doc.createElement("SW-VALUES-PHYS");
        swValueCont.appendChild(swValuePhys);

        Element swAxisConts;
        Element swAxisCont;

        String value;
        Element label;
        Element vg;
        Element valueNode;

        // boolean modifiedVar = var.isModified();
        int idxPage = 0; // Page de travail

        switch (var.getType()) {
        case SCALAIRE:
            value = var.getValue(idxPage, 0, 0) != null ? var.getValue(idxPage, 0, 0).toString() : "0";
            valueNode = doc.createElement("V");
            valueNode.appendChild(doc.createTextNode(value));
            swValuePhys.appendChild(valueNode);
            break;
        case COURBE:

            for (short x = 0; x < var.getDimX(); x++) {
                value = var.getValue(idxPage, 1, x) != null ? var.getValue(idxPage, 1, x).toString() : "0";
                if (!var.isTextValue()) {
                    valueNode = doc.createElement("V");
                } else {
                    valueNode = doc.createElement("VT");
                }
                valueNode.appendChild(doc.createTextNode(value));
                swValuePhys.appendChild(valueNode);
            }

            // <SW-AXIS-CONTS>
            swAxisConts = doc.createElement("SW-AXIS-CONTS");
            swInstance.appendChild(swAxisConts);

            // <SW-AXIS-CONT>
            swAxisCont = doc.createElement("SW-AXIS-CONT");
            swAxisConts.appendChild(swAxisCont);

            category = doc.createElement("CATEGORY");
            category.appendChild(doc.createTextNode("STD_AXIS"));
            swAxisCont.appendChild(category);

            swValuePhys = doc.createElement("SW-VALUES-PHYS");
            swAxisCont.appendChild(swValuePhys);

            for (short x = 0; x < var.getDimX(); x++) {
                value = var.getValue(idxPage, 0, x) != null ? var.getValue(idxPage, 0, x).toString() : "0";
                valueNode = doc.createElement("V");
                valueNode.appendChild(doc.createTextNode(value));
                swValuePhys.appendChild(valueNode);
            }

            break;
        case MAP:

            // <VG> Par ligne
            // <LABEL> Par ligne
            // <V>

            for (short y = 1; y < var.getDimY(); y++) {
                vg = doc.createElement("VG");
                swValuePhys.appendChild(vg);

                for (short x = 0; x < var.getDimX(); x++) {

                    value = var.getValue(idxPage, y, x) != null ? var.getValue(idxPage, y, x).toString() : "0";

                    // System.out.println(value);

                    if (x == 0) {
                        label = doc.createElement("LABEL");
                        label.appendChild(doc.createTextNode(value));
                        vg.appendChild(label);
                    } else {
                        valueNode = doc.createElement("V");
                        valueNode.appendChild(doc.createTextNode(value));
                        vg.appendChild(valueNode);
                    }

                }
            }

            swAxisConts = doc.createElement("SW-AXIS-CONTS");
            swInstance.appendChild(swAxisConts);

            // Axe X
            swAxisCont = doc.createElement("SW-AXIS-CONT");
            swAxisConts.appendChild(swAxisCont);

            category = doc.createElement("CATEGORY");
            category.appendChild(doc.createTextNode("STD_AXIS"));
            swAxisCont.appendChild(category);

            swValuePhys = doc.createElement("SW-VALUES-PHYS");
            swAxisCont.appendChild(swValuePhys);

            for (short x = 1; x < var.getDimX(); x++) {
                value = var.getValue(idxPage, 0, x) != null ? var.getValue(idxPage, 0, x).toString() : "0";
                valueNode = doc.createElement("V");
                valueNode.appendChild(doc.createTextNode(value));
                swValuePhys.appendChild(valueNode);
            }

            // Axe Y
            swAxisCont = doc.createElement("SW-AXIS-CONT");
            swAxisConts.appendChild(swAxisCont);

            category = doc.createElement("CATEGORY");
            category.appendChild(doc.createTextNode("STD_AXIS"));
            swAxisCont.appendChild(category);

            swValuePhys = doc.createElement("SW-VALUES-PHYS");
            swAxisCont.appendChild(swValuePhys);

            for (short y = 1; y < var.getDimY(); y++) {
                value = var.getValue(idxPage, y, 0) != null ? var.getValue(idxPage, y, 0).toString() : "0";
                valueNode = doc.createElement("V");
                valueNode.appendChild(doc.createTextNode(value));
                swValuePhys.appendChild(valueNode);
            }

            break;
        case ARRAY:

            for (short x = 0; x < var.getDimX(); x++) {
                value = var.getValue(idxPage, 0, x) != null ? var.getValue(idxPage, 0, x).toString() : "0";
                if (!var.isTextValue()) {
                    valueNode = doc.createElement("V");
                } else {
                    valueNode = doc.createElement("VT");
                }
                valueNode.appendChild(doc.createTextNode(value));
                swValuePhys.appendChild(valueNode);
            }

            break;
        case TEXT:

            for (short y = 0; y < var.getDimY(); y++) {
                vg = doc.createElement("VG");
                swValuePhys.appendChild(vg);

                for (short x = 0; x < var.getDimX(); x++) {

                    value = var.getValue(idxPage, y, x) != null ? var.getValue(idxPage, y, x).toString() : " ";

                    valueNode = doc.createElement("VT");
                    valueNode.appendChild(doc.createTextNode(value));
                    vg.appendChild(valueNode);

                }
            }

            break;

        default:
            break;
        }

        // SW-CS-HISTORY
        Element swCsHistory = doc.createElement("SW-CS-HISTORY");
        swInstance.appendChild(swCsHistory);

        // CS-ENTRY
        Element swCsEntry = doc.createElement("CS-ENTRY");
        swCsHistory.appendChild(swCsEntry);

        Element state = doc.createElement("STATE");
        swCsEntry.appendChild(state);
        state.appendChild(doc.createTextNode("---"));

        Element date = doc.createElement("DATE");
        swCsEntry.appendChild(date);
        date.appendChild(doc.createTextNode(sDate));

        Element user = doc.createElement("CSUS");
        swCsEntry.appendChild(user);
        user.appendChild(doc.createTextNode("User"));

        Element remark = doc.createElement("REMARK");
        swCsEntry.appendChild(remark);

        Element com = doc.createElement("P");
        remark.appendChild(com);
        com.appendChild(doc.createTextNode("Commentaire pour " + var.getName()));

    }

    public static final boolean exportMap(List<Variable> listVariable, final File file) {

        final String COLONNES = "colonnes";
        final String BKPTCOL = "bkptcol";
        final String LIGNE = "ligne";
        final String BKPTLIGN = "bkptlign";
        final String SEMICOLON = ";";
        final String EGALE = "=";

        try (PrintWriter pw = new PrintWriter(file, "ISO-8859-1")) {

            String variableName;
            Object value;

            for (Variable var : listVariable) {

                variableName = var.getName();
                int idxPage = 0;

                pw.println("[" + variableName + "]");

                switch (var.getType()) {
                case SCALAIRE:

                    value = var.getValue(idxPage, 0, 0);

                    if (value == null || "NaN".equals(value.toString())) {
                        value = "";
                    }

                    pw.println(COLONNES + EGALE + value + SEMICOLON);
                    break;

                case ARRAY:

                    pw.print(COLONNES + EGALE);

                    for (short x = 0; x < var.getDimX(); x++) {

                        value = var.getValue(idxPage, 0, x);

                        if (value == null || "NaN".equals(value.toString())) {
                            value = "";
                        }

                        pw.print(value + SEMICOLON);
                    }

                    pw.println();

                    break;

                case COURBE:

                    pw.print(BKPTCOL + EGALE);

                    for (byte y = 0; y < 2; y++) {
                        for (short x = 0; x < var.getDimX(); x++) {
                            if (y == 1 && x == 0) {
                                pw.print(LIGNE + EGALE);
                            }

                            value = var.getValue(idxPage, y, x);

                            if (value == null || "NaN".equals(value.toString())) {
                                value = "";
                            }

                            pw.print(value + SEMICOLON);
                        }
                        pw.println();
                    }

                    break;

                case MAP:

                    pw.print(BKPTCOL + EGALE);

                    for (short x = 1; x < var.getDimX(); x++) {

                        value = var.getValue(idxPage, 0, x);

                        if (value == null || "NaN".equals(value.toString())) {
                            value = "";
                        }
                        pw.print(value + SEMICOLON);
                    }
                    pw.println();

                    pw.print(BKPTLIGN + EGALE);

                    for (short y = 1; y < var.getDimY(); y++) {

                        value = var.getValue(idxPage, y, 0);

                        if (value == null || "NaN".equals(value.toString())) {
                            value = "";
                        }
                        pw.print(value + SEMICOLON);
                    }
                    pw.println();

                    for (short y = 1; y < var.getDimY(); y++) {
                        for (short x = 0; x < var.getDimX(); x++) {

                            value = var.getValue(idxPage, y, x);

                            if (value == null || "NaN".equals(value.toString())) {
                                value = "";
                            }

                            if (x == 0) {
                                pw.print(LIGNE + value + EGALE);
                                continue;
                            }
                            pw.print(value + SEMICOLON);
                        }
                        pw.println();
                    }
                    break;

                case TEXT:

                    for (short y = 0; y < var.getDimY(); y++) {

                        pw.print(LIGNE + (y + 1) + EGALE);

                        for (short x = 0; x < var.getDimX(); x++) {

                            value = var.getValue(idxPage, y, x);

                            if (value == null) {
                                value = "";
                            }

                            pw.print(value + SEMICOLON);
                        }
                        pw.println();
                    }
                    break;

                default:
                    break;
                }
            }
        } catch (FileNotFoundException | UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return true;
    }

    public static final boolean toExcel(List<Variable> listVariable, final File file) {

        WritableWorkbook workbook = null;

        try {
            workbook = Workbook.createWorkbook(file);

            final WritableFont arial10Bold = new WritableFont(WritableFont.ARIAL, 10, WritableFont.BOLD);
            final WritableCellFormat arial10format = new WritableCellFormat(arial10Bold);

            final WritableCellFormat borderFormat = new WritableCellFormat();
            borderFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderFormat.setAlignment(Alignment.CENTRE);
            borderFormat.setVerticalAlignment(VerticalAlignment.CENTRE);

            final WritableCellFormat borderBoldFormat = new WritableCellFormat(arial10Bold);
            borderBoldFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            borderBoldFormat.setAlignment(Alignment.CENTRE);
            borderBoldFormat.setVerticalAlignment(VerticalAlignment.CENTRE);

            final WritableCellFormat axisFormat = new WritableCellFormat(arial10Bold);
            axisFormat.setBorder(Border.ALL, BorderLineStyle.THIN);
            axisFormat.setBackground(Colour.VERY_LIGHT_YELLOW);
            axisFormat.setAlignment(Alignment.CENTRE);
            axisFormat.setVerticalAlignment(VerticalAlignment.CENTRE);

            // Entete de la feuille Valeurs
            final WritableSheet sheetValues = workbook.createSheet("Valeurs", 0);
            sheetValues.getSettings().setShowGridLines(false);
            //

            int row = 0; // Point de depart, une ligne d'espace par rapport au titre

            WritableCellFormat centerAlignYellow = new WritableCellFormat();
            centerAlignYellow.setAlignment(Alignment.CENTRE);
            centerAlignYellow.setVerticalAlignment(VerticalAlignment.CENTRE);
            centerAlignYellow.setBackground(Colour.VERY_LIGHT_YELLOW);

            String variableName;

            for (Variable var : listVariable) {

                variableName = var.getName();
                int idxPage = 0;

                writeCell(sheetValues, 0, row, variableName, arial10format);

                int col;

                switch (var.getType()) {
                case SCALAIRE:

                    row += 1;
                    writeCell(sheetValues, 0, row, var.getValue(idxPage, 0, 0), borderFormat);
                    row += 2;
                    break;

                case ARRAY:

                    col = 0;
                    row += 1;
                    for (short x = 0; x < var.getDimX(); x++) {
                        writeCell(sheetValues, col, row, var.getValue(idxPage, 0, x), borderFormat);
                        col += 1;
                    }

                    row += 2;

                    break;

                case COURBE:

                    for (byte y = 0; y < 2; y++) {
                        col = 0;
                        row += 1;
                        for (short x = 0; x < var.getDimX(); x++) {
                            if (y == 0) {
                                writeCell(sheetValues, col, row, var.getValue(idxPage, y, x), axisFormat);
                            } else {
                                writeCell(sheetValues, col, row, var.getValue(idxPage, y, x), borderFormat);
                            }
                            col += 1;
                        }
                    }
                    row += 2;
                    break;

                case MAP:

                    for (short y = 0; y < var.getDimY(); y++) {
                        col = 0;
                        row += 1;
                        for (short x = 0; x < var.getDimX(); x++) {
                            if (y == 0 | x == 0) {
                                writeCell(sheetValues, col, row, var.getValue(idxPage, y, x), axisFormat);
                            } else {
                                writeCell(sheetValues, col, row, var.getValue(idxPage, y, x), borderFormat);
                            }
                            col += 1;
                        }
                    }
                    row += 2;
                    break;

                case TEXT:

                    for (short y = 0; y < var.getDimY(); y++) {
                        col = 0;
                        row += 1;
                        for (short x = 0; x < var.getDimX(); x++) {
                            writeCell(sheetValues, col, row, var.getValue(idxPage, y, x), borderFormat);
                            col += 1;
                        }
                    }
                    row += 2;
                    break;

                default:
                    break;
                }
            }

            workbook.write();

        } catch (IOException e) {
            return false;
        } catch (RowsExceededException rowException) {
            return false;
        } catch (WriteException e) {

        } finally {
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (WriteException e1) {
                    e1.printStackTrace();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return true;
    }

    private static final void writeCell(WritableSheet sht, int col, int row, Object value, WritableCellFormat format)
            throws RowsExceededException, WriteException {

        if (value instanceof Number) {
            sht.addCell(new Number(col, row, Double.parseDouble(value.toString()), format));
        } else {
            if (value != null) {
                sht.addCell(new Label(col, row, value.toString(), format));
            } else {
                sht.addCell(new Label(col, row, "", format));
            }
        }

    }

    public static final ComparResult compare(MapCal ref, MapCal work, final File file) {

        int nbVariable = Math.min(ref.listVariable.size(), work.listVariable.size());
        Variable varRef;
        Variable varWork;

        Map<Variable, Variable> listDiff = new LinkedHashMap<>();

        for (int i = 0; i < nbVariable; i++) {
            varRef = ref.listVariable.get(i);

            int idxVarWork = work.listVariable.indexOf(varRef);

            if (idxVarWork > -1) {
                varWork = work.listVariable.get(idxVarWork);
                if (varRef.getChecksum() != varWork.getChecksum()) {
                    listDiff.put(varRef, varWork);
                }
            }
        }

        if (listDiff.size() > 0) {
            return toHtml(ref.name, work.name, listDiff, file);
        }
        return ComparResult.NO_DIFFERENCE;
    }

    public static final ComparResult toHtml(String refMap, String workMap, Map<Variable, Variable> diffs, final File file) {

        PrintWriter printWriter = null;

        try {
            printWriter = new PrintWriter(file);

            printWriter.println("<!DOCTYPE html>");
            printWriter.println("<html>");
            printWriter.println("<head>");
            printWriter.println("<title>DataLogViewer - Comparaison calibration</title>");
            printWriter.println("<meta charset=utf-8/>");
            printWriter.println("</head>");
            printWriter.println("<body>");

            printWriter.println("<h2 align=center>" + refMap + "</h2>");
            printWriter.println("<h2 align=center><font color=red>" + workMap + "</font></h2>");

            // Sommaire
            printWriter.println("<h2 align=center><u>" + "Sommaire" + "</u></h2>");

            for (Variable var : diffs.keySet()) {
                printWriter.println("<p align=center><a href=#" + var.getName() + ">" + var.getName() + "</a></p>");
            }
            //
            Variable workVar;
            Object refValue;
            Object workValue;

            for (Variable var : diffs.keySet()) {

                printWriter.println("<hr align=center size=1 width=90%>");
                printWriter.println("<h2><font color=blue id=" + var.getName() + ">" + var.getName() + "</font></h2>");
                printWriter.println("<blockquote>");
                printWriter.println("<p>Valeur(s) :");
                printWriter.println("<table border cellpadding=3>");

                for (int y = 0; y < var.getDimY(); y++) {

                    printWriter.println("<tr>"); // Debut d'une ligne

                    for (int x = 0; x < var.getDimX(); x++) {

                        refValue = var.getValue(1, y, x);
                        workVar = diffs.get(var);

                        if (x < workVar.getDimX() && y < workVar.getDimY()) {
                            workValue = workVar.getValue(1, y, x);
                        } else {
                            workValue = Float.NaN;
                        }

                        if (refValue.toString().equals(workValue.toString())) {
                            printWriter.println("<th align=center>" + refValue + "</th>");
                        } else {
                            printWriter.println("<th align=center>" + refValue + " | " + "<font color=red>" + workValue + "</font>" + "</th>");
                        }
                    }

                    printWriter.println("</tr>"); // Fin d'une ligne
                }

                printWriter.println("</table>");
                printWriter.println("</blockquote>");
            }

            printWriter.println("</body>");
            printWriter.println("</html>");

        } catch (FileNotFoundException e) {
            return ComparResult.ERROR;
        } finally {
            if (printWriter != null)
                printWriter.close();
        }
        return ComparResult.RESULT_OK;

    }

}

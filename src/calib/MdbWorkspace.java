/*
 * Creation : 3 nov. 2020
 */
package calib;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.healthmarketscience.jackcess.Database;
import com.healthmarketscience.jackcess.DatabaseBuilder;
import com.healthmarketscience.jackcess.Row;
import com.healthmarketscience.jackcess.Table;

public final class MdbWorkspace {

    private static final String AFFICHEUR = "Afficheurs";
    private static final String NOM = "nomaffich";
    private static final String TYPE = "Type";
    private static final String ACTIF = "actif";
    private static final String X_POS = "Xpos";
    private static final String Y_POS = "Ypos";
    private static final String WIDTH = "Width";
    private static final String HEIGHT = "height";
    private static final String BG_COLOR = "Backcolor";

    private static final String COMVARLIST = "comvarlist";
    private static final String NOM_VAR = "Nom";
    private static final String AFFICHEUR_VAR = "Afficheur";
    private static final String FONT_SIZE = "Taille_caract";
    private static final String FONT_COLOR = "couleur_caract";
    private static final String BOLD = "Gras";
    private static final String UNDERLINE = "Sousligne";
    private static final String ITALIC = "Italique";

    private static final String VARIABLES = "variables";
    private static final String VARIABLE_NOM = "nom";
    private static final String VARIABLE_UNIT = "Unit";
    private static final String VARIABLE_OFFSET = "offset";
    private static final String VARIABLE_FCONV = "facteur";
    private static final String VARIABLE_SIGNED = "signed";
    private static final String VARIABLE_NBDEC = "ndecimal";
    private static final String VARIABLE_DETAIL = "detail";
    private static final String VARIABLE_CLASSE = "Classe";

    private final String name;
    private Map<String, Afficheur> afficheurs;
    private Map<Integer, VariableDisplay> comVarList;
    private Map<String, VariableECU> variablesECU;

    public MdbWorkspace(File mdbFile) {
        this.name = mdbFile.getName().replace(".mdb", "");
        readDatabase(mdbFile);
        writeToXml();
    }

    public String getName() {
        return name;
    }

    public Map<String, Afficheur> getInfos() {
        return afficheurs != null ? afficheurs : new HashMap<String, Afficheur>();
    }

    private final void readDatabase(File mdbFile) {

        try {
            Database db = DatabaseBuilder.open(mdbFile);

            Table tableAfficheurs = db.getTable(AFFICHEUR);
            this.afficheurs = getAfficheurInfo(tableAfficheurs);

            Table tableComVarList = db.getTable(COMVARLIST);
            this.comVarList = getComVarList(tableComVarList);

            Table tableVariablesECU = db.getTable(VARIABLES);
            this.variablesECU = getVariablesECU(tableVariablesECU);

            db.close();
        } catch (IOException e) {
            if (e instanceof FileNotFoundException) {
                System.out.println("Fichier non trouve : " + mdbFile.getAbsolutePath());
            }
        }
    }

    public final Map<String, Afficheur> getAfficheurInfo(Table tableAfficheurs) {

        HashMap<String, Afficheur> listInfos = new HashMap<String, Afficheur>(tableAfficheurs.getRowCount());

        for (Row row : tableAfficheurs) {
            listInfos.put(row.getString(NOM), new Afficheur(row.getString(NOM), row.getString(TYPE), row.getBoolean(ACTIF), row.getInt(X_POS),
                    row.getInt(Y_POS), row.getInt(WIDTH), row.getInt(HEIGHT), row.getInt(BG_COLOR)));
        }

        return listInfos;
    }

    public final Map<Integer, VariableDisplay> getComVarList(Table table) {

        HashMap<Integer, VariableDisplay> listVariable = new HashMap<Integer, VariableDisplay>(table.getRowCount());

        int cnt = 0;

        Afficheur afficheur;
        String afficheurVar;
        VariableDisplay var;

        for (Row row : table) {

            afficheurVar = row.getString(AFFICHEUR_VAR);
            var = new VariableDisplay(row.getString(NOM_VAR), row.getString(AFFICHEUR_VAR), row.getInt(FONT_SIZE), row.getInt(FONT_COLOR),
                    row.getByte(BOLD), row.getByte(UNDERLINE), row.getByte(ITALIC));

            afficheur = this.afficheurs.get(afficheurVar);
            if (afficheur != null) {
                afficheur.addVariable(var);
            }

            listVariable.put(cnt++, var);
        }

        return listVariable;
    }

    public final Map<String, VariableECU> getVariablesECU(Table tableVariablesECU) {

        HashMap<String, VariableECU> listVariables = new HashMap<String, VariableECU>(tableVariablesECU.getRowCount());

        for (Row row : tableVariablesECU) {
            listVariables.put(row.getString(VARIABLE_NOM),
                    new VariableECU(row.getString(VARIABLE_NOM), row.getString(VARIABLE_UNIT), row.getInt(VARIABLE_OFFSET),
                            row.getDouble(VARIABLE_FCONV), row.getShort(VARIABLE_SIGNED), row.getInt(VARIABLE_NBDEC), row.getString(VARIABLE_DETAIL),
                            row.getString(VARIABLE_CLASSE)));
        }

        return listVariables;
    }

    protected class Afficheur {

        private String name;
        private String type;
        private boolean actif;
        private int xPos;
        private int yPos;
        private int width;
        private int height;
        private int bgColor;

        private List<VariableDisplay> variables;

        public Afficheur(String name, String type, boolean actif, int xPos, int yPos, int width, int height, int bgColor) {
            this.name = name;
            this.type = type;
            this.actif = actif;
            this.xPos = xPos;
            this.yPos = yPos;
            this.width = width;
            this.height = height;
            this.bgColor = bgColor;
            variables = new ArrayList<VariableDisplay>();
        }

        public String getName() {
            return name;
        }

        public String getType() {
            return type;
        }

        public boolean getActif() {
            return actif;
        }

        public int getxPos() {
            return xPos;
        }

        public int getyPos() {
            return yPos;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getBgColor() {
            return bgColor;
        }

        public void addVariable(VariableDisplay var) {
            this.variables.add(var);
        }

        public final List<VariableDisplay> getVariables() {
            return this.variables;
        }

    }

    protected class VariableDisplay {

        private String name;
        private String afficheur;
        private int fontSize;
        private int fontColor;
        private int bold;
        private int underline;
        private int italic;

        public VariableDisplay(String name, String afficheur, int fontSize, int fontColor, int bold, int underline, int italic) {
            this.name = name;
            this.afficheur = afficheur;
            this.fontSize = fontSize;
            this.fontColor = fontColor;
            this.bold = 0xff & bold;
            this.underline = 0xff & underline;
            this.italic = 0xff & italic;
        }

        public String getName() {
            return name;
        }

        public String getAfficheur() {
            return afficheur;
        }

        public int getFontSize() {
            return fontSize;
        }

        public int getFontColor() {
            return fontColor;
        }

        public int getBold() {
            return bold;
        }

        public int getUnderline() {
            return underline;
        }

        public int getItalic() {
            return italic;
        }

    }

    protected class VariableECU {
        private String nom;
        private String unit;
        private int offset;
        private double fconv;
        private int signed;
        private int nbDecimal;
        private String detail;
        private String classe;

        public VariableECU(String nom, String unit, int offset, double fconv, int signed, int nbDecimal, String detail, String classe) {
            this.nom = nom;
            this.unit = unit != null ? unit : "su";
            this.offset = offset;
            this.fconv = fconv;
            this.signed = signed;
            this.nbDecimal = nbDecimal;
            this.detail = detail != null ? detail : "...";
            this.classe = classe;
        }

        public String getNom() {
            return nom;
        }

        public String getUnit() {
            return unit;
        }

        public int getOffset() {
            return offset;
        }

        public double getFconv() {
            return fconv;
        }

        public int getSigned() {
            return signed;
        }

        public int getNbDecimal() {
            return nbDecimal;
        }

        public String getDetail() {
            return detail;
        }

        public String getClasse() {
            return classe;
        }

    }

    public final boolean writeToXml() {

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try {
            docBuilder = dbFactory.newDocumentBuilder();

            Document doc = docBuilder.newDocument();
            Element racine = doc.createElement("Workspace");
            doc.appendChild(racine);

            Element afficheurs = doc.createElement("Afficheurs");
            racine.appendChild(afficheurs);

            Element noeudAfficheur;
            Element attributAfficheur;
            Afficheur afficheur;

            for (Entry<String, Afficheur> entry : this.afficheurs.entrySet()) {
                noeudAfficheur = doc.createElement("Afficheur");
                afficheurs.appendChild(noeudAfficheur);

                afficheur = entry.getValue();

                attributAfficheur = doc.createElement(NOM);
                attributAfficheur.appendChild(doc.createTextNode(afficheur.name));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(TYPE);
                attributAfficheur.appendChild(doc.createTextNode(afficheur.type));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(ACTIF);
                attributAfficheur.appendChild(doc.createTextNode(String.valueOf(afficheur.actif)));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(X_POS);
                attributAfficheur.appendChild(doc.createTextNode(String.valueOf(afficheur.xPos)));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(Y_POS);
                attributAfficheur.appendChild(doc.createTextNode(String.valueOf(afficheur.yPos)));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(WIDTH);
                attributAfficheur.appendChild(doc.createTextNode(String.valueOf(afficheur.width)));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(HEIGHT);
                attributAfficheur.appendChild(doc.createTextNode(String.valueOf(afficheur.height)));
                noeudAfficheur.appendChild(attributAfficheur);

                attributAfficheur = doc.createElement(BG_COLOR);
                attributAfficheur.appendChild(doc.createTextNode(String.valueOf(afficheur.bgColor)));
                noeudAfficheur.appendChild(attributAfficheur);

                Element variables = doc.createElement("VariablesDisplay");
                noeudAfficheur.appendChild(variables);

                Element attributVar;

                for (VariableDisplay var : afficheur.getVariables()) {
                    Element noeudVar = doc.createElement("VariableDisplay");
                    variables.appendChild(noeudVar);

                    attributVar = doc.createElement(NOM_VAR);
                    attributVar.appendChild(doc.createTextNode(var.name));
                    noeudVar.appendChild(attributVar);

                    attributVar = doc.createElement(VARIABLE_OFFSET);
                    VariableECU variableECU = this.variablesECU.get(var.name);
                    if (variableECU != null) {
                        int offset = variableECU.offset;
                        String stringAdress = "0x" + Integer.toHexString(offset).toUpperCase();
                        attributVar.appendChild(doc.createTextNode(stringAdress));
                    } else {
                        attributVar.appendChild(doc.createTextNode("0xFFFF"));
                    }

                    noeudVar.appendChild(attributVar);

                    attributVar = doc.createElement(FONT_SIZE);
                    attributVar.appendChild(doc.createTextNode(String.valueOf(var.fontSize)));
                    noeudVar.appendChild(attributVar);

                    attributVar = doc.createElement(FONT_COLOR);
                    attributVar.appendChild(doc.createTextNode(String.valueOf(var.fontColor)));
                    noeudVar.appendChild(attributVar);

                    attributVar = doc.createElement(BOLD);
                    attributVar.appendChild(doc.createTextNode(String.valueOf(var.bold)));
                    noeudVar.appendChild(attributVar);

                    attributVar = doc.createElement(UNDERLINE);
                    attributVar.appendChild(doc.createTextNode(String.valueOf(var.underline)));
                    noeudVar.appendChild(attributVar);

                    attributVar = doc.createElement(ITALIC);
                    attributVar.appendChild(doc.createTextNode(String.valueOf(var.italic)));
                    noeudVar.appendChild(attributVar);
                }

            }

            Element variablesECU = doc.createElement("VariablesECU");
            racine.appendChild(variablesECU);

            Element noeudVariableECU;
            Element attributVariableECU;
            VariableECU variableECU;

            for (Entry<String, VariableECU> entry : this.variablesECU.entrySet()) {
                noeudVariableECU = doc.createElement("VariableECU");
                variablesECU.appendChild(noeudVariableECU);

                variableECU = entry.getValue();

                attributVariableECU = doc.createElement(VARIABLE_NOM);
                attributVariableECU.appendChild(doc.createTextNode(variableECU.nom));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_UNIT);
                attributVariableECU.appendChild(doc.createTextNode(variableECU.unit));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_OFFSET);
                attributVariableECU.appendChild(doc.createTextNode(String.valueOf(variableECU.offset)));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_FCONV);
                attributVariableECU.appendChild(doc.createTextNode(String.valueOf(variableECU.fconv)));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_SIGNED);
                attributVariableECU.appendChild(doc.createTextNode(String.valueOf(variableECU.signed)));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_NBDEC);
                attributVariableECU.appendChild(doc.createTextNode(String.valueOf(variableECU.nbDecimal)));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_DETAIL);
                attributVariableECU.appendChild(doc.createTextNode(variableECU.detail));
                noeudVariableECU.appendChild(attributVariableECU);

                attributVariableECU = doc.createElement(VARIABLE_CLASSE);
                attributVariableECU.appendChild(doc.createTextNode(variableECU.classe));
                noeudVariableECU.appendChild(attributVariableECU);

            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            DOMSource source = new DOMSource(doc);
            StreamResult resultat = new StreamResult(new File("C:\\User\\U354706\\Perso\\Clio\\Ecran_ProLog_stepper.xml"));

            transformer.transform(source, resultat);

            System.out.println("Fichier sauvegardé avec succès!");

        } catch (ParserConfigurationException | TransformerException e) {
            e.printStackTrace();
        }

        return false;
    }

}

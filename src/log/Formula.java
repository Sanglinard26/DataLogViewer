package log;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;
import org.mariuszgromada.math.mxparser.Function;

import calib.MapCal;
import calib.Variable;
import utils.Interpolation;

public final class Formula extends Measure {

    private String literalExpression;
    private String internExpression;
    private Expression expression;
    private Map<Character, String> variables;
    private boolean syntaxOK = false;
    private boolean valid = true; // init premier calcul
    private boolean mapCalBased = false;
    private boolean upToDate = false;

    public static final Map<String, String> mapRegexCal;
    static {
        mapRegexCal = new HashMap<>();
        mapRegexCal.put("TABLE2D", "TABLE2D\\{.*?,.*?,.*?\\}");
        mapRegexCal.put("TABLE1D", "TABLE1D\\{.*?,.*?\\}");
        mapRegexCal.put("SCALAIRE", "SCALAIRE\\{.*?\\}");
    }

    private static String[] functions = new String[] { "delta", "passeBasTypeK", "bitactif", "saturation" };

    public Formula(String name, String unit, String baseExpression, Log log, MapCal calib) {
        super(name, log.getNbPoints());

        this.unit = unit;

        if ("".equals(baseExpression)) {
            return;
        }
        this.literalExpression = baseExpression;

        build();

        if (syntaxOK) {
            calculate(log, calib);
        } else {
            JOptionPane.showMessageDialog(null, "V\u00e9rifiez la synthaxe svp \n" + expression.getErrorMessage(), "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    public Formula(String name, String unit, String baseExpression) {
        super(name);

        this.unit = unit;

        if ("".equals(baseExpression)) {
            return;
        }
        this.literalExpression = baseExpression;

        build();

        if (syntaxOK) {
            upToDate = false;
        } else {
            JOptionPane.showMessageDialog(null, "V\u00e9rifiez la synthaxe svp \n" + expression.getErrorMessage(), "Erreur",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private final void build() {
        variables = new LinkedHashMap<Character, String>();

        renameVariables();

        Argument[] args = new Argument[variables.size()];

        int cnt = 0;
        for (Character var : variables.keySet()) {
            args[cnt++] = new Argument(var.toString(), Double.NaN);
        }

        this.expression = new Expression(this.internExpression, args);

        Function f = null;
        for (String s : functions) {
            switch (s) {
            case "delta":
                f = new Function(s, new DeltaFunction());
                this.expression.addFunctions(f);
                break;
            case "passeBasTypeK":
                f = new Function(s, new LowPassFilterK());
                this.expression.addFunctions(f);
                break;
            case "bitactif":
                f = new Function(s, new GetBit());
                this.expression.addFunctions(f);
                break;
            case "saturation":
                f = new Function(s, new Limit());
                this.expression.addFunctions(f);
                break;
            }

        }

        syntaxOK = expression.checkSyntax();
    }

    private final void renameVariables() {

        int charDec = 97;

        // Traitement des variables issues du log
        Pattern pattern = Pattern.compile("\\#(.*?)\\#");
        Matcher regexMatcher = pattern.matcher(literalExpression);

        String matchedMeasure;

        while (regexMatcher.find()) {
            matchedMeasure = regexMatcher.group(1);
            if (charDec == 101) {
                charDec++;
            }
            variables.put((char) charDec++, matchedMeasure);
        }

        internExpression = literalExpression.replaceAll("#", "");
        // *****

        // Traitement des paramètres de calibration
        String matchedParam;

        Set<Entry<String, String>> entries = mapRegexCal.entrySet();

        int compteurArgCal = 0;

        for (Entry<String, String> entryRegex : entries) {

            pattern = Pattern.compile(entryRegex.getValue());
            regexMatcher = pattern.matcher(literalExpression);

            while (regexMatcher.find()) {
                matchedParam = regexMatcher.group(0);
                if (charDec == 101) {
                    charDec++;
                }
                variables.put((char) charDec++, matchedParam.trim());
                compteurArgCal++;
            }
        }
        // *****

        if (compteurArgCal > 0) {
            this.mapCalBased = true;
        } else {
            this.mapCalBased = false;
        }

        for (Entry<Character, String> entry : variables.entrySet()) {
            internExpression = internExpression.replace(entry.getValue(), entry.getKey().toString());
        }

    }

    public final boolean isMapCalBased() {
        return mapCalBased;
    }

    public final boolean needUpdate() {
        return valid && !upToDate;
    }

    public final boolean isUpToDate() {
        return upToDate;
    }

    public final void setOutdated() {
        if (isUpToDate()) {
            this.upToDate = false;
        }
    }

    public final void changeData(Log log) {
        if (log.getNbPoints() != this.getDataLength()) {
            this.data = new double[log.getNbPoints()];
            // Arrays.fill(data, Double.NaN);
            min = Double.POSITIVE_INFINITY;
            max = Double.NEGATIVE_INFINITY;
        }
        this.idx = 0;

    }

    public final void calculate(final Log log, final MapCal calib) {

        Argument arg;
        Measure[] measures = new Measure[expression.getArgumentsNumber()];
        String var;

        if (!needUpdate() && !isMapCalBased()) {
            return;
        }

        changeData(log);

        if (log != null) {

            int argNumber = expression.getArgumentsNumber();

            for (int j = 0; j < argNumber; j++) {
                arg = expression.getArgument(j);
                var = variables.get(arg.getArgumentName().charAt(0));

                int idxAccolade = var.indexOf('{');
                String keyWordCalib = null;

                if (idxAccolade > 6) {
                    keyWordCalib = var.substring(0, idxAccolade);
                }

                if (keyWordCalib != null) { // C'est une formule avec des paramètres de calibration
                    measures[j] = generateMeasureFromCal(log, calib, var.substring(idxAccolade + 1).replace("}", ""));
                } else {
                    measures[j] = log.getMeasure(var);
                    if (measures[j] instanceof Formula) {
                        ((Formula) measures[j]).calculate(log, calib);
                    }
                }

                if (measures[j].isEmpty()) {
                    valid = false;
                    upToDate = false;
                    return;
                }
            }

            for (int i = 0, dataSize = log.getTime().getDataLength(); i < dataSize; i++) {
                for (int j = 0; j < argNumber; j++) {
                    arg = expression.getArgument(j);
                    arg.setArgumentValue(measures[j].get(i));
                }
                double res = expression.calculate();
                this.addPoint(res);
            }

            if (Double.isInfinite(min) && Double.isInfinite(max)) {
                upToDate = false;
            } else {
                upToDate = true;
            }

            valid = true;
        } else {
            double res = expression.calculate();
            this.addPoint(res);

            upToDate = false;
        }
    }

    public final boolean isSyntaxOK() {
        return syntaxOK;
    }

    public final String getExpression() {
        return this.literalExpression;
    }

    public final void setExpression(String expression) {
        if (expression != null && !expression.isEmpty() && !expression.equals(literalExpression)) {
            this.literalExpression = expression;
            upToDate = false;
            build();
        }

    }

    private final Measure generateMeasureFromCal(Log log, MapCal cal, String params) {

        String[] splitParams = params.split(",");

        int nbParams = splitParams.length;

        Variable variable = null;
        Measure x;
        Measure y;
        Measure z = null;

        final int dataSize = log.getTime().getDataLength();

        if (cal != null) {
            variable = cal.getVariable(splitParams[0]);
            if (variable != null) {
                z = new Measure(variable.getName(), log.getNbPoints());
            } else {
                z = new Measure(splitParams[0], log.getNbPoints());
                nbParams = 0;
            }
        } else {
            z = new Measure(splitParams[0], log.getNbPoints());
            nbParams = 0;
        }

        switch (nbParams) {
        case 1:
            for (int i = 0; i < dataSize; i++) {
                double res = variable.getDoubleValue(0, 0, 0);
                z.addPoint(res);
            }
            return z;
        case 2:
            x = log.getMeasure(splitParams[1]);

            double[][] courbe = variable.toDouble2D(0);

            for (int i = 0; i < dataSize; i++) {
                double res = Interpolation.interpLinear1D(courbe, x.get(i));
                z.addPoint(res);
            }
            return z;
        case 3:
            x = log.getMeasure(splitParams[1]);
            y = log.getMeasure(splitParams[2]);

            double[][] table = variable.toDouble2D(0);

            for (int i = 0; i < dataSize; i++) {
                double res = Interpolation.interpLinear2D(table, x.get(i), y.get(i));
                z.addPoint(res);
            }
            return z;
        default:
            for (int i = 0; i < dataSize; i++) {
                double res = Double.NaN;
                z.addPoint(res);
            }
            return z;
        }

    }

}

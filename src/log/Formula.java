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

import calib.MapCal;
import calib.Variable;
import utils.Interpolation;

public final class Formula extends Measure {

    private static final long serialVersionUID = 1L;

    private String literalExpression;
    private String internExpression;
    private transient Expression expression;
    private Map<Character, String> variables;
    private boolean valid = false;
    private boolean mapCalBased = false;
    private boolean upToDate = false;

    public static Map<String, String> mapRegexCal;
    static {
        mapRegexCal = new HashMap<>();
        mapRegexCal.put("TABLE2D", "TABLE2D\\{.*?,.*?,.*?\\}");
        mapRegexCal.put("TABLE1D", "TABLE1D\\{.*?,.*?\\}");
        mapRegexCal.put("SCALAIRE", "SCALAIRE\\{.*?\\}");
    }

    public Formula(String name, String unit, String baseExpression, Log log, MapCal calib) {
        super(name);

        this.unit = unit;

        if ("".equals(baseExpression)) {
            return;
        }
        this.literalExpression = baseExpression;

        build();

        if (valid) {
            calculate(log, calib);
        } else {
            JOptionPane.showMessageDialog(null, "V\u00e9rifiez la synthaxe svp", "Erreur", JOptionPane.ERROR_MESSAGE);
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
        valid = expression.checkSyntax();

    }

    public final void deserialize() {
        build();
    }

    private final void renameVariables() {
        // char � = 101
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
                variables.put((char) charDec++, matchedParam);
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

    public boolean isMapCalBased() {
        return mapCalBased;
    }

    public boolean isUpToDate() {
        return upToDate;
    }

    public final void setOutdated() {
        if (isUpToDate()) {
            this.upToDate = false;
        }
    }

    public final void calculate(Log log, MapCal calib) {
        Argument arg;
        Measure[] measures = new Measure[expression.getArgumentsNumber()];
        String var;

        if (isUpToDate() && !isMapCalBased()) {
            return;
        }

        if (!data.isEmpty()) {
            clearData();
        }

        if (log != null) {

            for (int j = 0; j < expression.getArgumentsNumber(); j++) {
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

            }

            for (int i = 0; i < log.getTime().getData().size(); i++) {
                for (int j = 0; j < expression.getArgumentsNumber(); j++) {
                    arg = expression.getArgument(j);
                    if (measures[j].getData().isEmpty()) {
                        break;
                    }
                    arg.setArgumentValue(measures[j].getData().get(i).doubleValue());
                }
                double res = expression.calculate();
                this.data.add(res);
                this.setMin(res);
                this.setMax(res);
            }
            if (Double.isInfinite(min) && Double.isInfinite(max)) {
                upToDate = false;
            } else {
                upToDate = true;
            }

        } else {
            double res = expression.calculate();
            this.data.add(res);
            this.setMin(res);
            this.setMax(res);

            upToDate = false;
        }
    }

    public final boolean isValid() {
        return valid;
    }

    public final String getExpression() {
        return this.literalExpression;
    }

    public void setExpression(String expression) {
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

        if (cal != null) {
            variable = cal.getVariable(splitParams[0]);
            if (variable != null) {
                z = new Measure(variable.getName());
            } else {
                z = new Measure(splitParams[0]);
                nbParams = 0;
            }
        } else {
            z = new Measure(splitParams[0]);
            nbParams = 0;
        }

        switch (nbParams) {
        case 1:

            for (int i = 0; i < log.getTime().getData().size(); i++) {
                double res = Double.parseDouble(variable.getValue(true, 0, 0).toString());
                z.data.add(res);
                z.setMin(res);
                z.setMax(res);
            }

            return z;
        case 2:
            x = log.getMeasure(splitParams[1]);

            for (int i = 0; i < log.getTime().getData().size(); i++) {
                double res = Interpolation.interpLinear1D(variable.toDouble2D(true), x.getData().get(i).doubleValue());
                z.data.add(res);
                z.setMin(res);
                z.setMax(res);
            }

            return z;
        case 3:
            x = log.getMeasure(splitParams[1]);
            y = log.getMeasure(splitParams[2]);

            for (int i = 0; i < log.getTime().getData().size(); i++) {
                double res = Interpolation.interpLinear2D(variable.toDouble2D(true), x.getData().get(i).doubleValue(),
                        y.getData().get(i).doubleValue());
                z.data.add(res);
                z.setMin(res);
                z.setMax(res);
            }
            return z;
        default:

            for (int i = 0; i < log.getTime().getData().size(); i++) {
                double res = Double.NaN;
                z.data.add(res);
                z.setMin(res);
                z.setMax(res);
            }

            return z;
        }

    }

}

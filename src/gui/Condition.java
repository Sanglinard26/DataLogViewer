/*
 * Creation : 4 juin 2021
 */
package gui;

import java.awt.Color;
import java.io.Serializable;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

import log.Log;
import log.Measure;

public final class Condition implements Serializable {

    private static final long serialVersionUID = 1L;

    private String name;
    private String expression;
    private boolean active;
    private Color color;

    public Condition(String name, String expression, Color color) {
        this.name = name;
        this.expression = expression;
        this.color = color;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        if (!isEmpty()) {
            this.active = active;
        } else {
            this.active = false;
        }

    }

    public final boolean isEmpty() {
        return "".equals(expression);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String operateur) {
        this.expression = operateur;
    }

    public BitSet applyCondition(Log log) {
        BitSet bitCondition = new BitSet(log.getTime().getData().size());

        if ("".equals(expression) || !active) {
            return bitCondition;
        }

        Map<Character, String> variables = new LinkedHashMap<Character, String>();

        // char � = 101
        int charDec = 97;

        // Traitement des variables issues du log
        Pattern pattern = Pattern.compile("\\#(.*?)\\#");
        Matcher regexMatcher = pattern.matcher(expression);

        String matchedMeasure;

        while (regexMatcher.find()) {
            matchedMeasure = regexMatcher.group(1);
            if (charDec == 101) {
                charDec++;
            }
            variables.put((char) charDec++, matchedMeasure);
        }

        String internExpression = expression.replaceAll("#", "");

        for (Entry<Character, String> entry : variables.entrySet()) {
            internExpression = internExpression.replace(entry.getValue(), entry.getKey().toString());
        }

        Argument[] args = new Argument[variables.size()];

        int cnt = 0;
        for (Character var : variables.keySet()) {
            args[cnt++] = new Argument(var.toString(), Double.NaN);
        }

        Expression conditionExpression = new Expression(internExpression, args);

        Measure[] measures = new Measure[conditionExpression.getArgumentsNumber()];
        String var;
        Argument arg;

        for (int j = 0; j < conditionExpression.getArgumentsNumber(); j++) {
            arg = conditionExpression.getArgument(j);
            var = variables.get(arg.getArgumentName().charAt(0));
            measures[j] = log.getMeasure(var);
        }

        for (int i = 0; i < log.getTime().getData().size(); i++) {
            for (int j = 0; j < conditionExpression.getArgumentsNumber(); j++) {
                arg = conditionExpression.getArgument(j);
                if (measures[j].getData().isEmpty()) {
                    break;
                }
                arg.setArgumentValue(measures[j].getData().get(i).doubleValue());
            }
            double res = conditionExpression.calculate();
            if (res == 1) {
                bitCondition.set(i);
            }
        }

        return bitCondition;
    }

}

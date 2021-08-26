/*
 * Creation : 4 juin 2021
 */
package gui;

import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.swing.JOptionPane;

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
        this.active = active;
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

    private String replaceVarName(Map<String, String> variables) {

        String internExpression = this.expression.replaceAll("#", "");

        for (Entry<String, String> entry : variables.entrySet()) {
            internExpression = internExpression.replace(entry.getValue(), entry.getKey().toString());
        }

        return internExpression;
    }

    private Map<String, String> findMeasure() {
        LinkedHashMap<String, String> variables = new LinkedHashMap<String, String>();

        Pattern pattern = Pattern.compile("\\#(.*?)\\#");
        final Matcher regexMatcher = pattern.matcher(this.expression);

        String matchedMeasure;

        int cnt = 1;

        while (regexMatcher.find()) {
            matchedMeasure = regexMatcher.group(1);

            variables.put("a" + cnt++, matchedMeasure);
        }

        return variables;
    }

    public BitSet apply(Log log) {

        BitSet bitCondition = new BitSet(log.getTime().getData().size());

        if ("".equals(expression) || !active) {
            return bitCondition;
        }

        ScriptEngineManager factory = new ScriptEngineManager();
        ScriptEngine engine = factory.getEngineByName("JavaScript");

        Map<String, String> variables = findMeasure();
        String renameExpression = replaceVarName(variables);

        List<Measure> measures = new ArrayList<Measure>(variables.size());
        for (String measureName : variables.values()) {
            measures.add(log.getMeasure(measureName));
        }

        try {

            String val;

            for (int i = 0; i < log.getTime().getData().size(); i++) {

                for (int j = 0; j < measures.size(); j++) {

                    val = "a" + (j + 1) + "=" + measures.get(j).getData().get(i);
                    engine.eval(val);
                }

                boolean result = (boolean) engine.eval(renameExpression);
                if (result) {
                    bitCondition.set(i);
                }

            }

        } catch (ScriptException se) {
            JOptionPane.showMessageDialog(null, "Problème de synthaxe !", "Erreur", JOptionPane.ERROR_MESSAGE);
            bitCondition.set(0, bitCondition.size(), false);
        }

        return bitCondition;
    }

}

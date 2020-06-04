package log;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;

import org.mariuszgromada.math.mxparser.Argument;
import org.mariuszgromada.math.mxparser.Expression;

public final class Formula extends Measure {

	private static final long serialVersionUID = 1L;

	private String literalExpression;
	private String internExpression;
	private transient Expression expression;
	private Map<Character, String> variables;
	private boolean valid = false;

	public Formula(String name, String baseExpression, Log log) {
		super(name);
		if("".equals(baseExpression))
		{
			return;
		}
		this.literalExpression = baseExpression;

		build();

		if(valid)
		{
			calculate(log);
		}else{
			JOptionPane.showMessageDialog(null, "V\u00e9rifiez la synthaxe svp", "Erreur", JOptionPane.ERROR_MESSAGE);
		}
	}

	private final void build()
	{
		variables = new LinkedHashMap<Character, String>();
		renameVariables();

		Argument[] args = new Argument[variables.size()];

		int cnt = 0;
		for(Character var : variables.keySet())
		{
			args[cnt++] = new Argument(var.toString(),Double.NaN);
		}

		this.expression = new Expression(this.internExpression, args);
		valid = expression.checkSyntax();

	}
	
	public final void deserialize()
	{
		build();
	}

	private final void renameVariables()
	{
		// char é = 101
		int charDec = 97;
		Pattern pattern = Pattern.compile("\\<(.*?)\\>");
		final Matcher regexMatcher = pattern.matcher(literalExpression);

		String matchedMeasure;

		while (regexMatcher.find()) {
			matchedMeasure = regexMatcher.group(1);
			if(charDec == 101)
			{
				charDec++;
			}
			variables.put((char) charDec++, matchedMeasure);
		}

		internExpression = literalExpression.replaceAll("<", "").replaceAll(">", "");

		for(Entry<Character, String> entry : variables.entrySet())
		{
			internExpression = internExpression.replace(entry.getValue(), entry.getKey().toString());
		}

	}

	public final void calculate(Log log)
	{
		Argument arg;
		Measure[] measures  = new Measure[expression.getArgumentsNumber()];
		String var;

		if(!data.isEmpty())
		{
			clearData();
		}

		if(log!=null)
		{
			for(int j = 0; j<expression.getArgumentsNumber(); j++)
			{
				arg = expression.getArgument(j);
				var = variables.get(arg.getArgumentName().charAt(0));
				measures[j] = log.getMeasure(var);
			}

			for(int i = 0; i<log.getTime().getData().size(); i++)
			{
				for(int j = 0; j<expression.getArgumentsNumber(); j++)
				{
					arg = expression.getArgument(j);
					if(measures[j].getData().isEmpty())
					{
						break;
					}
					arg.setArgumentValue(measures[j].getData().get(i));
				}
				double res = expression.calculate();
				this.data.add(res);
				this.setMin(res);
				this.setMax(res);
			}
		}else{
			double res = expression.calculate();
			this.data.add(res);
			this.setMin(res);
			this.setMax(res);
		}

	}

	public final boolean isValid()
	{
		return valid;
	}

	public final String getExpression()
	{
		return this.literalExpression;
	}

	public void setExpression(String expression) {
		this.literalExpression = expression;

		build();
	}

}

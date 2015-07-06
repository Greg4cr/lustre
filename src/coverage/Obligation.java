package coverage;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.Expr;

public class Obligation {
	// Mapping from an arithmetic expression to its name
	protected static Map<String, String> arithExprByExpr = new HashMap<String, String>();
	// Mapping from a name to an arithmetic expression
	protected static Map<String, String> arithExprById = new HashMap<String, String>();

	protected String condition;
	// Polarity of the current condition
	protected boolean polarity;
	// The obligation as an expression
	protected Expr obligation;
	// Polarity of the expression that uses the current condition
	protected boolean expressionPolarity;

	public Obligation(String condition, boolean polarity, Expr obligation) {
		this.condition = condition;
		this.polarity = polarity;
		this.obligation = obligation;
		this.expressionPolarity = polarity;
	}

	public Obligation(Expr condition, boolean polarity, Expr obligation) {
		this.condition = this.createArithVar(condition);
		this.polarity = polarity;
		this.obligation = obligation;
		this.expressionPolarity = polarity;
	}

	// Create a variable for an arithmetic expression
	private String createArithVar(Expr expr) {
		String exprStr = expr.toString();
		if (arithExprByExpr.get(exprStr) == null) {
			String arithExprId = "ArithExpr_" + arithExprByExpr.size();
			arithExprByExpr.put(exprStr, arithExprId);
			arithExprById.put(arithExprId, exprStr);
		}
		return arithExprByExpr.get(exprStr);
	}
}

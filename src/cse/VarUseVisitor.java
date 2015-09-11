package cse;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.visitors.ExprIterVisitor;

public class VarUseVisitor extends ExprIterVisitor {
	public static Map<String, Integer> get(Node node) {
		VarUseVisitor visitor = new VarUseVisitor();

		for (Equation equation : node.equations) {
			equation.expr.accept(visitor);
		}
		return visitor.varUse;
	}

	private final Map<String, Integer> varUse;

	private VarUseVisitor() {
		this.varUse = new HashMap<String, Integer>();
	}

	@Override
	public Void visit(IdExpr e) {
		addVar(e);
		return super.visit(e);
	}

	private void addVar(IdExpr e) {
		String eStr = e.toString();
		if (!this.varUse.containsKey(eStr)) {
			this.varUse.put(eStr, 0);
		}
		this.varUse.put(eStr, this.varUse.get(eStr) + 1);
	}
}

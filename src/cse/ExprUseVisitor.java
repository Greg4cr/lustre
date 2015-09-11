package cse;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.visitors.ExprIterVisitor;

/**
 * Get a mapping from an subexpression as a String to the number of times it is
 * used in a node.
 */
public final class ExprUseVisitor extends ExprIterVisitor {
	public static Map<String, Integer> get(Node node) {
		ExprUseVisitor visitor = new ExprUseVisitor();

		for (Equation equation : node.equations) {
			equation.expr.accept(visitor);
		}
		return visitor.exprUse;
	}

	private final Map<String, Integer> exprUse;

	private ExprUseVisitor() {
		this.exprUse = new HashMap<String, Integer>();
	}

	@Override
	public Void visit(ArrayExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(ArrayUpdateExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(BinaryExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(CastExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(CondactExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(IfThenElseExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(NodeCallExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(RecordExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(RecordUpdateExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	@Override
	public Void visit(UnaryExpr e) {
		addExpr(e);
		return super.visit(e);
	}

	private void addExpr(Expr e) {
		String eStr = e.toString();
		if (!this.exprUse.containsKey(eStr)) {
			this.exprUse.put(eStr, 0);
		}
		this.exprUse.put(eStr, this.exprUse.get(eStr) + 1);
	}
}

package cse;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.ArrayAccessExpr;
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
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.visitors.ExprIterVisitor;

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
	public Void visit(ArrayAccessExpr e) {
		e.array.accept(this);
		e.index.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(ArrayExpr e) {
		visitExprs(e.elements);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(ArrayUpdateExpr e) {
		e.array.accept(this);
		e.index.accept(this);
		e.value.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(BinaryExpr e) {
		e.left.accept(this);
		e.right.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(CastExpr e) {
		e.expr.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(CondactExpr e) {
		e.clock.accept(this);
		// Do not visit calls
		// e.call.accept(this);
		visitExprs(e.args);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(IfThenElseExpr e) {
		e.cond.accept(this);
		e.thenExpr.accept(this);
		e.elseExpr.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(NodeCallExpr e) {
		visitExprs(e.args);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(RecordAccessExpr e) {
		e.record.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(RecordExpr e) {
		visitExprs(e.fields.values());
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(RecordUpdateExpr e) {
		e.record.accept(this);
		e.value.accept(this);
		addExpr(e);
		return null;
	}

	@Override
	public Void visit(UnaryExpr e) {
		e.expr.accept(this);
		addExpr(e);
		return null;
	}

	private void addExpr(Expr e) {
		String eStr = e.toString();
		if (!this.exprUse.containsKey(eStr)) {
			this.exprUse.put(eStr, 0);
		}
		this.exprUse.put(eStr, this.exprUse.get(eStr) + 1);
	}
}

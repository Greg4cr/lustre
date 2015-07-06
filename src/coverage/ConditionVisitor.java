package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.CondactExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

/**
 * Generate obligations for condition coverage
 */
public class ConditionVisitor extends CoverageVisitor {
	public ConditionVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<Obligation> visit(ArrayAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions
		currentObs.addAll(this.addConditions(expr));
		currentObs.addAll(super.visit(expr));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		if (expr.op.equals(BinaryOp.GREATER) || expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)) {
			// The expression itself is a boolean expression and thus a
			// condition, boolean EQUAL and NOTEQUAL have been translated at
			// this point
			currentObs.add(new Obligation(expr, true, expr));
			currentObs.add(new Obligation(expr, false, new UnaryExpr(
					UnaryOp.NOT, expr)));
		}

		currentObs.addAll(super.visit(expr));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(CondactExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions
		currentObs.addAll(this.addConditions(expr));
		currentObs.addAll(super.visit(expr));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(IdExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions
		currentObs.addAll(this.addConditions(expr));
		currentObs.addAll(super.visit(expr));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions
		currentObs.addAll(this.addConditions(expr));
		currentObs.addAll(super.visit(expr));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RecordAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions
		currentObs.addAll(this.addConditions(expr));
		currentObs.addAll(super.visit(expr));

		return currentObs;
	}

	// Check if an expression is a boolean
	// Boolean expressions are conditions
	protected List<Obligation> addConditions(Expr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions for booleans
		if (expr.accept(this.exprTypeVisitor).equals(NamedType.BOOL)) {
			currentObs.add(new Obligation(expr, true, expr));
			currentObs.add(new Obligation(expr, false, new UnaryExpr(
					UnaryOp.NOT, expr)));
		}

		return currentObs;
	}
}

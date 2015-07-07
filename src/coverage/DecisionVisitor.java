package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

/**
 * Generate obligations for decision coverage
 */
public final class DecisionVisitor extends CoverageVisitor {
	public DecisionVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<Obligation> visit(ArrayAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions
		currentObs.addAll(this.addDecisions(expr));
		if (currentObs.isEmpty()) {
			currentObs.addAll(super.visit(expr));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions
		currentObs.addAll(this.addDecisions(expr));
		if (currentObs.isEmpty()) {
			currentObs.addAll(super.visit(expr));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(CondactExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions
		currentObs.addAll(this.addDecisions(expr));
		if (currentObs.isEmpty()) {
			currentObs.addAll(super.visit(expr));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(IdExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions
		currentObs.addAll(this.addDecisions(expr));
		if (currentObs.isEmpty()) {
			currentObs.addAll(super.visit(expr));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions
		currentObs.addAll(this.addDecisions(expr));
		if (currentObs.isEmpty()) {
			currentObs.addAll(super.visit(expr));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RecordAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions
		currentObs.addAll(this.addDecisions(expr));
		if (currentObs.isEmpty()) {
			currentObs.addAll(super.visit(expr));
		}

		return currentObs;
	}

	// Check if an expression is a boolean
	// Boolean expressions are decisions
	protected List<Obligation> addDecisions(Expr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add decisions for booleans
		if (expr.accept(this.exprTypeVisitor).equals(NamedType.BOOL)) {
			currentObs.add(new Obligation(expr, true, expr));
			currentObs.add(new Obligation(expr, false, new UnaryExpr(
					UnaryOp.NOT, expr)));
		}

		return currentObs;
	}
}

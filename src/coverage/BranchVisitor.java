package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public final class BranchVisitor extends CoverageVisitor {
	public BranchVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add branches
		currentObs.add(new Obligation(expr.cond, true, expr.cond));
		currentObs.add(new Obligation(expr.cond, false, new UnaryExpr(
				UnaryOp.NOT, expr.cond)));

		currentObs.addAll(super.visit(expr));

		return currentObs;
	}
}

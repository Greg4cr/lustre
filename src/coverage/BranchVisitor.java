package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class BranchVisitor extends MCDCVisitor {
	public BranchVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);
		List<Obligation> currentObs = new ArrayList<Obligation>();
		// AND ("and")
		// OR ("or")
		// IMPLIES ("=>")
		// a => b
		// !a or b
		// GREATER (">"), LESS ("<"), GREATEREQUAL (">="), LESSEQUAL ("<=")
		// EQUAL ("="), NOTEQUAL ("<>")
		// PLUS ("+"), MINUS ("-"), MULTIPLY ("*"), DIVIDE ("/")
		// INT_DIVIDE ("div"), MODULUS ("mod")
		if (expr.op.equals(BinaryOp.AND) || expr.op.equals(BinaryOp.OR)
				|| expr.op.equals(BinaryOp.IMPLIES)
				|| expr.op.equals(BinaryOp.GREATER)
				|| expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)
				|| expr.op.equals(BinaryOp.PLUS)
				|| expr.op.equals(BinaryOp.MINUS)
				|| expr.op.equals(BinaryOp.MULTIPLY)
				|| expr.op.equals(BinaryOp.DIVIDE)
				|| expr.op.equals(BinaryOp.INT_DIVIDE)
				|| expr.op.equals(BinaryOp.MODULUS)) {
			currentObs.addAll(leftObs);
			currentObs.addAll(rightObs);
		}
		// ARROW ("->"), can be both boolean and non-boolean
		else if (expr.op.equals(BinaryOp.ARROW)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.ARROW, new BoolExpr(false));
				currentObs.add(leftOb);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(new BoolExpr(false),
						BinaryOp.ARROW, rightOb.obligation);
				currentObs.add(rightOb);
			}
		}
		// No more binary operators
		else {
			throw new IllegalArgumentException("Unknown binary operator: "
					+ expr.op);
		}
		return currentObs;
	}

	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> condObs = expr.cond.accept(this);
		List<Obligation> thenObs = expr.thenExpr.accept(this);
		List<Obligation> elseObs = expr.elseExpr.accept(this);

		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add branches
		currentObs.add(new Obligation(expr.cond, true, expr.cond));
		currentObs.add(new Obligation(expr.cond, false, new UnaryExpr(
				UnaryOp.NOT, expr.cond)));

		currentObs.addAll(condObs);
		for (Obligation thenOb : thenObs) {
			thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND,
					thenOb.obligation);
			currentObs.add(thenOb);
		}
		for (Obligation elseOb : elseObs) {
			elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
					expr.cond), BinaryOp.AND, elseOb.obligation);
			currentObs.add(elseOb);
		}
		return currentObs;
	}

	@Override
	public List<Obligation> visit(IdExpr expr) {
		return new ArrayList<Obligation>();
	}
}
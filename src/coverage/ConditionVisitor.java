package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class ConditionVisitor extends MCDCVisitor {
	public ConditionVisitor(ExprTypeVisitor exprTypeVisitor) {
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
		if (expr.op.equals(BinaryOp.AND) || expr.op.equals(BinaryOp.OR)
				|| expr.op.equals(BinaryOp.IMPLIES)) {
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
		// GREATER (">"), LESS ("<"), GREATEREQUAL (">="), LESSEQUAL ("<=")
		// EQUAL ("="), NOTEQUAL ("<>")
		else if (expr.op.equals(BinaryOp.GREATER)
				|| expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)) {
			// The expression itself is a boolean expression and thus a
			// condition
			currentObs.add(new Obligation(expr, true, expr));
			currentObs.add(new Obligation(expr, false, new UnaryExpr(
					UnaryOp.NOT, expr)));
			currentObs.addAll(leftObs);
			currentObs.addAll(rightObs);
		}
		// PLUS ("+"), MINUS ("-"), MULTIPLY ("*"), DIVIDE ("/")
		// INT_DIVIDE ("div"), MODULUS ("mod")
		else if (expr.op.equals(BinaryOp.PLUS)
				|| expr.op.equals(BinaryOp.MINUS)
				|| expr.op.equals(BinaryOp.MULTIPLY)
				|| expr.op.equals(BinaryOp.DIVIDE)
				|| expr.op.equals(BinaryOp.INT_DIVIDE)
				|| expr.op.equals(BinaryOp.MODULUS)) {
			currentObs.addAll(leftObs);
			currentObs.addAll(rightObs);
		}
		// No more binary operators
		else {
			throw new IllegalArgumentException("Unknown binary operator: "
					+ expr.op);
		}
		return currentObs;
	}
}
package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RealExpr;
import jkind.lustre.TupleExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class MCDCVisitor extends CoverageVisitor {
	public MCDCVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);
		List<Obligation> currentObs = new ArrayList<Obligation>();
		// AND ("and")
		if (expr.op.equals(BinaryOp.AND)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.AND, expr.right);
				currentObs.add(leftOb);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(expr.left, BinaryOp.AND,
						rightOb.obligation);
				currentObs.add(rightOb);
			}
		}
		// OR ("or")
		else if (expr.op.equals(BinaryOp.OR)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
				currentObs.add(leftOb);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
						expr.left), BinaryOp.AND, rightOb.obligation);
				currentObs.add(rightOb);
			}
		}
		// IMPLIES ("=>")
		// a => b
		// !a or b
		else if (expr.op.equals(BinaryOp.IMPLIES)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(leftOb.obligation,
						BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
				currentObs.add(leftOb);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BinaryExpr(expr.left, BinaryOp.AND,
						rightOb.obligation);
				currentObs.add(rightOb);
			}
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

	@Override
	public List<Obligation> visit(UnaryExpr expr) {
		List<Obligation> unaryObs = expr.expr.accept(this);
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Obligation unaryOb : unaryObs) {
			if (expr.op.equals(UnaryOp.NOT)) {
				unaryOb.expressionPolarity = !unaryOb.expressionPolarity;
			} else {
				unaryOb.obligation = new UnaryExpr(expr.op, unaryOb.obligation);
			}
			currentObs.add(unaryOb);
		}
		return currentObs;
	}

	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> condObs = expr.cond.accept(this);
		List<Obligation> thenObs = expr.thenExpr.accept(this);
		List<Obligation> elseObs = expr.elseExpr.accept(this);

		List<Obligation> currentObs = new ArrayList<Obligation>();

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
		List<Obligation> currentObs = new ArrayList<Obligation>();
		String id = expr.id;
		Type type = expr.accept(this.exprTypeVisitor);

		// If this is a boolean variable, then it is a condition
		if (type.equals(NamedType.BOOL)) {
			currentObs.add(new Obligation(id, true, expr));
			currentObs.add(new Obligation(id, false, new UnaryExpr(UnaryOp.NOT,
					expr)));
		}
		return currentObs;
	}

	@Override
	public List<Obligation> visit(BoolExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(IntExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(RealExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		Type type = expr.accept(this.exprTypeVisitor);

		// If the node call expression is of type bool, add it as a condition
		if (type.equals(NamedType.BOOL)) {
			currentObs.add(new Obligation(expr, true, expr));
			currentObs.add(new Obligation(expr, false, new UnaryExpr(
					UnaryOp.NOT, expr)));
		}
		return currentObs;
	}

	@Override
	public List<Obligation> visit(CastExpr expr) {
		return expr.expr.accept(this);
	}

	@Override
	public List<Obligation> visit(TupleExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.elements) {
			currentObs.addAll(e.accept(this));
		}
		return currentObs;
	}
}

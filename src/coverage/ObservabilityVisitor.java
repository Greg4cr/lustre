package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.NamedType;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

/* Generate COMB_USED equations */
public class ObservabilityVisitor extends ConditionVisitor {
	private boolean isDef = false;
	private final String prefix = "token_";
	
	public ObservabilityVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}
	
	public void setIsDef(boolean isDef) {
		this.isDef = isDef;
	}
	
	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);
		
		// and
		// for one opr not be masked, the other one must be true
		if (expr.op.equals(BinaryOp.AND)) {
			for (Obligation leftOb : leftObs) {
				if (expr.left instanceof IdExpr 
						|| expr.left instanceof NodeCallExpr
						|| (expr.left instanceof UnaryExpr 
								&& ((UnaryExpr) expr.left).op.equals(UnaryOp.NOT))) {
					// A and subexpr, or
					// nodeA_call(args), or
					// (not A) and subexpr
					leftOb.obligation = expr.right;
				} else {
					// nesting
					leftOb.obligation = new BinaryExpr(leftOb.obligation,
							BinaryOp.AND, expr.right);
				}
			}
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr 
						|| expr.right instanceof NodeCallExpr
						|| (expr.right instanceof UnaryExpr 
								&& ((UnaryExpr) expr.right).op.equals(UnaryOp.NOT))) {
					// subexpr and A, or
					// subexpr and nodeA_call(args), or
					// subexpr and (not A)
					rightOb.obligation = expr.left;
				} else {
					// nesting
					rightOb.obligation = new BinaryExpr(expr.left, BinaryOp.AND,
							rightOb.obligation);
				}
			}
		}
		// or
		// for one opr not be masked, the other one must be false
		else if (expr.op.equals(BinaryOp.OR)) {
			for (Obligation leftOb : leftObs) {
				if (expr.left instanceof IdExpr
						|| expr.left instanceof NodeCallExpr
						|| (expr.left instanceof UnaryExpr
								&& ((UnaryExpr) expr.left).op.equals(UnaryOp.NOT))) {
					// A or subexpr
					// (not A) or subexpr
					leftOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.right);
				} else {
					// nesting
					leftOb.obligation = new BinaryExpr(leftOb.obligation,
							BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
				}
			}
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr
						|| expr.right instanceof NodeCallExpr
						|| (expr.right instanceof UnaryExpr
								&& ((UnaryExpr) expr.right).op.equals(UnaryOp.NOT))) {
					// subexpr or A
					// subexpr or (not A)
					rightOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.left);
				} else {
					// nesting
					rightOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
							expr.left), BinaryOp.AND, rightOb.obligation);
				}
			}
		} 
		
		// >, >=, <, <=
		// +, -, *, /, div, %
		// ==, <>
		// opr is never masked
		else if (expr.op.equals(BinaryOp.GREATER)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.PLUS)
				|| expr.op.equals(BinaryOp.MINUS)
				|| expr.op.equals(BinaryOp.MULTIPLY)
				|| expr.op.equals(BinaryOp.DIVIDE)
				|| expr.op.equals(BinaryOp.INT_DIVIDE)
				|| expr.op.equals(BinaryOp.MODULUS)
				// Equation of (bool_A = bool_B) has been translated into AND/OR equation
				// The EQUAL equations we meet here must be in forms of (int_A = num) or (num = int_B)
				// And the expression itself is a boolean expression and thus a condition
				// Similar to <>
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BoolExpr(true);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BoolExpr(true);
			}
		}
		
		// a => b
		// it can be treated as an OR expression (!a or b)
		else if (expr.op.equals(BinaryOp.IMPLIES)) {
			for (Obligation leftOb : leftObs) {
				if (expr.left instanceof IdExpr
						|| expr.left instanceof NodeCallExpr
						|| (expr.left instanceof UnaryExpr
								&& ((UnaryExpr) expr.left).op.equals(UnaryOp.NOT))) {
					leftOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.right);
				} else {
					leftOb.obligation = new BinaryExpr(leftOb.obligation,
							BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
				}
			}
			
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr
						|| expr.right instanceof NodeCallExpr
						|| (expr.right instanceof UnaryExpr
								&& ((UnaryExpr) expr.right).op.equals(UnaryOp.NOT))) {
					rightOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.left);
				} else {
					rightOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
							expr.left), BinaryOp.AND, rightOb.obligation);
				}
			}				
		}
		
		// expr_a -> expr_b
		else if (expr.op.equals(BinaryOp.ARROW)) {
			// prepare for ((not (...)) -> ...)
			if (expr.left instanceof UnaryExpr
					&& (((UnaryExpr)expr.left).op.equals(UnaryOp.NOT))
					&& ((UnaryExpr)expr.left).expr instanceof BinaryExpr) {
				BinaryExpr subexpr = ((BinaryExpr)((UnaryExpr)expr.left).expr);
				leftObs = this.visit(subexpr);
			}
			
			// prepare for (... -> (not (...)))
			if (expr.right instanceof UnaryExpr
					&& (((UnaryExpr)expr.right).op.equals(UnaryOp.NOT))
					&& ((UnaryExpr)expr.right).expr instanceof BinaryExpr) {
				BinaryExpr subexpr = ((BinaryExpr)((UnaryExpr)expr.right).expr);
				rightObs = this.visit(subexpr);
			}
			
			// generate obligations
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(new BoolExpr(true),
						BinaryOp.ARROW, new BoolExpr(false));
			}
			
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr
						|| expr.right instanceof NodeCallExpr) {
					// ... -> A
					rightOb.obligation = new BinaryExpr(new BoolExpr(false),
							BinaryOp.ARROW, new BoolExpr(true));
				} else if (expr.right instanceof UnaryExpr
						&& ((UnaryExpr)expr.right).op.equals(UnaryOp.NOT)
						&& (((UnaryExpr)expr.right).expr instanceof IdExpr
							|| ((UnaryExpr)expr.right).expr instanceof NodeCallExpr)) {
					// ... -> (not A)
					rightOb.obligation = new BinaryExpr(new BoolExpr(false),
							BinaryOp.ARROW, new BoolExpr(true));
				} else if (expr.right instanceof UnaryExpr
						&& ((UnaryExpr)expr.right).op.equals(UnaryOp.PRE)) {
					// ... -> (pre A)
					// ... -> (pre ...)
					rightOb.obligation = new BoolExpr(false);
				} else {
					// expr.right is an instance of BinaryExpr
					// A = (not (C and D) -> (not (E or F)));
					// A = (C or D -> (E and F));
					// NOTE: not (...) has been processed
					// at the beginning of this scenario (... -> ...)
					rightOb.obligation = new BinaryExpr(new BoolExpr(false),
							BinaryOp.ARROW, rightOb.obligation);
				}
			}
		}
		
		// xor
		else if (expr.op.equals(BinaryOp.XOR)) {
			throw new IllegalArgumentException(
					"XOR should have been translated.");
		}
		
		obligations.addAll(leftObs);
		obligations.addAll(rightObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		
		if (expr.cond instanceof IdExpr
				|| expr.cond instanceof NodeCallExpr
				|| (expr.cond instanceof UnaryExpr
						&& ((((UnaryExpr)expr.cond).expr instanceof IdExpr)
							|| ((UnaryExpr) expr.cond).expr instanceof NodeCallExpr))) {
			// if (A) else subexpr, or if (not A) else subexpr
			setIsDef(true);
		} else {
			setIsDef(false);
		}
		
		obligations.addAll(expr.cond.accept(this));
		setIsDef(false);
		
		List<Obligation> thenObs = expr.thenExpr.accept(this);
		List<Obligation> elseObs = expr.elseExpr.accept(this);
		
		for (Obligation thenOb : thenObs) {
			if ((expr.thenExpr instanceof IdExpr
					|| expr.thenExpr instanceof NodeCallExpr)
					|| (expr.thenExpr instanceof UnaryExpr
						&& ((UnaryExpr)expr.thenExpr).op.equals(UnaryOp.NOT)
						&& ((UnaryExpr)expr.thenExpr).expr instanceof IdExpr)) {
				thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND, new BoolExpr(true));
			} else {
				thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND, thenOb.obligation);
			}
		}
		
		for (Obligation elseOb : elseObs) {
			if ((expr.elseExpr instanceof IdExpr
					|| expr.elseExpr instanceof NodeCallExpr)
					|| (expr.elseExpr instanceof UnaryExpr
							&& ((UnaryExpr)expr.elseExpr).op.equals(UnaryOp.NOT)
							&& ((UnaryExpr)expr.elseExpr).expr instanceof IdExpr)) {
				elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT, expr.cond), BinaryOp.AND, new BoolExpr(true));
			} else {
				elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT, expr.cond),
						BinaryOp.AND, elseOb.obligation);
			}
		}
		
		obligations.addAll(thenObs);
		obligations.addAll(elseObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(UnaryExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		List<Obligation> unaryObs = expr.expr.accept(this);
		
		for (Obligation unaryOb : unaryObs) {
			if (isDef) {
				unaryOb.obligation = new BoolExpr(true);
			} else {
				if (expr.op.equals(UnaryOp.PRE)) {
					unaryOb.obligation = new BoolExpr(false);
				}
				else { // NOT
					// keep original value
				}
			}
		}
		
		obligations.addAll(unaryObs);
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(IdExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		
		if (isDef) {
			// definition, A = B or A = not B
			obligations.add(new Obligation(expr, true, new BoolExpr(true)));
		} else {
			obligations.add(new Obligation(expr, true, expr));
		}

		return obligations;
	}
	
	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions
		currentObs.addAll(this.addConditions(expr));
		
		for (Expr e : expr.args) {
			if (e.toString().toLowerCase().startsWith(prefix)) {
				continue;
			}
			
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}
	
	@Override
	public List<Obligation> addConditions(Expr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		// Add conditions for booleans
		if (expr.accept(this.exprTypeVisitor).equals(NamedType.BOOL)) {
			currentObs.add(new Obligation(expr, true, expr));
		}

		return currentObs;
	}
}

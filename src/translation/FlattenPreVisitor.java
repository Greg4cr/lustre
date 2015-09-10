package translation;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.EnumType;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.ExprMapVisitor;

/**
 * Move PRE operator to top level, if possible.
 */
public class FlattenPreVisitor extends ExprMapVisitor {
	private final ExprTypeVisitor exprTypeVisitor;
	private boolean replacable = true;

	public FlattenPreVisitor(ExprTypeVisitor exprTypeVisitor) {
		this.exprTypeVisitor = exprTypeVisitor;
	}

	public Expr expr(Expr expr) {
		// Reset flag variable
		this.replacable = true;

		Expr newExpr = expr.accept(this);

		if (this.replacable) {
			return new UnaryExpr(UnaryOp.PRE, newExpr);
		} else {
			return expr;
		}
	}

	@Override
	public Expr visit(BinaryExpr e) {
		// We are already in a followed-by expression from an ARROW operator,
		// remove left expression from additional ARROW operators. We made the
		// assumption that node calls do not store states.
		if (e.op.equals(BinaryOp.ARROW)) {
			return e.right.accept(this);
		}
		return super.visit(e);
	}

	@Override
	public Expr visit(IdExpr e) {
		Type type = e.accept(this.exprTypeVisitor);

		if (type instanceof EnumType) {
			// EnumType variables are values
		} else {
			this.replacable = false;
		}
		return super.visit(e);
	}

	@Override
	public Expr visit(UnaryExpr e) {
		if (e.op.equals(UnaryOp.PRE)) {
			return e.expr;
		}

		return super.visit(e);
	}
}
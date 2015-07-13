package cse;

import jkind.lustre.Expr;
import jkind.lustre.Type;

public final class CSEExpr {
	// The original expression
	public final Expr expr;
	// The IdExpr that represents this.expr
	public final Expr exprVar;
	// Type of this.expr
	public final Type type;

	public CSEExpr(Expr expr, Expr exprVar, Type type) {
		this.expr = expr;
		this.exprVar = exprVar;
		this.type = type;
	}
}

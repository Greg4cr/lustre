package observability.subexpr;

import jkind.lustre.Expr;
import jkind.lustre.Type;

public final class PreSubExpr {
	// the original sub-expression of PRE
	public final Expr preExpr;
	
	// the IdExpr that represent the sub-expression
	public final Expr exprVar;
	
	// type of exprVar
	public final Type type;
	
	public PreSubExpr(Expr preExpr, Expr exprVar, Type type) {
		this.preExpr = preExpr;
		this.exprVar = exprVar;
		this.type = type;
	}
}

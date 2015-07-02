package simulation;

import java.util.HashSet;
import java.util.Set;

import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.ExprIterVisitor;

/**
 * Visit the set of variables that an expression depends on
 */
public final class DependencyVisitor extends ExprIterVisitor {
	public static Set<String> get(Expr expr) {
		DependencyVisitor visitor = new DependencyVisitor();
		expr.accept(visitor);
		return visitor.set;
	}

	private Set<String> set = new HashSet<String>();

	@Override
	public Void visit(IdExpr expr) {
		set.add(expr.id);
		return null;
	}

	@Override
	public Void visit(UnaryExpr expr) {
		// Delayed variables are always available
		if (!expr.op.equals(UnaryOp.PRE)) {
			expr.expr.accept(this);
		}
		return null;
	}
}

package observability;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import types.ExprTypeVisitor;

public final class ObserverVisitor extends VariableVisitor {
	public ObserverVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}

	@Override
	public List<String> visit(UnaryExpr expr) {
		
		if (expr.op.equals(UnaryOp.PRE)) {
			return new ArrayList<String>();
		}
		
		List<String> nodes = new ArrayList<>();
		List<String> unaryNodes = expr.expr.accept(this);
		nodes.addAll(unaryNodes);
				
		return nodes;
	}

}
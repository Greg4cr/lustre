package observability;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import types.ExprTypeVisitor;

public final class DelayVisitor extends VariableVisitor {
	private boolean isImpacted = false;
	
	public DelayVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}
		
	@Override
	public List<String> visit(IdExpr expr) {
		List<String> nodes = new ArrayList<>();
		
		if (isImpacted) {
			nodes.add(expr.id);
//			System.out.println("IdExpr ::: " + nodes);
		}
		
		return nodes;
	}

	@Override
	public List<String> visit(UnaryExpr expr) {
//		System.out.println("UnaryExpr ::: " + expr.toString());
		List<String> nodes = new ArrayList<>();
		
		if (expr.op.equals(UnaryOp.PRE)) {
			isImpacted = true;
			List<String> unaryNodes = expr.expr.accept(this);
			nodes.addAll(unaryNodes);
			isImpacted = false;
		} else {
			List<String> unaryNodes = expr.expr.accept(this);
			nodes.addAll(unaryNodes);
		} 
		
		return nodes;
	}
}
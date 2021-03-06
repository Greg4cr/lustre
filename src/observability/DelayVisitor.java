package observability;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import types.ExprTypeVisitor;

public final class DelayVisitor extends VariableVisitor {
	private boolean isImpacted = false;
	private String prefix = "token_";
	
	public DelayVisitor(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}
		
	@Override
	public List<String> visit(IdExpr expr) {
		List<String> nodes = new ArrayList<>();
		
		if (isImpacted) {
			nodes.add(expr.id);
		}
		
		return nodes;
	}

	@Override
	public List<String> visit(NodeCallExpr expr) {
		List<String> nodes = new ArrayList<>();
		
		if (isImpacted) {
			nodes.add(expr.toString());
			
			for (Expr e : expr.args) {
				if (e.toString().toLowerCase().startsWith(prefix)) {
					continue;
				}
				nodes.addAll(e.accept(this));
			}
		}
		
		return nodes;
	}
	
	@Override
	public List<String> visit(UnaryExpr expr) {
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
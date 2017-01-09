package observability;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Equation;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.visitors.ExprVisitor;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RealExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.TupleExpr;
import types.ExprTypeVisitor;

public class NodeCallVisitor implements ExprVisitor<Map<String, Type>> {
	private ExprTypeVisitor exprTypeVisitor;
	
	public static Map<String, Type> get(ExprTypeVisitor exprTypeVisitor, 
									Node node) {
		return new NodeCallVisitor(exprTypeVisitor).get(node);
	}
	
	
	private NodeCallVisitor(ExprTypeVisitor exprTypeVisitor) {
		this.exprTypeVisitor = exprTypeVisitor;
	}
	
	private Map<String, Type> get(Node node) {
		Map<String, Type> nodecalls = new HashMap<>();
		
		for (Equation e : node.equations) {
			nodecalls.putAll(e.expr.accept(this));
		}
		
		return nodecalls;
	}
	
	@Override
	public Map<String, Type> visit(NodeCallExpr expr) {
		Map<String, Type> nodecalls = new HashMap<>();
				
		nodecalls.put(expr.toString(), this.exprTypeVisitor.visit(expr));
		
		return nodecalls;
	}
	
	@Override
	public Map<String, Type> visit(IdExpr expr) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(ArrayAccessExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(ArrayExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(ArrayUpdateExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(BinaryExpr e) {
		Map<String, Type> nodecalls = new HashMap<>();
		
		nodecalls.putAll(e.left.accept(this));
		nodecalls.putAll(e.right.accept(this));
		return nodecalls;
	}

	@Override
	public Map<String, Type> visit(BoolExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(CastExpr e) {
		return e.expr.accept(this);
	}

	@Override
	public Map<String, Type> visit(CondactExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(IfThenElseExpr e) {
		Map<String, Type> nodecalls = new HashMap<>();
		
		nodecalls.putAll(e.cond.accept(this));
		nodecalls.putAll(e.thenExpr.accept(this));
		nodecalls.putAll(e.elseExpr.accept(this));
		
		return nodecalls;
	}

	@Override
	public Map<String, Type> visit(IntExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(RealExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(RecordAccessExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(RecordExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(RecordUpdateExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(TupleExpr e) {
		return new HashMap<String, Type>();
	}

	@Override
	public Map<String, Type> visit(UnaryExpr e) {
		
		return e.expr.accept(this);
	}
		
}

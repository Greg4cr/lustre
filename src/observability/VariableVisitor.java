package observability;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.RealExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.TupleExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.visitors.ExprVisitor;
import types.ExprTypeVisitor;

public class VariableVisitor implements ExprVisitor<List<String>> {
	protected final ExprTypeVisitor exprTypeVisitor;
	
	public VariableVisitor(ExprTypeVisitor exprTypeVisitor) {
		this.exprTypeVisitor = exprTypeVisitor;
	}
	
	@Override
	public List<String> visit(ArrayAccessExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("ArrayAccessExpr ::: " + expr.toString());
		
		nodes.addAll(expr.array.accept(this));
		nodes.addAll(expr.index.accept(this));
		
		return null;
	}

	@Override
	public List<String> visit(ArrayExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("ArrayExpr ::: " + expr.toString());
		
		for (Expr e : expr.elements) {
			nodes.addAll(e.accept(this));
		}
		
		return nodes;
	}

	@Override
	public List<String> visit(ArrayUpdateExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("ArrayUpdateExpr ::: " + expr.toString());
		
		nodes.addAll(expr.array.accept(this));
		nodes.addAll(expr.index.accept(this));
		nodes.addAll(expr.value.accept(this));
		
		return nodes;
	}

	@Override
	public List<String> visit(BinaryExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("BinaryExpr ::: " + expr.toString());
		
		List<String> leftNodes = expr.left.accept(this);
		List<String> rightNodes = expr.right.accept(this);
		
		nodes.addAll(leftNodes);
		nodes.addAll(rightNodes);
		
		return nodes;
	}

	@Override
	public List<String> visit(BoolExpr expr) {
//		System.out.println("BoolExpr ::: " + expr.toString());
		
		return new ArrayList<String>();
	}

	@Override
	public List<String> visit(CastExpr expr) {
//		System.out.println("CastExpr ::: " + expr.toString());
		return expr.expr.accept(this);
	}

	@Override
	public List<String> visit(CondactExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("CondactExpr ::: " + expr.toString());
		
		nodes.addAll(expr.clock.accept(this));
		nodes.addAll(expr.call.accept(this));
		
		for (Expr e : expr.args) {
			nodes.addAll(e.accept(this));
		}
		
		return nodes;
	}

	@Override
	public List<String> visit(IdExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("IdExpr ::: " + expr.toString());
		nodes.add(expr.id);
		
		return nodes;
	}

	@Override
	public List<String> visit(IfThenElseExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("IfThenElseExpr ::: " + expr.toString());
		
		nodes.addAll(expr.cond.accept(this));
		List<String> thenNodes = expr.thenExpr.accept(this);
		List<String> elseNodes = expr.elseExpr.accept(this);
		
		nodes.addAll(thenNodes);
		nodes.addAll(elseNodes);
		
		return nodes;
	}

	@Override
	public List<String> visit(IntExpr expr) {
//		System.out.println("IntExpr ::: " + expr.toString());
		
		return new ArrayList<String>();
	}

	@Override
	public List<String> visit(NodeCallExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("NodeCallExpr ::: " + expr.toString());
		
		for (Expr e : expr.args) {
			nodes.addAll(e.accept(this));
		}
		
		return nodes;
	}

	@Override
	public List<String> visit(RealExpr expr) {
//		System.out.println("RealExpr ::: " + expr.toString());
		return new ArrayList<String>();
	}

	@Override
	public List<String> visit(RecordAccessExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("RecordAccessExpr ::: " + expr.toString());
		
		nodes.addAll(expr.record.accept(this));
		
		return nodes;
	}

	@Override
	public List<String> visit(RecordExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("RecordExpr ::: " + expr.toString());
		
		for (Expr e : expr.fields.values()) {
			nodes.addAll(e.accept(this));
		}
		return nodes;
	}

	@Override
	public List<String> visit(RecordUpdateExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("RecordUpdateExpr ::: " + expr.toString());
		
		nodes.addAll(expr.record.accept(this));
		nodes.addAll(expr.value.accept(this));
		
		return nodes;
	}

	@Override
	public List<String> visit(TupleExpr expr) {
		List<String> nodes = new ArrayList<>();
//		System.out.println("TupleExpr ::: " + expr.toString());
		
		for (Expr e : expr.elements) {
			nodes.addAll(e.accept(this));
		}
		
		return nodes;
	}

	@Override
	public List<String> visit(UnaryExpr expr) {
//		System.out.println("UnaryExpr ::: " + expr.toString());
		List<String> nodes = new ArrayList<>();
		List<String> unaryNodes = expr.expr.accept(this);
		nodes.addAll(unaryNodes);
				
		return nodes;
	}
}

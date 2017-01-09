package observability.nodecall;

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
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.ExprVisitor;
import types.ExprTypeVisitor;

public class LustreDelayCounter implements ExprVisitor<Integer> {
	protected final ExprTypeVisitor exprTypeVisitor;
	
	public LustreDelayCounter(ExprTypeVisitor exprTypeVisitor) {
		this.exprTypeVisitor = exprTypeVisitor;
	}
	
	@Override
	public Integer visit(ArrayAccessExpr expr) {
		Integer delayNum = 0;
		
		delayNum = Math.max(delayNum, expr.array.accept(this));
		delayNum = Math.max(delayNum, expr.index.accept(this));
		
		return delayNum;
	}

	@Override
	public Integer visit(ArrayExpr expr) {
		Integer delayNum = 0;
		
		for (Expr e : expr.elements) {
			delayNum = Math.max(delayNum, e.accept(this));
		}
		
		return delayNum;
	}

	@Override
	public Integer visit(ArrayUpdateExpr expr) {
		Integer delayNum = 0;
		
		delayNum = Math.max(delayNum, expr.array.accept(this));
		delayNum = Math.max(delayNum, expr.index.accept(this));
		delayNum = Math.max(delayNum, expr.value.accept(this));
		
		return delayNum;
	}

	@Override
	public Integer visit(BinaryExpr expr) {
		Integer delayNum = 0;
		
		delayNum = Math.max(delayNum, expr.left.accept(this));
		delayNum = Math.max(delayNum, expr.right.accept(this));

		return delayNum;
	}

	@Override
	public Integer visit(BoolExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(CastExpr expr) {
		return expr.expr.accept(this);
	}

	@Override
	public Integer visit(CondactExpr expr) {
		Integer delayNum = 0;

		delayNum = Math.max(delayNum, expr.clock.accept(this));
		delayNum = Math.max(delayNum, expr.call.accept(this));
		
		for (Expr e : expr.args) {
			delayNum = Math.max(delayNum, e.accept(this));
		}
		
		return delayNum;
	}

	@Override
	public Integer visit(IdExpr expr) {		
		return 0;
	}

	@Override
	public Integer visit(IfThenElseExpr expr) {
		Integer delayNum = 0;
//		System.out.println("IfThenElseExpr ::: " + expr.toString());
		
		delayNum = Math.max(delayNum, expr.cond.accept(this));
		delayNum = Math.max(delayNum, expr.thenExpr.accept(this));
		delayNum = Math.max(delayNum, expr.elseExpr.accept(this));
		
		return delayNum;
	}

	@Override
	public Integer visit(IntExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(NodeCallExpr expr) {
		Integer delayNum = 0;
		
		for (Expr e : expr.args) {
			delayNum = Math.max(delayNum, e.accept(this));
		}
		return delayNum;
	}

	@Override
	public Integer visit(RealExpr expr) {
		return 0;
	}

	@Override
	public Integer visit(RecordAccessExpr expr) {
		Integer delayNum = 0;
		
		delayNum = Math.max(delayNum, expr.record.accept(this));
		
		return delayNum;
	}

	@Override
	public Integer visit(RecordExpr expr) {
		Integer delayNum = 0;
		
		for (Expr e : expr.fields.values()) {
			delayNum = Math.max(delayNum, e.accept(this));
		}
		return delayNum;
	}

	@Override
	public Integer visit(RecordUpdateExpr expr) {
		Integer delayNum = 0;
		
		delayNum = Math.max(delayNum, expr.record.accept(this));
		delayNum = Math.max(delayNum, expr.value.accept(this));
		
		return delayNum;
	}

	@Override
	public Integer visit(TupleExpr expr) {
		Integer delayNum = 0;
		
		for (Expr e : expr.elements) {
			delayNum = Math.max(delayNum, e.accept(this));
		}
		
		return delayNum;
	}

	@Override
	public Integer visit(UnaryExpr expr) {
//		System.out.println("UnaryExpr ::: " + expr.toString());
		
		if (expr.op.equals(UnaryOp.PRE)) {
			return 1;
		} else {
			Integer delayNum = 0;
			delayNum = Math.max(delayNum, expr.expr.accept(this));
			
			return delayNum;
		}
	}
}

package coverage;

import java.util.ArrayList;
import java.util.List;

import types.ExprTypeVisitor;
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

public class CoverageVisitor implements ExprVisitor<List<Obligation>> {
	protected final ExprTypeVisitor exprTypeVisitor;

	public CoverageVisitor(ExprTypeVisitor exprTypeVisitor) {
		this.exprTypeVisitor = exprTypeVisitor;
	}

	@Override
	public List<Obligation> visit(ArrayAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.array.accept(this));
		currentObs.addAll(expr.index.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(ArrayExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.elements) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(ArrayUpdateExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.array.accept(this));
		currentObs.addAll(expr.index.accept(this));
		currentObs.addAll(expr.value.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.left.accept(this));
		currentObs.addAll(expr.right.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(BoolExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(CastExpr expr) {
		return expr.expr.accept(this);
	}

	@Override
	public List<Obligation> visit(CondactExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.clock.accept(this));
		currentObs.addAll(expr.call.accept(this));

		for (Expr e : expr.args) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(IdExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.cond.accept(this));
		currentObs.addAll(expr.thenExpr.accept(this));
		currentObs.addAll(expr.elseExpr.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(IntExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.args) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RealExpr expr) {
		return new ArrayList<Obligation>();
	}

	@Override
	public List<Obligation> visit(RecordAccessExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.record.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RecordExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.fields.values()) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(RecordUpdateExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.record.accept(this));
		currentObs.addAll(expr.value.accept(this));

		return currentObs;
	}

	@Override
	public List<Obligation> visit(TupleExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		for (Expr e : expr.elements) {
			currentObs.addAll(e.accept(this));
		}

		return currentObs;
	}

	@Override
	public List<Obligation> visit(UnaryExpr expr) {
		List<Obligation> currentObs = new ArrayList<Obligation>();

		currentObs.addAll(expr.expr.accept(this));

		return currentObs;
	}
}
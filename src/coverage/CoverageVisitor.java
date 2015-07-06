package coverage;

import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(ArrayExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(ArrayUpdateExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(BoolExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(CastExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(CondactExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(IdExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(IntExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(NodeCallExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(RealExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(RecordAccessExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(RecordExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(RecordUpdateExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(TupleExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Obligation> visit(UnaryExpr expr) {
		// TODO Auto-generated method stub
		return null;
	}
}
package translation;

import java.util.Map;

import types.ExprTypeVisitor;
import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Expr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.TupleExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.visitors.AstMapVisitor;

public class LustreCSE extends AstMapVisitor {
	public static Program get(Program program) {
		LustreCSE cse = new LustreCSE(program);
		return cse.visit(program);
	}

	private final ExprTypeVisitor exprTypeVisitor;
	private Map<String, Integer> exprUse;

	private LustreCSE(Program program) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
	}

	@Override
	public Node visit(Node node) {
		this.exprTypeVisitor.setNodeContext(node);
		this.exprUse = ExprUseVisitor.get(node);
		return super.visit(node);
	}

	@Override
	public Expr visit(ArrayAccessExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(ArrayExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(ArrayUpdateExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(BinaryExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(CastExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(CondactExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(IfThenElseExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(NodeCallExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(RecordAccessExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(RecordExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(RecordUpdateExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(TupleExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}

	@Override
	public Expr visit(UnaryExpr e) {
		// TODO Auto-generated method stub
		return super.visit(e);
	}
}

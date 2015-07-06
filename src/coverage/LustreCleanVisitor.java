package coverage;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Remove XOR and boolean equality/inequality
 */
public final class LustreCleanVisitor extends AstMapVisitor {
	private final ExprTypeVisitor exprTypeVisitor;

	public static Program program(Program program) {
		return new LustreCleanVisitor(program).visit(program);
	}

	private LustreCleanVisitor(Program program) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
	}

	@Override
	public Node visit(Node node) {
		this.exprTypeVisitor.setNodeContext(node);
		return super.visit(node);
	}

	@Override
	public Expr visit(BinaryExpr expr) {
		Expr leftExpr = expr.left.accept(this);
		Expr rightExpr = expr.right.accept(this);

		if ((expr.op.equals(BinaryOp.XOR) || expr.op.equals(BinaryOp.EQUAL) || expr.op
				.equals(BinaryOp.NOTEQUAL))
				&& expr.left.accept(this.exprTypeVisitor)
						.equals(NamedType.BOOL)) {
			if (expr.op.equals(BinaryOp.EQUAL)) {
				Expr left = new BinaryExpr(leftExpr, BinaryOp.AND, rightExpr);
				Expr right = new BinaryExpr(
						new UnaryExpr(UnaryOp.NOT, leftExpr), BinaryOp.AND,
						new UnaryExpr(UnaryOp.NOT, rightExpr));
				return new BinaryExpr(left, BinaryOp.OR, right);
			}
			// Otherwise it should be boolean unequal or XOR
			else {
				Expr left = new BinaryExpr(leftExpr, BinaryOp.AND,
						new UnaryExpr(UnaryOp.NOT, rightExpr));
				Expr right = new BinaryExpr(
						new UnaryExpr(UnaryOp.NOT, leftExpr), BinaryOp.AND,
						rightExpr);
				return new BinaryExpr(left, BinaryOp.OR, right);
			}
		}
		return super.visit(expr);
	}
}

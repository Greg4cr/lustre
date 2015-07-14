package coverage;

import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
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
	public Program visit(Program e) {
		// Only visit nodes
		List<Node> nodes = visitNodes(e.nodes);
		return new Program(e.location, e.types, e.constants, nodes, e.main);
	}

	@Override
	public Node visit(Node e) {
		this.exprTypeVisitor.setNodeContext(e);
		// Only visit equations
		List<Equation> equations = visitEquations(e.equations);
		// Get rid of e.realizabilityInputs
		return new Node(e.location, e.id, e.inputs, e.outputs, e.locals,
				equations, e.properties, e.assertions, null);
	}

	@Override
	public Expr visit(BinaryExpr e) {
		if ((e.op.equals(BinaryOp.XOR) || e.op.equals(BinaryOp.EQUAL) || e.op
				.equals(BinaryOp.NOTEQUAL))
				&& e.left.accept(this.exprTypeVisitor).equals(NamedType.BOOL)) {
			Expr leftExpr = e.left.accept(this);
			Expr rightExpr = e.right.accept(this);

			if (e.op.equals(BinaryOp.EQUAL)) {
				Expr left = new BinaryExpr(leftExpr, BinaryOp.AND, rightExpr);
				Expr right = new BinaryExpr(
						new UnaryExpr(UnaryOp.NOT, leftExpr), BinaryOp.AND,
						new UnaryExpr(UnaryOp.NOT, rightExpr));
				return new BinaryExpr(e.location, left, BinaryOp.OR, right);
			}
			// Otherwise it should be boolean unequal or XOR
			else {
				Expr left = new BinaryExpr(leftExpr, BinaryOp.AND,
						new UnaryExpr(UnaryOp.NOT, rightExpr));
				Expr right = new BinaryExpr(
						new UnaryExpr(UnaryOp.NOT, leftExpr), BinaryOp.AND,
						rightExpr);
				return new BinaryExpr(e.location, left, BinaryOp.OR, right);
			}
		}
		return super.visit(e);
	}
}

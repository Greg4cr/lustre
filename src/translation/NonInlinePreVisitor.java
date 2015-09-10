package translation;

import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Non-inline PRE expressions. Assuming all PRE expressions are correctly
 * guarded.
 */
public class NonInlinePreVisitor extends AstMapVisitor {
	public static Program program(Program program) {
		return new NonInlinePreVisitor(program).visit(program);
	}

	private final ExprTypeVisitor exprTypeVisitor;

	private NonInlinePreVisitor(Program program) {
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

		// Also added variables that replace PRE expressions

		// Get rid of e.realizabilityInputs
		return new Node(e.location, e.id, e.inputs, e.outputs, e.locals,
				equations, e.properties, e.assertions, null);
	}

	@Override
	public Equation visit(Equation e) {
		// Handle top-level ARROW and PRE here
		if (e.expr instanceof BinaryExpr) {
			BinaryExpr be = (BinaryExpr) e.expr;
			if (be.op.equals(BinaryOp.ARROW) && be.right instanceof UnaryExpr) {
				UnaryExpr ue = (UnaryExpr) be.right;
				if (ue.op.equals(UnaryOp.PRE)) {
					if (ue.expr instanceof IdExpr) {
						// Leave this equation unchanged
					} else {
						// Replace ue.expr with a variable
					}
				}
			}
		}
		// Do not traverse e.lhs since they do not really act like Exprs
		return new Equation(e.location, e.lhs, e.expr.accept(this));
	}

	@Override
	public Expr visit(UnaryExpr e) {
		if (e.op.equals(UnaryOp.PRE)) {
			// Non-inline this PRE expression
			if (e.expr instanceof IdExpr) {
			} else {
			}
		}

		return super.visit(e);
	}
}

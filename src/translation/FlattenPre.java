package translation;

import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Optimization to reduce the number of PRE operators.
 */
public final class FlattenPre extends AstMapVisitor {
	public static Program program(Program program) {
		return new FlattenPre(program).visit(program);
	}

	private final ExprTypeVisitor exprTypeVisitor;
	private final FlattenPreVisitor flattenPreVisitor;

	public FlattenPre(Program program) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.flattenPreVisitor = new FlattenPreVisitor(this.exprTypeVisitor);
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
		// For ARROW operators, check for PRE operators
		if (e.op.equals(BinaryOp.ARROW)) {
			return new BinaryExpr(e.location, e.left, BinaryOp.ARROW,
					this.flattenPreVisitor.expr(e.right));
		}
		return super.visit(e);
	}
}

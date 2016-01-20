package concatenation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Node;
import jkind.lustre.RealExpr;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.AstMapVisitor;
import lustre.LustreTrace;

/**
 * Insert history values for a node, used for incremental test generation.
 */
public final class CreateHistoryVisitor extends AstMapVisitor {
	public static Node node(Node node, LustreTrace history) {
		CreateHistoryVisitor visitor = new CreateHistoryVisitor(node, history);
		return visitor.visit(node);
	}

	private final Map<String, VarDecl> inputMapping;
	private final LustreTrace history;
	private final int lastStep;

	private CreateHistoryVisitor(Node node, LustreTrace history) {
		this.inputMapping = new HashMap<String, VarDecl>();
		for (VarDecl var : node.inputs) {
			this.inputMapping.put(var.id, new VarDecl(
					var.id + "_concatenation", var.type));
		}

		this.history = history;
		this.lastStep = history.getLength() - 1;
	}

	private Expr getHistoryExpr(Value value) {
		if (value instanceof BooleanValue) {
			BooleanValue v = (BooleanValue) value;
			return new BoolExpr(v.value);
		} else if (value instanceof IntegerValue) {
			IntegerValue v = (IntegerValue) value;
			return new IntExpr(v.value);
		} else if (value instanceof RealValue) {
			RealValue v = (RealValue) value;
			BigDecimal num = new BigDecimal(v.value.getNumerator());
			BigDecimal deno = new BigDecimal(v.value.getDenominator());
			return new RealExpr(num.divide(deno));
		} else {
			throw new IllegalArgumentException("Unknown value: " + value);
		}
	}

	@Override
	public Node visit(Node e) {
		List<VarDecl> locals = new ArrayList<VarDecl>();
		locals.addAll(e.locals);
		locals.addAll(this.inputMapping.values());

		List<Equation> equations = visitEquations(e.equations);

		// Add equations for auxiliary variables for inputs
		for (String input : this.inputMapping.keySet()) {
			IdExpr lhs = new IdExpr(this.inputMapping.get(input).id);
			Value value = this.history.getVariable(input).getValue(lastStep);
			Expr newExpr = new BinaryExpr(this.getHistoryExpr(value),
					BinaryOp.ARROW, new IdExpr(input));
			equations.add(new Equation(lhs, newExpr));
		}

		// Get rid of e.properties, e.assertions, and e.realizabilityInputs
		return new Node(e.location, e.id, e.inputs, e.outputs, locals,
				equations, null, null, null, null, null);
	}

	@Override
	public Equation visit(Equation e) {
		if (e.lhs.size() != 1) {
			throw new IllegalArgumentException(
					"Invalid number of lhs variables: " + e);
		}

		String id = e.lhs.get(0).id;
		Value value = this.history.getVariable(id).getValue(lastStep);
		Expr newExpr = new BinaryExpr(this.getHistoryExpr(value),
				BinaryOp.ARROW, e.expr.accept(this));

		return new Equation(e.location, e.lhs, newExpr);
	}

	@Override
	public Expr visit(IdExpr e) {
		if (this.inputMapping.containsKey(e.id)) {
			return new IdExpr(this.inputMapping.get(e.id).id);
		} else {
			return super.visit(e);
		}
	}
}

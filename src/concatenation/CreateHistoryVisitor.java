package concatenation;

import java.math.BigDecimal;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IntExpr;
import jkind.lustre.Node;
import jkind.lustre.RealExpr;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.AstMapVisitor;
import lustre.LustreTrace;

public final class CreateHistoryVisitor extends AstMapVisitor {
	public static Node node(Node node, LustreTrace history) {
		CreateHistoryVisitor visitor = new CreateHistoryVisitor(node, history);
		return visitor.visit(node);
	}

	private final LustreTrace history;
	private final int lastStep;

	private CreateHistoryVisitor(Node node, LustreTrace history) {
		this.history = history;
		this.lastStep = history.getLength() - 1;
	}

	@Override
	public Equation visit(Equation e) {
		if (e.lhs.size() != 1) {
			throw new IllegalArgumentException(
					"Invalid number of lhs variables: " + e);
		}
		String id = e.lhs.get(0).id;

		Value value = this.history.getVariable(id).getValue(lastStep);

		Expr historyExpr = null;
		if (value instanceof BooleanValue) {
			BooleanValue v = (BooleanValue) value;
			historyExpr = new BoolExpr(v.value);
		} else if (value instanceof IntegerValue) {
			IntegerValue v = (IntegerValue) value;
			historyExpr = new IntExpr(v.value);
		} else if (value instanceof RealValue) {
			RealValue v = (RealValue) value;
			BigDecimal num = new BigDecimal(v.value.getNumerator());
			BigDecimal deno = new BigDecimal(v.value.getDenominator());
			historyExpr = new RealExpr(num.divide(deno));
		} else {
			throw new IllegalArgumentException("Unknown value: " + value);
		}

		Expr newExpr = new BinaryExpr(historyExpr, BinaryOp.ARROW, e.expr);

		return new Equation(e.location, e.lhs, newExpr);
	}
}

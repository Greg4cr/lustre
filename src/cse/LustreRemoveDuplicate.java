package cse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.LustreMain;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Remove duplicate expressions.
 */
public final class LustreRemoveDuplicate extends AstMapVisitor {
	public static Program program(Program program) {
		LustreMain.log("------------Removing duplicate expressions.");
		return new LustreRemoveDuplicate().visit(program);
	}

	private final Map<String, List<IdExpr>> exprToVarMapping;
	private final Map<String, IdExpr> varToReplace;

	private LustreRemoveDuplicate() {
		this.exprToVarMapping = new HashMap<String, List<IdExpr>>();
		this.varToReplace = new HashMap<String, IdExpr>();
	}

	@Override
	public Node visit(Node node) {
		LustreMain.log("Node: " + node.id);

		// Reset
		this.exprToVarMapping.clear();
		this.varToReplace.clear();

		List<Equation> equations = new ArrayList<Equation>();

		// Add existing expressions
		for (Equation equation : node.equations) {
			String exprStr = equation.expr.toString();
			// Record duplicate expressions
			if (this.exprToVarMapping.containsKey(exprStr)) {
				List<IdExpr> existing = this.exprToVarMapping.get(exprStr);

				if (equation.lhs.size() == existing.size()) {
					for (int i = 0; i < existing.size(); i++) {
						this.varToReplace.put(equation.lhs.get(i).toString(),
								existing.get(i));
					}
				} else {
					throw new IllegalArgumentException(
							"Mismatched lhs expressions: " + equation.lhs + " "
									+ existing);
				}
			} else {
				this.exprToVarMapping.put(exprStr, equation.lhs);
				equations.add(equation);
			}
		}

		equations = visitEquations(equations);

		// Iterate on locals
		List<VarDecl> locals = new ArrayList<VarDecl>();

		for (VarDecl varDecl : node.locals) {
			if (!this.varToReplace.containsKey(varDecl.id)) {
				locals.add(varDecl);
			}
		}

		LustreMain.log("Duplicate expressions: " + this.varToReplace.size());

		// Get rid of e.realizabilityInputs
		return new Node(node.location, node.id, node.inputs, node.outputs,
				locals, equations, node.properties, node.assertions, null,
				null, null);
	}

	@Override
	public Expr visit(IdExpr e) {
		if (this.varToReplace.containsKey(e.id)) {
			return this.varToReplace.get(e.id);
		} else {
			return super.visit(e);
		}
	}
}

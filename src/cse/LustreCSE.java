package cse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.LustreMain;
import types.ExprTypeVisitor;
import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.TupleExpr;
import jkind.lustre.TupleType;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Perform common subexpression elimination on the Lustre program
 */
public final class LustreCSE extends AstMapVisitor {
	public static Program get(Program program, int cse) {
		LustreMain.log("------------Eliminating subexpressions used more than "
				+ cse + " time(s)");
		LustreCSE lustreCSE = new LustreCSE(program, cse);

		Program output = program;
		int iteration = 1;

		do {
			LustreMain.log("------------Iteration: " + iteration++);
			output = lustreCSE.visit(output);
		} while (lustreCSE.changed);

		return output;
	}

	private final ExprTypeVisitor exprTypeVisitor;
	private final int cse;
	// Count replaced expressions
	private int count;
	// Indicate whether there are replaced expressions in one iteration
	private boolean changed;

	private Map<String, Integer> exprUse;
	private Map<String, CSEExpr> exprToVarMapping;

	private LustreCSE(Program program, int cse) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.cse = cse;
		this.count = 0;
		this.changed = false;
	}

	// Get a variable Expr for an Expr
	private Expr getExprVar(Expr expr) {
		String exprStr = expr.toString();
		if (this.exprToVarMapping.containsKey(exprStr)) {
			return this.exprToVarMapping.get(exprStr).exprVar;
		} else {
			Type type = expr.accept(this.exprTypeVisitor);
			List<Expr> elements = new ArrayList<Expr>();

			if (type instanceof TupleType) {
				TupleType tupleType = (TupleType) type;

				// Create the same number of IdExpr for this TupleType
				for (int i = 0; i < tupleType.types.size(); i++) {
					elements.add(new IdExpr("CSEVar_" + count++));
				}
			} else {
				elements.add(new IdExpr("CSEVar_" + count++));
			}

			Expr exprVar = TupleExpr.compress(elements);
			this.exprToVarMapping
					.put(exprStr, new CSEExpr(expr, exprVar, type));
			return exprVar;
		}
	}

	@Override
	public Program visit(Program program) {
		this.changed = false;
		// Iterate on nodes
		List<Node> nodes = visitNodes(program.nodes);
		return new Program(program.location, program.types, program.constants,
				nodes, program.main);
	}

	@Override
	public Node visit(Node node) {
		this.exprTypeVisitor.setNodeContext(node);
		this.exprUse = ExprUseVisitor.get(node);
		this.exprToVarMapping = new HashMap<String, CSEExpr>();

		// Iterate on locals and equations
		List<VarDecl> locals = visitVarDecls(node.locals);
		List<Equation> equations = visitEquations(node.equations);

		LustreMain.log(node.id + ": " + this.exprToVarMapping.size());

		if (!this.exprToVarMapping.isEmpty()) {
			this.changed = true;
		}

		for (CSEExpr cseExpr : this.exprToVarMapping.values()) {
			List<IdExpr> elements = new ArrayList<IdExpr>();

			if (cseExpr.type instanceof TupleType) {
				TupleExpr tupleExpr = (TupleExpr) cseExpr.exprVar;
				TupleType tupleType = (TupleType) cseExpr.type;

				// Create the same number of IdExpr for this TupleType
				for (int i = 0; i < tupleType.types.size(); i++) {
					String id = tupleExpr.elements.get(i).toString();
					locals.add(new VarDecl(id, tupleType.types.get(i)));
					elements.add(new IdExpr(id));
				}
			} else {
				String id = cseExpr.exprVar.toString();
				locals.add(new VarDecl(id, cseExpr.type));
				elements.add(new IdExpr(id));
			}
			equations.add(new Equation(elements, cseExpr.expr));
		}

		return new Node(node.location, node.id, node.inputs, node.outputs,
				locals, equations, node.properties, node.assertions,
				node.realizabilityInputs);
	}

	@Override
	public Expr visit(ArrayAccessExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(ArrayExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(ArrayUpdateExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(BinaryExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(CastExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(CondactExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			// Do not visit expr.call, CondactExpr requires expr.call to be a
			// NodeCallExpr (cannot be replaced with an IdExpr)
			return new CondactExpr(expr.clock.accept(this), expr.call,
					visitExprs(expr.args));
		}
	}

	@Override
	public Expr visit(IfThenElseExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(NodeCallExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(RecordAccessExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(RecordExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(RecordUpdateExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}

	@Override
	public Expr visit(UnaryExpr expr) {
		if (this.exprUse.get(expr.toString()) > cse) {
			return this.getExprVar(expr);
		} else {
			return super.visit(expr);
		}
	}
}

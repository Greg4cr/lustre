package cse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import types.ExprTypeVisitor;
import main.LustreMain;
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
 * Perform common subexpression elimination on a program.
 */
public final class LustreCSE extends AstMapVisitor {
	public static Program program(Program program, int cse) {
		LustreMain.log("------------Eliminating subexpressions used more than "
				+ cse + " time(s)");
		ExprTypeVisitor exprTypeVisitor = new ExprTypeVisitor(program);

		List<Node> nodes = new ArrayList<Node>();

		for (Node node : program.nodes) {
			LustreMain.log("Node: " + node.id);
			LustreCSE nodeCSE = new LustreCSE(exprTypeVisitor, node, cse);

			Node translated = node;
			int prevCSESize = -1;

			while (nodeCSE.exprToVarMapping.size() > prevCSESize) {
				prevCSESize = nodeCSE.exprToVarMapping.size();
				translated = nodeCSE.visit(translated);
			}

			LustreMain.log("Replaced expressions: "
					+ nodeCSE.exprToVarMapping.size());

			nodes.add(translated);
		}

		return new Program(program.location, program.types, program.constants,
				nodes, program.main);
	}

	private final ExprTypeVisitor exprTypeVisitor;
	// Mapping from an expression as a String to the number of times it is used
	private final Map<String, Integer> exprUse;
	// Mapping from an expression as a String to a CSEExpr
	private final Map<String, CSEExpr> exprToVarMapping;
	// The set of subexpressions that are replaced in an iteration
	private final List<CSEExpr> replacedExprs;

	private final int cse;
	// Count replaced expressions
	private int count;

	private LustreCSE(ExprTypeVisitor exprTypeVisitor, Node node, int cse) {
		this.exprTypeVisitor = exprTypeVisitor;
		this.exprTypeVisitor.setNodeContext(node);
		this.exprUse = new HashMap<String, Integer>();
		this.exprToVarMapping = new HashMap<String, CSEExpr>();
		this.replacedExprs = new ArrayList<CSEExpr>();
		this.cse = cse;
		this.count = 0;
		// Add existing variables
		for (Equation equation : node.equations) {
			String exprStr = equation.expr.toString();
			if (!this.exprToVarMapping.containsKey(exprStr)) {
				Type type = equation.expr.accept(this.exprTypeVisitor);
				Expr exprVar = TupleExpr.compress(equation.lhs);
				this.exprToVarMapping.put(exprStr, new CSEExpr(equation.expr,
						exprVar, type));
			}
		}
	}

	// Get a variable Expr for an Expr
	private Expr getExprVar(Expr expr) {
		String exprStr = expr.toString();
		// Get and return the variable if the expression already exists
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
			CSEExpr cseExpr = new CSEExpr(expr, exprVar, type);
			this.exprToVarMapping.put(exprStr, cseExpr);
			this.replacedExprs.add(cseExpr);
			return exprVar;
		}
	}

	@Override
	public Program visit(Program program) {
		return null;
	}

	@Override
	public Node visit(Node node) {
		this.exprTypeVisitor.setNodeContext(node);
		this.exprUse.clear();
		this.exprUse.putAll(ExprUseVisitor.get(node));
		this.replacedExprs.clear();

		// Iterate on locals and equations
		List<VarDecl> locals = new ArrayList<VarDecl>();
		locals.addAll(node.locals);

		List<Equation> equations = visitEquations(node.equations);

		for (CSEExpr cseExpr : this.replacedExprs) {
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
		// Get rid of e.realizabilityInputs
		return new Node(node.location, node.id, node.inputs, node.outputs,
				locals, equations, node.properties, node.assertions, null,
				null, null);
	}

	@Override
	public Equation visit(Equation equation) {
		Expr replaced = equation.expr.accept(this);

		String lhs = TupleExpr.compress(equation.lhs).toString();

		// Avoid replacing the expression with the variable itself
		if (lhs.equals(replaced.toString())) {
			return equation;
		} else {
			return new Equation(equation.location, equation.lhs, replaced);
		}
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
			// NodeCallExpr (cannot be replaced with other types of expressions)
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

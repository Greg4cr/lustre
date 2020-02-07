package cse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import types.ExprTypeVisitor;
import main.LustreMain;
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
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.TupleExpr;
import jkind.lustre.TupleType;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Perform common subexpression elimination on a program.
 */
public final class LustreCSE extends AstMapVisitor {
	public static Program program(Program program, int cse) {
		LustreMain.log("------------Eliminating subexpressions used more than "
				+ cse + " time(s)");
		return new LustreCSE(program, cse).visit(program);
	}
	
	// Perform an aggressive non-inlining.
	public static Program program(Program program, int cse, boolean noninline) {
		LustreMain.log("------------Eliminating subexpressions used more than "
				+ cse + " time(s)");
		return new LustreCSE(program, cse, noninline).visit(program);
	}

	private final ExprTypeVisitor exprTypeVisitor;
	private final int cse;

	// All existing variables in a node
	private final List<String> existingVars;
	// Mapping from an expression as a String to the number of times it is used
	private final Map<String, Integer> exprUse;
	// Mapping from an expression as a String to a CSEExpr
	private final Map<String, CSEExpr> exprToVarMapping;
	// The set of subexpressions that are replaced in an iteration
	private final List<CSEExpr> replacedExprs;
	// Count replaced expressions
	private int count;

	private LustreCSE(Program program, int cse) {
		if (cse < 1) {
			throw new IllegalArgumentException("CSE has to be positive.");
		}
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.cse = cse;
		this.existingVars = new ArrayList<String>();
		this.exprUse = new HashMap<String, Integer>();
		this.exprToVarMapping = new HashMap<String, CSEExpr>();
		this.replacedExprs = new ArrayList<CSEExpr>();
		this.count = 0;
	}
	
	/* Alternative constructor used for noninlining. In the noninlined version, 
	 * we do not perform CSE elimination on pre expressions */
	private LustreCSE(Program program, int cse, boolean noninline) {
		if ((cse < 0) || (cse == 0 && !noninline)) {//TODO
			throw new IllegalArgumentException("CSE has to be positive.");
		}
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.cse = cse;
		this.existingVars = new ArrayList<String>();
		this.exprUse = new HashMap<String, Integer>();
		this.exprToVarMapping = new HashMap<String, CSEExpr>();
		this.replacedExprs = new ArrayList<CSEExpr>();
		this.count = 0;
	}

	@Override
	public Program visit(Program e) {
		List<Node> nodes = new ArrayList<Node>();

		for (Node node : e.nodes) {
			LustreMain.log("Node: " + node.id);

			Node translated = node;
			int prevCSESize = -1;

			while (this.exprToVarMapping.size() > prevCSESize) {
				prevCSESize = this.exprToVarMapping.size();
				translated = this.visit(translated);
			}

			nodes.add(translated);
		}

		return new Program(e.location, e.types, e.constants, nodes, e.main);
	}

	@Override
	public Node visit(Node e) {
		// Reset
		this.exprTypeVisitor.setNodeContext(e);
		this.existingVars.clear();
		this.exprUse.clear();
		this.exprToVarMapping.clear();
		this.replacedExprs.clear();
		this.count = 0;

		// Add existing variables
		for (VarDecl varDecl : e.inputs) {
			this.existingVars.add(varDecl.id);
		}
		for (VarDecl varDecl : e.locals) {
			this.existingVars.add(varDecl.id);
		}
		for (VarDecl varDecl : e.outputs) {
			this.existingVars.add(varDecl.id);
		}

		// Add existing expressions
		for (Equation equation : e.equations) {
			String exprStr = equation.expr.toString();
			// Add the expression only once, if there are duplicate expressions
			if (!this.exprToVarMapping.containsKey(exprStr)) {
				Type type = equation.expr.accept(this.exprTypeVisitor);
				Expr exprVar = TupleExpr.compress(equation.lhs);
				this.exprToVarMapping.put(exprStr, new CSEExpr(equation.expr,
						exprVar, type));
			}
		}

		this.exprUse.putAll(ExprUseVisitor.get(e));

		// Iterate on locals and equations
		List<VarDecl> locals = new ArrayList<VarDecl>();
		locals.addAll(e.locals);

		List<Equation> equations = visitEquations(e.equations);

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

		LustreMain.log("Replaced expressions: " + this.replacedExprs.size());

		// Get rid of e.realizabilityInputs
		return new Node(e.location, e.id, e.inputs, e.outputs, locals,
				equations, e.properties, e.assertions, null, null, null);
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
	public Expr visit(ArrayExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(ArrayUpdateExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(BinaryExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(CastExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(CondactExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(IfThenElseExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(NodeCallExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(RecordExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(RecordUpdateExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		if (this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	@Override
	public Expr visit(UnaryExpr e) {
		Expr newExpr = super.visit(e);
		Type type = e.accept(this.exprTypeVisitor);

		// Always non-inline PRE expressions
		if (e.op.equals(UnaryOp.PRE) || this.exprUse.get(e.toString()) > cse) {
			return this.getExprVar(newExpr, type);
		} else {
			return newExpr;
		}
	}

	// Get a variable Expr for an Expr
	private Expr getExprVar(Expr expr, Type type) {
		String exprStr = expr.toString();
		// Get and return the variable if the expression already exists
		if (this.exprToVarMapping.containsKey(exprStr)) {
			return this.exprToVarMapping.get(exprStr).exprVar;
		} else {
			List<Expr> elements = new ArrayList<Expr>();

			// Although we do not replace TupleExpr directly, they can appear in
			// other expressions.
			if (type instanceof TupleType) {
				TupleType tupleType = (TupleType) type;

				// Create the same number of IdExpr for this TupleType
				for (int i = 0; i < tupleType.types.size(); i++) {
					elements.add(new IdExpr(getExprId()));
				}
			} else {
				elements.add(new IdExpr(getExprId()));
			}

			Expr exprVar = TupleExpr.compress(elements);
			CSEExpr cseExpr = new CSEExpr(expr, exprVar, type);
			this.exprToVarMapping.put(exprStr, cseExpr);
			this.replacedExprs.add(cseExpr);
			return exprVar;
		}
	}

	// Get an unused expression id
	private String getExprId() {
		while (this.existingVars.contains("CSEVar_" + count)) {
			count++;
		}
		return "CSEVar_" + count++;
	}
	
}

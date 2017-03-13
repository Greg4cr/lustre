package observability.subexpr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cse.CSEExpr;
import cse.ExprUseVisitor;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
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
import main.LustreMain;
import types.ExprTypeVisitor;

public final class LustrePreSubexpr extends AstMapVisitor {
	public static Program program(Program program) {
		return new LustrePreSubexpr(program).visit(program);
	}
	
	private final ExprTypeVisitor exprTypeVisitor;

	// All existing variables in a node
	private final List<String> existingVars;
	// Mapping from an expression as a String to the number of times it is used
	private final Map<String, Integer> exprUse;
	// Mapping from an expression as a String to a PreSubVar
	private final Map<String, PreSubExpr> exprToVarMapping;
	// The set of subexpressions that are replaced in an iteration
	private final List<PreSubExpr> replacedExprs;
	// Count replaced expressions
	private int count;
	
	private LustrePreSubexpr(Program program) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.existingVars = new ArrayList<String>();
		this.exprUse = new HashMap<String, Integer>();
		this.exprToVarMapping = new HashMap<String, PreSubExpr>();
		this.replacedExprs = new ArrayList<PreSubExpr>();
		this.count = 0;
	}
	
	@Override
	public Program visit(Program program) {
		List<Node> nodes = new ArrayList<Node>();

		for (Node node : program.nodes) {
			LustreMain.log("Node: " + node.id);

			Node translated = this.visit(node);
						
			nodes.add(translated);
		}

		return new Program(program.location, program.types, program.constants, 
				nodes, program.main);
	}
	
	@Override
	public Node visit(Node node) {
		// Reset
		this.exprTypeVisitor.setNodeContext(node);
		this.existingVars.clear();
		this.exprUse.clear();
		this.exprToVarMapping.clear();
		this.replacedExprs.clear();
		this.count = 0;

		// Add existing variables
		for (VarDecl varDecl : node.inputs) {
			this.existingVars.add(varDecl.id);
		}
		for (VarDecl varDecl : node.locals) {
			this.existingVars.add(varDecl.id);
		}
		for (VarDecl varDecl : node.outputs) {
			this.existingVars.add(varDecl.id);
		}

		// Add existing expressions
		for (Equation equation : node.equations) {
			String exprStr = equation.expr.toString();
			// Add the expression only once, if there are duplicate expressions
			if (!this.exprToVarMapping.containsKey(exprStr)) {
				Type type = equation.expr.accept(this.exprTypeVisitor);
				Expr exprVar = TupleExpr.compress(equation.lhs);
				this.exprToVarMapping.put(exprStr, new PreSubExpr(equation.expr,
						exprVar, type));
			}
		}

		this.exprUse.putAll(ExprUseVisitor.get(node));

		// Iterate on locals and equations
		List<VarDecl> locals = new ArrayList<VarDecl>();
		locals.addAll(node.locals);

		List<Equation> equations = visitEquations(node.equations);
		
		for (PreSubExpr preSubExpr : this.replacedExprs) {
			List<IdExpr> elements = new ArrayList<IdExpr>();

			if (preSubExpr.type instanceof TupleType) {
				TupleExpr tupleExpr = (TupleExpr) preSubExpr.exprVar;
				TupleType tupleType = (TupleType) preSubExpr.type;

				// Create the same number of IdExpr for this TupleType
				for (int i = 0; i < tupleType.types.size(); i++) {
					String id = tupleExpr.elements.get(i).toString();
					locals.add(new VarDecl(id, tupleType.types.get(i)));
					elements.add(new IdExpr(id));
				}
			} else {
				String id = preSubExpr.exprVar.toString();
				locals.add(new VarDecl(id, preSubExpr.type));
				elements.add(new IdExpr(id));
			}
			equations.add(new Equation(elements, preSubExpr.preExpr));
		}

		LustreMain.log("Replaced expressions: " + this.replacedExprs.size());

		// Get rid of e.realizabilityInputs
		return new Node(node.location, node.id, node.inputs, node.outputs, locals,
				equations, node.properties, node.assertions, null, null, null);
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
	public Expr visit(UnaryExpr e) {
		Expr newExpr = super.visit(e);
		Expr unaryExpr = e.expr;
		
		Type type = e.accept(this.exprTypeVisitor);

		// un-inline PRE sub-expressions
		if (e.op.equals(UnaryOp.PRE)
				&& this.exprUse.containsKey(e.toString())
				&& !(unaryExpr instanceof IdExpr)
				&& !isArithExpr(unaryExpr)) {
			Expr newPreExpr = new UnaryExpr(UnaryOp.PRE, this.getPreSubexprVar(newExpr, type));
			
			return newPreExpr;
		} else {
			return newExpr;
		}
	}
	
	private boolean isArithExpr(Expr expr) {
		if (expr instanceof BinaryExpr) {
			BinaryExpr binaryExpr = (BinaryExpr)expr;
			
			if (binaryExpr.op.equals(BinaryOp.EQUAL)
					|| binaryExpr.op.equals(BinaryOp.NOTEQUAL)
					|| binaryExpr.op.equals(BinaryOp.GREATER)
					|| binaryExpr.op.equals(BinaryOp.GREATEREQUAL)
					|| binaryExpr.op.equals(BinaryOp.LESS)
					|| binaryExpr.op.equals(BinaryOp.LESSEQUAL)
					|| binaryExpr.op.equals(BinaryOp.DIVIDE)
					|| binaryExpr.op.equals(BinaryOp.INT_DIVIDE)
					|| binaryExpr.op.equals(BinaryOp.MINUS)
					|| binaryExpr.op.equals(BinaryOp.MODULUS)
					|| binaryExpr.op.equals(BinaryOp.MULTIPLY)
					|| binaryExpr.op.equals(BinaryOp.PLUS)) {
				return true;
			}
		}
		
		return false;
	}

	// Get a variable Expr for a sub-expression under PRE
	private Expr getPreSubexprVar(Expr expr, Type type) {
		String exprStr = expr.toString();
		
		// Get and return the variable if the expression already exists
		if (this.exprToVarMapping.containsKey(exprStr)) {
			return this.exprToVarMapping.get(exprStr).exprVar;
		}
		
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
		PreSubExpr preSubExpr = new PreSubExpr(((UnaryExpr)expr).expr, exprVar, type);
		this.exprToVarMapping.put(exprStr, preSubExpr);
		this.replacedExprs.add(preSubExpr);
		return exprVar;
	}

	// Get an unused expression id
	private String getExprId() {
		while (this.existingVars.contains("PreVar_" + this.count)) {
			this.count++;
		}
		
		return "PreVar_" + this.count++;
	}	
}
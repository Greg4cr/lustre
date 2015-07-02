package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import types.ExprTypeVisitor;
import values.DefaultValueVisitor;
import jkind.Main;
import jkind.SolverOption;
import jkind.analysis.StaticAnalyzer;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.EnumType;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.RealExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.EnumValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.translation.Translate;
import jkind.util.BigFraction;
import lustre.LustreTrace;

public final class LustreSimulator {
	private final Node node;
	private final List<Equation> equations;
	private final Map<String, Signal<Value>> values;

	private final List<String> inputVars;
	private final List<String> localVars;
	private final List<String> outputVars;

	private final Set<String> oracleVars;

	private final ExprTypeVisitor exprTypeVisitor;

	public LustreSimulator(String fileName) throws Exception {
		this(Main.parseLustre(fileName));
	}

	public LustreSimulator(Program program) {
		StaticAnalyzer.check(program, SolverOption.Z3);
		this.node = Translate.translate(program);
		System.out.println(this.node);
		this.values = new HashMap<String, Signal<Value>>();
		this.equations = new ArrayList<Equation>();

		this.inputVars = new ArrayList<String>();
		this.localVars = new ArrayList<String>();
		this.outputVars = new ArrayList<String>();
		this.oracleVars = new HashSet<String>();

		// Create expression type visitor
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.exprTypeVisitor.setNodeContext(this.node);

		this.initialize();
		this.reOrderEquations();
	}

	private void initialize() {
		// Add inputs
		for (VarDecl varDecl : this.node.inputs) {
			this.inputVars.add(varDecl.id);
		}

		// Add locals
		for (VarDecl varDecl : this.node.locals) {
			this.localVars.add(varDecl.id);
		}

		// Add outputs
		for (VarDecl varDecl : this.node.outputs) {
			this.outputVars.add(varDecl.id);
		}

		// Add oracle variables
		this.oracleVars.addAll(this.inputVars);
		this.oracleVars.addAll(this.localVars);
		this.outputVars.addAll(this.outputVars);
	}

	// Re-order equations
	private void reOrderEquations() {
		List<DependencySet> allEquations = new ArrayList<DependencySet>();

		// Go through all expressions and get the set of non-input variables
		// that is used by each equation
		for (Equation equation : this.node.equations) {
			Set<String> use = DependencyVisitor.get(equation.expr);
			DependencySet current = new DependencySet(equation);
			current.dependOn.addAll(use);
			allEquations.add(current);
		}

		List<String> availableVars = new ArrayList<String>();

		// Input variables are available already
		availableVars.addAll(this.inputVars);

		while (!allEquations.isEmpty()) {
			// Remove all right hand variables that have been available
			for (DependencySet current : allEquations) {
				current.dependOn.removeAll(availableVars);
			}

			// Sort by the number of variables that the equation depends on
			Collections.sort(allEquations);

			while (!allEquations.isEmpty()) {
				DependencySet current = allEquations.get(0);
				if (current.dependOn.isEmpty()) {
					this.equations.add(current.equation);
					for (IdExpr idExpr : current.equation.lhs) {
						availableVars.add(idExpr.id);
					}
					allEquations.remove(current);
				} else {
					break;
				}
			}
		}
	}

	// Get type of an expression
	private Type getType(Expr expr) {
		Type type = expr.accept(this.exprTypeVisitor);
		if (type == null) {
			throw new IllegalArgumentException(expr
					+ " type cannot be determined.");
		}
		return type;
	}

	public List<String> getInputVars() {
		return this.inputVars;
	}

	public List<String> getLocalVars() {
		return this.localVars;
	}

	public List<String> getOutputVars() {
		return this.outputVars;
	}

	public List<String> getProperties() {
		return this.node.properties;
	}

	public List<LustreTrace> simulate(List<LustreTrace> testSuite,
			Set<String> oracles) {
		if (oracles != null) {
			this.oracleVars.clear();
			this.oracleVars.addAll(oracles);
		}

		List<LustreTrace> traces = new ArrayList<LustreTrace>();
		int count = 1;
		for (LustreTrace testCase : testSuite) {
			System.out.println("Executing Test Case (" + (count++) + "/"
					+ testSuite.size() + ") ... \n");
			traces.add(this.simulate(testCase));
		}
		return traces;
	}

	private LustreTrace simulate(LustreTrace testCase) {
		int totalSteps = this.evaluate(testCase);
		LustreTrace trace = new LustreTrace(totalSteps);
		for (Signal<Value> variable : this.values.values()) {
			// If this variable is in oracle
			if (this.oracleVars.contains(variable.getName())) {
				trace.addVariable(variable);
			}
		}
		return trace;
	}

	// Evaluate the result of a given test case
	private int evaluate(LustreTrace testCase) {
		// Clear values whenever executing a new test
		this.values.clear();
		// Add all variables
		for (String variable : this.inputVars) {
			this.values.put(variable, new Signal<Value>(variable));
		}
		for (String variable : this.localVars) {
			this.values.put(variable, new Signal<Value>(variable));
		}
		for (String variable : this.outputVars) {
			this.values.put(variable, new Signal<Value>(variable));
		}

		// Add input values
		int totalSteps = testCase.getLength();
		Set<String> inputs = testCase.getVariableNames();
		for (String input : inputs) {
			for (int step = 0; step < totalSteps; step++) {
				Value value = testCase.getVariable(input).getValue(step);
				values.get(input).putValue(step, value);
			}
		}

		for (int step = 0; step < totalSteps; step++) {
			for (Equation equation : this.equations) {
				if (equation.lhs.size() != 1) {
					throw new IllegalArgumentException(
							"more than one lhs variables.");
				}

				Expr expr = equation.expr;
				Value value = this.evaluate(expr, step);
				if (value == null) {
					// Most likely, this variable is an unguarded PRE
					System.out
							.println("null value for variable (most likely an unguarded PRE): "
									+ equation);
				}
				String id = equation.lhs.get(0).id;
				this.values.get(id).putValue(step, value);
			}
		}
		return totalSteps;
	}

	// Evaluate the result of a given expression at STEP
	private Value evaluate(Expr expr, int step) {
		// If this is an unguarded PRE, assign a default value
		if (step < 0) {
			Type type = this.getType(expr);
			return DefaultValueVisitor.get(type);
		}
		// Unary operators
		if (expr instanceof UnaryExpr) {
			UnaryExpr ue = (UnaryExpr) expr;
			Value value = null;
			if (ue.op.equals(UnaryOp.PRE)) {
				value = this.evaluate(ue.expr, step - 1);
				return value;
			} else {
				value = this.evaluate(ue.expr, step);
				return value.applyUnaryOp(ue.op);
			}
		}
		// Binary operators
		else if (expr instanceof BinaryExpr) {
			BinaryExpr be = (BinaryExpr) expr;

			if (be.op.equals(BinaryOp.ARROW)) {
				if (step == 0) {
					return this.evaluate(be.left, step);
				} else {
					return this.evaluate(be.right, step);
				}
			} else {
				Value leftValue = this.evaluate(be.left, step);
				Value rightValue = this.evaluate(be.right, step);

				// The only valid operation on EnumValues are equals
				if (leftValue instanceof EnumValue
						&& rightValue instanceof EnumValue) {
					if (be.op.equals(BinaryOp.EQUAL)) {
						return BooleanValue.fromBoolean(leftValue
								.equals(rightValue));
					} else {
						return BooleanValue.fromBoolean(!(leftValue
								.equals(rightValue)));
					}
				} else {
					return leftValue.applyBinaryOp(be.op, rightValue);
				}
			}
		}
		// If-then-else
		else if (expr instanceof IfThenElseExpr) {
			IfThenElseExpr itee = (IfThenElseExpr) expr;
			BooleanValue cond = (BooleanValue) this.evaluate(itee.cond, step);
			if (cond.value) {
				return this.evaluate(itee.thenExpr, step);
			} else {
				return this.evaluate(itee.elseExpr, step);
			}
		}
		// Id expression
		else if (expr instanceof IdExpr) {
			IdExpr id = (IdExpr) expr;
			Type type = this.getType(expr);

			// EnumType variable has its own as value
			if (type instanceof EnumType) {
				return new EnumValue(id.id);
			}
			return this.values.get(id.id).getValue(step);
		} else if (expr instanceof IntExpr) {
			IntExpr value = (IntExpr) expr;
			return new IntegerValue(value.value);
		} else if (expr instanceof RealExpr) {
			RealExpr value = (RealExpr) expr;
			return new RealValue(new BigFraction(value.value));
		} else if (expr instanceof BoolExpr) {
			BoolExpr value = (BoolExpr) expr;
			return BooleanValue.fromBoolean(value.value);
		} else if (expr instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) expr;

			Value value = this.evaluate(castExpr.expr, step);

			if (castExpr.type.equals(NamedType.REAL)) {
				if (value instanceof IntegerValue) {
					return new RealValue(new BigFraction(
							((IntegerValue) value).value));
				} else if (value instanceof RealValue) {
					return value;
				} else {
					throw new IllegalArgumentException(
							"Unknown cast value type: " + value.getClass());
				}
			} else if (castExpr.type.equals(NamedType.INT)) {
				if (value instanceof IntegerValue) {
					return value;
				} else if (value instanceof RealValue) {
					return new IntegerValue(((RealValue) value).value.floor());
				} else {
					throw new IllegalArgumentException(
							"Unknown cast value type: " + value.getClass());
				}
			} else {
				throw new IllegalArgumentException("Unknown cast type: "
						+ castExpr.type);
			}
		} else {
			/*
			 * Inlined/flattened expressions: ArrayAccessExpr ArrayExpr
			 * ArrayUpdateExpr CondactExpr NodeCallExpr RecordAccessExpr
			 * RecordExpr RecordUpdateExpr TupleExpr
			 */
			return null;
		}
	}
}

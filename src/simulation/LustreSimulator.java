package simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import enums.Simulation;
import testsuite.VerifyTestSuite;
import types.ExprTypeVisitor;
import values.DefaultValueVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.RealExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.translation.Translate;
import jkind.util.BigFraction;
import jkind.util.Util;
import lustre.LustreTrace;
import main.LustreMain;

public final class LustreSimulator {
	private final List<Equation> equations;
	private final Map<String, Signal<Value>> values;

	private final List<String> inputVars;
	private final List<String> localVars;
	private final List<String> outputVars;
	private final List<String> properties;
	private final List<String> oracleVars;

	private final ExprTypeVisitor exprTypeVisitor;

	private Simulation simulation;

	public LustreSimulator(Program program) {
		Node node = Translate.translate(program);

		this.equations = new ArrayList<Equation>();
		this.values = new HashMap<String, Signal<Value>>();

		this.inputVars = new ArrayList<String>();
		this.localVars = new ArrayList<String>();
		this.outputVars = new ArrayList<String>();
		this.properties = new ArrayList<String>();
		this.oracleVars = new ArrayList<String>();

		// Create expression type visitor
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.exprTypeVisitor.setNodeContext(node);

		this.simulation = null;
		this.initialize(node);
	}

	// Add all variables, re-order equations based on dependency
	private void initialize(Node node) {
		// Add inputs
		for (VarDecl varDecl : node.inputs) {
			this.inputVars.add(varDecl.id);
		}

		// Add locals
		for (VarDecl varDecl : node.locals) {
			this.localVars.add(varDecl.id);
		}

		// Add outputs
		for (VarDecl varDecl : node.outputs) {
			this.outputVars.add(varDecl.id);
		}

		// Add properties
		this.properties.addAll(node.properties);

		// Add oracle variables
		// By default, all output variables
		// this.oracleVars.addAll(this.localVars);
		this.oracleVars.addAll(this.outputVars);

		// Re-order equations
		List<DependencySet> allEquations = new ArrayList<DependencySet>();

		// Go through all equations and get the set of non-delayed variables
		// that are used by each equation
		for (Equation equation : node.equations) {
			Set<String> use = DependencyVisitor.get(equation.expr);
			DependencySet current = new DependencySet(equation, use);
			allEquations.add(current);
		}

		List<String> availableVars = new ArrayList<String>();

		// Input variables are available already
		availableVars.addAll(this.inputVars);

		boolean progress = false;

		while (!allEquations.isEmpty()) {
			progress = false;
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
					progress = true;
				} else {
					break;
				}
			}
			if (!progress) {
				throw new IllegalArgumentException(
						"Dependency cannot be resolved, possible algebraic loop");
			}
		}
	}

	public List<String> getAllVars() {
		List<String> allVars = new ArrayList<String>();
		allVars.addAll(this.inputVars);
		allVars.addAll(this.localVars);
		allVars.addAll(this.outputVars);
		return allVars;
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
		return this.properties;
	}

	private void setSimulation(boolean isComplete, Simulation simulation) {
		switch (simulation) {
		case COMPLETE:
			if (!isComplete) {
				LustreMain.error("Test suite has null values");
			}
			LustreMain.log("------------Starting complete simulator");
			break;
		case PARTIAL:
			if (!isComplete) {
				LustreMain.log("Test suite has null values.");
			}
			LustreMain.log("------------Starting partial simulator");
			break;
		default:
			throw new IllegalArgumentException("Unknown simulation: "
					+ simulation);
		}
		this.simulation = simulation;
	}

	private void setOracle(List<String> oracle) {
		this.oracleVars.clear();
		this.oracleVars.addAll(oracle);
	}

	// Simulate a test suite
	public List<LustreTrace> simulate(List<LustreTrace> testSuite,
			Simulation simulation) {
		this.setSimulation(
				VerifyTestSuite.isComplete(testSuite, this.inputVars),
				simulation);
		return this.simulate(testSuite);
	}

	public List<LustreTrace> simulate(List<LustreTrace> testSuite,
			Simulation simulation, List<String> oracle) {
		this.setSimulation(
				VerifyTestSuite.isComplete(testSuite, this.inputVars),
				simulation);
		this.setOracle(oracle);
		return this.simulate(testSuite);
	}

	// Simulate a test case
	public LustreTrace simulate(LustreTrace testCase, Simulation simulation) {
		this.setSimulation(
				VerifyTestSuite.isComplete(testCase, this.inputVars),
				simulation);
		return this.simulate(testCase);
	}

	public LustreTrace simulate(LustreTrace testCase, Simulation simulation,
			List<String> oracle) {
		this.setSimulation(
				VerifyTestSuite.isComplete(testCase, this.inputVars),
				simulation);
		this.setOracle(oracle);
		return this.simulate(testCase);
	}

	// Simulate a test suite, assuming simulation and oracle have been set
	private List<LustreTrace> simulate(List<LustreTrace> testSuite) {
		List<LustreTrace> traces = new ArrayList<LustreTrace>();
		int count = 1;
		for (LustreTrace testCase : testSuite) {
			LustreMain.log("Executing test case (" + (count++) + "/"
					+ testSuite.size() + ") ...");
			traces.add(this.simulate(testCase));
		}
		return traces;
	}

	// Simulate a test case, assuming simulation and oracle have been set
	private LustreTrace simulate(LustreTrace testCase) {
		int length = this.evaluate(testCase);
		LustreTrace trace = new LustreTrace(length);

		// Add values of oracle variables
		for (String oracle : this.oracleVars) {
			trace.addVariable(this.values.get(oracle));
		}
		return trace;
	}

	// Evaluate the result of a given test case
	private int evaluate(LustreTrace testCase) {
		// Clear values before executing a new test
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

		int length = testCase.getLength();

		// Add input values
		Set<String> inputs = testCase.getVariableNames();

		for (String input : inputs) {
			for (int step = 0; step < length; step++) {
				Value value = testCase.getVariable(input).getValue(step);
				values.get(input).putValue(step, value);
			}
		}

		// Iterate from step 0 to (length - 1)
		for (int step = 0; step < length; step++) {
			for (Equation equation : this.equations) {
				if (equation.lhs.isEmpty()) {
					continue;
				}
				if (equation.lhs.size() > 1) {
					throw new IllegalArgumentException(
							"Multiple lhs variables should have been flattened");
				}

				Expr expr = equation.expr;
				Value value = this.evaluate(expr, step);

				// Error if value evaluates to null with complete evaluation
				if (value == null && this.simulation == Simulation.COMPLETE) {
					throw new NullPointerException("Value evaluates to null: "
							+ equation);
				}
				String id = equation.lhs.get(0).id;
				this.values.get(id).putValue(step, value);
			}
		}
		return length;
	}

	// Evaluate the result of a given expression at STEP
	private Value evaluate(Expr expr, int step) {
		// If this is an unguarded PRE
		if (step < 0) {
			switch (this.simulation) {
			case COMPLETE:
				// Assign a default value for complete evaluation
				Type type = expr.accept(this.exprTypeVisitor);
				return DefaultValueVisitor.get(type);
			case PARTIAL:
				// Assign null for partial evaluation
				return null;
			default:
				throw new IllegalArgumentException("Unknown simulation: "
						+ simulation);
			}
		}
		// Unary operators
		if (expr instanceof UnaryExpr) {
			UnaryExpr ue = (UnaryExpr) expr;

			if (ue.op.equals(UnaryOp.PRE)) {
				return this.evaluate(ue.expr, step - 1);
			} else {
				Value ueValue = this.evaluate(ue.expr, step);

				if (ueValue == null) {
					return null;
				} else {
					return ueValue.applyUnaryOp(ue.op);
				}
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

				if (leftValue == null || rightValue == null) {
					switch (this.simulation) {
					case COMPLETE:
						return null;
					case PARTIAL:
						return booleanPartialEvaluation(leftValue, rightValue,
								be.op);
					default:
						throw new IllegalArgumentException(
								"Unknown simulation: " + simulation);
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

			if (cond == null) {
				return null;
			}

			if (cond.value) {
				return this.evaluate(itee.thenExpr, step);
			} else {
				return this.evaluate(itee.elseExpr, step);
			}
		}
		// IdExpr
		else if (expr instanceof IdExpr) {
			IdExpr id = (IdExpr) expr;
			return this.values.get(id.id).getValue(step);
		}
		// IntExpr
		else if (expr instanceof IntExpr) {
			IntExpr value = (IntExpr) expr;
			return new IntegerValue(value.value);
		}
		// RealExpr
		else if (expr instanceof RealExpr) {
			RealExpr value = (RealExpr) expr;
			return new RealValue(new BigFraction(value.value));
		}
		// BoolExpr
		else if (expr instanceof BoolExpr) {
			BoolExpr value = (BoolExpr) expr;
			return BooleanValue.fromBoolean(value.value);
		} else if (expr instanceof CastExpr) {
			CastExpr castExpr = (CastExpr) expr;

			Value value = this.evaluate(castExpr.expr, step);

			if (value == null) {
				return null;
			}

			return Util.cast(castExpr.type, value);
		} else {
			/*
			 * Inlined/flattened expressions: ArrayAccessExpr ArrayExpr
			 * ArrayUpdateExpr CondactExpr NodeCallExpr RecordAccessExpr
			 * RecordExpr RecordUpdateExpr TupleExpr
			 */
			throw new IllegalArgumentException(expr.getClass()
					+ " should have been inlined/flattened");
		}
	}

	// Partial evaluation of booleans on logical operators
	private Value booleanPartialEvaluation(Value leftValue, Value rightValue,
			BinaryOp op) {
		if (leftValue == null && rightValue == null) {
			return null;
		}

		switch (op) {
		case AND:
			if (leftValue != null && leftValue.equals(BooleanValue.FALSE)) {
				return BooleanValue.FALSE;
			}
			if (rightValue != null && rightValue.equals(BooleanValue.FALSE)) {
				return BooleanValue.FALSE;
			}
			break;
		case OR:
			if (leftValue != null && leftValue.equals(BooleanValue.TRUE)) {
				return BooleanValue.TRUE;
			}
			if (rightValue != null && rightValue.equals(BooleanValue.TRUE)) {
				return BooleanValue.TRUE;
			}
			break;
		default:
			break;
		}
		return null;
	}
}

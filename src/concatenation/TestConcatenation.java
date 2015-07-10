package concatenation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import enums.Generation;
import enums.Simulation;
import jkind.JKindExecution;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.translation.Translate;
import simulation.LustreSimulator;
import testsuite.FillNullValues;
import lustre.LustreTrace;
import main.LustreMain;

public class TestConcatenation {
	private final Node node;
	private final LustreSimulator simulator;
	private final LustreTrace testCase;
	private final List<String> stateVariables;

	private static final String PROPERTY = "trapProperty";

	public TestConcatenation(Program program) {
		this.node = Translate.translate(program);
		this.simulator = new LustreSimulator(program);
		this.testCase = new LustreTrace(0);

		for (VarDecl variable : node.inputs) {
			this.testCase.addVariable(new Signal<Value>(variable.id));
		}

		this.stateVariables = new ArrayList<String>();
		// Add local and output variables as state variables
		for (VarDecl var : node.locals) {
			this.stateVariables.add(var.id);
		}
		for (VarDecl var : node.outputs) {
			this.stateVariables.add(var.id);
		}
	}

	public LustreTrace generate() {
		Expr constraint = null;

		while ((constraint = ConcatenationMain.getConstraint()) != null) {
			// First step generation
			if (this.testCase.getLength() == 0) {
				LustreMain.log("\nConstraint: " + constraint);
				LustreTrace test = this.generateTest(this.node, constraint);

				if (test == null) {
					return this.testCase;
				}

				this.testCase.addLustreTrace(test);
				continue;
			}

			// Followed by step generation
			LustreTrace history = simulator.simulate(this.testCase,
					Simulation.COMPLETE, this.stateVariables);
			Node nodeWithHistory = CreateHistoryVisitor
					.node(this.node, history);

			LustreMain.log("\nConstraint: " + constraint);
			LustreTrace test = this.generateTest(nodeWithHistory, constraint);

			if (test == null) {
				return this.testCase;
			}

			this.testCase.addLustreTrace(test);
		}

		return this.testCase;
	}

	private Node addConstraint(Node node, Expr constraint) {
		NodeBuilder builder = new NodeBuilder(node);
		VarDecl trapVar = new VarDecl(PROPERTY, NamedType.BOOL);
		Equation trapProperty = new Equation(new IdExpr(trapVar.id),
				new UnaryExpr(UnaryOp.NOT, constraint));

		builder.addLocal(trapVar);
		builder.addEquation(trapProperty);
		builder.addProperty(PROPERTY);

		return builder.build();
	}

	private LustreTrace generateTest(Node node, Expr constraint) {
		Node newNode = this.addConstraint(node, constraint);

		Map<String, LustreTrace> tests = JKindExecution.execute(new Program(
				newNode));
		LustreTrace test = tests.get(PROPERTY);

		if (test == null) {
			System.out
					.println("WARNING: Test case cannot be generated for constraint "
							+ constraint);
			return null;
		}

		return FillNullValues.fill(test, node, Generation.DEFAULT);
	}
}

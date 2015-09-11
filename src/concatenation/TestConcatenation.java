package concatenation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import enums.Generation;
import enums.Simulation;
import jkind.JKindExecution;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.translation.RemoveEnumTypes;
import jkind.translation.Translate;
import simulation.LustreSimulator;
import testsuite.FillNullValues;
import lustre.LustreTrace;
import main.LustreMain;

public class TestConcatenation {
	// All existing constraints from the program in order
	private final List<String> properties;

	private final Program program;
	private final Node node;
	private final LustreSimulator simulator;
	private final LustreTrace testCase;

	// private final List<String> stateVariables;

	public TestConcatenation(Program program) {
		this.properties = new ArrayList<String>();
		this.properties.addAll(program.getMainNode().properties);

		ProgramBuilder programBuilder = new ProgramBuilder();
		programBuilder.addTypes(program.types).addConstants(program.constants)
				.setMain(program.main);

		for (Node node : program.nodes) {
			NodeBuilder nodeBuilder = new NodeBuilder(node);
			// Clear existing properties
			nodeBuilder.clearProperties();
			programBuilder.addNode(nodeBuilder.build());
		}

		this.program = programBuilder.build();
		// Besides translating program to simple format, also remove enum types
		// in node. Otherwise, node does not contain type defines for enums.
		this.node = RemoveEnumTypes.node(Translate.translate(this.program));
		this.simulator = new LustreSimulator(this.program);
		this.testCase = new LustreTrace(0);

		for (VarDecl variable : node.inputs) {
			this.testCase.addVariable(new Signal<Value>(variable.id));
		}

		// this.stateVariables = new ArrayList<String>();
		// Add all variables as state variables
		// for (VarDecl var : node.inputs) {
		// this.stateVariables.add(var.id);
		// }
		// for (VarDecl var : node.locals) {
		// this.stateVariables.add(var.id);
		// }
		// for (VarDecl var : node.outputs) {
		// this.stateVariables.add(var.id);
		// }
	}

	public LustreTrace generate() {
		for (String property : this.properties) {
			// First step generation
			if (this.testCase.getLength() == 0) {
				LustreMain.log("\nConstraint: " + property);
				LustreTrace test = this.generateTest(this.node, property);

				if (test == null) {
					return this.testCase;
				}

				this.testCase.addLustreTrace(test);
				continue;
			}

			// Followed by step generation
			LustreTrace history = simulator.simulate(this.testCase,
					Simulation.COMPLETE);
			Node nodeWithHistory = CreateHistoryVisitor
					.node(this.node, history);

			LustreMain.log("\nConstraint: " + property);
			LustreTrace test = this.generateTest(nodeWithHistory, property);

			if (test == null) {
				return this.testCase;
			}

			this.testCase.addLustreTrace(test);
		}

		return this.testCase;
	}

	private Node addConstraint(Node node, String property) {
		return new NodeBuilder(node).addProperty(property).build();
	}

	private LustreTrace generateTest(Node node, String property) {
		Node newNode = this.addConstraint(node, property);

		Map<String, LustreTrace> tests = JKindExecution.execute(new Program(
				newNode));
		LustreTrace test = tests.get(property);

		if (test == null) {
			System.out
					.println("WARNING: Test case cannot be generated for constraint "
							+ property);
			return null;
		}

		return FillNullValues.fill(test, this.program, Generation.DEFAULT);
	}
}

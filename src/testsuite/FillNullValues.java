package testsuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import enums.Generation;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import values.DefaultValueVisitor;
import values.RandomValueVisitor;
import lustre.LustreTrace;

/**
 * Fill in null values with concrete values
 */
public final class FillNullValues {
	// Fill in a test suite
	public static List<LustreTrace> fill(List<LustreTrace> testSuite,
			Program program, Generation generation) {
		return new FillNullValues().fillNullValues(testSuite,
				program.getMainNode().inputs, generation);
	}

	public static List<LustreTrace> fill(List<LustreTrace> testSuite,
			Node node, Generation generation) {
		return new FillNullValues().fillNullValues(testSuite, node.inputs,
				generation);
	}

	// Fill in a test case
	public static LustreTrace fill(LustreTrace testCase, Program program,
			Generation generation) {
		return new FillNullValues().fillNullValues(testCase,
				program.getMainNode().inputs, generation);
	}

	public static LustreTrace fill(LustreTrace testCase, Node node,
			Generation generation) {
		return new FillNullValues().fillNullValues(testCase, node.inputs,
				generation);
	}

	// Fill null values for a test suite
	private List<LustreTrace> fillNullValues(List<LustreTrace> testSuite,
			List<VarDecl> inputs, Generation generation) {
		List<LustreTrace> newTestSuite = new ArrayList<LustreTrace>();

		for (LustreTrace testCase : testSuite) {
			newTestSuite.add(this.fillNullValues(testCase, inputs, generation));
		}

		return newTestSuite;
	}

	// Fill null values for a test case
	private LustreTrace fillNullValues(LustreTrace testCase,
			List<VarDecl> inputs, Generation generation) {
		int length = testCase.getLength();
		LustreTrace newTestCase = new LustreTrace(length);

		// Get input names and types
		Map<String, Type> inputTypes = new HashMap<String, Type>();

		for (VarDecl input : inputs) {
			inputTypes.put(input.id, input.type);
		}

		Set<String> variables = testCase.getVariableNames();

		for (String variable : variables) {
			Signal<Value> signal = testCase.getVariable(variable);
			Signal<Value> newSignal = new Signal<Value>(variable);

			Type type = inputTypes.get(variable);

			// Iterate from step 0 to (length - 1)
			for (int step = 0; step < length; step++) {
				Value value = signal.getValue(step);
				if (value == null) {
					switch (generation) {
					case NULL:
						break;
					case DEFAULT:
						value = DefaultValueVisitor.get(type);
						break;
					case RANDOM:
						value = RandomValueVisitor.get(type);
						break;
					default:
						throw new IllegalArgumentException(
								"Unknown generation: " + generation);
					}
				}
				newSignal.putValue(step, value);
			}
			newTestCase.addVariable(newSignal);
		}

		return newTestCase;
	}
}

package testsuite;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.translation.Translate;
import values.DefaultValueVisitor;
import values.RandomValueVisitor;
import values.ValueType;
import lustre.LustreTrace;

/**
 * Fill in null values with concrete values
 */
public class FillNullValues {
	// Fill in default values for a test suite
	public static List<LustreTrace> defaultValue(List<LustreTrace> testSuite,
			Program program) {
		Node node = Translate.translate(program);
		List<VarDecl> inputs = node.inputs;

		return new FillNullValues().fillNullValues(testSuite,
				ValueType.DEFAULT, inputs);
	}

	// Fill in default values for a test case
	public static LustreTrace defaultValue(LustreTrace testCase, Program program) {
		Node node = Translate.translate(program);
		List<VarDecl> inputs = node.inputs;

		return new FillNullValues().fillNullValues(testCase, ValueType.DEFAULT,
				inputs);
	}

	// Fill in random values for a test suite
	public static List<LustreTrace> randomValue(List<LustreTrace> testSuite,
			Program program) {
		Node node = Translate.translate(program);
		List<VarDecl> inputs = node.inputs;

		return new FillNullValues().fillNullValues(testSuite, ValueType.RANDOM,
				inputs);

	}

	// Fill in random values for a test case
	public static LustreTrace randomValue(LustreTrace testCase, Program program) {
		Node node = Translate.translate(program);
		List<VarDecl> inputs = node.inputs;

		return new FillNullValues().fillNullValues(testCase, ValueType.RANDOM,
				inputs);
	}

	// Fill null values for a test suite
	private List<LustreTrace> fillNullValues(List<LustreTrace> testSuite,
			ValueType valueType, List<VarDecl> inputs) {
		List<LustreTrace> newTestSuite = new ArrayList<LustreTrace>();

		for (LustreTrace testCase : testSuite) {
			newTestSuite.add(this.fillNullValues(testCase, valueType, inputs));
		}

		return newTestSuite;
	}

	// Fill null values for a test case
	private LustreTrace fillNullValues(LustreTrace testCase,
			ValueType valueType, List<VarDecl> inputs) {
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
					if (valueType.equals(ValueType.DEFAULT)) {
						value = DefaultValueVisitor.get(type);
					} else if (valueType.equals(ValueType.RANDOM)) {
						value = RandomValueVisitor.get(type);
					} else {
						throw new IllegalArgumentException(
								"Unknown value type: " + valueType);
					}
				}
				newSignal.putValue(step, value);
			}
		}

		return newTestCase;
	}
}

package testsuite;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import enums.Generation;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import types.ResolvedTypeTable;
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
		FillNullValues fill = new FillNullValues(program);

		List<LustreTrace> newTestSuite = new ArrayList<LustreTrace>();

		for (LustreTrace testCase : testSuite) {
			newTestSuite.add(fill.fillNullValues(testCase, generation));
		}

		return newTestSuite;
	}

	// Fill in a test case
	public static LustreTrace fill(LustreTrace testCase, Program program,
			Generation generation) {
		FillNullValues fill = new FillNullValues(program);
		return fill.fillNullValues(testCase, generation);
	}

	private final Map<String, Type> typeMap;

	private FillNullValues(Program program) {
		this.typeMap = ResolvedTypeTable.get(program);
	}

	// Fill null values for a test case
	private LustreTrace fillNullValues(LustreTrace testCase,
			Generation generation) {
		int length = testCase.getLength();
		LustreTrace newTestCase = new LustreTrace(length);

		Set<String> variables = testCase.getVariableNames();

		for (String variable : variables) {
			Signal<Value> signal = testCase.getVariable(variable);
			Signal<Value> newSignal = new Signal<Value>(variable);

			Type type = typeMap.get(variable);

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

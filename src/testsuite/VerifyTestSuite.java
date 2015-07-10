package testsuite;

import java.util.List;

import jkind.lustre.values.Value;
import jkind.results.Signal;
import lustre.LustreTrace;

/**
 * Verify if a test suite/case has null values
 */
public class VerifyTestSuite {
	// Check null for a test suite
	public static boolean isComplete(List<LustreTrace> testSuite,
			List<String> inputs) {
		for (LustreTrace testCase : testSuite) {
			if (!isComplete(testCase, inputs)) {
				return false;
			}
		}

		return true;
	}

	// Check null for a test case
	public static boolean isComplete(LustreTrace testCase, List<String> inputs) {
		for (String variable : inputs) {
			Signal<Value> signal = testCase.getVariable(variable);

			if (signal == null) {
				return false;
			}

			int length = testCase.getLength();

			// Iterate from step 0 to (length - 1)
			for (int step = 0; step < length; step++) {
				Value value = signal.getValue(step);
				if (value == null) {
					return false;
				}
			}
		}
		return true;
	}
}

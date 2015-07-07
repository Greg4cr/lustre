package testsuite;

import java.util.List;
import java.util.Set;

import jkind.lustre.values.Value;
import jkind.results.Signal;
import lustre.LustreTrace;

/**
 * Verify if a test suite/case has null values
 */
public class VerifyTestSuite {
	// Check null for a test suite
	public static boolean isComplete(List<LustreTrace> testSuite) {
		for (LustreTrace testCase : testSuite) {
			if (!isComplete(testCase)) {
				return false;
			}
		}

		return true;
	}

	// Check null for a test case
	public static boolean isComplete(LustreTrace testCase) {
		Set<String> variables = testCase.getVariableNames();

		for (String variable : variables) {
			Signal<Value> signal = testCase.getVariable(variable);

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

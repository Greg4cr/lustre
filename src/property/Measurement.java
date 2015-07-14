package property;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jkind.lustre.Program;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import lustre.LustreTrace;
import main.LustreMain;

/**
 * Measure coverage for a given test suite and reduce test suite size while
 * maintaining coverage. Assuming all properties are trap properties.
 */
public final class Measurement {
	private Set<LustreProperty> properties = new HashSet<LustreProperty>();

	public static List<LustreTrace> measure(List<LustreTrace> testSuite,
			List<LustreTrace> traces, Program program) {
		Measurement measure = new Measurement();

		measure.properties.addAll(LustreProperty.getProperties(program));

		List<PropertySet> satisfiedByEachTest = measure.satisfiedByEachTest(
				testSuite, traces, program);

		return measure.testSuiteReduction(satisfiedByEachTest);
	}

	// Get the set of properties that can be falsified by each test case
	private List<PropertySet> satisfiedByEachTest(List<LustreTrace> testSuite,
			List<LustreTrace> traces, Program program) {
		List<PropertySet> satisfiedByEachTest = new ArrayList<PropertySet>();

		for (int testIndex = 0; testIndex < testSuite.size(); testIndex++) {
			LustreTrace testCase = testSuite.get(testIndex);
			LustreTrace trace = traces.get(testIndex);

			PropertySet satisfied = new PropertySet(testCase);
			Set<String> variables = trace.getVariableNames();

			for (String variable : variables) {
				LustreProperty property = LustreProperty.convert(variable,
						program.main);
				if (!properties.contains(property)) {
					throw new IllegalArgumentException("Uknown property: "
							+ variable);
				}
				Signal<Value> signal = trace.getVariable(variable);
				for (int step = 0; step < trace.getLength(); step++) {
					// For trap properties, FALSE means the original obligation
					// is satisfied
					if (signal.getValue(step) != null
							&& signal.getValue(step).equals(BooleanValue.FALSE)) {
						satisfied.properties.add(property);
					}
				}
			}
			satisfiedByEachTest.add(satisfied);
		}

		return satisfiedByEachTest;
	}

	// Reduce a test suite using a greedy algorithm, while maintaining coverage
	private List<LustreTrace> testSuiteReduction(
			List<PropertySet> satisfiedByEachTest) {
		List<PropertySet> allTests = new ArrayList<PropertySet>();
		List<LustreTrace> reducedTestSuite = new ArrayList<LustreTrace>();

		allTests.addAll(satisfiedByEachTest);

		List<LustreProperty> satisfiedProperties = new ArrayList<LustreProperty>();
		Collections.sort(allTests);

		while (!allTests.isEmpty() && !allTests.get(0).properties.isEmpty()) {
			// Sort by the number of variables that the equation depends on
			PropertySet current = allTests.remove(0);
			reducedTestSuite.add(current.testCase);
			satisfiedProperties.addAll(current.properties);

			for (PropertySet test : allTests) {
				test.properties.removeAll(satisfiedProperties);
			}
			Collections.sort(allTests);
		}

		LustreMain.log("Satisfied obligations: " + satisfiedProperties.size()
				+ "/" + properties.size());

		properties.removeAll(satisfiedProperties);

		LustreMain.log("------------Unsatisfied obligations: "
				+ properties.size());

		for (LustreProperty current : properties) {
			LustreMain.log(current.toString());
		}

		return reducedTestSuite;
	}
}

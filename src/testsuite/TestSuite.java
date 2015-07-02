package testsuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import values.ValueFromString;
import jkind.lustre.Node;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.util.Util;
import lustre.LustreTrace;

public class TestSuite {
	public static List<LustreTrace> readTestsFromFile(Node node, String fileName) {
		Map<String, Type> typeMap = Util.getTypeMap(node);
		return new TestSuite()._readTestsFromFile(typeMap, fileName);
	}

	// Read tests from a file, which may contain multiple test cases
	private List<LustreTrace> _readTestsFromFile(Map<String, Type> typeMap,
			String fileName) {
		List<LustreTrace> testSuite = new ArrayList<LustreTrace>();

		Scanner sc = null;

		try {
			sc = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String[] variables = null;
		String allLinesStr = "";

		while (sc.hasNext()) {
			String line = sc.nextLine();
			line = line.replaceAll("\\s", "");
			// Start with variable names
			if (variables == null) {
				variables = line.split(",");
			}
			// Then values
			else {
				allLinesStr += line + "\n";
			}
		}
		sc.close();

		int numOfInputs = variables.length;

		// Initialize input variables
		Map<Integer, Signal<Value>> inputVariables = new HashMap<Integer, Signal<Value>>();

		for (int inputIndex = 0; inputIndex < numOfInputs; inputIndex++) {
			inputVariables.put(inputIndex, null);
		}

		// Get all test cases
		String[] allTestCases = allLinesStr.split("\n\n");

		// Start adding values
		for (int testIndex = 0; testIndex < allTestCases.length; testIndex++) {
			for (int inputIndex = 0; inputIndex < numOfInputs; inputIndex++) {
				inputVariables.put(inputIndex, new Signal<Value>(
						variables[inputIndex]));
			}

			String[] testCaseStr = allTestCases[testIndex].split("\n");
			LustreTrace testCase = new LustreTrace(testCaseStr.length);

			// Go through each step
			for (int step = 0; step < testCaseStr.length; step++) {
				String[] values = testCaseStr[step].split(",");
				if (values.length != numOfInputs) {
					throw new IllegalArgumentException(
							"The number of variables and values do not match.");
				}

				// The order of variables from the test suite file and
				// inputVariables are the same
				for (int inputIndex = 0; inputIndex < numOfInputs; inputIndex++) {
					Value value = ValueFromString.get(values[inputIndex],
							typeMap.get(variables[inputIndex]));
					inputVariables.get(inputIndex).putValue(step, value);
				}
			}

			for (Signal<Value> variable : inputVariables.values()) {
				testCase.addVariable(variable);
			}

			testSuite.add(testCase);
		}

		return testSuite;
	}
}

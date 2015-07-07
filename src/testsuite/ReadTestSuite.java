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
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.results.Signal;
import jkind.translation.Translate;
import lustre.LustreTrace;

/**
 * Read a test suite from a file. The test suite file may not contain all inputs
 * (i.e., there are null values).
 */
public class ReadTestSuite {
	public static List<LustreTrace> read(String fileName, Program program) {
		ReadTestSuite reader = new ReadTestSuite();

		Node node = Translate.translate(program);

		return reader.read(fileName, node.inputs);
	}

	private List<LustreTrace> read(String fileName, List<VarDecl> inputs) {
		List<LustreTrace> testSuite = new ArrayList<LustreTrace>();

		// Get input names and types
		List<String> inputVars = new ArrayList<String>();
		Map<String, Type> inputTypes = new HashMap<String, Type>();
		for (VarDecl input : inputs) {
			inputVars.add(input.id);
			inputTypes.put(input.id, input.type);
		}

		Scanner sc = null;

		try {
			sc = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Read in variable names and all lines
		String[] variables = null;
		String allLines = "";

		while (sc.hasNext()) {
			String line = sc.nextLine().replaceAll("\\s", "");
			// Start with variable names
			if (variables == null) {
				variables = line.split(",");
			}
			// Then values
			else {
				allLines += line + "\n";
			}
		}
		sc.close();

		Map<String, Signal<Value>> inputVariables = new HashMap<String, Signal<Value>>();

		// Split test cases by double newline
		String[] testCaseArray = allLines.split("\n\n");

		// Iterate all test cases
		for (String testCaseStr : testCaseArray) {
			// Initialize input variables
			for (String input : inputVars) {
				inputVariables.put(input, new Signal<Value>(input));
			}

			// Split test steps by newline
			String[] testStepArray = testCaseStr.split("\n");
			LustreTrace testCase = new LustreTrace(testStepArray.length);

			// Iterate from step 0 to (length - 1)
			for (int step = 0; step < testStepArray.length; step++) {
				// Split values by comma
				String[] values = testStepArray[step].split(",");

				if (values.length != variables.length) {
					throw new IllegalArgumentException(
							"The number of variables and values do not match.");
				}

				for (int inputIndex = 0; inputIndex < variables.length; inputIndex++) {
					String variable = variables[inputIndex];

					// If this variable is an input
					if (inputVars.contains(variable)) {
						Value value = ValueFromString.get(values[inputIndex],
								inputTypes.get(variable));
						inputVariables.get(variable).putValue(step, value);
					}
				}
			}

			// Add all signals
			for (Signal<Value> variable : inputVariables.values()) {
				testCase.addVariable(variable);
			}

			testSuite.add(testCase);
			inputVariables.clear();
		}

		return testSuite;
	}
}

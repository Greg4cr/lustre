package testsuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import values.ValueToString;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.translation.Translate;
import lustre.LustreTrace;

/**
 * Write a test suite to a file in CSV format. The test suite may contain
 * "don't care" values (i.e., null values in the file). EnumType variable values
 * are translated back to Enum values.
 */
public class WriteTestSuite {
	public static void write(List<LustreTrace> testSuite, String fileName,
			Program program) {
		WriteTestSuite writer = new WriteTestSuite();

		Node node = Translate.translate(program);
		List<VarDecl> inputs = writer.naturalOrderedInputs(node);

		writer.write(testSuite, fileName, inputs);
	}

	private void write(List<LustreTrace> testSuite, String fileName,
			List<VarDecl> inputs) {
		String output = "";

		Map<String, Type> inputTypes = new HashMap<String, Type>();

		// Write variable names
		Iterator<VarDecl> inputIter = inputs.iterator();

		while (inputIter.hasNext()) {
			VarDecl input = inputIter.next();
			output += input.id;
			inputTypes.put(input.id, input.type);
			if (inputIter.hasNext()) {
				output += ",";
			}
		}

		output += "\n";

		// Write values
		Iterator<LustreTrace> testIter = testSuite.iterator();

		// Iterate all test cases
		while (testIter.hasNext()) {
			LustreTrace testCase = testIter.next();
			int length = testCase.getLength();

			// Iterate from step 0 to (length - 1)
			for (int step = 0; step < length; step++) {
				inputIter = inputs.iterator();

				// Iterate all input variables
				while (inputIter.hasNext()) {
					String variable = inputIter.next().id;

					// Value can be null
					Value value = testCase.getVariable(variable).getValue(step);
					Type type = inputTypes.get(variable);

					if (value == null) {
						output += "null";
					} else {
						// Also Convert EnumType values from integer back to
						// EnumValue
						String valueStr = ValueToString.get(value, type);
						output += valueStr;
					}

					// Add comma if not ending
					if (inputIter.hasNext()) {
						output += ",";
					}
				}
				output += "\n";
			}

			// Add new line if not ending
			if (testIter.hasNext()) {
				output += "\n";
			}
		}

		// Print to file
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(fileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw.print(output);
		pw.close();
	}

	private List<VarDecl> naturalOrderedInputs(Node node) {
		List<VarDecl> inputs = new ArrayList<VarDecl>();
		inputs.addAll(node.inputs);
		Collections.sort(inputs, new VarDeclNaturalOrdering());
		return inputs;
	}
}

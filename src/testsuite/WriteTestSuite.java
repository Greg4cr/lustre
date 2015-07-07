package testsuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.translation.Translate;
import lustre.LustreTrace;

/**
 * Write a test suite to a file. The test suite may contain "don't care" values
 * (i.e., null values in the file).
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

		// Write variable names
		Iterator<VarDecl> inputIter = inputs.iterator();

		while (inputIter.hasNext()) {
			output += inputIter.next().id;
			if (inputIter.hasNext()) {
				output += ",";
			}
		}

		output += "\n";

		// Write values
		Iterator<LustreTrace> testIter = testSuite.iterator();

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
					output += testCase.getVariable(variable).getValue(step);

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

package testsuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import types.ResolvedTypeTable;
import values.ValueToString;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.values.Value;
import jkind.util.StringNaturalOrdering;
import lustre.LustreTrace;

/**
 * Write a test suite/trace to a file in CSV format. The test suite/trace may
 * contain "don't care" values (i.e., null values in the file). EnumType
 * variable values are translated back to EnumValue from IntegerValue. All
 * signals in a trace are written to the file.
 */
public final class WriteTrace {
	public static void write(List<LustreTrace> testSuite, String fileName,
			Program program) {
		if (testSuite.isEmpty()) {
			throw new IllegalArgumentException("Empty test suite.");
		}

		Map<String, Type> typeMap = ResolvedTypeTable.get(program);

		new WriteTrace().write(testSuite, fileName, typeMap);
	}

	private void write(List<LustreTrace> traces, String fileName,
			Map<String, Type> typeMap) {
		String output = "";

		List<String> variables = new ArrayList<String>();
		variables.addAll(traces.get(0).getVariableNames());

		Collections.sort(variables, new StringNaturalOrdering());

		// Write variable names
		Iterator<String> variableIter = variables.iterator();

		while (variableIter.hasNext()) {
			output += variableIter.next();
			if (variableIter.hasNext()) {
				output += ",";
			}
		}

		output += "\n";

		// Write values
		Iterator<LustreTrace> traceIter = traces.iterator();

		// Iterate all test cases
		while (traceIter.hasNext()) {
			LustreTrace trace = traceIter.next();
			int length = trace.getLength();

			// Iterate from step 0 to (length - 1)
			for (int step = 0; step < length; step++) {
				variableIter = variables.iterator();

				// Iterate all input variables
				while (variableIter.hasNext()) {
					String variable = variableIter.next();

					// Value can be null
					Value value = trace.getVariable(variable).getValue(step);
					Type type = typeMap.get(variable);

					if (value == null) {
						output += "null";
					} else {
						// Also Convert EnumType values from integer back to
						// EnumValue
						String valueStr = ValueToString.get(value, type);
						output += valueStr;
					}

					// Add comma if not ending
					if (variableIter.hasNext()) {
						output += ",";
					}
				}
				output += "\n";
			}

			// Add new line if not ending
			if (traceIter.hasNext()) {
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
}

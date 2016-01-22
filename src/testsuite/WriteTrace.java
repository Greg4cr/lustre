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
import main.LustreMain;

/**
 * Write a test suite/trace to a file in CSV format. The test suite/trace may
 * contain "don't care" values (i.e., null values in the file). EnumType
 * variable values are translated back to EnumValue from IntegerValue. All
 * signals in a trace are written to the file.
 */
public final class WriteTrace {
	// Convert a list of traces to String
	public static String write(List<LustreTrace> traces, Program program) {
		if (traces.isEmpty()) {
			LustreMain.log("WARNING Empty traces");
			return null;
		}

		if (traces.get(0).getVariableNames().isEmpty()) {
			LustreMain.log("WARNING Empty variable set");
			return null;
		}

		String output = new WriteTrace().traceToString(traces, program);
		return output;
	}

	// Print a list of traces to a file
	public static void write(List<LustreTrace> traces, Program program,
			String fileName) {
		String output = write(traces, program);

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

	private String traceToString(List<LustreTrace> traces, Program program) {
		Map<String, Type> typeMap = ResolvedTypeTable.get(program);

		StringBuilder builder = new StringBuilder();

		List<String> variables = new ArrayList<String>();
		variables.addAll(traces.get(0).getVariableNames());

		Collections.sort(variables, new StringNaturalOrdering());

		// Write variable names
		Iterator<String> variableIter = variables.iterator();

		while (variableIter.hasNext()) {
			builder.append(variableIter.next());
			if (variableIter.hasNext()) {
				builder.append(",");
			}
		}

		builder.append("\n");

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

					if (type == null) {
						throw new IllegalArgumentException("Type of "
								+ variable + " cannot be resolved.");
					}

					if (value == null) {
						builder.append("null");
					} else {
						// Also Convert EnumType values from integer back to
						// EnumValue
						String valueStr = ValueToString.get(value, type);
						builder.append(valueStr);
					}

					// Add comma if not ending
					if (variableIter.hasNext()) {
						builder.append(",");
					}
				}
				builder.append("\n");
			}

			// Add new line if not ending
			if (traceIter.hasNext()) {
				builder.append("\n");
			}
		}

		return builder.toString();
	}
}

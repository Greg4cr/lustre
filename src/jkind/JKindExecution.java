package jkind;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.SolverOption;
import jkind.api.JKindApi;
import jkind.api.results.JKindResult;
import jkind.api.results.PropertyResult;
import jkind.api.results.Status;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.Signal;
import jkind.translation.Translate;
import lustre.LustreTrace;
import main.LustreMain;

import org.eclipse.core.runtime.NullProgressMonitor;

public class JKindExecution {
	// Default parameters for JKind
	public static int iteration = 40; // 40 steps
	public static int timeout = 172800; // 48 hours

	public static List<LustreTrace> generateTests(String fileName,
			Program program) {
		Map<String, LustreTrace> mapping = execute(fileName, program);
		List<LustreTrace> testSuite = new ArrayList<LustreTrace>();

		for (LustreTrace testCase : mapping.values()) {
			if (testCase != null) {
				testSuite.add(testCase);
			}
		}

		LustreMain.log(testSuite.size() + "/" + mapping.size()
				+ " properties have counterexamples.\n");

		return testSuite;
	}

	// Returns a mapping from a property name to its counterexample (if exists)
	// or null
	public static Map<String, LustreTrace> execute(String fileName,
			Program program) {
		Map<String, LustreTrace> output = new HashMap<String, LustreTrace>();

		JKindResult result = new JKindResult(null);
		NullProgressMonitor monitor = new NullProgressMonitor();

		JKindApi jkind = new JKindApi();
		jkind.setSolver(SolverOption.Z3);

		// Set timeout
		jkind.setN(iteration);
		jkind.setTimeout(timeout);
		LustreMain.log("------------Executing JKind\n");
		LustreMain.log("Iterations: " + iteration + "\n");
		LustreMain.log("Timeout: " + timeout + " seconds\n");

		jkind.execute(new File(fileName), result, monitor);

		LustreMain.log("------------JKind checked "
				+ result.getPropertyResults().size() + " properties.\n");

		for (PropertyResult pr : result.getPropertyResults()) {
			if (pr.getStatus().equals(Status.INVALID)) {
				InvalidProperty ipr = (InvalidProperty) pr.getProperty();
				Counterexample ce = ipr.getCounterexample();

				LustreTrace testCase = generateInputValues(ce, program);
				output.put(pr.getName(), testCase);
			} else {
				output.put(pr.getName(), null);
			}
		}
		return output;
	}

	// Generate input values for the program from a counter-example
	// Null values are added if an input variable is "don't care"
	// Each signal may also contain null values.
	public static LustreTrace generateInputValues(Counterexample ce,
			Program program) {
		LustreTrace output = new LustreTrace(ce.getLength());

		// Translate Lustre program to simple format
		Node node = Translate.translate(program);

		for (VarDecl input : node.inputs) {
			Signal<Value> signal = ce.getSignal(input.id);

			// If JKind does not produce values for this variable
			if (signal == null) {
				output.addVariable(new Signal<Value>(input.id));
			} else {
				output.addVariable(signal);
			}
		}

		return output;
	}
}

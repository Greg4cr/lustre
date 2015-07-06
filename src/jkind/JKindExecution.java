package jkind;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.SolverOption;
import jkind.api.JKindApi;
import jkind.api.results.JKindResult;
import jkind.api.results.PropertyResult;
import jkind.api.results.Status;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.lustre.values.Value;
import jkind.results.Counterexample;
import jkind.results.InvalidProperty;
import jkind.results.Signal;
import lustre.LustreTrace;

import org.eclipse.core.runtime.NullProgressMonitor;

public class JKindExecution {
	// Default parameters for JKind
	public static int iteration = 40; // 40 steps
	public static int timeout = 172800; // 48 hours

	// Returns a mapping from a property name to its counterexample (if exists)
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
		System.out.println("Iterations: " + iteration + "\n");
		System.out.println("Timeout: " + timeout + "\n");
		jkind.execute(new File(fileName), result, monitor);

		System.out.println("------------JKind checked "
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

		List<VarDecl> inputs = program.getMainNode().inputs;

		for (VarDecl input : inputs) {
			Signal<Value> signal = ce.getSignal(input.id);

			if (signal != null) {
				output.addVariable(signal);
				continue;
			}

			// This is a don't care variable
			Signal<Value> variable = new Signal<Value>(input.id);

			int length = ce.getLength();

			for (int step = 0; step < length; step++) {
				variable.putValue(step, null);
			}
			output.addVariable(variable);
		}

		return output;
	}
}

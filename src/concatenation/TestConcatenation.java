package concatenation;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.VarDecl;
import jkind.translation.Translate;
import simulation.LustreSimulator;
import lustre.LustreTrace;

public class TestConcatenation {
	private final Node node;
	private final LustreTrace testCase;
	private final LustreSimulator simulator;
	private final List<String> stateVariables;

	public TestConcatenation(Program program) {
		this.node = Translate.translate(program);
		this.testCase = new LustreTrace(0);
		this.simulator = new LustreSimulator(program);
		this.stateVariables = new ArrayList<String>();
		// Add local and output variables as state variables
		for (VarDecl var : node.locals) {
			this.stateVariables.add(var.id);
		}
		for (VarDecl var : node.outputs) {
			this.stateVariables.add(var.id);
		}
	}

	public void generate() {
	}
}

package concatenation;

import java.util.ArrayList;
import java.util.List;

import enums.Simulation;
import simulation.LustreSimulator;
import testsuite.WriteTrace;
import jkind.lustre.Program;
import lustre.LustreTrace;
import main.LustreMain;

public class ConcatenationMain {
	public static final String lustreFile = "example.lus";

	public static void main(String[] args) {
		Program program = LustreMain.getProgram(lustreFile);
		TestConcatenation tc = new TestConcatenation(program);

		List<LustreTrace> tests = new ArrayList<LustreTrace>();
		tests.add(tc.generate());

		String testStr = WriteTrace.write(tests, program);

		System.out.println("------------Generated tests");
		System.out.println(testStr);

		LustreSimulator simulator = new LustreSimulator(program);
		List<LustreTrace> traces = simulator.simulate(tests,
				Simulation.COMPLETE);
		String traceStr = WriteTrace.write(traces, program);

		System.out.println("------------Simulated results");
		System.out.println(traceStr);
	}
}

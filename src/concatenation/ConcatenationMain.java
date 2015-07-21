package concatenation;

import java.util.ArrayList;
import java.util.List;

import testsuite.WriteTrace;
import jkind.lustre.Program;
import lustre.LustreTrace;
import main.LustreMain;
import main.LustreProcessing;

public class ConcatenationMain {
	public static final String lustreFile = "farmer2.lus";

	public static void main(String[] args) {
		Program program = LustreMain.getProgram(lustreFile);

		TestConcatenation tc = new TestConcatenation(program);

		List<LustreTrace> tests = new ArrayList<LustreTrace>();
		tests.add(tc.generate());

		String testSuiteFile = LustreProcessing.removeFileExtension(lustreFile)
				+ ".incremental.csv";

		WriteTrace.write(tests, testSuiteFile, program);
		System.out.println("------------Completed incremental test generation");
		System.out.println("Test suite has been printed to " + testSuiteFile);
	}
}

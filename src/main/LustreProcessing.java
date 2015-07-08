package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import simulation.LustreSimulator;
import testsuite.FillNullValues;
import testsuite.ReadOracle;
import testsuite.ReadTestSuite;
import testsuite.WriteTrace;
import coverage.LustreCoverage;
import jkind.JKindExecution;
import jkind.lustre.Program;
import lustre.LustreTrace;

public class LustreProcessing {
	private final Program program;
	private final LustreSettings settings;
	private final String nameNoExtension;

	public LustreProcessing(Program program, LustreSettings settings) {
		this.program = program;
		this.settings = settings;
		this.nameNoExtension = removeFileExtension(settings.program);
	}

	public void process() {
		Program programTranslated = this.program;

		// Process coverage
		if (settings.coverage != null) {
			if (settings.polarity == null) {
				programTranslated = LustreCoverage.program(programTranslated,
						settings.coverage);
			} else {
				programTranslated = LustreCoverage.program(programTranslated,
						settings.coverage, settings.polarity);
			}

			String outputFile = this.nameNoExtension + "." + settings.coverage
					+ ".lus";
			LustreMain.log("------------Printing obligations to file");
			LustreMain.log(outputFile);
			printToFile(outputFile, programTranslated.toString());
		}

		// Process generation
		if (settings.generation != null) {
			List<LustreTrace> testSuite = JKindExecution
					.generateTests(programTranslated);

			List<LustreTrace> newTestSuite = FillNullValues.fill(testSuite,
					programTranslated, settings.generation);

			String outputFile = this.nameNoExtension + ".testsuite.csv";
			LustreMain.log("------------Printing test suite to file");
			LustreMain.log(outputFile);
			WriteTrace.write(newTestSuite, outputFile, programTranslated);
		}

		// Process simulation
		if (settings.simulation != null) {
			List<LustreTrace> testSuite = ReadTestSuite.read(settings.tests,
					programTranslated);
			LustreSimulator simulator = new LustreSimulator(programTranslated);

			List<LustreTrace> traces = null;
			if (settings.oracle == null) {
				traces = simulator.simulate(testSuite, settings.simulation);
			} else {
				List<String> oracle = ReadOracle.read(settings.oracle);
				traces = simulator.simulate(testSuite, settings.simulation,
						oracle);
			}

			String outputFile = this.nameNoExtension + ".trace.csv";
			LustreMain.log("------------Printing trace to file");
			LustreMain.log(outputFile);
			WriteTrace.write(traces, outputFile, programTranslated);
		}
	}

	// Print content to a file
	public static void printToFile(String fileName, String content) {
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new File(fileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw.print(content);
		pw.close();
	}

	// Remove file extension
	public static String removeFileExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf("."));
	}
}

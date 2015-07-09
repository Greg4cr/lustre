package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

import property.Measurement;
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

			LustreMain.log("------------Printing test suite to file");
			LustreMain.log(settings.tests);
			WriteTrace.write(newTestSuite, settings.tests, programTranslated);
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

		// Process measurement
		if (settings.measure != null) {
			List<LustreTrace> testSuite = ReadTestSuite.read(settings.tests,
					programTranslated);
			LustreSimulator simulator = new LustreSimulator(programTranslated);

			List<LustreTrace> traces = simulator.simulate(testSuite,
					settings.measure, simulator.getProperties());

			// Skip printing trace
			// String outputFile = this.nameNoExtension + ".trace.csv";
			// LustreMain.log("------------Printing trace to file");
			// LustreMain.log(outputFile);
			// WriteTrace.write(traces, outputFile, programTranslated);

			LustreMain
					.log("------------Measuring coverage and reducing test suite");
			List<LustreTrace> reducedTestSuite = Measurement.measure(testSuite,
					traces, programTranslated);
			String outputFile = removeFileExtension(settings.tests)
					+ ".reduced.csv";
			LustreMain.log("------------Printing reduced test suite to file");
			LustreMain.log(outputFile);
			LustreMain.log("Reduced test suite size: "
					+ reducedTestSuite.size() + "/" + testSuite.size());
			WriteTrace.write(reducedTestSuite, outputFile, programTranslated);
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

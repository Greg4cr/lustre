package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import property.LustreProperty;
import simulation.LustreSimulator;
import testsuite.FillNullValues;
import testsuite.ReadOracle;
import testsuite.ReadTestSuite;
import testsuite.WriteTrace;
import coverage.LustreCoverage;
import jkind.JKindExecution;
import jkind.lustre.Program;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.Value;
import jkind.results.Signal;
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

			String outputFile = this.nameNoExtension + ".trace.csv";
			LustreMain.log("------------Printing trace to file");
			LustreMain.log(outputFile);
			WriteTrace.write(traces, outputFile, programTranslated);

			LustreMain.log("------------Coverage measurement summary");
			this.satisfiedProperties(traces, programTranslated);
		}
	}

	private void satisfiedProperties(List<LustreTrace> traces, Program program) {
		Set<LustreProperty> properties = LustreProperty.getProperties(program);
		LustreMain.log("Number of trap properties: " + properties.size());

		Set<LustreProperty> satisfied = new HashSet<LustreProperty>();

		for (LustreTrace trace : traces) {
			Set<String> variables = trace.getVariableNames();
			for (String variable : variables) {
				Signal<Value> signal = trace.getVariable(variable);
				for (int step = 0; step < trace.getLength(); step++) {
					// For trap properties, FALSE means the original obligation
					// is satisfied
					if (signal.getValue(step) != null
							&& signal.getValue(step).equals(BooleanValue.FALSE)) {
						LustreProperty property = LustreProperty.convert(
								variable, program.main);
						if (properties.contains(property)) {
							satisfied.add(property);
						} else {
							throw new IllegalArgumentException(
									"Uknown property: " + variable);
						}
					}
				}
			}
		}

		LustreMain
				.log("------------Satisfied obligations: " + satisfied.size());
		for (LustreProperty property : satisfied) {
			LustreMain.log(property.toString());
			properties.remove(property);
		}

		LustreMain.log("------------Unsatisfied obligations: "
				+ properties.size());
		for (LustreProperty property : properties) {
			LustreMain.log(property.toString());
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

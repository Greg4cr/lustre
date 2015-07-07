package main;

import jkind.ArgumentParser;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

import coverage.Coverage;

public class LustreArgumentParser extends ArgumentParser {
	private static final String COVERAGE = "coverage";
	private static final String POLARITY = "polarity";
	private static final String GENERATE = "generate";
	private static final String SIMULATE = "simulate";
	private static final String ORACLE = "oracle";
	private static final String MEASURE = "measure";

	private final LustreSettings settings;

	private LustreArgumentParser() {
		this("LustreMain", new LustreSettings());
	}

	private LustreArgumentParser(String name, LustreSettings settings) {
		super(name, settings);
		this.settings = settings;
	}

	@Override
	protected Options getOptions() {
		Options options = super.getOptions();
		options.addOption(COVERAGE, true, "generate coverage obligations");
		options.addOption(POLARITY, true,
				"polarity of generated coverage obligations");
		options.addOption(GENERATE, true, "generate test cases");
		options.addOption(SIMULATE, true, "execute a test suite");
		options.addOption(ORACLE, true, "test oracle for test execution");
		options.addOption(MEASURE, true, "measure satisfaction of properties");
		return options;
	}

	public static LustreSettings parse(String[] args) {
		LustreArgumentParser parser = new LustreArgumentParser();
		parser.parseArguments(args);
		parser.checkSettings();
		return parser.settings;
	}

	@Override
	protected void parseCommandLine(CommandLine line) {
		super.parseCommandLine(line);

		if (line.hasOption(COVERAGE)) {
			settings.coverage = getCoverageOption(line.getOptionValue(COVERAGE));
		}

		if (line.hasOption(POLARITY)) {

		}

		if (line.hasOption(GENERATE)) {

		}

		if (line.hasOption(SIMULATE)) {

		}

		if (line.hasOption(ORACLE)) {

		}

		if (line.hasOption(MEASURE)) {

		}
	}

	private void checkSettings() {
		return;
	}

	private static Coverage getCoverageOption(String coverage) {
		Coverage[] options = Coverage.values();
		for (Coverage option : options) {
			if (coverage.equals(option.toString())) {
				return option;
			}
		}

		System.out.println("Unknown coverage: " + coverage);
		System.out.println("Valid options: " + options);
		System.exit(0);
		return null;
	}
}

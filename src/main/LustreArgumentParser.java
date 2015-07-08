package main;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class LustreArgumentParser {
	private static final String HELP = "help";
	private static final String COVERAGE = "coverage";
	private static final String POLARITY = "polarity";
	private static final String GENERATE = "generate";
	private static final String SIMULATE = "simulate";
	private static final String ORACLE = "oracle";
	private static final String MEASURE = "measure";

	private final String name;
	private final LustreSettings settings;

	private LustreArgumentParser() {
		this.name = "LustreMain";
		this.settings = new LustreSettings();
	}

	protected Options getOptions() {
		Options options = new Options();
		options.addOption(HELP, false, "print this message");
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

	private void parseArguments(String[] args) {
		CommandLineParser parser = new BasicParser();
		try {
			parseCommandLine(parser.parse(getOptions(), args));
		} catch (ParseException e) {
			System.out.println(e.getMessage());
			System.exit(0);
		}
	}

	private void parseCommandLine(CommandLine line) {
		if (line.hasOption(HELP)) {
			printHelp();
			System.exit(0);
		}

		String[] input = line.getArgs();
		if (input.length != 1) {
			printHelp();
			System.exit(0);
		}

		this.settings.fileName = input[0];

		if (line.hasOption(COVERAGE)) {

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

	private void printHelp() {
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp(this.name + " [options] <input>", getOptions());
	}
}

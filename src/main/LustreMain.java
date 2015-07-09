package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import org.antlr.v4.runtime.RecognitionException;

import jkind.Main;
import jkind.SolverOption;
import jkind.analysis.StaticAnalyzer;
import jkind.lustre.Program;

public class LustreMain {
	private static String log = "";
	private static String logFile = "LustreMain.log";

	public static void main(String[] args) {
		LustreSettings settings = LustreArgumentParser.parse(args);
		Program program = getProgram(settings.program);
		log("Main node: " + program.main);

		LustreProcessing lustre = new LustreProcessing(program, settings);
		lustre.process();

		writeLogToFile();
	}

	// Print error message and exit
	public static void error(String msg) {
		System.out.println("ERROR " + msg);
		System.exit(0);
	}

	// Print and add log message
	public static void log(String msg) {
		System.out.println(msg);
		log += msg + "\n";
	}

	public static void writeLogToFile() {
		System.out.println("------------Writing log file\n" + logFile);

		// Initialize log file writer
		File file = new File(logFile);
		// Delete existing file
		if (file.exists()) {
			file.delete();
		}

		PrintWriter pw = null;
		try {
			pw = new PrintWriter(file);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		pw.write(log);
		pw.close();
	}

	// Read in Lustre program from a file
	public static Program getProgram(String fileName) {
		Program program = null;
		try {
			program = Main.parseLustre(fileName);
		} catch (RecognitionException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		StaticAnalyzer.check(program, SolverOption.Z3);
		return program;
	}
}

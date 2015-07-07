package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

import jkind.Main;
import jkind.SolverOption;
import jkind.analysis.StaticAnalyzer;
import jkind.lustre.Program;

import org.antlr.v4.runtime.RecognitionException;

public class Utils {
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

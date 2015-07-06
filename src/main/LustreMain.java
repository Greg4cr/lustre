package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

public class LustreMain {
	private static String log = "";
	private static String logFile = "LustreMain.log";

	public static void main(String[] args) throws Exception {
		System.out.println("this is main");
	}

	public static void log(String msg) {
		System.out.print(msg);
		log += msg;
	}

	public static void writeLogToFile() {
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

	public static String removeFileExtension(String fileName) {
		return fileName.substring(0, fileName.lastIndexOf("."));
	}
}

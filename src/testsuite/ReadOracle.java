package testsuite;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Read optional oracle variables from a file, in which variables are separated
 * by commas.
 */
public final class ReadOracle {
	public static List<String> read(String fileName) {
		return new ReadOracle()._read(fileName);
	}

	private List<String> _read(String fileName) {
		List<String> oracle = new ArrayList<String>();

		Scanner sc = null;
		try {
			sc = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringBuilder builder = new StringBuilder();

		while (sc.hasNext()) {
			builder.append(sc.nextLine());
		}
		sc.close();

		String allLines = builder.toString();
		allLines = allLines.replaceAll("\\s", "");
		String[] variables = allLines.split(",");

		for (String variable : variables) {
			// Add this variable as oracle if it is not added yet
			if (!oracle.contains(variable)) {
				oracle.add(variable);
			}
		}

		return oracle;
	}
}

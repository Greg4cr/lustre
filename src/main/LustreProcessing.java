package main;

import jkind.lustre.Program;

public class LustreProcessing {
	private final Program program;
	private final LustreSettings settings;

	public LustreProcessing(Program program, LustreSettings settings) {
		this.program = program;
		this.settings = settings;
	}

	public void process() {
		System.out.println(program);
		System.out.println(settings);
	}
}

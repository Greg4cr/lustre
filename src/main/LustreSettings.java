package main;

import enums.Coverage;
import enums.Generation;
import enums.Polarity;
import enums.Simulation;

public final class LustreSettings {
	public String program = null;
	public String tests = null;
	public String oracle = null;

	public Coverage coverage = null;
	public Polarity polarity = null;
	public Generation generation = null;
	public Simulation simulation = null;
	public Simulation measure = null;

	public boolean cse = true;
}

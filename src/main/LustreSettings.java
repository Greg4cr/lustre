package main;

import values.ValueType;
import coverage.Coverage;
import coverage.Polarity;
import jkind.Settings;

public class LustreSettings extends Settings {
	public Coverage coverage = null;
	public Polarity polarity = null;
	public ValueType generate = null;
	public boolean simulate = false;
	public boolean oracle = false;
	public boolean measure = false;
}

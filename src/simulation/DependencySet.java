package simulation;

import java.util.HashSet;
import java.util.Set;

import jkind.lustre.Equation;

/**
 * The set of variables that an equation depends on
 */
public final class DependencySet implements Comparable<DependencySet> {
	public final Equation equation;
	public final Set<String> dependOn;

	public DependencySet(Equation equation, Set<String> dependOn) {
		this.equation = equation;
		this.dependOn = new HashSet<String>();
		this.dependOn.addAll(dependOn);
	}

	@Override
	public int compareTo(DependencySet other) {
		if (this.dependOn.size() > other.dependOn.size()) {
			return 1;
		} else if (this.dependOn.size() == other.dependOn.size()) {
			return 0;
		} else {
			return -1;
		}
	}

	@Override
	public String toString() {
		return "DependencySet [equation=" + equation + ", dependOn=" + dependOn
				+ "]";
	}
}

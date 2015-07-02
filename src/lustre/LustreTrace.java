package lustre;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import jkind.lustre.values.Value;
import jkind.results.Signal;

public class LustreTrace {
	private final Map<String, Signal<Value>> variables;
	private int length;

	public LustreTrace(int length) {
		this.variables = new HashMap<String, Signal<Value>>();
		this.length = length;
	}

	public int getLength() {
		return this.length;
	}

	public void addVariable(Signal<Value> variable) {
		this.variables.put(variable.getName(), variable);
	}

	public Signal<Value> getVariable(String name) {
		return this.variables.get(name);
	}

	public Set<String> getVariableNames() {
		return this.variables.keySet();
	}
}

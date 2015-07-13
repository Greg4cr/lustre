package property;

import java.util.HashSet;
import java.util.Set;

import jkind.lustre.Node;
import jkind.lustre.Program;

public final class LustreProperty {
	public final String node;
	public final String property;

	private LustreProperty(String node, String property) {
		this.node = node;
		this.property = property;
	}

	// Get all properties in a program
	public static Set<LustreProperty> getProperties(Program program) {
		Set<LustreProperty> properties = new HashSet<LustreProperty>();

		for (Node node : program.nodes) {
			for (String property : node.properties) {
				properties.add(new LustreProperty(node.id, property));
			}
		}

		return properties;
	}

	// Convert a translated property to its original form
	// boat~0.swap~0.ArithExpr_0_TRUE_AT_other_MCDC_TRUE_0
	public static LustreProperty convert(String property, String main) {
		if (property.contains("~")) {
			String prefix = property.substring(0, property.lastIndexOf("~"));
			String suffix = property.substring(property.lastIndexOf("~") + 1);

			prefix = prefix.substring(prefix.lastIndexOf(".") + 1);
			suffix = suffix.substring(suffix.indexOf(".") + 1);

			return new LustreProperty(prefix, suffix);
		} else {
			return new LustreProperty(main, property);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((node == null) ? 0 : node.hashCode());
		result = prime * result
				+ ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LustreProperty other = (LustreProperty) obj;
		if (node == null) {
			if (other.node != null)
				return false;
		} else if (!node.equals(other.node))
			return false;
		if (property == null) {
			if (other.property != null)
				return false;
		} else if (!property.equals(other.property))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return node + ": " + property;
	}
}

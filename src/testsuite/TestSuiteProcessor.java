package testsuite;

import jkind.lustre.EnumType;
import jkind.lustre.Type;
import jkind.lustre.values.EnumValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.Value;

public class TestSuiteProcessor {
	// Convert an enum to int
	public static void convert(Type type, Value value) {
		if (type instanceof EnumType) {
			EnumType et = (EnumType) type;
			System.out.println(et.values);
			System.out.println(value);
			System.out.println(et.values.indexOf(value.toString()));
		}
	}

	// Convert an int to enum
	private Value convert(String base, Value value) {
		Type type = null;// typeMap.get(base);
		if (type instanceof EnumType) {
			EnumType et = (EnumType) type;
			IntegerValue iv = (IntegerValue) value;
			return new EnumValue(et.values.get(iv.value.intValue()));
		}
		return value;
	}
}

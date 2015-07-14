package values;

import java.math.BigDecimal;
import java.math.BigInteger;

import jkind.lustre.ArrayType;
import jkind.lustre.EnumType;
import jkind.lustre.NamedType;
import jkind.lustre.RecordType;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.TupleType;
import jkind.lustre.Type;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.TypeVisitor;
import jkind.util.BigFraction;

/**
 * Convert a value from String to Value. This class assumes that ArrayType,
 * RecordType, and TupleType have been inlined/flattened. Also Convert EnumType
 * values from EnumValue to integers.
 */
public final class StringToValue implements TypeVisitor<Value> {
	public static Value get(String valueStr, Type type) {
		StringToValue visitor = new StringToValue(valueStr);
		Value value = type.accept(visitor);
		if (value == null) {
			throw new IllegalArgumentException("Null value for valueStr "
					+ valueStr + " and type " + type);
		}
		return value;
	}

	private final String valueStr;

	private StringToValue(String valueStr) {
		this.valueStr = valueStr;
	}

	@Override
	public Value visit(ArrayType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value visit(EnumType type) {
		// If Enum values are translated to integers
		if (type.values.contains(valueStr)) {
			int index = type.values.indexOf(valueStr);
			return new IntegerValue(new BigInteger("" + index));
		} else {
			throw new IllegalArgumentException("Invalid valueStr: " + valueStr);
		}
	}

	@Override
	public Value visit(NamedType type) {
		if (type.equals(NamedType.BOOL)) {
			if (valueStr.equals("true")) {
				return BooleanValue.TRUE;
			} else if (valueStr.equals("false")) {
				return BooleanValue.FALSE;
			} else {
				throw new IllegalArgumentException("Unknown Boolean value: "
						+ valueStr);
			}
		} else if (type.equals(NamedType.INT)) {
			return new IntegerValue(new BigInteger(valueStr));
		} else if (type.equals(NamedType.REAL)) {
			BigFraction fractionValue = null;
			// If raw value is a fraction
			if (valueStr.contains("/")) {
				String numerator = valueStr.substring(0, valueStr.indexOf("/"));
				String denominator = valueStr
						.substring(valueStr.indexOf("/") + 1);
				fractionValue = new BigFraction(new BigInteger(numerator),
						new BigInteger(denominator));
			}
			// Otherwise, it should be a decimal
			else {
				fractionValue = new BigFraction(new BigDecimal(valueStr));
			}
			return new RealValue(fractionValue);
		} else {
			throw new IllegalArgumentException("Unknown NamedType value: "
					+ valueStr);
		}
	}

	@Override
	public Value visit(RecordType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value visit(TupleType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value visit(SubrangeIntType type) {
		BigInteger intermediateValue = new BigInteger(valueStr);

		if (intermediateValue.compareTo(type.high) <= 0
				&& intermediateValue.compareTo(type.low) >= 0) {
			return new IntegerValue(intermediateValue);
		} else {
			throw new IllegalArgumentException("Out of range SubrangeIntType: "
					+ type);
		}
	}
}

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
import jkind.lustre.values.EnumValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.TypeVisitor;
import jkind.util.BigFraction;

/**
 * Convert a value from String to Value. This class assumes that ArrayType,
 * RecordType,and TupleType have been flattened
 */
public class ValueFromString implements TypeVisitor<Value> {
	public static Value get(String value, Type type) {
		if (value.equals("null")) {
			return null;
		}
		ValueFromString visitor = new ValueFromString(value);
		return type.accept(visitor);
	}

	private final String value;

	private ValueFromString(String value) {
		this.value = value;
	}

	@Override
	public Value visit(ArrayType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value visit(EnumType type) {
		return new EnumValue(value);
	}

	@Override
	public Value visit(NamedType type) {
		if (type.equals(NamedType.BOOL)) {
			if (value.equals("true") || value.equals("1")) {
				return BooleanValue.TRUE;
			} else if (value.equals("false") || value.equals("0")) {
				return BooleanValue.FALSE;
			} else {
				throw new IllegalArgumentException("Unknown Boolean value: "
						+ value);
			}
		} else if (type.equals(NamedType.INT)) {
			return new IntegerValue(new BigInteger(value));
		} else if (type.equals(NamedType.REAL)) {
			BigFraction fractionValue = null;
			// If raw value is a fraction
			if (value.contains("/")) {
				String numerator = value.substring(0, value.indexOf("/"));
				String denominator = value.substring(value.indexOf("/") + 1);
				fractionValue = new BigFraction(new BigInteger(numerator),
						new BigInteger(denominator));
			}
			// Otherwise, it should be a decimal
			else {
				fractionValue = new BigFraction(new BigDecimal(value));
			}
			return new RealValue(fractionValue);
		} else {
			throw new IllegalArgumentException("Unknown NamedType value: "
					+ value);
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
		BigInteger intermediateValue = new BigInteger(value);

		if (intermediateValue.compareTo(type.high) <= 0
				&& intermediateValue.compareTo(type.low) >= 0) {
			return new IntegerValue(intermediateValue);
		} else {
			throw new IllegalArgumentException("Out of range: " + type);
		}
	}
}

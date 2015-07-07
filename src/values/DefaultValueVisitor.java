package values;

import java.math.BigDecimal;
import java.math.BigInteger;

import jkind.lustre.EnumType;
import jkind.lustre.NamedType;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.Type;
import jkind.lustre.values.BooleanValue;
import jkind.lustre.values.IntegerValue;
import jkind.lustre.values.RealValue;
import jkind.lustre.values.Value;
import jkind.util.BigFraction;

/**
 * Generate a default value for a type. Used to fill in null values for test
 * suites. Assuming Lustre is in simple format, EnumType is treated as integers.
 */
public final class DefaultValueVisitor extends ValueVisitor {
	public static Value get(Type type) {
		DefaultValueVisitor visitor = new DefaultValueVisitor();
		return type.accept(visitor);
	}

	@Override
	public Value visit(EnumType type) {
		return new IntegerValue(new BigInteger("0"));
	}

	@Override
	public Value visit(NamedType type) {
		if (type.equals(NamedType.BOOL)) {
			return BooleanValue.FALSE;
		} else if (type.equals(NamedType.INT)) {
			return new IntegerValue(new BigInteger("0"));
		} else if (type.equals(NamedType.REAL)) {
			return new RealValue(new BigFraction(new BigDecimal("0.0")));
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Value visit(SubrangeIntType type) {
		return new IntegerValue(type.low);
	}
}

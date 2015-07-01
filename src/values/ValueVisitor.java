package values;

import jkind.lustre.ArrayType;
import jkind.lustre.EnumType;
import jkind.lustre.NamedType;
import jkind.lustre.RecordType;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.TupleType;
import jkind.lustre.values.Value;
import jkind.lustre.visitors.TypeVisitor;

public class ValueVisitor implements TypeVisitor<Value> {
	@Override
	public Value visit(ArrayType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value visit(EnumType type) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Value visit(NamedType type) {
		// TODO Auto-generated method stub
		return null;
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
		// TODO Auto-generated method stub
		return null;
	}
}

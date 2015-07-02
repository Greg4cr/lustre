package types;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.ArrayAccessExpr;
import jkind.lustre.ArrayExpr;
import jkind.lustre.ArrayType;
import jkind.lustre.ArrayUpdateExpr;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BoolExpr;
import jkind.lustre.CastExpr;
import jkind.lustre.CondactExpr;
import jkind.lustre.Constant;
import jkind.lustre.EnumType;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.RealExpr;
import jkind.lustre.RecordAccessExpr;
import jkind.lustre.RecordExpr;
import jkind.lustre.RecordType;
import jkind.lustre.RecordUpdateExpr;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.TupleExpr;
import jkind.lustre.TupleType;
import jkind.lustre.Type;
import jkind.lustre.TypeDef;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.ExprVisitor;
import jkind.lustre.visitors.TypeMapVisitor;
import jkind.util.Util;

/**
 * Get type of an expression
 */
public final class ExprTypeVisitor implements ExprVisitor<Type> {
	private final Map<String, Type> typeTable = new HashMap<>();
	private final Map<String, Type> constantTable = new HashMap<>();
	private final Map<String, EnumType> enumValueTable = new HashMap<>();
	private final Map<String, Type> variableTable = new HashMap<>();
	private final Map<String, Node> nodeTable = new HashMap<>();

	public void print() {
		System.out.println("---------------------------");
		for (String str : this.typeTable.keySet()) {
			System.out.println(str + " " + this.typeTable.get(str) + " "
					+ this.typeTable.get(str).getClass());
		}
		System.out.println("---------------------------");
		for (String str : this.constantTable.keySet()) {
			System.out.println(str + " " + this.constantTable.get(str) + " "
					+ this.constantTable.get(str).getClass());
		}
		System.out.println("---------------------------");
		for (String str : this.enumValueTable.keySet()) {
			System.out.println(str + " " + this.enumValueTable.get(str));
		}
		System.out.println("---------------------------");
		for (String str : this.variableTable.keySet()) {
			System.out.println(str + " " + this.variableTable.get(str) + " "
					+ this.variableTable.get(str).getClass());
		}
		System.out.println("---------------------------");
	}

	public ExprTypeVisitor(Program program) {
		populateTypeTable(program.types);
		populateEnumValueTable(program.types);
		populateConstantTable(program.constants);
		nodeTable.putAll(Util.getNodeTable(program.nodes));
	}

	private void populateTypeTable(List<TypeDef> typeDefs) {
		typeTable.putAll(Util.createResolvedTypeTable(typeDefs));
	}

	private void populateConstantTable(List<Constant> constants) {
		for (Constant c : constants) {
			if (c.type == null) {
				constantTable.put(c.id, c.expr.accept(this));
			} else {
				constantTable.put(c.id, this.resolveType(c.type));
			}
		}
	}

	private void populateEnumValueTable(List<TypeDef> typeDefs) {
		for (EnumType et : Util.getEnumTypes(typeDefs)) {
			for (String id : et.values) {
				enumValueTable.put(id, et);
			}
		}
	}

	public void setNodeContext(Node node) {
		variableTable.clear();
		Util.getVarDecls(node).forEach(this::addVariable);
	}

	private void addVariable(VarDecl varDecl) {
		variableTable.put(varDecl.id, this.resolveType(varDecl.type));
		System.out.println("addvariable: " + varDecl.id + " "
				+ this.resolveType(varDecl.type));
	}

	@Override
	public Type visit(ArrayAccessExpr e) {
		ArrayType array = (ArrayType) e.array.accept(this);
		return array.base;
	}

	@Override
	public Type visit(ArrayExpr e) {
		return new ArrayType(e.elements.get(0).accept(this), e.elements.size());
	}

	@Override
	public Type visit(ArrayUpdateExpr e) {
		return e.array.accept(this);
	}

	@Override
	public Type visit(BinaryExpr expr) {
		Type type = null;

		switch (expr.op) {
		case EQUAL:
		case NOTEQUAL:
		case GREATER:
		case LESS:
		case GREATEREQUAL:
		case LESSEQUAL:
		case OR:
		case AND:
		case XOR:
		case IMPLIES:
			return NamedType.BOOL;

		case PLUS:
		case MINUS:
		case MULTIPLY:
			type = expr.left.accept(this);
			if (type instanceof SubrangeIntType) {
				// Cast SubrangeIntType to Int
				return NamedType.INT;
			} else {
				return type;
			}

		case DIVIDE:
			return NamedType.REAL;

		case INT_DIVIDE:
		case MODULUS:
			return NamedType.INT;

		case ARROW:
			type = expr.left.accept(this);
			if (type instanceof SubrangeIntType) {
				// Resolve SubrangeIntType to SubrangeIntType or Int
				return resolveSubrangeIntType(type, expr.right.accept(this));
			} else {
				return type;
			}

		default:
			throw new IllegalArgumentException();
		}
	}

	@Override
	public Type visit(BoolExpr e) {
		return NamedType.BOOL;
	}

	@Override
	public Type visit(CastExpr e) {
		return e.type;
	}

	@Override
	public Type visit(CondactExpr e) {
		return e.call.accept(this);
	}

	@Override
	public Type visit(IdExpr e) {
		if (variableTable.containsKey(e.id)) {
			return variableTable.get(e.id);
		} else if (constantTable.containsKey(e.id)) {
			return constantTable.get(e.id);
		} else if (enumValueTable.containsKey(e.id)) {
			return enumValueTable.get(e.id);
		} else {
			throw new IllegalArgumentException("Unknown variable: " + e.id);
		}
	}

	@Override
	public Type visit(IfThenElseExpr expr) {
		Type type = expr.thenExpr.accept(this);
		if (type instanceof SubrangeIntType) {
			// Resolve SubrangeIntType to SubrangeIntType or Int
			return resolveSubrangeIntType(type, expr.elseExpr.accept(this));
		} else {
			return type;
		}
	}

	@Override
	public Type visit(IntExpr expr) {
		// Integers are assigned SubrangeIntType temporarily
		return new SubrangeIntType(new BigInteger(expr.toString()),
				new BigInteger(expr.toString()));
	}

	@Override
	public Type visit(NodeCallExpr e) {
		Node node = nodeTable.get(e.node);
		List<Type> outputs = new ArrayList<>();
		for (VarDecl output : node.outputs) {
			outputs.add(resolveType(output.type));
		}
		return TupleType.compress(outputs);
	}

	@Override
	public Type visit(RealExpr e) {
		return NamedType.REAL;
	}

	@Override
	public Type visit(RecordAccessExpr e) {
		RecordType record = (RecordType) e.record.accept(this);
		return record.fields.get(e.field);
	}

	@Override
	public Type visit(RecordExpr e) {
		if (typeTable.containsKey(e.id)) {
			return typeTable.get(e.id);
		} else {
			// If user types have already been inlined, we reconstruct the type
			Map<String, Type> fields = new HashMap<>();
			for (String field : e.fields.keySet()) {
				fields.put(field, e.fields.get(field).accept(this));
			}
			return new RecordType(e.id, fields);
		}
	}

	@Override
	public Type visit(RecordUpdateExpr e) {
		return e.record.accept(this);
	}

	@Override
	public Type visit(TupleExpr e) {
		List<Type> types = new ArrayList<>();
		for (Expr expr : e.elements) {
			types.add(expr.accept(this));
		}
		return new TupleType(types);
	}

	@Override
	public Type visit(UnaryExpr expr) {
		if (expr.op.equals(UnaryOp.NOT)) {
			return NamedType.BOOL;
		} else {
			return expr.expr.accept(this);
		}
	}

	public Type resolveType(Type type) {
		return type.accept(new TypeMapVisitor() {
			@Override
			public Type visit(NamedType e) {
				if (e.isBuiltin()) {
					return e;
				} else {
					return typeTable.get(e.name);
				}
			}
		});
	}

	// Resolve SubrangeIntType to SubrangeIntType or Int
	public Type resolveSubrangeIntType(Type leftType, Type rightType) {
		if (!(leftType instanceof SubrangeIntType)) {
			throw new IllegalArgumentException("Invalid leftType: " + leftType);
		}

		if (rightType instanceof SubrangeIntType) {
			// Extend the range of combined SubrangeIntType
			BigInteger low = ((SubrangeIntType) leftType).low
					.compareTo(((SubrangeIntType) rightType).low) < 0 ? ((SubrangeIntType) leftType).low
					: ((SubrangeIntType) rightType).low;
			BigInteger high = ((SubrangeIntType) leftType).high
					.compareTo(((SubrangeIntType) rightType).high) > 0 ? ((SubrangeIntType) leftType).high
					: ((SubrangeIntType) rightType).high;
			return new SubrangeIntType(low, high);
		} else if (rightType.equals(NamedType.INT)) {
			return NamedType.INT;
		} else {
			throw new IllegalArgumentException("Invalid rightType: "
					+ rightType);
		}
	}
}

package types;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;

public final class ResolvedTypeTable {
	public static Map<String, Type> get(Program program) {
		Map<String, Type> mapping = new HashMap<String, Type>();
		Node main = program.getMainNode();

		ExprTypeVisitor exprTypeVisitor = new ExprTypeVisitor(program);
		exprTypeVisitor.setNodeContext(main);

		for (VarDecl var : main.inputs) {
			mapping.put(var.id, exprTypeVisitor.resolveType(var.type));
		}

		for (VarDecl var : main.locals) {
			mapping.put(var.id, exprTypeVisitor.resolveType(var.type));
		}

		for (VarDecl var : main.outputs) {
			mapping.put(var.id, exprTypeVisitor.resolveType(var.type));
		}
		return mapping;
	}
}

package types;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.Type;
import jkind.lustre.VarDecl;
import jkind.translation.Translate;

/**
 * Get a type table for all variables in a Lustre program, compound types are
 * resolved. The program is first translated.
 */
public final class ResolvedTypeTable {
	public static Map<String, Type> get(Program program) {
		Map<String, Type> mapping = new HashMap<String, Type>();

		Node node = Translate.translate(program);

		ExprTypeVisitor exprTypeVisitor = new ExprTypeVisitor(program);
		exprTypeVisitor.setNodeContext(node);

		for (VarDecl var : node.inputs) {
			mapping.put(var.id, exprTypeVisitor.resolveType(var.type));
		}

		for (VarDecl var : node.locals) {
			mapping.put(var.id, exprTypeVisitor.resolveType(var.type));
		}

		for (VarDecl var : node.outputs) {
			mapping.put(var.id, exprTypeVisitor.resolveType(var.type));
		}
		return mapping;
	}
}

package testsuite;

import java.util.Comparator;

import jkind.lustre.VarDecl;
import jkind.util.StringNaturalOrdering;

public class VarDeclNaturalOrdering implements Comparator<VarDecl> {
	@Override
	public int compare(VarDecl a, VarDecl b) {
		return new StringNaturalOrdering().compare(a.id, b.id);
	}
}

package coverage;

import java.util.ArrayList;
import java.util.List;

import enums.Coverage;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import types.ExprTypeVisitor;

public class ObservedCoverage {
// OMCDC, OCONDITION, OBRANCH, ODECISION
	private final Coverage coverage;
	private final ExprTypeVisitor exprTypeVisitor;
	private int count;
	
	public ObservedCoverage(Coverage coverage, ExprTypeVisitor exprTypeVisitor) {
		this.coverage = coverage;
		this.exprTypeVisitor = exprTypeVisitor;
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		CoverageVisitor coverageVisitor = null;
		
		
		
		return obligations;
	}
}

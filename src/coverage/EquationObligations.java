package coverage;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import types.ExprTypeVisitor;

public class EquationObligations extends ConditionVisitor{
	String omcdc = "omcdc_";
	int count = 0;
	IdExpr token = new IdExpr("token");
	IdExpr outState = new IdExpr("TOKEN_OUTPUT_STATE");
	String affect = "_AFFECTING_AT_CAPTURE";
	String t = "_TRUE", f = "_FALSE", at = "_AT_", cov = "_MCDC";
	String observed = "_COMB_OBSERVED";
	ObservedTreeNode node, parent;
	IdExpr lhs;
	IdExpr[] nonMaskedExpr = new IdExpr[2], affectExpr = new IdExpr[2];
	Expr lOperand, rOperand;
	
	public EquationObligations(ExprTypeVisitor exprTypeVisitor) {
		super(exprTypeVisitor);
	}
	
	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);
		
		for (Obligation leftOb : leftObs) {
			if (leftOb.obligation instanceof UnaryExpr) {
				continue;
			}
			if (leftOb.obligation.toString().equalsIgnoreCase(expr.left.toString())) {
				continue;
			}
			System.out.println("left");
			
			nonMaskedExpr[0] = new IdExpr(leftOb.obligation + t + at + expr.left + cov + t);
			nonMaskedExpr[1] = new IdExpr(leftOb.obligation + f + at + expr.left + cov + f);
			affectExpr[0] = new IdExpr(leftOb.obligation + t + at + expr.left + affect);
			affectExpr[1] = new IdExpr(leftOb.obligation + f + at + expr.left + affect);
			for (int k = 0; k < 2; k++) {
				IdExpr lhs = new IdExpr(omcdc + (count++));
				Expr lOperand = new BinaryExpr(nonMaskedExpr[k], BinaryOp.AND, 
										new IdExpr(expr.left + observed));
				Expr rOperand = new BinaryExpr(affectExpr[k], BinaryOp.AND,
										new BinaryExpr(token, BinaryOp.EQUAL, outState));
				leftOb = new Obligation(lhs, true, new UnaryExpr(UnaryOp.NOT, (new BinaryExpr(
						lOperand, BinaryOp.OR, rOperand))));
				
				obligations.add(leftOb);
			}
		}
		
		for (Obligation rightOb : rightObs) {
			if (rightOb.obligation instanceof UnaryExpr) {
				continue;
			}
			if (rightOb.obligation.toString().equalsIgnoreCase(expr.left.toString())) {
				continue;
			}
			System.out.println("right");
			
			nonMaskedExpr[0] = new IdExpr(rightOb.obligation + t + at + expr.left + cov + t);
			nonMaskedExpr[1] = new IdExpr(rightOb.obligation + f + at + expr.left + cov + f);
			affectExpr[0] = new IdExpr(rightOb.obligation + t + at + expr.left + affect);
			affectExpr[1] = new IdExpr(rightOb.obligation + f + at + expr.left + affect);
			for (int k = 0; k < 2; k++) {
				IdExpr lhs = new IdExpr(omcdc + (count++));
				Expr lOperand = new BinaryExpr(nonMaskedExpr[k], BinaryOp.AND, 
										new IdExpr(expr.left + observed));
				Expr rOperand = new BinaryExpr(affectExpr[k], BinaryOp.AND,
										new BinaryExpr(token, BinaryOp.EQUAL, outState));
				rightOb = new Obligation(lhs, true, new UnaryExpr(UnaryOp.NOT, (new BinaryExpr(
						lOperand, BinaryOp.OR, rOperand))));
				
				obligations.add(rightOb);
			}
		}
//		
//		obligations.addAll(leftObs);
//		obligations.addAll(rightObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		obligations.addAll(expr.cond.accept(this));
		return obligations;
	}
	
}

package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import enums.Coverage;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class ObservedCoverageObligation {
	private HashMap<String, HashMap<String, Integer>> obligationMap;
	private Coverage coverage;
	
	public ObservedCoverageObligation(HashMap<String, 
									HashMap<String, Integer>> obligationMap,
									Coverage coverage) {
		this.obligationMap = obligationMap;
		this.coverage = coverage;
	}
		
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", cov = "_"+coverage.name();
		String observed = "_COMB_OBSERVED";
		int count = 0;
		String property = "property";
		IdExpr lhs;
		String[] vals = {"_TRUE", "_FALSE"};
		IdExpr[] nonMaskedExpr = new IdExpr[2], affectExpr = new IdExpr[2];
		Expr leftOperand, rightOperand;
		Obligation obligation;
		Expr transition = new BinaryExpr(new IdExpr("token"), BinaryOp.EQUAL, 
				new IdExpr("TOKEN_OUTPUT_STATE"));
		
		for (String key : obligationMap.keySet()) {
			HashMap<String, Integer> conditions = obligationMap.get(key);
			for (String cond : conditions.keySet()) {
				int occurence = conditions.get(cond) / 2;
				for (int i = 0; i < occurence; i++) {
					if (occurence > 1) {
						cond = cond + "_" + i;
					}
					for (int j = 0; j < vals.length; j++) {
						lhs = new IdExpr(property + "_" + count++);
						nonMaskedExpr[j] = new IdExpr(cond + vals[j] + at + key + cov + vals[j]);
						affectExpr[j] = new IdExpr(cond + vals[j] + at + key + affect);
						
						leftOperand = new BinaryExpr(nonMaskedExpr[j], BinaryOp.AND, 
												new IdExpr(key + observed));
						rightOperand = new BinaryExpr(affectExpr[j], BinaryOp.AND,
												transition);
						Expr condition = new UnaryExpr(UnaryOp.NOT, (new BinaryExpr(
											leftOperand, BinaryOp.OR, rightOperand)));
						
						obligation = new Obligation(lhs, false, condition);
						obligations.add(obligation);
					}
					
				}
			}
		}
		
		return obligations;
	}
}

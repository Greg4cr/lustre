package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import coverage.Obligation;
import enums.Coverage;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public class ObservedCoverageObligation {
	private TreeMap<String, TreeMap<String, Integer>> idToCondMap;
	private Coverage coverage;
	private HashMap<String, List<String>> affectPairs = new HashMap<>();
	
	public ObservedCoverageObligation(TreeMap<String, TreeMap<String, Integer>> idToCondMap,
									HashMap<String, List<String>> affectPairs,
									Coverage coverage) {
		this.idToCondMap = idToCondMap;
		this.affectPairs = affectPairs;
		this.coverage = coverage;
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", cov = "_" + coverage.name();
		String observed = "_COMB_OBSERVED";
		int count = 0;
		String property = "property";
		IdExpr lhs;
		String[] vals = {"_TRUE", "_FALSE"};
		Expr[] nonMaskedExpr = new Expr[2], affectExpr = new Expr[2];
		Expr leftOperand, rightOperand;
		Obligation obligation;
		String condStr = "";
		Expr transition = new BinaryExpr(new IdExpr("token"), BinaryOp.EQUAL, 
				new IdExpr("TOKEN_OUTPUT_STATE"));
		
		for (String key : idToCondMap.keySet()) {
			TreeMap<String, Integer> conditions = idToCondMap.get(key);
			for (String cond : conditions.keySet()) {
				condStr = cond;
				int occurence = conditions.get(cond) / 2;
				for (int i = 0; i < occurence; i++) {
					if (occurence > 1) {
						condStr = cond + "_" + i;
					}
					for (int j = 0; j < vals.length; j++) {
						lhs = new IdExpr(property + "_" + count++);
						nonMaskedExpr[j] = new IdExpr(condStr + vals[j] + at + key + cov + vals[j]);
						
						leftOperand = new BinaryExpr(nonMaskedExpr[j], BinaryOp.AND, 
												new IdExpr(key + observed));
						
//						if (affectPairs.containsKey(condStr)) {
//							if (affectPairs.get(condStr).contains(key)) {
//								System.out.println("[v] <" + condStr + ", " + key + ">");
//							} else {
//								System.out.println("[-] <" + condStr + ", ***>");
//							}
//						} else {
//							System.out.println("[x] " + condStr);
//						}
						
						if (affectPairs.containsKey(condStr)
								&& affectPairs.get(condStr).contains(key)) {
							affectExpr[j] = new IdExpr(condStr + vals[j] + at + key + affect);
						} else {
							affectExpr[j] = new BoolExpr(true);
						}
						
//						System.out.println("affecting pair:\n\t" + affectExpr[j]);
						
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

package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import coverage.Obligation;
import enums.Coverage;
import enums.TokenState;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

public final class ObservabilityObligation {
	private Map<String, Map<String, Integer>> affectAtCaptureMap;
	private Coverage coverage;
	private Map<String, List<String>> affectPairs = new HashMap<>();
	
	private ObservabilityObligation(Map<String, Map<String, Integer>> affectAtCaptureMap,
									Map<String, List<String>> affectPairs,
									Coverage coverage) {
		this.affectAtCaptureMap = affectAtCaptureMap;
		this.affectPairs = affectPairs;
		this.coverage = coverage;
	}
	
	public static List<Obligation> generate(Map<String, Map<String, Integer>> affectAtCaptureMap,
			Map<String, List<String>> affectPairs,
			Coverage coverage) {
		return new ObservabilityObligation(affectAtCaptureMap, 
				affectPairs, coverage).generate();
	}
	
	private List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", cov = "_" + coverage.name();
		String observed = "_COMB_OBSERVED";
		int count = 0;
		String property = "property";
		String token = "token";
		IdExpr lhs;
		String[] vals = {"_TRUE", "_FALSE"};
		Expr[] nonMaskedExpr = new Expr[2], affectExpr = new Expr[2];
		Expr leftOperand, rightOperand;
		Obligation obligation;
		String condStr = "";
		Expr transition = new BinaryExpr(new IdExpr(token), 
				BinaryOp.EQUAL, new IdExpr(TokenState.TOKEN_OUTPUT_STATE.name()));
		
		for (String key : affectAtCaptureMap.keySet()) {
			Map<String, Integer> conditions = affectAtCaptureMap.get(key);
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

						if (affectPairs.containsKey(condStr)
								&& affectPairs.get(condStr).contains(key)) {
							affectExpr[j] = new IdExpr(condStr + vals[j] + at + key + affect);
						} else {
							affectExpr[j] = new BoolExpr(false);
						}
						
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

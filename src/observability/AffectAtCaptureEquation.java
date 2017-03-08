package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;

import enums.Coverage;
import coverage.Obligation;
import observability.tree.Tree;
import observability.tree.TreeNode;

public final class AffectAtCaptureEquation {
	private Map<String, Expr> map = new HashMap<>();
	private Map<String, List<String>> affectPairs = new HashMap<>();
	
	private Map<String, Tree> delayTrees;
	private Coverage coverage;
	private String cov;
	
	private final String seq = "_SEQ_USED_BY_";
	private final String affect = "_AFFECTING_AT_CAPTURE";
	private final String at = "_AT_";
	private final String[] vals = {"_TRUE", "_FALSE"};
	
	// tree node (root) to token, Map<Node, Token>
	private Map<TreeNode, IdExpr> nodeToToken = new HashMap<>();
	// id to condition & occurrence, Map<id, <condition, occurrence>>
	private Map<String, Map<String, Integer>> coverageTable = new TreeMap<>();
	
	public AffectAtCaptureEquation(Map<String, Tree> delayTrees,
									Map<String, Map<String, Integer>> affectAtCaptureTable,
									Coverage coverage,
									Map<TreeNode, IdExpr> nodeToToken) {
		this.delayTrees = delayTrees;
		this.coverageTable = affectAtCaptureTable;
		this.coverage = coverage;
		this.cov = "_" + coverage.name();
		this.nodeToToken = nodeToToken;
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		
		drawMap(map);
		
		obligations.addAll(getObligations(map));
		
		return obligations;
	}
	
	private List<Obligation> getObligations(Map<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<>();
		for (String lhs : map.keySet()) {
			Obligation obligation = new Obligation(new IdExpr(lhs), true, map.get(lhs));
			obligations.add(obligation);
		}
		
		return obligations;
	}

	private void drawMap(Map<String, Expr> map) {
		String node = "";
		
		String lhs = "";
		String nonMasked = "";
		
		Expr premise;
		Expr conclusion1;
		Expr conclusion2;
		Expr expr;
		
		for (String father : this.coverageTable.keySet()) {
			Map<String, Integer> vars = this.coverageTable.get(father);
			
			for (String var : vars.keySet()) {
				int occurrence = vars.get(var) / 2;
				
				for (int i = 0; i < occurrence; ++i) {
					node = var;
					
					if (occurrence > 1) {
						node = var + "_" + i;
					}
					
					for (int j = 0; j < vals.length; ++j) {
						nonMasked = node + vals[j] + at + father + cov + vals[j];
						lhs = node + vals[j] + at + father + affect;
						
						Expr tokenExpr = null;
						Expr seqUsedExpr = null;
						
						// sequential path
						for (String root : this.delayTrees.keySet()) {
							Tree tree = delayTrees.get(root);
							
							if (!father.equalsIgnoreCase(root) && tree.containsNode(father)) {
								String seqUsedVar = father + seq + root;
								
								IdExpr tokenId = nodeToToken.get(tree.root);
								
								tokenExpr = new BinaryExpr(new IdExpr("token"),
										BinaryOp.EQUAL, tokenId);
								
								if (seqUsedExpr == null) {
									seqUsedExpr = new BinaryExpr(new IdExpr(seqUsedVar),
											BinaryOp.AND, tokenExpr);
								} else {
									seqUsedExpr = new BinaryExpr(seqUsedExpr, BinaryOp.OR,
											new BinaryExpr(new IdExpr(seqUsedVar), BinaryOp.AND, tokenExpr));
								}
								
								
							}
						}
						
						// there is no sequential path
						if (seqUsedExpr == null) {
							seqUsedExpr = new BoolExpr(false);		
						}
						
						premise = new BinaryExpr(new IdExpr(nonMasked),
								BinaryOp.AND, seqUsedExpr);
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs));
						
						expr = new BinaryExpr(premise, BinaryOp.ARROW,
								new BinaryExpr(conclusion1, BinaryOp.OR, conclusion2));
						
						trackAffectPairs(node, father);
						
						map.put(lhs, expr);
					}
				}
			}
		}
	}
	
	public Map<String, List<String>> getAffectPairs() {
		return affectPairs;
	}
	
	private void trackAffectPairs(String affecter, String affectee) {
		List<String> affectingList = new ArrayList<>();
		
		if (! affectPairs.containsKey(affecter)) {
			affectingList.add(affectee);
			affectPairs.put(affecter, affectingList);
		} else {
			affectingList.addAll(affectPairs.get(affecter));
			
			if (!affectingList.contains(affectee)) {
				affectingList.add(affectee);
				affectPairs.put(affecter, affectingList);
			}
		}
	}
}
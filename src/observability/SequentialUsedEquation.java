package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import coverage.Obligation;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import observability.tree.Tree;
import observability.tree.TreeNode;

public class SequentialUsedEquation {
	private Map<String, Tree> delayTrees;
	private Map<String, Expr> exprsMap = new HashMap<>();
	private final String seqUsedBy = "_SEQ_USED_BY_";
	private final String combUsedBy = "_COMB_USED_BY_";
	
	public SequentialUsedEquation(Map<String, Tree> delayTrees) {
		this.delayTrees = delayTrees;
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<Obligation>();
		Tree tree;
		
		for (String root: delayTrees.keySet()) {
			tree = delayTrees.get(root);
			generateObligationForTree(exprsMap, tree);
		}
		
		obligations.addAll(getObligations(exprsMap));
		return obligations;
	}

	
	private List<Obligation> getObligations(Map<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<Obligation>();
		
		for (String lhs : map.keySet()) {
			obligations.add(new Obligation(new IdExpr(lhs), true, map.get(lhs)));
		}
		
		return obligations;
	}
	
	private void generateObligationForTree(Map<String, Expr> exprsMap, 
												Tree tree) {
		TreeNode root = tree.root;
		List<TreeNode> firstLevel = root.children;
		String lhs, rhs;
		
		for (TreeNode node : firstLevel) {
			lhs = node.rawId + seqUsedBy + root.rawId;
			rhs = node.rawId + combUsedBy + root.rawId;
			exprsMap.put(lhs, new IdExpr(rhs));
			
			for (TreeNode child : node.children) {
				generateObligation(exprsMap, child, root);
			}
		}
	}
	
	private void generateObligation(Map<String, Expr> exprsMap,
									TreeNode node,
									TreeNode root) {
		if (node == null) {
			return;
		}
		
		String lhs;
		
		lhs = node.rawId + seqUsedBy + root.rawId;
		IdExpr opr1 = new IdExpr(node.rawId + combUsedBy + node.parent.rawId);
		IdExpr opr2 = new IdExpr(node.parent.rawId + seqUsedBy + root.rawId);
		BinaryExpr expr = new BinaryExpr(opr1, BinaryOp.AND, opr2);
		
		if (!exprsMap.containsKey(lhs)) {
			exprsMap.put(lhs, expr);
		} else if (!exprsMap.get(lhs).toString().contains(expr.toString())) {
			expr = new BinaryExpr(expr, BinaryOp.OR, exprsMap.get(lhs));
			exprsMap.put(lhs, expr);
		}
		
		for (TreeNode child : node.children) {
			generateObligation(exprsMap, child, root);
		}
	}

}

package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import coverage.Obligation;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import observability.tree.Tree;
import observability.tree.TreeNode;

public class CombObservedEquation {
	List<String> deadNodes;
	Map<String, Expr> map = new HashMap<>();
	Map<String, Map<String, Map<String, Integer>>> observerTable = new HashMap<>();
	Map<String, Tree> observerTrees;
	
	public CombObservedEquation(Map<String, Tree> observerTrees,
								List<String> deadNodes) {
		this.observerTrees = observerTrees;
		this.deadNodes = deadNodes;
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		Tree tree;
		
		for (String rootStr : observerTrees.keySet()) {
			tree = observerTrees.get(rootStr);
			if (tree.root.children.isEmpty()) {
				// single-node
				genereateForSingleNodes(map, tree.root);
			} else {
				// for nodes in observer trees
				generateForTree(map, tree.root);
				
				// for dead nodes
				genereateForSingleNodes(map, tree.root);
			}
		}
		obligations.addAll(getObligations(map));
		
		return obligations;
	}
		
	private List<Obligation> getObligations(Map<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<>();
		
		for (String lhs : map.keySet()) {
			Obligation obligation = new Obligation(new IdExpr(lhs), true,
											map.get(lhs));
			obligations.add(obligation);
		}
		return obligations;
	}

	private void genereateForSingleNodes(Map<String, Expr> map,
										TreeNode root) {
		String combObs = "_COMB_OBSERVED";
		String lhs;
		
		for (String node : deadNodes) {
			lhs = node + combObs;

			if (node.equals(root.rawId)) {
				map.put(lhs, new BoolExpr(true));
			} else {
				map.put(lhs, new BoolExpr(false));
			}
		}
	}
	
	private void generateForTree(Map<String, Expr> map, TreeNode root) {
		String combObs = "_COMB_OBSERVED";
		String lhs = root.rawId + combObs;
		// COMB_OBSERVED for root
		map.put(lhs, new BoolExpr(true));
		
		for (TreeNode node : root.children) {
			generateForNode(map, node);
		}
	}
	
	private void generateForNode(Map<String, Expr> map, TreeNode node) {
		if (node == null) {
			return;
		}
		
		String lhs;
		String combObs = "_COMB_OBSERVED";
		String combUsedBy = "_COMB_USED_BY_";
		lhs = node.rawId + combObs;
		IdExpr opr1 = new IdExpr(node.rawId + combUsedBy + node.parent.rawId);
		IdExpr opr2 = new IdExpr(node.parent.rawId + combObs);
		BinaryExpr expr = new BinaryExpr(opr1, BinaryOp.AND, opr2);
		
		if (!map.containsKey(lhs)) {
			map.put(lhs, expr);
		} else if (!map.get(lhs).toString().contains(expr.toString())) {
			expr = new BinaryExpr(expr, BinaryOp.OR, map.get(lhs));
			map.put(lhs, expr);
		}
		
		for (TreeNode child : node.children) {
			generateForNode(map, child);
		}
	}
}

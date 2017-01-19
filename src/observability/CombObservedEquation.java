package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import coverage.Obligation;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import observability.tree.Tree;
import observability.tree.TreeNode;

public final class CombObservedEquation {
	private final String combObs = "_COMB_OBSERVED";
	private final String combUsedBy = "_COMB_USED_BY_";
	private Map<String, Expr> combObservedMap = new HashMap<>();
	
	private final List<String> deadNodes;
	private final Map<String, Tree> observerTrees;
	private final Map<String, Tree> delayTrees;
	private final Set<String> nodecalls;
	
	private CombObservedEquation(Map<String, Tree> observerTrees,
								Map<String, Tree> delayTrees,
								List<String> deadNodes,
								Set<String> nodecalls) {
		this.observerTrees = observerTrees;
		this.delayTrees = delayTrees;
		this.deadNodes = deadNodes;
		this.nodecalls = nodecalls;
	}
	
	public static List<Obligation> generate(Map<String, Tree> observerTrees,
			Map<String, Tree> delayTrees,
			List<String> deadNodes, Set<String> nodecalls) {
		return new CombObservedEquation(observerTrees, 
				delayTrees, deadNodes, nodecalls).generate();
	}
	
	private List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		Tree tree;
		
		for (String rootStr : observerTrees.keySet()) {
			tree = observerTrees.get(rootStr);
			
			generateForTree(combObservedMap, tree.root);
		}
		
		for (String rootStr : delayTrees.keySet()) {
			List<String> nodeStr = new ArrayList<>();
			
			for (TreeNode node : delayTrees.get(rootStr).convertToList()) {
				if (nodecalls.contains(node.rawId)) {
					continue;
				}
				nodeStr.add(node.rawId);
			}
			
			genereateForSingleNodes(combObservedMap, nodeStr);
		}
		
		// for dead nodes
		if (! deadNodes.isEmpty()) {
			genereateForSingleNodes(combObservedMap, deadNodes);
		}
		
		obligations.addAll(getObligations(combObservedMap));
		
		return obligations;
	}
		
	private List<Obligation> getObligations(Map<String, Expr> combObservedMap) {
		List<Obligation> obligations = new ArrayList<>();
		
		for (String lhs : combObservedMap.keySet()) {
			Obligation obligation = new Obligation(new IdExpr(lhs), true,
											combObservedMap.get(lhs));
			obligations.add(obligation);
		}
		return obligations;
	}

	private void genereateForSingleNodes(Map<String, Expr> map,
										List<String> nonObservedNodes) {
		for (String node : nonObservedNodes) {
			if (nodecalls.contains(node)) {
				continue;
			}
			String lhs = node + combObs;
			if (map.containsKey(lhs)) {
				continue;
			}
			map.put(lhs, new BoolExpr(false));
		}
	}
	
	private void generateForTree(Map<String, Expr> map, TreeNode root) {
		String lhs = root.rawId + combObs;
		// COMB_OBSERVED for root
		map.put(lhs, new BoolExpr(true));
		
		for (TreeNode node : root.children) {
			generateForNode(map, node);
		}
	}
	
	private void generateForNode(Map<String, Expr> map, TreeNode node) {
		if (node == null || nodecalls.contains(node.rawId)) {
			return;
		}
		
		String lhs = node.rawId + combObs;
				
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

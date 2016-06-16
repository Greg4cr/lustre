package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.VarDecl;

public class CombObservedEquation {
	List<VarDecl> singleNodeList;
	HashMap<String, Expr> map = new HashMap<>();
	
	public List<Obligation> generate(HashMap<VarDecl, ObservedTree> referenceTrees,
											List<VarDecl> idList) {
		List<Obligation> obligations = new ArrayList<>();
		ObservedTree tree;
		
		for (VarDecl rootVar: referenceTrees.keySet()) {
//			System.out.println("Generate comb observed expressions for [" + root + "]...");
			tree = referenceTrees.get(rootVar);
//			System.out.println("single node tree? " + tree.root.getChildren().isEmpty());
			if (tree.root.getChildren().isEmpty()) {
				// for single-node tree
				genereateForSingleNodes(map, tree.root);
			} else {
				// for nodes in reference trees
				generateForTree(map, tree.root);
				
				// for single nodes
				genereateForSingleNodes(map, tree.root);
			}
		}
		obligations.addAll(getObligations(map));
		
		return obligations;
	}
	
	private List<Obligation> getObligations(HashMap<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<>();
		
		for (String lhs : map.keySet()) {
			Obligation obligation = new Obligation(new IdExpr(lhs), true,
											map.get(lhs));
			obligations.add(obligation);
		}
		return obligations;
	}
	
	public void setSingleNodeList(List<VarDecl> singleNodeList) {
		this.singleNodeList = singleNodeList;
	}

	private void genereateForSingleNodes(HashMap<String, Expr> map,
										ObservedTreeNode root) {
		String combObs = "_COMB_OBSERVED";
		String lhs;
		
		for (VarDecl id : singleNodeList) {
			lhs = id.id + combObs;

			if (id.id.equals(root.data)) {
				map.put(lhs, new BoolExpr(true));
			} else {
				map.put(lhs, new BoolExpr(false));
			}
		}
	}
	
	private void generateForTree(HashMap<String, Expr> map, ObservedTreeNode root) {
		String combObs = "_COMB_OBSERVED";
		String lhs = root.data + combObs;
		// COMB_OBSERVED for root
		map.put(lhs, new BoolExpr(true));
		
		for (ObservedTreeNode node : root.getChildren()) {
			generateForNode(map, node);
		}
	}
	
	private void generateForNode(HashMap<String, Expr> map, ObservedTreeNode node) {
		if (node == null) {
			return;
		}
		
		String lhs;
		String combObs = "_COMB_OBSERVED";
		String combUsedBy = "_COMB_USED_BY_";
		lhs = node.data + combObs;
		IdExpr opr1 = new IdExpr(node.data + combUsedBy + node.parent.data);
		IdExpr opr2 = new IdExpr(node.parent.data + combObs);
		BinaryExpr expr = new BinaryExpr(opr1, BinaryOp.AND, opr2);
		
		if (!map.containsKey(lhs)) {
			map.put(lhs, expr);
		} else if (!map.get(lhs).toString().contains(expr.toString())) {
			expr = new BinaryExpr(expr, BinaryOp.OR, map.get(lhs));
			map.put(lhs, expr);
		}
		
		for (ObservedTreeNode child : node.getChildren()) {
			generateForNode(map, child);
		}
	}
}

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

public class SequentialEquation {
	HashMap<String, Expr> exprsMap = new HashMap<>();
	
	public List<Obligation> generate(HashMap<VarDecl, ObservedTree> sequantialTrees) {
		List<Obligation> obligations = new ArrayList<Obligation>();
		ObservedTree tree;
		
		for (VarDecl root: sequantialTrees.keySet()) {
			System.out.println("Generate delay dependency epxressions for [" + root + "]...");
			tree = sequantialTrees.get(root);
			generateObligationForTree(exprsMap, tree);
		}
		
		obligations.addAll(getObligations(exprsMap));
		return obligations;
	}

	
	private List<Obligation> getObligations(HashMap<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<Obligation>();
		
		for (String lhs : map.keySet()) {
			obligations.add(new Obligation(new IdExpr(lhs), true, map.get(lhs)));
		}
		
		return obligations;
	}
	
	private void generateObligationForTree(HashMap<String, Expr> exprsMap, 
												ObservedTree tree) {
		ObservedTreeNode root = tree.root;
		List<ObservedTreeNode> firstLevel = root.children;
		String seqUsedBy = "_SEQ_USED_BY_";
		String lhs;
		
		for (ObservedTreeNode node : firstLevel) {
			lhs = node.data + seqUsedBy + root.data;
			exprsMap.put(lhs, new BoolExpr(true));
			
			for (ObservedTreeNode child : node.children) {
				generateObligation(exprsMap, child, root);
			}
		}
	}
	
	private void generateObligation(HashMap<String, Expr> exprsMap,
									ObservedTreeNode node,
									ObservedTreeNode root) {
		if (node == null) {
			return;
		}
		
		String lhs;
		String seqUsedBy = "_SEQ_USED_BY_";
		String combUsedBy = "_COMB_USED_BY_";
		
		lhs = node.data + seqUsedBy + root.data;
		IdExpr opr1 = new IdExpr(node.data + combUsedBy + node.parent.data);
		IdExpr opr2 = new IdExpr(node.parent.data + seqUsedBy + root.data);
		BinaryExpr expr = new BinaryExpr(opr1, BinaryOp.AND, opr2);
		
		if (!exprsMap.containsKey(lhs)) {
			exprsMap.put(lhs, expr);
		} else if (!exprsMap.get(lhs).toString().contains(expr.toString())) {
//			System.out.println(">>>>>>>> " + exprsMap.get(lhs).toString());
//			System.out.println(">>>>>>>>>>>>>>> " + expr.toString());
			expr = new BinaryExpr(expr, BinaryOp.OR, exprsMap.get(lhs));
			exprsMap.put(lhs, expr);
		}
		
//		if (node.getChildren() != null) {
			for (ObservedTreeNode child : node.children) {
				generateObligation(exprsMap, child, root);
			}
//		}
	}

}

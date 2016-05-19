package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.IdExpr;
import jkind.lustre.VarDecl;

public class CombObservedEquation {
	List<VarDecl> singleNodeList;
	
	public List<Obligation> generate(HashMap<VarDecl, ObservedTree> referenceTrees,
											List<VarDecl> idList) {
		List<Obligation> obligations = new ArrayList<>();
		ObservedTree tree;
		
		for (VarDecl root: referenceTrees.keySet()) {
//			System.out.println("Generate comb observed expressions for [" + root + "]...");
			tree = referenceTrees.get(root);
//			this.singleNodeList = getSingleNodeList(tree, idList);
//			System.out.println("single node tree? " + tree.root.getChildren().isEmpty());
			if (tree.root.getChildren().isEmpty()) {
				// for single-node tree
				obligations.addAll(generateExprForSingleNodeTree(tree));
			} else {
				// for nodes in reference trees
				obligations.addAll(gerenateExprForTree(tree));
				
				// for single nodes
				obligations.addAll(generateExprForSingleNodeTree(tree));
			}
		}
		return obligations;
	}
	
	public void setSingleNodeList(List<VarDecl> singleNodeList) {
		this.singleNodeList = singleNodeList;
	}

	private List<Obligation> generateExprForSingleNodeTree(ObservedTree tree) {
		List<Obligation> obligations = new ArrayList<>();
		String combObs = "_COMB_OBSERVED";
		IdExpr lhs;
		
		Obligation obligation;
		for (VarDecl id : singleNodeList) {
			lhs = new IdExpr(id.id + combObs);
			if (id.id.equals(tree.root.data)) {
				obligation  = new Obligation(lhs, false, new BoolExpr(true));
			} else {
				obligation = new Obligation(lhs, false, new BoolExpr(false));
			}
			obligations.add(obligation);
		}
		return obligations;
	}
	
	private List<Obligation> gerenateExprForTree(ObservedTree tree) {
		List<Obligation> obligationForTree = new ArrayList<Obligation>();
		String combObs = "_COMB_OBSERVED";
		IdExpr lhs;
		lhs = new IdExpr(tree.root.data + combObs);
		// COMB_OBSERVED obligation for root
		Obligation obligation = new Obligation(lhs, false, new BoolExpr(true));
//		System.out.println(obligation.condition + " = " + obligation);
		obligationForTree.add(obligation);
		
		for (ObservedTreeNode node : tree.root.getChildren()) {
			obligationForTree.addAll(genereateExprForNode(node));
		}
		
		return obligationForTree;
	}
	
	private List<Obligation> genereateExprForNode(ObservedTreeNode node) {
		List<Obligation> obligationOfNode = new ArrayList<Obligation>();
		if (node == null) {
			return null;
		}
		
		IdExpr lhs;
		Obligation obligation;
		String combObs = "_COMB_OBSERVED";
		String combUsedBy = "_COMB_USED_BY_";
		lhs = new IdExpr(node.data + combObs);
		IdExpr opr1 = new IdExpr(node.data + combUsedBy + node.getParent().data);
		IdExpr opr2 = new IdExpr(node.getParent().data + combObs);
		BinaryExpr expr = new BinaryExpr(opr1, BinaryOp.AND, opr2);
		obligation = new Obligation(lhs, false, expr);
		obligationOfNode.add(obligation);
//		System.out.println(obligation.condition + " = " + obligation);
		
		if (node.getChildren() != null) {
			for (ObservedTreeNode child : node.getChildren()) {
				obligationOfNode.addAll(genereateExprForNode(child));
			}
		}
		return obligationOfNode;
	}
}

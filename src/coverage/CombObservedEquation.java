package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.VarDecl;

public class CombObservedEquation {

	public List<Obligation> generate(HashMap<Node, HashMap<VarDecl, ObservedTree>> referenceTrees) {
		List<Obligation> obligations = new ArrayList<Obligation>();
		HashMap<VarDecl, ObservedTree> seqTreesOfNode;
		ObservedTree tree;
		
		for (Node node : referenceTrees.keySet()) {
			// generate obligation equations node by node
			seqTreesOfNode = referenceTrees.get(node);
			for (VarDecl root: seqTreesOfNode.keySet()) {
				System.out.println("Generate comb observed epxressions for [" + root + "]...");
				tree = seqTreesOfNode.get(root);
				obligations.addAll(gerenateExprForTree(tree));
			}
		}
		return obligations;
	}
	
	private List<Obligation> gerenateExprForTree(ObservedTree tree) {
		List<Obligation> obligationForTree = new ArrayList<Obligation>();
		ObservedTreeNode root = tree.getroot();
		String combObs = "_COMB_OBSERVED";
		IdExpr lhs;
		lhs = new IdExpr(root.data + combObs);
		Obligation obligation = new Obligation(lhs, false, new BoolExpr(true));
		System.out.println(obligation.condition + " = " + obligation);
		obligationForTree.add(obligation);
		
		for (ObservedTreeNode node : root.getChildren()) {
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
		IdExpr opr1 = new IdExpr(node.data + combUsedBy + node.parent.data);
		IdExpr opr2 = new IdExpr(node.parent.data + combObs);
		BinaryExpr expr = new BinaryExpr(opr1, BinaryOp.AND, opr2);
		obligation = new Obligation(lhs, false, expr);
		obligationOfNode.add(obligation);
		System.out.println(obligation.condition + " = " + obligation);
		
		if (node.children != null) {
			for (ObservedTreeNode child : node.children) {
				obligationOfNode.addAll(genereateExprForNode(child));
			}
		}
		return obligationOfNode;
	}
}

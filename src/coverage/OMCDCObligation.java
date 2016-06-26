package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class OMCDCObligation {
	HashMap<VarDecl, ObservedTree> sequentialTrees;
	HashMap<VarDecl, ObservedTree> combUsedByTrees;
		
	public OMCDCObligation(HashMap<VarDecl, ObservedTree> seqTrees,
			HashMap<VarDecl, ObservedTree> combUsedTrees) {
		this.sequentialTrees = seqTrees;
		this.combUsedByTrees = combUsedTrees;
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		List<List<ObservedTreeNode>> paths = new ArrayList<>();
		paths.addAll(drawPaths(sequentialTrees));
		paths.addAll(drawPaths(combUsedByTrees));
		
		IdExpr token = new IdExpr("token");
		IdExpr outState = new IdExpr("TOKEN_OUTPUT_STATE");
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", cov = "_MCDC";
		String observed = "_COMB_OBSERVED";
		int count = 0;
		String omcdc = "omcdc_";
		ObservedTreeNode node, parent;
		String nodeName;
//		String node, parent;
		IdExpr lhs;
		IdExpr[] nonMaskedExpr = new IdExpr[2], affectExpr = new IdExpr[2];
		Expr lOperand, rOperand;
		Obligation obligation;
//		StringBuilder allObs = new StringBuilder();
		String allObs = "";
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			
			for (int j = 1; j < path.size(); j++) {
				int occurrence = path.get(j).occurrence;
				
				for (int l = 0; l < occurrence; l++) {
					if (occurrence == 1) {
						node = path.get(j);
						nodeName = node.data;
					} else {
						node = path.get(j);
						nodeName = node.data + "_" + l;
					}
					
					parent = path.get(j - 1);
					
					if (!("int".equals(node.type.toString()))) {
						nonMaskedExpr[0] = new IdExpr(nodeName + t + at + parent.data + cov + t);
						nonMaskedExpr[1] = new IdExpr(nodeName + f + at + parent.data + cov + f);
						affectExpr[0] = new IdExpr(nodeName + t + at + parent.data + affect);
						affectExpr[1] = new IdExpr(nodeName + f + at + parent.data + affect);
						for (int k = 0; k < nonMaskedExpr.length; k++) {
							lOperand = new BinaryExpr(nonMaskedExpr[k], BinaryOp.AND, 
												new IdExpr(parent.data + observed));
							rOperand = new BinaryExpr(affectExpr[k], BinaryOp.AND,
												new BinaryExpr(token, BinaryOp.EQUAL, outState));
							Expr condition = new UnaryExpr(UnaryOp.NOT, (new BinaryExpr(
									lOperand, BinaryOp.OR, rOperand)));
							
							if (!allObs.contains(condition.toString())) {
								lhs = new IdExpr(omcdc + (count++));
								obligation = new Obligation(lhs, true, condition);
								obligations.add(obligation);
							}
	//						allObs.append(obligation.condition).append(", ");
							allObs += condition + ", ";
						}
					} else {
						continue;
					}
				}
			}
		}
		
		return obligations;
	}
	
	private List<List<ObservedTreeNode>> drawPaths(HashMap<VarDecl, ObservedTree> trees) {
		List<List<ObservedTreeNode>> paths = new ArrayList<>();
		
		for (VarDecl node : trees.keySet()) {
			ObservedTreeNode root = trees.get(node).root;
			root.getPaths(paths);
		}
						
		System.out.println("Number of paths: " + paths.size());
		for (int i = 0; i < paths.size(); i++) {
			System.out.println(paths.get(i) + ", len = " + paths.get(i).size());
		}
		
		return paths;
	}

}

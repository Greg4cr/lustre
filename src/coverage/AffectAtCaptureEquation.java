package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class AffectAtCaptureEquation {
	HashMap<String, Expr> map = new HashMap<>();
	
	HashMap<VarDecl, ObservedTree> sequentialTrees;
	HashMap<VarDecl, ObservedTree> combUsedByTrees;
	HashMap<String, List<String>> delayMap = new HashMap<>();
	List<VarDecl> idList;
	List<VarDecl> singleNodeList;
	HashMap<VarDecl, ObservedTreeNode> singleNodeTrees = new HashMap<>();
	
	List<List<String>> leavesToRoots = new ArrayList<>();
	List<List<String>> rootToLeaves = new ArrayList<>();
	
	final String TYPE_SEQ = "SEQ";
	final String TYPE_COMB = "COMB";
	
	// dynamic tokens
	String prefix = "TOKEN_D";
	IdExpr[] tokens;
	int count;
	
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	HashMap<ObservedTreeNode, List<ObservedTreeNode>> rootToLeavesMap = new HashMap<>();
	// token to tree node (root), Map<Token, Node>
	HashMap<IdExpr, String> tokenToNode = new HashMap<IdExpr, String>();
	// tree node (root) to token, Map<Node, Token>
	HashMap<String, IdExpr> nodeToToken = new HashMap<String, IdExpr>();
	
	public AffectAtCaptureEquation(HashMap<VarDecl, ObservedTree> seqTrees,
									HashMap<VarDecl, ObservedTree> combUsedTrees,
									HashMap<String, List<String>> delayMap) {
		this.sequentialTrees = seqTrees;
		this.combUsedByTrees = combUsedTrees;
		this.delayMap = delayMap;
		populateMaps();
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		generateForSeqDepTrees(map);
		generateForCombTrees(map);
		generateForSingleNodes(map);
		obligations.addAll(getObligations(map));
		return obligations;
	}
	
	private List<Obligation> getObligations(HashMap<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<>();
		for (String lhs : map.keySet()) {
			Obligation obligation = new Obligation(new IdExpr(lhs), true, map.get(lhs));
			obligations.add(obligation);
		}
		
		return obligations;
	}
	
	public void setSingleNodeList(List<VarDecl> singleNodeList) {
		if (singleNodeList == null || singleNodeList.isEmpty()) {
			this.singleNodeList = new ArrayList<VarDecl>();
		} else {
			this.singleNodeList = singleNodeList;
		}
	}
	
	public void setSingleNodeTrees(HashMap<VarDecl, ObservedTreeNode> trees) {
		if (trees == null || trees.keySet().size() == 0) {
			this.singleNodeTrees = new HashMap<VarDecl, ObservedTreeNode>();
		} else {
			this.singleNodeTrees = trees;
		}
	}
	
	private ObservedTreeNode getSuperRoot(ObservedTreeNode node, 
											List<List<ObservedTreeNode>> paths) {
		ObservedTreeNode supRoot = null;
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			int len = path.size();
			
			if (node.data.equals(path.get(len - 1).data)) {
				supRoot = path.get(0);
			}
		}
		
		return supRoot;
	}
	
	private void generateForSingleNodes(HashMap<String, Expr> map) {
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", c = "_MCDC";
		String node = "", father;
		String[] lhs = new String[2];
		IdExpr[] nonMasked = new IdExpr[2];
		Expr premise, conclusion1, conclusion2;
		
		if (this.singleNodeTrees.keySet() == null) {
			return;
		}
		
		for (VarDecl exprId : this.singleNodeTrees.keySet()) {
			ObservedTreeNode root = this.singleNodeTrees.get(exprId);
			List<ObservedTreeNode> children = root.getChildren();
			for (ObservedTreeNode child : children) {
				if ("int".equals(child.type.toString())) {
					node = "ArithExpr";
				} else {
					node = child.data;
				}
				
				father = root.data;
				
				for (int i = 0; i < child.occurrence; i++) {
					if (child.occurrence > 1) {
						node += "_" + i;
					}
					lhs[0] = node + t + at + father + affect;
					lhs[1] = node + f + at + father + affect;
					nonMasked[0] = new IdExpr(node + t + at + father + c + t);
					nonMasked[1] = new IdExpr(node + f + at + father + c + f);
					for (int k = 0; k < lhs.length; k++) {
						premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, new BoolExpr(false));
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs[k]));
						
						Expr expr = new BinaryExpr(premise, BinaryOp.ARROW, 
													new BinaryExpr(conclusion1, 
															BinaryOp.OR, conclusion2));
						if (!map.containsKey(lhs[k])) {
							map.put(lhs[k], expr);
						} else if (!map.get(lhs[k]).toString().contains(expr.toString())) {
							expr = new BinaryExpr(expr, BinaryOp.OR, map.get(lhs[k]));
							map.put(lhs[k], expr);
						}
					}
				}
			}
		}
	}
	
	private void generateForCombTrees(HashMap<String, Expr> map) {
		List<List<ObservedTreeNode>> paths = drawPaths(combUsedByTrees, TYPE_COMB);
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", c = "_MCDC";
		String node = "", father;
		String[] lhs = new String[2];
		IdExpr[] nonMasked = new IdExpr[2];
		Expr premise, conclusion1, conclusion2;
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			
			for (int j = path.size() - 1; j > 0; j--) {
				int occurence = path.get(j).occurrence;
				
				if (delayMap.containsKey(path.get(j).data)) {
					continue;
				}
				if ("int".equals(path.get(j - 1).type.toString())) {
					continue;
				}
				
				for (int l = 0; l < occurence; l++) {
					if ("int".equals(path.get(j).type.toString())) {
						node = "ArithExpr_" + l;
					} else {
						node = path.get(j).data;
					}
					
					if (occurence > 1) {
						node = node + "_" + l;
					}
					
					father = path.get(j - 1).data;
					
					lhs[0] = node + t + at + father + affect;
					lhs[1] = node + f + at + father + affect;
					nonMasked[0] = new IdExpr(node + t + at + father + c + t);
					nonMasked[1] = new IdExpr(node + f + at + father + c + f);
					for (int k = 0; k < lhs.length; k++) {
						premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, 
												new BoolExpr(false));
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs[k]));
						
						Expr expr = new BinaryExpr(premise, BinaryOp.ARROW, 
													new BinaryExpr(conclusion1, 
															BinaryOp.OR, conclusion2));
						if (!map.containsKey(lhs[k])) {
							map.put(lhs[k], expr);
						} else if (!map.get(lhs[k]).toString().contains(expr.toString())) {
							expr = new BinaryExpr(expr, BinaryOp.OR, map.get(lhs[k]));
							map.put(lhs[k], expr);
						}
					}
				}
			}
		}
	}
	
	/*private <T> boolean isInList(List<T> list, Object obj) {
		boolean inList = false;
		if (list == null) {
			return inList;
		}
		
		for (int i = 0; i < list.size(); i++) {
			if (list.get(i).toString().equals(obj.toString())) {
				inList = true;
				break;
			}
		}
		
		return inList;
	}*/
	
	private void generateForSeqDepTrees(HashMap<String, Expr> map) {
		List<List<ObservedTreeNode>> paths = drawPaths(sequentialTrees, TYPE_SEQ);
		IdExpr token = new IdExpr("token");
		String seq = "_SEQ_USED_BY_";
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", c = "_MCDC";
		ObservedTreeNode node, father, root, supRoot;
		IdExpr seqUsed, rootToken;
		String[] lhs = new String[2];
		IdExpr[] nonMasked = new IdExpr[2];
		Expr premise, conclusion1, conclusion2;
		
		for (int i = 0; i < paths.size(); i++) {
			// get find superToken if there is any
			List<ObservedTreeNode> path = paths.get(i);
			System.out.println("::: path :::\n\t" + path);
			root = path.get(0);
			// get superRoot and superToken if there is any
			// cases:	root -> nodeA -> nodeB -> ...
			// 			superRoot -> ... -> root
			supRoot = getSuperRoot(root, paths);
			
			for (int index = path.size() - 1; index > 0; index--) {
				int occurence = path.get(index).occurrence;
				for (int j = 0; j < occurence; j++) {
					if ("int".equals(path.get(index).type.toString())) {
						node = new ObservedTreeNode("ArithExpr_" + index, path.get(index).type);
						node.occurrence = path.get(index).occurrence;
					} else {
						node = path.get(index);
					}
					
					if (occurence > 1) {
						node.data = node.data + "_" + j;
					}
					
					father = path.get(index - 1);
					lhs[0] = node.data + t + at + father.data + affect;
					lhs[1] = node.data + f + at + father.data + affect;
					nonMasked[0] = new IdExpr(node.data + t + at + father.data + c + t);
					nonMasked[1] = new IdExpr(node.data + f + at + father.data + c + f);
					
					for (int k = 0; k < lhs.length; k++) {
						
						if (path.size() <= 2) {
							premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, new BoolExpr(false));
						} else {
							if (supRoot != null && father.data.equals(root.data)) {
								seqUsed = new IdExpr(father.data + seq + supRoot.data);
								rootToken = nodeToToken.get(supRoot.data);
							} else {
								seqUsed = new IdExpr(father.data + seq + root.data);
								rootToken = nodeToToken.get(root.data);
							}
							
							premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, 
												new BinaryExpr(seqUsed, BinaryOp.AND, 
														new BinaryExpr(token, BinaryOp.EQUAL, rootToken)));
						}
						
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs[k]));
						
						Expr expr = new BinaryExpr(premise, BinaryOp.ARROW, 
													new BinaryExpr(conclusion1, 
															BinaryOp.OR, conclusion2));
						if (!map.containsKey(lhs[k])) {
							map.put(lhs[k], expr);
						} else if (!map.get(lhs[k]).toString().contains(expr.toString())) {
							expr = new BinaryExpr(expr, BinaryOp.OR, map.get(lhs[k]));
							map.put(lhs[k], expr);
						}
					}					
				}
			}
			
		}
	}
	
	private List<List<ObservedTreeNode>> drawPaths(HashMap<VarDecl, ObservedTree> trees, 
													String type) {
		List<List<ObservedTreeNode>> paths = new ArrayList<>();
		
		for (VarDecl node : trees.keySet()) {
			ObservedTreeNode root = trees.get(node).root;
			root.getPaths(paths);
		}
		
		if (type.equals(TYPE_COMB)) {
			for (int i = 0; i < paths.size(); i++) {
				int len = paths.get(i).size();
				
				ObservedTreeNode leaf = paths.get(i).get(len -1);
				if (leaf.isPre) {
					// remove leaf in a path only if it's in form of "pre leaf"
					paths.get(i).remove(len - 1);
					if (i > 0 && (paths.get(i).equals(paths.get(i - 1)))) {
						paths.remove(i);
					}
				} else {
					continue;
				}
			}
		}
		
		/*System.out.println("Number of paths: " + paths.size());
		
		for (int i = 0; i < paths.size(); i++) {
			System.out.println(paths.get(i) + ", " + paths.get(i).size());
		}*/
		
		return paths;
	}
		
	// build token-to-node, node-to-token, tokennode-dependency maps
	private void populateMaps() {
		tokens = new IdExpr[sequentialTrees.size()];
		count = 0;
		
		System.out.println("============= drawing maps =============");
		for (VarDecl treeRoot : sequentialTrees.keySet()) {
			tokens[count] = new IdExpr(prefix + (count + 1));
			tokenToNode.put(tokens[count], treeRoot.id);
			nodeToToken.put(treeRoot.id, tokens[count]);
			
			ObservedTreeNode root = sequentialTrees.get(treeRoot).root;
			rootToLeavesMap.put(root, root.getAllLeafNodes());
			
			System.out.println(count + " token-to-node: " + tokens[count] + " - " + tokenToNode.get(tokens[count]));
			System.out.println(count + " node-to-token: " + treeRoot.id + " - " + nodeToToken.get(treeRoot.id));
			System.out.println(count + " dependency: " + treeRoot.id + " >>> " + rootToLeavesMap.get(treeRoot.id));
			count++;
		}
	}
}

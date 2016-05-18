package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Node;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class AffectAtCaptureEquation {
	HashMap<VarDecl, ObservedTree> sequentialTrees;
	HashMap<VarDecl, ObservedTree> combUsedByTrees;
	
	List<List<String>> leavesToRoots = new ArrayList<>();
	List<List<String>> rootToLeaves = new ArrayList<>();
	
	final String TYPE_SEQ = "SEQ";
	final String TYPE_COMB = "COMB";
	
	// dynamic tokens
	String prefix = "TOKEN_D";
	IdExpr[] tokens;
	int count;
	
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
//	HashMap<String, List<String>> rootToLeavesMap = new HashMap<String, List<String>>();
	HashMap<ObservedTreeNode, List<ObservedTreeNode>> rootToLeavesMap = new HashMap<>();
	// token to tree node (root), Map<Token, Node>
	HashMap<IdExpr, String> tokenToNode = new HashMap<IdExpr, String>();
	// tree node (root) to token, Map<Node, Token>
	HashMap<String, IdExpr> nodeToToken = new HashMap<String, IdExpr>();
	
	public AffectAtCaptureEquation(HashMap<VarDecl, ObservedTree> seqTrees,
									HashMap<VarDecl, ObservedTree> combUsedTrees) {
		this.sequentialTrees = seqTrees;
		this.combUsedByTrees = combUsedTrees;
		drawMaps();
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		obligations.addAll(generateForSeqTrees());
		obligations.addAll(genergateForCombTrees());
		obligations.addAll(generateForLoops());
		return obligations;
	}
	
	private List<Obligation> generateForLoops() {
		List<Obligation> obligations = new ArrayList<>();
		List<List<ObservedTreeNode>> paths = drawPaths(sequentialTrees, TYPE_SEQ);
		IdExpr token = new IdExpr("token");
		String seq = "_SEQ_USED_BY_";
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", c = "_MCDC";
		ObservedTreeNode node, leaf, root;
		IdExpr seqUsed, rToken;
		IdExpr[] lhs = new IdExpr[2];
		IdExpr[] nonMasked = new IdExpr[2];
		Expr premise, conclusion1, conclusion2;
		Obligation obligation;
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			root = path.get(0);
			leaf = path.get(path.size() - 1);
			boolean isInLoop = isInLoop(root, leaf, paths);
			if (isInLoop) {
				// found a loop (hiding in two trees)
				// generate obligations for the loop
				for (int j = 1; j < path.size() - 1; j++) {
					node = path.get(j);
					
					lhs[0] = new IdExpr(node.data + t + at + root.data + affect);
					lhs[1] = new IdExpr(node.data + f + at + root.data + affect);
					nonMasked[0] = new IdExpr(node.data + t + at + root.data + c + t);
					nonMasked[1] = new IdExpr(node.data + f + at + root.data + c + f);
					for (int k = 0; k < lhs.length; k++) {
						seqUsed = new IdExpr(root.data + seq + leaf.data);
						rToken = nodeToToken.get(leaf.data);
						
						premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, 
											new BinaryExpr(seqUsed, BinaryOp.AND, 
													new BinaryExpr(token, BinaryOp.EQUAL, rToken)));
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, lhs[k]);
						
						obligation = new Obligation(lhs[k], true, 
											new BinaryExpr(premise, BinaryOp.ARROW, 
													new BinaryExpr(conclusion1, BinaryOp.OR, conclusion2)));
						obligations.add(obligation);
					}
				}
				
			}
		}
		
		return obligations;
	}
	
	private boolean isInLoop(ObservedTreeNode root, ObservedTreeNode leaf,
							List<List<ObservedTreeNode>> paths) {
		Set<String> selfLoopNode = getSelfLoopRoots(paths);
		boolean isLoop = false;
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			
			if (!selfLoopNode.contains(root.data) && 
					root.data.equals(path.get(path.size() - 1).data) &&
					leaf.data.equals(path.get(0).data)) {
					// if root is the leaf of some tree and
					// leaf is the root of the tree, a loop has been found
				isLoop = true;
				break;
			}
		}
		return isLoop;
	}
	
	private Set<String> getSelfLoopRoots(List<List<ObservedTreeNode>> paths) {
		Set<String> selfLoopRoots = new HashSet<>();
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			if (path.get(0).data.equals(path.get(path.size() - 1).data)) {
				selfLoopRoots.add(path.get(0).data);
			}
		}
		return selfLoopRoots;
	}
		
	private List<Obligation> genergateForCombTrees() {
		List<Obligation> obligations = new ArrayList<>();
		List<List<ObservedTreeNode>> paths = drawPaths(combUsedByTrees, TYPE_COMB);
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", c = "_MCDC";
		String node = "", father;
		IdExpr[] lhs = new IdExpr[2];
		IdExpr[] nonMasked = new IdExpr[2];
		Expr premise, conclusion1, conclusion2;
		Obligation obligation;
		
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			for (int j = path.size() - 1; j > 0; j--) {
				if ("int".equals(path.get(j).type.toString())) {
					node = "ArithExpr_1";
				} else {
					node = path.get(j).data;
				}
				father = path.get(j - 1).data;
				
				lhs[0] = new IdExpr(node + t + at + father + affect);
				lhs[1] = new IdExpr(node + f + at + father + affect);
				nonMasked[0] = new IdExpr(node + t + at + father + c + t);
				nonMasked[1] = new IdExpr(node + f + at + father + c + f);
				for (int k = 0; k < lhs.length; k++) {
					premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, new BoolExpr(false));
					conclusion1 = premise;
					conclusion2 = new UnaryExpr(UnaryOp.PRE, lhs[k]);
					
					obligation = new Obligation(lhs[k], true, 
										new BinaryExpr(premise, BinaryOp.ARROW, 
												new BinaryExpr(conclusion1, BinaryOp.OR, conclusion2)));
//					System.out.println("contains " + obligation + "? " + isInList(obligations, obligation));
					if (!isInList(obligations, obligation)) {
						obligations.add(obligation);
					}
				}
			}
		}
		
		return obligations;
	}
	
	private <T> boolean isInList(List<T> list, Object obj) {
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
	}
	
	private List<Obligation> generateForSeqTrees() {
		List<Obligation> obligations = new ArrayList<>();
		List<List<ObservedTreeNode>> paths = drawPaths(sequentialTrees, TYPE_SEQ);
		IdExpr token = new IdExpr("token");
		String seq = "_SEQ_USED_BY_";
		String affect = "_AFFECTING_AT_CAPTURE";
		String t = "_TRUE", f = "_FALSE", at = "_AT_", c = "_MCDC";
		String node, father, root;
		IdExpr seqUsed, rToken;
		IdExpr[] lhs = new IdExpr[2];
		IdExpr[] nonMasked = new IdExpr[2];
		Expr premise, conclusion1, conclusion2;
		Obligation obligation;
		
		for (int i = 0; i < paths.size(); i++) {
			List<ObservedTreeNode> path = paths.get(i);
			int index = path.size() - 1;
			node = path.get(index).data;
			father = path.get(index - 1).data;
			root = path.get(0).data;
			
			lhs[0] = new IdExpr(node + t + at + father + affect);
			lhs[1] = new IdExpr(node + f + at + father + affect);
			nonMasked[0] = new IdExpr(node + t + at + father + c + t);
			nonMasked[1] = new IdExpr(node + f + at + father + c + f);
			
			for (int k = 0; k < lhs.length; k++) {
				seqUsed = new IdExpr(father + seq + root);
				rToken = nodeToToken.get(root);
				
				premise = new BinaryExpr(nonMasked[k], BinaryOp.AND, 
									new BinaryExpr(seqUsed, BinaryOp.AND, 
											new BinaryExpr(token, BinaryOp.EQUAL, rToken)));
				conclusion1 = premise;
				conclusion2 = new UnaryExpr(UnaryOp.PRE, lhs[k]);
				
				obligation = new Obligation(lhs[k], true, 
									new BinaryExpr(premise, BinaryOp.ARROW, 
											new BinaryExpr(conclusion1, BinaryOp.OR, conclusion2)));
				obligations.add(obligation);
			}
		}
		
		return obligations;
	}
	
	private List<List<ObservedTreeNode>> drawPaths(HashMap<VarDecl, ObservedTree> trees, String type) {
		List<List<ObservedTreeNode>> paths = new ArrayList<>();
		
		for (VarDecl node : trees.keySet()) {
			ObservedTreeNode root = trees.get(node).root;
			root.getPaths(paths);
		}
		
		if (type.equals(TYPE_COMB)) {
			for (int i = 0; i < paths.size(); i++) {
				int len = paths.get(i).size();
				paths.get(i).remove(len - 1);
				if (i > 0 && (paths.get(i).equals(paths.get(i - 1)))) {
					paths.remove(i);
				}
			}
		}
		
//		System.out.println("Number of paths: " + paths.size());
//		for (int i = 0; i < paths.size(); i++) {
//			System.out.println(paths.get(i) + ", " + paths.get(i).size());
//		}
		
		return paths;
	}
		
	// build token-to-node, node-to-token, tokennode-dependency maps
	private void drawMaps() {
		tokens = new IdExpr[sequentialTrees.size()];
		count = 0;
		
//			System.out.println("============= drawing maps =============");
		for (VarDecl treeRoot : sequentialTrees.keySet()) {
			tokens[count] = new IdExpr(prefix + (count + 1));
			tokenToNode.put(tokens[count], treeRoot.id);
			nodeToToken.put(treeRoot.id, tokens[count]);
			
			ObservedTreeNode root = sequentialTrees.get(treeRoot).root;
//			rootToLeavesMap.put(treeRoot.id, root.getAllLeaves());
			rootToLeavesMap.put(root, root.getAllLeafNodes());
			
//				System.out.println(count + " token-to-node: " + tokens[count] + " - " + tokenToNode.get(tokens[count]));
//				System.out.println(count + " node-to-token: " + tree.id + " - " + nodeToToken.get(tree.id));
//				System.out.println(count + " dependency: " + tree.id + " >>> " + rootToLeavesMap.get(tree.id));
			count++;
		}
	}
}

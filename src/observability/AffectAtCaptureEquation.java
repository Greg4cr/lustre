package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import coverage.Obligation;

import java.util.Iterator;

import enums.Coverage;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import observability.tree.ObservedTree;
import observability.tree.ObservedTreeNode;

public class AffectAtCaptureEquation {
	HashMap<String, Expr> map = new HashMap<>();
	HashMap<String, List<String>> affectPairs = new HashMap<>();
	
	HashMap<VarDecl, ObservedTree> sequentialTrees;
	HashMap<VarDecl, ObservedTree> combUsedByTrees;
	HashMap<String, List<String>> delayMap = new HashMap<>();
	Coverage coverage;
	
	List<VarDecl> idList;
	List<VarDecl> singleNodeList;
	HashMap<VarDecl, ObservedTreeNode> singleNodeTrees = new HashMap<>();
	
	List<List<String>> leavesToRoots = new ArrayList<>();
	List<List<String>> rootToLeaves = new ArrayList<>();
	// 200 iterations; 60 minutes
	final String TYPE_SEQ = "SEQ";
	final String TYPE_COMB = "COMB";
	
	// dynamic tokens
	String prefix = "TOKEN_D";
	IdExpr[] tokens;
	int tokenCount = 0;
	
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	HashMap<ObservedTreeNode, List<ObservedTreeNode>> rootToLeavesMap = new HashMap<>();
	// token to tree node (root), Map<Token, Node>
	HashMap<IdExpr, String> tokenToNode = new HashMap<>();
	// tree node (root) to token, Map<Node, Token>
	HashMap<String, IdExpr> nodeToToken = new HashMap<>();
	// id to condition & occurrence, Map<id, <condition, occurrence>>
	TreeMap<String, TreeMap<String, Integer>> idToCondMap = new TreeMap<>();
	
	List<String> handledList = new ArrayList<>();
	
	public AffectAtCaptureEquation(HashMap<VarDecl, ObservedTree> seqTrees,
									HashMap<VarDecl, ObservedTree> combUsedTrees,
									HashMap<String, List<String>> delayMap,
									TreeMap<String, TreeMap<String, Integer>> idToCondMap,
									Coverage coverage) {
		this.sequentialTrees = seqTrees;
		this.combUsedByTrees = combUsedTrees;
		this.delayMap = delayMap;
		this.idToCondMap = idToCondMap;
		this.coverage = coverage;
		populateMaps();
		this.handledList = getHandledIds();
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
	
	private HashMap<ObservedTreeNode, List<ObservedTreeNode>> populateSuperRootMap(List<List<ObservedTreeNode>> paths) {
		HashMap<ObservedTreeNode, List<ObservedTreeNode>> superRootsMap = new HashMap<>();
				
		for (int i = 0; i < paths.size(); i++) {
			ObservedTreeNode root = paths.get(i).get(0);
			List<ObservedTreeNode> supRoots = new ArrayList<>();
			
			for (int j = 0; j < paths.size(); j++) {
				List<ObservedTreeNode> path = paths.get(j);
				int len = path.size();
				
				if (root.data.equals(path.get(len - 1).data)) {
					if (supRoots.contains(path.get(0))) {
						continue;
					}
					supRoots.add(path.get(0));
				}
			}
			superRootsMap.put(root, supRoots);
		}
		
		return superRootsMap;
	}
	
	private void generateForSingleNodes(HashMap<String, Expr> map) {
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", c = "_" + coverage.name();
		String node = "", father;
		String[] vals = {"_TRUE", "_FALSE"};
		String lhs = "";
		String nonMasked = "";
		Expr premise, conclusion1, conclusion2;
		
		if (this.singleNodeTrees.keySet() == null) {
			return;
		}
		
		for (VarDecl exprId : this.singleNodeTrees.keySet()) {
			ObservedTreeNode root = this.singleNodeTrees.get(exprId);
			List<ObservedTreeNode> children = root.children;
			for (ObservedTreeNode child : children) {
				if ("int".equals(child.type.toString()) && !child.isArithExpr) {
					continue;
				} else if (child.isArithExpr) {
					node = child.arithId;
				} else {
					node = child.data;
				}
				
				father = root.data;
				
				for (int i = 0; i < child.occurrence; i++) {
					if (child.occurrence > 1) {
						node += "_" + i;
					}
					
					for (int k = 0; k < vals.length; k++) {
						nonMasked = node + vals[k] + at + father + c + vals[k];
						if (!handledList.contains(nonMasked)) {
							break;
						}
						
						lhs = node + vals[k] + at + father + affect;
						
						premise = new BinaryExpr(new IdExpr(nonMasked), BinaryOp.AND, new BoolExpr(false));
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs));
						
						Expr expr = new BinaryExpr(premise, BinaryOp.ARROW, 
													new BinaryExpr(conclusion1, 
															BinaryOp.OR, conclusion2));
						if (!map.containsKey(lhs)) {
//							System.out.print("single node:\t");
							trackAffectPairs(node, father);
							map.put(lhs, expr);
						} else if (!map.get(lhs).toString().contains(expr.toString())) {
							continue;
						}
					}
				}
			}
		}
	}
	
	private void generateForCombTrees(HashMap<String, Expr> map) {
		List<List<ObservedTreeNode>> paths = drawPaths(combUsedByTrees, TYPE_COMB);
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", c = "_" + coverage.name();
		String node = "", father, fNode;
		ObservedTreeNode child = null;
		String[] vals = {"_TRUE", "_FALSE"};
		String lhs = "";
		String nonMasked = "";
		Expr premise, conclusion1, conclusion2;
		
		for (int i = 0; i < paths.size(); ++i) {
			List<ObservedTreeNode> path = paths.get(i);
			
			for (int j = path.size() - 1; j > 0; --j) {
				child = path.get(j);
				node = child.data;
				father = path.get(j - 1).data;
				
				if (delayMap.containsKey(node)) {
					continue;
				}
				if ("int".equals(path.get(j - 1).type.toString())) {
					continue;
				}
				if ("int".equals(child.type.toString()) && !child.isArithExpr) {
					continue;
				} else if (child.isArithExpr) {
					node = child.arithId;
				}
				
				if (!idToCondMap.containsKey(father) ||
						!idToCondMap.get(father).containsKey(node)) {
					continue;
				}

				int occurence = 0;
				
				if (child.isArithExpr) {
					occurence = idToCondMap.get(father).get(node) / 2;
				} else {
					occurence = child.occurrence;
				}
				
				for (int l = 0; l < occurence; l++) {
					fNode = node;
					if (occurence > 1) {
						fNode = node + "_" + l;
					}
					
					for (int k = 0; k < vals.length; k++) {
						nonMasked = fNode + vals[k] + at + father + c + vals[k];
						lhs = fNode + vals[k] + at + father + affect;
						
						premise = new BinaryExpr(new IdExpr(nonMasked), BinaryOp.AND, 
												new BoolExpr(false));
						conclusion1 = premise;
						conclusion2 = new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs));
						
						Expr expr = new BinaryExpr(premise, BinaryOp.ARROW, 
													new BinaryExpr(conclusion1, 
															BinaryOp.OR, conclusion2));
						if (!map.containsKey(lhs)) {
//							System.out.print("comb tree:\t");
							trackAffectPairs(fNode, father);
							map.put(lhs, expr);
							
							// otherwise, its value can be passed via other path/nodes
							// through some path in seq_dependent tree(s)
						} else if (!map.get(lhs).toString().contains(expr.toString())) {
							// its value can be passed via other nodes
							continue;
						}
					}
				}
			}
		}
	}
	
	private void generateForSeqDepTrees(HashMap<String, Expr> map) {
		// <lhs, <nonMasked, list of <dependency, token>>>
		TreeMap<String, TreeMap<String, List<TreeMap<String, String>>>> exprMap = buildSeqUsedMap();
		// <nonMasked, list of <dependency, token>>
		TreeMap<String, List<TreeMap<String, String>>> premisePairs = new TreeMap<>();
		// <seqUsed, token>
		TreeMap<String, String> tokenPairs = new TreeMap<>();
		
		String token = "token";
		String rootToken;
		
		
		for (String lhs : exprMap.keySet()) {
			premisePairs = exprMap.get(lhs);
			
			for (String nonMasked : premisePairs.keySet()) {
				List list = premisePairs.get(nonMasked);
				Iterator<TreeMap<String, String>> iterator = list.iterator();
				
				Expr tokenExpr = null;
				Expr seqExpr = null;
				
				while (iterator.hasNext()) {
					tokenPairs = iterator.next();
					
					for (String seqUsed : tokenPairs.keySet()) {
						rootToken = tokenPairs.get(seqUsed);
						
						tokenExpr = new BinaryExpr(new IdExpr(token),
											BinaryOp.EQUAL, new IdExpr(rootToken));
						
						if (seqExpr == null) {
							seqExpr = new BinaryExpr(new IdExpr(seqUsed),
									BinaryOp.AND, tokenExpr);
						} else {
							seqExpr = new BinaryExpr(seqExpr, BinaryOp.OR, 
									new BinaryExpr(new IdExpr(seqUsed), BinaryOp.AND,tokenExpr));
						}
					}
					
					Expr premiseExpr = new BinaryExpr(new IdExpr(nonMasked),
									BinaryOp.AND, seqExpr);
					Expr expr = new BinaryExpr(premiseExpr, BinaryOp.ARROW,
							new BinaryExpr(premiseExpr, BinaryOp.OR, 
									new UnaryExpr(UnaryOp.PRE, new IdExpr(lhs))));
					map.put(lhs, expr);
				}
			}
		}
	}
	
	private TreeMap<String, TreeMap<String, List<TreeMap<String, String>>>> buildSeqUsedMap() {
		// <lhs, <nonMasked, list of <dependency, token>>>
		TreeMap<String, TreeMap<String, List<TreeMap<String, String>>>> exprMap = new TreeMap<>();
		// <nonMasked, list of <dependency, token>>
		TreeMap<String, List<TreeMap<String, String>>> premisePairs = new TreeMap<>();
		TreeMap<String, List<TreeMap<String, String>>> addedNonMaskeds = new TreeMap<>();
		
		List<List<ObservedTreeNode>> paths = drawPaths(sequentialTrees, TYPE_SEQ);
		HashMap<ObservedTreeNode, List<ObservedTreeNode>> superRootsMap = populateSuperRootMap(paths);
		
		String seq = "_SEQ_USED_BY_";
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", cov = "_" + coverage.name();
		String nodeStr;
		ObservedTreeNode father, root;
		List<ObservedTreeNode> superRoots = new ArrayList<>();
		String seqUsed = "", rootToken = "";
		
		String[] val = {"_TRUE", "_FALSE"};
		String lhs = "";
		String nonMasked = "";
		
		for (int i = 0; i < paths.size(); ++i) {
			List<ObservedTreeNode> path = paths.get(i);
			root = path.get(0);
			
			for (int index = path.size() - 1; index > 0; --index) {
				int occurence = path.get(index).occurrence;
				ObservedTreeNode child = path.get(index);
				
				for (int j = 0; j < occurence; ++j) {
					if ("int".equals(child.type.toString())
							&& !child.isArithExpr) {
						continue;
					} else if (child.isArithExpr) {
						nodeStr = child.arithId;
					} else {
						nodeStr = child.data;
					}
					
					if (occurence > 1) {
						nodeStr = nodeStr + "_" + j;
					}
					
					father = path.get(index - 1);
					
					superRoots.clear();
					if (father.equals(root)) {
						superRoots.addAll(superRootsMap.get(father));
					} else {
						superRoots.add(root);
					}
					
					List<TreeMap<String, String>> list = new ArrayList<>();
					for (int k = 0; k < val.length; ++k) {
						nonMasked = nodeStr + val[k] + at + father.data + cov + val[k];
						
						if (!handledList.contains(nonMasked)) {
							break;
						}
												
						lhs = nodeStr + val[k] + at + father.data + affect;
						premisePairs.clear();
						
						TreeMap<String, String> tokenPairs = new TreeMap<>();
						
						if (path.size() <= 2) {
							if (!tokenPairs.containsKey("FALSE")) {
								tokenPairs.put("FALSE", "FALSE");
							}
						} else {
//							System.out.print("seq tree:\t");
							// track affecting_at_capture pairs
							trackAffectPairs(nodeStr, father.data);
							
							for (int m = 0; m < superRoots.size(); ++m) {
								list.clear();
								seqUsed = father.data + seq + superRoots.get(m).data;
								rootToken = nodeToToken.get(superRoots.get(m).data).id;
								
								if (!tokenPairs.containsKey(seqUsed)) {
									tokenPairs.put(seqUsed, rootToken);
									list.add(tokenPairs);
								}
							}
						}

						if (!addedNonMaskeds.containsKey(nonMasked)) {
							addedNonMaskeds.put(nonMasked, list);
							premisePairs.put(nonMasked, list);
						} else {
							getList(list, addedNonMaskeds.get(nonMasked));
							
							premisePairs.put(nonMasked, list);
							addedNonMaskeds.put(nonMasked, list);
						}
						
						exprMap.put(lhs, new TreeMap(premisePairs));
					}
				}
				
			}
		}

		return exprMap;
	}
	
	public HashMap<String, List<String>> getAffectPairs() {
		return affectPairs;
	}
	
	private void trackAffectPairs(String affecter, String affectee) {
//		System.out.println(affecter + ", " + affectee);
		List<String> affectingList = new ArrayList<>();
		
		if (! affectPairs.containsKey(affecter)) {
			affectingList.clear();
			affectingList.add(affectee);
			affectPairs.put(affecter, affectingList);
		} else {
			affectingList.clear();
			affectingList.addAll(affectPairs.get(affecter));
			
			if (!affectingList.contains(affectee)) {
				affectingList.add(affectee);
				affectPairs.put(affecter, affectingList);
			}
		}
	}
	
	private List<String> getHandledIds() {
		List<String> handledList = new ArrayList<>();
		
		String at = "_AT_", cov = "_" + coverage.name();
		String[] vals = {"_TRUE", "_FALSE"};
		String condStr = "";
				
		for (String key : idToCondMap.keySet()) {
			TreeMap<String, Integer> conditions = idToCondMap.get(key);
			
			for (String cond : conditions.keySet()) {
				condStr = cond;
				int occurence = conditions.get(cond) / 2;
				for (int i = 0; i < occurence; i++) {
					if (occurence > 1) {
						condStr = cond + "_" + i;
					}
					for (int j = 0; j < vals.length; j++) {
						String handled = condStr + vals[j] + at + key + cov + vals[j];
						handledList.add(handled);
					}
				}
			}
		}
		
		return handledList;
	}
		
	private void getList(List<TreeMap<String, String>> list,
			List<TreeMap<String, String>> addedList) {
		Iterator<TreeMap<String, String>> iterator = addedList.iterator();
		
		String listStr = list.toString();
		
		while (iterator.hasNext()) {
			TreeMap<String, String> tmp = iterator.next();
			if (listStr.contains(tmp.toString())) {
				continue;
			} else {
				list.add(tmp);
				listStr = list.toString();
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
		
		return paths;
	}
		
	// build token-to-node, node-to-token, tokennode-dependency maps
	private void populateMaps() {
		tokens = new IdExpr[sequentialTrees.size()];
		
		for (VarDecl treeRoot : sequentialTrees.keySet()) {
			tokens[tokenCount] = new IdExpr(prefix + (tokenCount + 1));
			tokenToNode.put(tokens[tokenCount], treeRoot.id);
			nodeToToken.put(treeRoot.id, tokens[tokenCount]);
			
			ObservedTreeNode root = sequentialTrees.get(treeRoot).root;
			rootToLeavesMap.put(root, root.getAllLeafNodes());

			tokenCount++;
		}
	}
}
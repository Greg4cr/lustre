package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import observability.tree.Tree;
import observability.tree.TreeNode;

public class AffectAtCaptureEquation {
	private Map<String, Expr> map = new HashMap<>();
	private Map<String, List<String>> affectPairs = new HashMap<>();
	
	private Map<String, Tree> delayTrees;
	private Map<String, Tree> observerTrees;
	private Map<String, List<String>> delayTable = new HashMap<>();
	private Coverage coverage;
	
	private List<String> deadNodes;
	private Map<String, Tree> deadNodeTrees = new HashMap<>();
	
	private final String TYPE_SEQ = "SEQ";
	private final String TYPE_COMB = "COMB";
	
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	private Map<TreeNode, List<TreeNode>> tokenDepTable = new HashMap<>();
	// tree node (root) to token, Map<Node, Token>
	private Map<TreeNode, IdExpr> nodeToToken = new HashMap<>();
	// id to condition & occurrence, Map<id, <condition, occurrence>>
	private Map<String, Map<String, Integer>> affectAtCaptureTable = new TreeMap<>();
	
	private List<String> handledList = new ArrayList<>();
	
	public AffectAtCaptureEquation(Map<String, Tree> delayTrees,
									Map<String, Tree> observerTrees,
									Map<String, List<String>> delayTable,
									Map<String, Map<String, Integer>> affectAtCaptureTable,
									Coverage coverage,
									Map<TreeNode, List<TreeNode>> tokenDepTable,
									Map<TreeNode, IdExpr> nodeToToken) {
		this.delayTrees = delayTrees;
		this.observerTrees = observerTrees;
		this.delayTable = delayTable;
		this.affectAtCaptureTable = affectAtCaptureTable;
		this.coverage = coverage;
		this.tokenDepTable = tokenDepTable;
		this.nodeToToken = nodeToToken;
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
	
	private List<Obligation> getObligations(Map<String, Expr> map) {
		List<Obligation> obligations = new ArrayList<>();
		for (String lhs : map.keySet()) {
			Obligation obligation = new Obligation(new IdExpr(lhs), true, map.get(lhs));
			obligations.add(obligation);
		}
		
		return obligations;
	}
	
	public void setDeadNodes(List<String> deadNodes) {
		if (deadNodes == null || deadNodes.isEmpty()) {
			this.deadNodes = new ArrayList<String>();
		} else {
			this.deadNodes = deadNodes;
		}
	}
	
	public void setSingleNodeTrees(Map<String, Tree> trees) {
		if (trees == null || trees.keySet().size() == 0) {
			deadNodeTrees = new HashMap<String, Tree>();
		} else {
			deadNodeTrees = trees;
		}
	}
	
	private void generateForSingleNodes(Map<String, Expr> map) {
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", c = "_" + coverage.name();
		String node = "", father;
		String[] vals = {"_TRUE", "_FALSE"};
		String lhs = "";
		String nonMasked = "";
		Expr premise, conclusion1, conclusion2;
		
		if (deadNodeTrees.keySet() == null) {
			return;
		}
		
		for (String exprId : deadNodeTrees.keySet()) {
			TreeNode root = deadNodeTrees.get(exprId).root;
			List<TreeNode> children = root.children;
			for (TreeNode child : children) {
				
				for (String childStr : child.renamedIds.keySet()) {
					if ("int".equals(child.type.toString()) && !child.isArithExpr) {
						continue;
					} else {
						node = childStr;
					}
					
					father = root.rawId;
					for (int i = 0; i < child.renamedIds.get(childStr); i++) {
						if (child.renamedIds.get(childStr) > 1) {
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
	}
	
	private void generateForCombTrees(Map<String, Expr> map) {
		List<List<TreeNode>> paths = drawPaths(observerTrees, TYPE_COMB);
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", c = "_" + coverage.name();
		String node = "", father, fNode;
		TreeNode child = null;
		String[] vals = {"_TRUE", "_FALSE"};
		String lhs = "";
		String nonMasked = "";
		Expr premise, conclusion1, conclusion2;
		
		for (int i = 0; i < paths.size(); ++i) {
			List<TreeNode> path = paths.get(i);
			
			for (int j = path.size() - 1; j > 0; --j) {
				child = path.get(j);
				
				if (delayTable.containsKey(child.rawId)) {
					continue;
				}
				if ("int".equals(path.get(j - 1).type.toString())) {
					continue;
				}
				
				if ("int".equals(child.type.toString()) && !child.isArithExpr) {
					continue;
				} 
				
				father = path.get(j - 1).rawId;
				for (String renamedId : child.renamedIds.keySet()) {
					node = renamedId;
					
					if (!affectAtCaptureTable.containsKey(father) ||
							!affectAtCaptureTable.get(father).containsKey(node)) {
						continue;
					}
					
					int occurence = 0;
					if (child.isArithExpr) {
						occurence = affectAtCaptureTable.get(father).get(node) / 2;
					} else {
						occurence = child.renamedIds.get(renamedId);
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
	}
	
	private void generateForSeqDepTrees(Map<String, Expr> map) {
		// <lhs, <nonMasked, list of <dependency, token>>>
		Map<String, Map<String, List<Map<String, String>>>> exprMap = buildSeqUsedMap();
		// <nonMasked, list of <dependency, token>>
		Map<String, List<Map<String, String>>> premisePairs = new TreeMap<>();
		// <seqUsed, token>
		Map<String, String> tokenPairs = new TreeMap<>();
		
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
	
	private Map<String, Map<String, List<Map<String, String>>>> buildSeqUsedMap() {
		// <lhs, <nonMasked, list of <dependency, token>>>
		Map<String, Map<String, List<Map<String, String>>>> exprMap = new TreeMap<>();
		// <nonMasked, list of <dependency, token>>
		Map<String, List<Map<String, String>>> premisePairs = new TreeMap<>();
		Map<String, List<Map<String, String>>> addedNonMaskeds = new TreeMap<>();
		
		List<List<TreeNode>> paths = drawPaths(delayTrees, TYPE_SEQ);
		
		String seq = "_SEQ_USED_BY_";
		String affect = "_AFFECTING_AT_CAPTURE";
		String at = "_AT_", cov = "_" + coverage.name();
		String nodeStr;
		TreeNode father, root;
		List<TreeNode> superRoots = new ArrayList<>();
		String seqUsed = "", rootToken = "";
		
		String[] val = {"_TRUE", "_FALSE"};
		String lhs = "";
		String nonMasked = "";
		
		for (int i = 0; i < paths.size(); ++i) {
			List<TreeNode> path = paths.get(i);
			root = path.get(0);
			
			for (int index = path.size() - 1; index > 0; --index) {
				TreeNode child = path.get(index);
				
				for (String childStr : child.renamedIds.keySet()) {
					for (int j = 0; j < child.renamedIds.get(childStr); ++j) {
						if ("int".equals(child.type.toString())
								&& ! child.isArithExpr) {
							continue;
						} else {
							nodeStr = childStr;
						}
						
						if (child.renamedIds.get(childStr) > 1) {
							nodeStr = nodeStr + "_" + j;
						}
						
						father = path.get(index - 1);
						
						superRoots.clear();
						if (index == 1) {
							superRoots.addAll(tokenDepTable.get(father));
						} else {
							superRoots.add(root);
						}
						
						List<Map<String, String>> list = new ArrayList<>();
						for (int k = 0; k < val.length; ++k) {
							nonMasked = nodeStr + val[k] + at + father.rawId + cov + val[k];
							
							if (!handledList.contains(nonMasked)) {
								break;
							}
													
							lhs = nodeStr + val[k] + at + father.rawId + affect;
							premisePairs.clear();
							
							Map<String, String> tokenPairs = new TreeMap<>();
							
							if (path.size() <= 2) {
								if (!tokenPairs.containsKey("FALSE")) {
									tokenPairs.put("FALSE", "FALSE");
								}
							} else {
								// track affecting_at_capture pairs
								trackAffectPairs(nodeStr, father.rawId);
								
								for (int m = 0; m < superRoots.size(); ++m) {
									list.clear();
									seqUsed = father.rawId + seq + superRoots.get(m).rawId;
									rootToken = nodeToToken.get(superRoots.get(m)).id;
									
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
		}

		return exprMap;
	}
	
	public Map<String, List<String>> getAffectPairs() {
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
				
		for (String key : affectAtCaptureTable.keySet()) {
			Map<String, Integer> conditions = affectAtCaptureTable.get(key);
			
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
		
	private void getList(List<Map<String, String>> list,
			List<Map<String, String>> addedList) {
		Iterator<Map<String, String>> iterator = addedList.iterator();
		
		String listStr = list.toString();
		
		while (iterator.hasNext()) {
			Map<String, String> tmp = iterator.next();
			if (listStr.contains(tmp.toString())) {
				continue;
			} else {
				list.add(tmp);
				listStr = list.toString();
			}
		}
		
	}
	
	private List<List<TreeNode>> drawPaths(Map<String, Tree> trees, 
													String type) {
		List<List<TreeNode>> paths = new ArrayList<>();
		
		for (String node : trees.keySet()) {
			TreeNode root = trees.get(node).root;
			root.getPaths(paths);
		}
		
		if (type.equals(TYPE_COMB)) {
			for (int i = 0; i < paths.size(); i++) {
				int len = paths.get(i).size();
				
				TreeNode leaf = paths.get(i).get(len -1);
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
		
//		System.out.println("paths :::\n\t" + paths);
		return paths;
	}
}
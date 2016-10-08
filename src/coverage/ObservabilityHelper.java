package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.Node;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import observability.tree.ObservedTree;
import observability.tree.ObservedTreeNode;

/*
 * Parse nodes into trees.
 * 	- Dependency relationship of equation ids and variables.
 *  - Delay dependency.
 *  - And any necessary dependencies.
 */
public class ObservabilityHelper {
	private Node node;
	private List<VarDecl> idList = new ArrayList<>();
	private boolean isPre = false;
	private List<VarDecl> singleNodeList = new ArrayList<>();
	private HashMap<String, List<String>> delayMap = new HashMap<>();
	
	public ObservabilityHelper(Node node) {
		this.node = node;
		this.idList = getIdList();
	}
	
	public List<String> getInStrList() {
		List<String> inList = new ArrayList<>();
		for (VarDecl id : node.inputs) {
			inList.add(id.id);
		}
		return inList;
	}
	
	public void setDelayMap(HashMap<String, List<String>> delayMap) {
		this.delayMap = delayMap;
	}
	
	/*
	 * Build referring trees for given node, one tree per output.
	 * Example: A is an output variable, and A = (B and C); B = (C or D);
	 * 			then the tree should look like,
	 * 					A
	 * 				  /   \
	 * 				 B	   C
	 *				/ \
	 *             C   D
	 */
	public HashMap<VarDecl, ObservedTree> buildRefTrees() {
		Map<String, Expr> expressions = getStrExprTable();
		HashMap<VarDecl, ObservedTree> subTrees = new HashMap<VarDecl, ObservedTree>();
		
		// create one tree per output
		for (VarDecl root : node.outputs) {
			ObservedTreeNode treeRoot = new ObservedTreeNode(root.id, root.type);
			buildTreeRecursively(treeRoot, expressions, true, 0);
			subTrees.put(root, new ObservedTree(treeRoot));
		}
		return subTrees;
	}
	
	public int getTokenNumber() {
		return (delayMap.keySet().size());
	}
	
	public HashMap<VarDecl, ObservedTree> buildSeqTrees() {
		HashMap<VarDecl, ObservedTree> seqTrees = new HashMap<>();
		HashMap<String, Expr> expressions = getStrExprTable();
		HashMap<String, VarDecl> idMap = getIds();
		
		for (String root : delayMap.keySet()) {
			ObservedTreeNode treeRoot = new ObservedTreeNode(root, idMap.get(root).type);
			buildTreeRecursively(treeRoot, expressions, false, 0);
			seqTrees.put(idMap.get(root), new ObservedTree(treeRoot));
		}
		
		return seqTrees;
	}
	
	/*
	 * Recursively build a subtree given a root.
	 */
	private ObservedTreeNode buildTreeRecursively(ObservedTreeNode root, 
												Map<String, Expr> exprs, 
												boolean buildRefTree,
												int seqMode) {
		List<String> strIds = getStrIds();
		HashMap<String, VarDecl> ids = getIds();
		HashMap<String, Integer> children = new HashMap<>();
		HashMap<String, Boolean> preNode = new HashMap<>();
		String firstPreExpr = "";
		HashMap<String, String> arithIds = new HashMap<>();
		
		if (buildRefTree) {
			// build reference tree
			if (!exprs.keySet().contains(root.data)) {
				throw new IllegalArgumentException("!!! No corresponding expression!!! >>> " + root.data);
			} else if (exprs.get(root.data).toString().contains(UnaryOp.PRE.toString() + " ")) {
				return null;
			}
		} else {
			// build sequential tree
			if (!exprs.keySet().contains(root.data)) {
				throw new IllegalArgumentException("!!! No corresponding expression!!! >>> " + root.data);
			} else {
				if (seqMode >= 3) {
					// stop building tree
					return null;
				} else if (seqMode > 0 
						&& exprs.get(root.data).toString().contains(UnaryOp.PRE.toString() + " ")) {
					if (!firstPreExpr.equals(exprs.get(root.data).toString())) {
						return null;
					} else {
						seqMode++;
					}
				} else if (seqMode == 0) {
					// build first-level
					firstPreExpr = exprs.get(root.data).toString();
					
					List<String> delays = delayMap.get(root.data);

					for (int i = 0; i < delays.size(); i++) {
						children.put(delays.get(i), 1);
						preNode.put(delays.get(i), true);
					}
				}
			}
		}
		
		for (String arithExpr : Obligation.arithExprByExpr.keySet()) {
			boolean containsArith = exprs.get(root.data).toString().contains(arithExpr);
			if (containsArith) {
				for (String lhs : exprs.keySet()) {
					if (exprs.get(root.data).toString().contains(lhs) && arithExpr.contains(lhs)) {
						arithIds.put(lhs, Obligation.arithExprByExpr.get(arithExpr));
					}
				}
				
			}
		}
		
		if (!buildRefTree && seqMode == 0) {
			// first-level of sequential tree has been added
			// stop adding more children
			seqMode = 1;
		} else {
			if (exprs.get(root.data).toString().indexOf(" ") > 0) {
				String[] items = exprs.get(root.data).toString().replaceAll("[(){}]", " ").split(" ");
				for (String item : items) {				
					item = item.trim();
									
					if (item.isEmpty()) {
						continue;
					} else if (!strIds.contains(item)) {
						if (item.equals(UnaryOp.PRE.toString())) {
							isPre = true;
						}
						continue;
					} else if (children.containsKey(item)) {
						if (isPre && item.equals(root.data)) {
							continue;
						}
						children.put(item, children.get(item) + 1);
						isPre = false;
					} else {
						if (isPre && item.equals(root.data)) {
							continue;
						}
						children.put(item, 1);
						preNode.put(item, isPre);
						isPre = false;
					}
				}
			} else {
				String item = exprs.get(root.data).toString().replaceAll("[ (){}]", "");
				if (strIds.contains(item)) {
					if (!children.containsKey(item)) {
						children.put(item, 1);
						preNode.put(item, false);
					} else {
						children.put(item, children.get(item) + 1);
						preNode.put(item, false);
					}
					isPre = false;
				}
			}
		}
		
		// add children to current root
		for (String child : children.keySet()) {
			
			ObservedTreeNode newTreeNode = new ObservedTreeNode(child, ids.get(child).type);
			newTreeNode.setOccurrence(children.get(child));
			newTreeNode.setIsPre(preNode.get(child));
			if (arithIds.get(child) != null) {
				newTreeNode.setIsArithExpr(true);
				newTreeNode.setArithId(arithIds.get(child));
			}
			root.addChild(newTreeNode);
			
			if (exprs.keySet().contains(child)) {
				// build the tree recursively if next node is 
				// on the left-hand-side of some expressions
				buildTreeRecursively(newTreeNode, exprs, buildRefTree, seqMode);
			} else {
				// skip over nodes that do not depend on others.
				continue;
			}
		}

		return root;
	}
	
	/*
	 * return all variables (inputs, outputs, and locals) given a node 
	 */
	public List<VarDecl> getIdList() {
		List<VarDecl> varList = new ArrayList<VarDecl>();
		
		varList.addAll(node.outputs);
		varList.addAll(node.inputs);
		varList.addAll(node.locals);

		return varList;
	}
	
	public HashMap<String, VarDecl> getIds() {
		HashMap<String, VarDecl> idMap = new HashMap<String, VarDecl>();
		for (VarDecl output : node.outputs) {
			idMap.put(output.id, output);
		}
		for (VarDecl input : node.inputs) {
			idMap.put(input.id, input);
		}
		for (VarDecl local : node.locals) {
			idMap.put(local.id, local);
		}
		return idMap;
	}
	
	private List<String> getStrIds() {
		List<String> strIdList = new ArrayList<String>();
		for (VarDecl id : idList) {
			strIdList.add(id.id);
		}
		return strIdList;
	}
	
	public HashMap<VarDecl, ObservedTreeNode> getSingleNodeTrees() {
		/* mapping root variable to corresponding tree */
		HashMap<VarDecl, ObservedTreeNode> trees = new HashMap<>();
		ObservedTreeNode root = null;
		/* mapping node name to occurrence of the node */
		HashMap<String, Integer> children = new HashMap<>();
		/* mapping var name to var variable */
		HashMap<String, VarDecl> ids = getIds();
		/* mapping lhs to corresponding expr (rhs) */
		HashMap<String, Expr> expressions = getStrExprTable();
		List<String> strIds = getStrIds();
		
		for (VarDecl node : this.singleNodeList) {
			Expr expr = expressions.get(node.id);
			root = new ObservedTreeNode(node.id, ids.get(node.id).type);
			
			if (expr.toString().indexOf(" ") > 0) {
				String[] items = expr.toString().replaceAll("[(){}]", " ").split(" ");
				for (String item : items) {
					item = item.trim();
					if (item.isEmpty()) {
						continue;
					} else if (!strIds.contains(item)) {
						continue;
					} else if (children.containsKey(item)) {
						children.put(item, children.get(item) + 1);
					} else {
						children.put(item, 1);
					}
				}
			} else {
				String item = expr.toString().replaceAll("[ (){}]", "");
				if (strIds.contains(item) && !children.containsKey(item)) {
					children.put(item, 1);
				}
			}
			
			// add nodes to the tree
			for (String child : children.keySet()) {
				ObservedTreeNode newTreeNode = new ObservedTreeNode(child, ids.get(child).type);
				root.addChild(newTreeNode);
			}
			
			trees.put(node, root);
		}
				
		return trees;
	}
	
	// return single nodes that are not in any reference trees
	public List<VarDecl> getSingleNodeList(HashMap<VarDecl, ObservedTree> referenceTrees) {
		List<ObservedTreeNode> nodes = new ArrayList<>();
		List<String> treeNodeList = new ArrayList<>();
		List<VarDecl> idList = getIdList();
		List<VarDecl> inputList = node.inputs;
		
		for (VarDecl root : referenceTrees.keySet()) {
			ObservedTree referenceTree = referenceTrees.get(root);
			nodes.addAll(referenceTree.convertToList());
		}
				
		for (ObservedTreeNode node : nodes) {
			treeNodeList.add(node.data);
		}
		
		for (VarDecl id : idList) {
			if (!treeNodeList.contains(id.id) && !inputList.contains(id)) {
				this.singleNodeList.add(id);
			}
		}
		
		return this.singleNodeList;
	}	
		
	/*
	 * re-organize expressions of given node with hashtable
	 */
	private HashMap<String, Expr> getStrExprTable() {
		HashMap<String, Expr> exprTable = new HashMap<>();
		List<Equation> equations = node.equations;
		
		/*
		 * put expressions of given node into a hashmap<id, expr>
		 */
		for (Equation equation : equations) {
			exprTable.put(equation.lhs.get(0).id, equation.expr);
		}	
		return exprTable;
	}	
}
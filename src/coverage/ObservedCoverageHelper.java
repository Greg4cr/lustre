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
import main.LustreMain;
/*
 * Parse nodes into trees.
 * 	- Dependency relationship of equation ids and variables.
 *  - Delay dependency.
 *  - And any necessary dependencies.
 */
public class ObservedCoverageHelper {
	private Node node;
	private List<VarDecl> idList = new ArrayList<VarDecl>();
	private boolean isSeqRoot;
	private boolean isPre = false;
	private List<VarDecl> singleNodeList = new ArrayList<VarDecl>();
	
	public ObservedCoverageHelper(Node node) {
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
		List<VarDecl> roots = node.outputs;
//		idList = populateIdList();
		
		Map<String, Expr> expressions = getStrExprTable();
		HashMap<VarDecl, ObservedTree> subTrees = new HashMap<VarDecl, ObservedTree>();
		System.out.println("All ids in node \"" + node.id + "\":\n" + idList.toString() + "\n");
		
		// create one tree per output
		for (VarDecl root : roots) {
			ObservedTreeNode treeRoot = new ObservedTreeNode(root.id, root.type);
			buildTreeRecursively(treeRoot, expressions, true);
			subTrees.put(root, new ObservedTree(treeRoot));
		}
		return subTrees;
	}
	
	public int getTokenNumber() {
		return getSeqTreeRoots(getStrExprTable()).size();
	}
	
	public HashMap<VarDecl, ObservedTree> buildSeqTrees() {
		HashMap<VarDecl, ObservedTree> seqTrees = new HashMap<>();
		HashMap<String, Expr> expressions = getStrExprTable();
		HashMap<String, VarDecl> idMap = getIds();
		List<String> roots = getSeqTreeRoots(expressions);
		ObservedTreeNode treeNode;
		
		for (String root : roots) {
			isSeqRoot = true;
			treeNode = buildTreeRecursively(new ObservedTreeNode(root, idMap.get(root).type), expressions, false);
			seqTrees.put(idMap.get(root), new ObservedTree(treeNode));
		}
		
		return seqTrees;
	}
	
	
	/*
	 * Recursively build a subtree given a root.
	 */
	private ObservedTreeNode buildTreeRecursively(ObservedTreeNode root, 
												Map<String, Expr> exprs, 
												boolean isBuildingRef) {
		List<String> strIds = getStrIds();
		HashMap<String, VarDecl> ids = getIds();
		HashMap<String, Integer> children = new HashMap<>();
		HashMap<String, Boolean> preNode = new HashMap<>();
		
		if (isBuildingRef) {
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
			} else if (exprs.get(root.data).toString().contains(UnaryOp.PRE.toString() + " ")) {
				if (isSeqRoot) {
					// if it's root, build the tree
					isSeqRoot = false;
				} else {
					// otherwise, stop building sub-tree
					return null;
				}
			}
		}
		
		LustreMain.log(">>>>>> Building tree for " + root);
		LustreMain.log(">>>>>> Expression: " + root.data + " = " + exprs.get(root.data));
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
					children.put(item, children.get(item) + 1);
					isPre = false;
					continue;
				} else {
					children.put(item, 1);
					preNode.put(item, isPre);
					isPre = false;
				}
			}
		} else {
			String item = exprs.get(root.data).toString().replaceAll("[ (){}]", "");
			if (strIds.contains(item) && !children.containsKey(item)) {
				children.put(item, 1);
			}
		}
		
		System.out.println("children of " + root.toString()	+
							"\t::::: " + children.keySet());
		
		// add children to current root
		for (String child : children.keySet()) {
			ObservedTreeNode newTreeNode = new ObservedTreeNode(child, ids.get(child).type);
			newTreeNode.setOccurrence(children.get(child));
			newTreeNode.setIsPre(preNode.get(child));
			root.addChild(newTreeNode);
			System.out.println("xxxxxxxx new node: " + newTreeNode);
			if (exprs.keySet().contains(newTreeNode.data)) {
				// build the tree recursively if next node is 
				// on the left-hand-side of some expressions
				buildTreeRecursively(newTreeNode, exprs, isBuildingRef);
			} else {
				// skip over nodes that do not depend on others.
				continue;
			}
		}
		System.out.println("(" + root.data + ") has " + root.getNumberOfChildren() 
							+ " children: "+ root.getChildren().toString());
		return root;
	}
			
	/*
	 * return all variables (inputs, outputs, and locals) given a node 
	 */
	public List<VarDecl> getIdList() {
		List<VarDecl> varList = new ArrayList<VarDecl>();
		for (VarDecl output : node.outputs) {
			varList.add(output);
		}
		for (VarDecl input : node.inputs) {
			varList.add(input);
		}
		for (VarDecl local : node.locals) {
			varList.add(local);
		}
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
		HashMap<VarDecl, ObservedTreeNode> trees = new HashMap<>();
		ObservedTreeNode root = null;
		HashMap<String, Integer> children = new HashMap<>();
		HashMap<String, VarDecl> ids = getIds();
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
//		List<VarDecl> singleNodeList = new ArrayList<>();
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

		System.out.println("####### all ids: " + idList.toString());
		System.out.println("####### nodes in tree: " + treeNodeList.toString());
		System.out.println("####### single nodes: " + singleNodeList.toString());
		
		return this.singleNodeList;
	}	
	
	/*
	 * Search for all roots of delay dependency trees in given node
	 */
	private List<String> getSeqTreeRoots(HashMap<String, Expr> exprTable) {
		List<String> rootStr = new ArrayList<String>();
		
		for (String lhs : exprTable.keySet()) {
			if (exprTable.get(lhs).toString().contains(UnaryOp.PRE + " ")) {
				rootStr.add(lhs);
			}
		}
		
		return rootStr;
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


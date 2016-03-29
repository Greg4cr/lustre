package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import main.LustreMain;
/*
 * Parse node into trees.
 * 	- Dependency relationship of equation ids and variables.
 *  - Delay dependency.
 *  - And any necessary dependencies.
 */
public class ObservedCoverageHelper {
	private Node node;
	private List<VarDecl> idList = new ArrayList<VarDecl>();
	private boolean isSeqRoot;
	
	public ObservedCoverageHelper(Node node) {
		this.node = node;
		idList = populateIdList();
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
			ObservedTreeNode treeRoot = new ObservedTreeNode(root.id);
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
			treeNode = buildTreeRecursively(new ObservedTreeNode(root), expressions, false);
			seqTrees.put(idMap.get(root), new ObservedTree(treeNode));
		}
		
		return seqTrees;
	}
	
	
	/*
	 * Recursively build a subtree given a root.
	 */
	private ObservedTreeNode buildTreeRecursively(ObservedTreeNode root, Map<String, Expr> exprs, 
			boolean isBuildingRef) {
		List<String> ids = getStrIds();
		ArrayList<String> childList = new ArrayList<String>();
		
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
		
//		LustreMain.log(">>>>>> Building tree for " + root.data);
//		LustreMain.log(">>>>>> Expression: " + root.data + " = " + exprs.get(root.data));
		if (exprs.get(root.data).toString().indexOf(" ") > 0) {
			String[] items = exprs.get(root.data).toString().replaceAll("[(){}]", " ").split(" ");
			for (String item : items) {
				item = item.trim();
				if (item.isEmpty()) {
					continue;
				} else if (!ids.contains(item)) {
					continue;
				} else if (childList.contains(item)) {
					continue;
				} else {
					childList.add(item);
				}
			}
		} else {
			childList.add(exprs.get(root.data).toString().replaceAll("[ (){}]", ""));
		}
		
		// add children to current root
		for (int i = 0; i < childList.size(); i++) {
			ObservedTreeNode node = new ObservedTreeNode(childList.get(i).toString());
			root.addChild(node);
			if (exprs.keySet().contains(node.data)) {
				// build the tree recursively if next node is 
				// on the left-hand-side of some expressions
				buildTreeRecursively(node, exprs, isBuildingRef);
			} else {
				// stop recursion if the next node does not depend on other variables
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
	private List<VarDecl> populateIdList() {
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


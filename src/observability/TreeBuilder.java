package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jkind.lustre.VarDecl;
import observability.tree.Tree;
import observability.tree.TreeNode;

public class TreeBuilder {
	/* <lhs_of_equation, <raw_var_in_rhs, <renamed_var, time_seen>>> */
	private Map<String, Map<String, Map<String, Integer>>> observerArithTable = new HashMap<>();
	/* <lhs_of_equation, <raw_var_in_rhs, <renamed_var, time_seen>>> */
	private Map<String, Map<String, Map<String, Integer>>> delayArithTable = new HashMap<>();
	
	private Map<String, VarDecl> ids = new HashMap<>();
	private List<String> outputs = new ArrayList<>();
	
	private final String ARITH_PREFIX = "ArithExpr_";
	
	public TreeBuilder(Map<String, Map<String, Map<String, Integer>>> observerArithTable,
			Map<String, Map<String, Map<String, Integer>>> delayArithTable,
			Map<String, VarDecl> ids, List<String> outputs) {
		this.observerArithTable = observerArithTable;
		this.delayArithTable = delayArithTable;
		this.ids = ids;
		this.outputs = outputs;
	}
	
	public Map<String, Tree> buildObserverTree() {
		Map<String, Tree> trees = new HashMap<>();
		
		for (String strRoot : outputs) {
			TreeNode root = new TreeNode(strRoot, ids.get(strRoot).type);
			Map<String, Integer> renamed = new HashMap<>();
			renamed.put(strRoot, 1);
			root.renamedIds = renamed;
			buildSubTree(root);
			trees.put(strRoot, new Tree(root));
		}
		
		return trees;
	}
	
	public Map<String, Tree> buildDeadRootTree(List<String> deadNodes) {
		Map<String, Tree> trees = new HashMap<>();
		
		for (String strRoot : deadNodes) {
			TreeNode root = new TreeNode(strRoot, ids.get(strRoot).type);
			buildSubTree(root);
			trees.put(strRoot, new Tree(root));
		}
		
		return trees;
	}
	
	public Map<String, Tree> buildDelayTree() {
		Map<String, Tree> trees = new HashMap<>();
				
		for (String rootStr : delayArithTable.keySet()) {
			TreeNode root = new TreeNode(rootStr, ids.get(rootStr).type);
			Map<String, Integer> renamed = new HashMap<>();
			renamed.put(rootStr, 1);
			root.renamedIds = renamed;
			
			Map<String, Map<String, Integer>> variableTable = delayArithTable.get(rootStr);
			
			for (String rawVar : variableTable.keySet()) {
				Map<String, Integer> renamedVars = variableTable.get(rawVar);
				TreeNode child = new TreeNode(rawVar, ids.get(rawVar).type);
				Map<String, Integer> arithNode = new HashMap<>();
				
				for (String varStr : renamedVars.keySet()) {
					arithNode.put(varStr, renamedVars.get(varStr));
					if (varStr.startsWith(ARITH_PREFIX)) {
						child.isArithExpr = true;
					} else {
						child.isArithExpr = false;
					}
				}
				
				child.setRenamedIds(arithNode);
				root.addChild(child);
			}
			
			for (TreeNode subRoot : root.children) {
				buildSubTree(subRoot);
			}
			trees.put(rootStr, new Tree(root));
		}
		
		return trees;
	}

	private void buildSubTree(TreeNode root) {
		if (observerArithTable.get(root.rawId) == null) {
			return;
		}
		Map<String, Map<String, Integer>> variableTable = observerArithTable.get(root.rawId);
		for (String rawVar : variableTable.keySet()) {
			Map<String, Integer> renamedVars = variableTable.get(rawVar);
			TreeNode child = new TreeNode(rawVar, ids.get(rawVar).type);
			Map<String, Integer> arithNode = new HashMap<>();
			
			for (String varStr : renamedVars.keySet()) {
				arithNode.put(varStr, renamedVars.get(varStr));
				if (varStr.startsWith(ARITH_PREFIX)) {
					child.isArithExpr = true;
				} else {
					child.isArithExpr = false;
				}
			}
			
			child.setRenamedIds(arithNode);
			root.addChild(child);
		}
		
//		System.out.println(root.data + " ^^^ " + root.children);
		
		for (TreeNode subRoot : root.children) {
			buildSubTree(subRoot);
		}
	}
}

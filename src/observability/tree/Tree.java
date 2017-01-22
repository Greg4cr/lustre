package observability.tree;

import java.util.List;

public class Tree {
 
    public TreeNode root;
    
    public Tree(TreeNode root) {
    	setroot(root);
    }
 
    private void setroot(TreeNode root) {
    	if (root == null) {
    		throw new IllegalArgumentException("null root!");
    	}
        this.root = root;
    }
    
    // convert the tree into a list
    public List<TreeNode> convertToList() {
    	return this.root.convertToList();
    }
    
    // check if given node contains a direct child
    // with data of given string
    public boolean containsChild(String child) {
    	return this.root.containsChild(child);
    }
    
    public boolean containsNode(String data) {
    	return this.root.containsNode(data);
    }
    
    public List<TreeNode> getAllLeaves() {
    	return this.root.getAllLeafNodes();
    }
}
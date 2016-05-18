package coverage;

import java.util.List;

public class ObservedTree {
 
    public ObservedTreeNode root;
    
    public ObservedTree(ObservedTreeNode root) {
    	setroot(root);
    }
 
    private void setroot(ObservedTreeNode root) {
    	if (root == null) {
    		throw new IllegalArgumentException("null root!");
    	}
        this.root = root;
    }
    
    // convert the tree into a list
    public List<ObservedTreeNode> convertToList() {
    	return this.root.convertToList();
    }
        
}
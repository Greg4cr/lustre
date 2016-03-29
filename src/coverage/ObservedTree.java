package coverage;

import java.util.ArrayList;
import java.util.List;

public class ObservedTree {
 
    private ObservedTreeNode root;
    
    public ObservedTree(ObservedTreeNode root) {
    	setroot(root);
    }
 
    public ObservedTreeNode getroot() {
        return this.root;
    }
 
    public void setroot(ObservedTreeNode root) {
    	if (root == null) {
    		throw new IllegalArgumentException("null root!");
    	}
        this.root = root;
    }
     
}
package coverage;

import java.util.ArrayList;
import java.util.List;

public class ObservedTreeNode {
    public String data;
    public String type;
    public ObservedTreeNode parent;
    public List<ObservedTreeNode> children;
 
    public ObservedTreeNode(String data) {
        setData(data);
    }

    public List<ObservedTreeNode> getChildren() {
        if (this.children == null) {
            return new ArrayList<ObservedTreeNode>();
        }
        return this.children;
    }
 
    public void setChildren(List<ObservedTreeNode> children) {
        this.children = children;
    }
 
    public int getNumberOfChildren() {
        if (children == null) {
            return 0;
        }
        return children.size();
    }
     
    public void addChild(ObservedTreeNode child) {
        if (children == null) {
            children = new ArrayList<ObservedTreeNode>();
        }
        child.parent = this;
        this.children.add(child);
    }
 
    public String getData() {
        return this.data;
    }
 
    public void setData(String data) {
        this.data = data;
    }
    
    // return all leaf nodes of specific node
    public List<String> getAllLeaves() {
    	List<String> leaves = new ArrayList<String>();
    	if (this.children == null) {
            leaves.add(this.data);
        } else {
            for (ObservedTreeNode child : this.children) {
                leaves.addAll(child.getAllLeaves());
            }
        }
    	
    	return leaves;
    }
    
    public void getPaths(List<List<ObservedTreeNode>> paths) {
    	this.getPaths(this, paths, new ArrayList<ObservedTreeNode>());
    }
    
    // return paths from root to each leaf node
    private void getPaths(ObservedTreeNode root, List<List<ObservedTreeNode>> paths,
    							List<ObservedTreeNode> path) {
    	if (root == null) {
    		return;
    	}
		while (root.parent != null 
				&& path.lastIndexOf(root.parent) != path.size() - 1) {
			// remove siblings of parent node
			path.remove(path.size() - 1);
		}
		// append the node to the path right after its parent
		path.add(root);
    	
    	if (root.children == null) {
    		// add one path (root -> node list -> leaf)
    		paths.add(new ArrayList<ObservedTreeNode>(path));
    	} else {
    		for (ObservedTreeNode child : root.children) {
    			child.getPaths(child, paths, path);
    		}
    	}
    }
    
    @Override
    public String toString() {
    	return this.data;
    }
}
package observability.tree;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Type;

public class ObservedTreeNode {
    public String data;
    public Type type;
    public boolean isPre;
    public ObservedTreeNode parent;
    public List<ObservedTreeNode> children;
    public int occurrence;
    public boolean isArithExpr;
    public String arithId;
    
    public ObservedTreeNode(String data, Type type) {
    	setName(data);
    	setType(type);
    	setIsPre(false);
    	setArithId(null);
    	setOccurrence(1);
    	this.parent = null;
    	children = new ArrayList<>();
    }
    
    public ObservedTreeNode(String data, Type type, boolean isPre) {
    	setName(data);
    	setType(type);
    	setIsPre(isPre);
    	setOccurrence(1);
    }

	public void setOccurrence(int occurrence) {
		this.occurrence = occurrence;
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

    public void setName(String data) {
        this.data = data;
    }
    
    public void setType(Type type) {
    	this.type = type;
    }

    public void setIsPre(boolean isPre) {
    	this.isPre = isPre;
    }
    
    public void setIsArithExpr(boolean isArithExpr) {
    	this.isArithExpr = isArithExpr;
    }
    
    public void setArithId(String arithId) {
    	this.arithId = arithId;
    }
    
    // return all leaf nodes of specific node
    public List<ObservedTreeNode> getAllLeafNodes() {
    	List<ObservedTreeNode> leaves = new ArrayList<>();
    	if (this.children == null || this.children.isEmpty()) {
    		leaves.add(this);
    	} else {
    		for (ObservedTreeNode child : this.children) {
    			leaves.addAll(child.getAllLeafNodes());
    		}
    	}

    	return leaves;
    }
    
    public List<ObservedTreeNode> convertToList() {
    	List<ObservedTreeNode> list = new ArrayList<>();
    	convertToList(this, list);
    	return list;
    }
    
    private void convertToList(ObservedTreeNode root, List<ObservedTreeNode> list) {
    	if (root == null) {
    		return;
    	}
    	list.add(root);
    	if (root.children != null) {
	    	for (ObservedTreeNode child : root.children) {
	    		convertToList(child, list);
	    	}
    	}
    }
    
    // return paths from current node to leaf nodes
    public void getPaths(List<List<ObservedTreeNode>> paths) {
    	this.getPaths(this, paths, new ArrayList<ObservedTreeNode>());
    }
    
    // return paths from given node to each leaf node in the subtree
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
		// append the node to the list next to its parent
		path.add(root);
    	
    	if (root.children == null || root.children.isEmpty()) {
    		// add one path (root -> node list -> leaf)
    		paths.add(new ArrayList<ObservedTreeNode>(path));
    	} else {
    		// explore current path
    		for (ObservedTreeNode child : root.children) {
    			child.getPaths(child, paths, path);
    		}
    	}
    }
    
    @Override
    public boolean equals(Object node) {
    	if (node == null) {
    		return false;
    	} else if (! (node instanceof ObservedTreeNode)) {
    		throw new IllegalArgumentException("Wrong type of node!");
    	} else {
    		// node data is the only identifier
    		return this.data.equals(((ObservedTreeNode)node).data);
    	}
    }
        
    @Override
    public String toString() {
    	StringBuilder node = new StringBuilder();
    	
    	node.append("(");
    	node.append("data=").append(this.data).append(", ");
    	node.append("type=").append(this.type.toString()).append(", ");
    	node.append("occur=").append(this.occurrence).append(", ");
    	node.append("isPre=").append(this.isPre);
    	if (this.isArithExpr) {
    		node.append(", ").append("arithId=").append(this.arithId);
    	}
    	node.append(")");
    	
    	return node.toString();
    }
}
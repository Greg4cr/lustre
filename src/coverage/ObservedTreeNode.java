package coverage;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Type;
import jkind.lustre.VarDecl;

public class ObservedTreeNode {
    public String data;
    public Type type;
    public boolean isPre;
    public ObservedTreeNode parent;
    private List<ObservedTreeNode> children;
    private int occurrence;
    
    public ObservedTreeNode(String data, Type type) {
    	setName(data);
    	setType(type);
    	setIsPre(false);
    	setOccurrence(1);
    }
    
    public ObservedTreeNode(String data, Type type, boolean isPre) {
    	setName(data);
    	setType(type);
    	setIsPre(isPre);
    	setOccurrence(1);
    }
    
	public int getOccurrence() {
		return occurrence;
	}

	public void setOccurrence(int occurrence) {
		this.occurrence = occurrence;
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

    public void setName(String data) {
        this.data = data;
    }
    
    public void setType(Type type) {
    	this.type = type;
    }
    
    /*public Type getType() {
    	return this.type;
    }
    
    public ObservedTreeNode getParent() {
    	return this.parent;
    }*/
    
    public void setIsPre(boolean isPre) {
    	this.isPre = isPre;
    }
    
//    // return all leaf nodes of specific node
//    public List<String> getAllLeaves() {
//    	List<String> leaves = new ArrayList<String>();
//    	if (this.children == null) {
//            leaves.add(this.data);
//        } else {
//            for (ObservedTreeNode child : this.children) {
//                leaves.addAll(child.getAllLeaves());
//            }
//        }
//    	
//    	return leaves;
//    }
    
    // return all leaf nodes of specific node
    public List<ObservedTreeNode> getAllLeafNodes() {
    	List<ObservedTreeNode> leaves = new ArrayList<>();
    	if (this.children == null) {
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
    	StringBuilder node = new StringBuilder();
    	
    	node.append("(");
    	node.append(this.data).append(", ");
    	node.append(this.type.toString()).append(", ");
    	node.append(this.occurrence).append(", ");
    	node.append(this.isPre);
    	node.append(")");
    	
    	return node.toString();
    }
}
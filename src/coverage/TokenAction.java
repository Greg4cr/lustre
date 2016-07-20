package coverage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.VarDecl;

public class TokenAction {
	// token states
	final IdExpr token_first = new IdExpr("token_first"); 
	final IdExpr token_init = new IdExpr("token_init");
	final IdExpr token_next = new IdExpr("token_next");
	final IdExpr token = new IdExpr("token");
	final IdExpr token_nondet = new IdExpr("token_nondet");
	
	final IdExpr TOKEN_INIT_STATE = new IdExpr("TOKEN_INIT_STATE");
	final IdExpr TOKEN_ERROR_STATE = new IdExpr("TOKEN_ERROR_STATE");
	final IdExpr TOKEN_OUTPUT_STATE = new IdExpr("TOKEN_OUTPUT_STATE");
	
	List<String> inList = new ArrayList<>();
	// dynamic tokens
	String prefix = "TOKEN_D";
	IdExpr[] tokens;
	int count;
	boolean hasDynamicTokens = false;
	
	// token generator assistants
	// sequential trees (one token to one tree / root)
	HashMap<VarDecl, ObservedTree> sequentialTrees;
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	HashMap<ObservedTreeNode, List<ObservedTreeNode>> rootToLeavesMap = new HashMap<>();
	// token to tree node (root), Map<Token, Node>
	HashMap<IdExpr, ObservedTreeNode> tokenToNode = new HashMap<>();
	// tree node (root) to token, Map<Node, Token>
	HashMap<ObservedTreeNode, IdExpr> nodeToToken = new HashMap<>();
	
	public TokenAction(HashMap<VarDecl, ObservedTree> seqTrees) {
		this.sequentialTrees = seqTrees;
		drawMaps();
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<Obligation>();
		Obligation currentOb;
		Expr preInitExpr, preErrExpr, preFinalExpr;
		Expr transitionExpr = null;
		
		// initialization
		currentOb = new Obligation(token_first, true, 
							new IfThenElseExpr(token_init, token_nondet, TOKEN_INIT_STATE));
		obligations.add(currentOb);
		
		// transitions between dynamic tokens
		if (hasDynamicTokens) {
			transitionExpr = transitions();
			preFinalExpr = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
					BinaryOp.EQUAL, TOKEN_OUTPUT_STATE), TOKEN_OUTPUT_STATE, transitionExpr);
		} else {
			preFinalExpr = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
					BinaryOp.EQUAL, TOKEN_OUTPUT_STATE), TOKEN_OUTPUT_STATE, TOKEN_ERROR_STATE);
		}
		
		preErrExpr = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
											BinaryOp.EQUAL, TOKEN_ERROR_STATE), TOKEN_ERROR_STATE, preFinalExpr);
		preInitExpr = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
											BinaryOp.EQUAL, TOKEN_INIT_STATE), token_first, preErrExpr);
		currentOb = new Obligation(token_next, true, preInitExpr);
		obligations.add(currentOb);
		
		// transition from token_first to token_next
		currentOb = new Obligation(token, true, new BinaryExpr(
							token_first, BinaryOp.ARROW, token_next));
		obligations.add(currentOb);
		
		return obligations;
	}
	
	public void setHasDynamic(boolean hasDynamicTokens) {
		this.hasDynamicTokens = hasDynamicTokens;
	}
	
	private Expr transitions() {
		int len = tokens.length;
		Expr[] transExprs = new Expr[len];
		Expr[] outputTrans = new Expr[len];
		Expr errTrans;
		String observed = "_COMB_OBSERVED", seq = "_SEQ_USED_BY_";
		String id;
		int i = 0;
		
		for (IdExpr sourceToken : tokens) {
			IdExpr targetToken = null;
			ObservedTreeNode sourceNode = tokenToNode.get(sourceToken);
			System.out.println("building transition equation for " + sourceToken.id + " :: " + sourceNode);
			for (ObservedTreeNode targetNode : rootToLeavesMap.get(sourceNode)) {
				System.out.println("create transition ::: (" + sourceNode +") --> (" 
											+ targetNode + ")");

				for (IdExpr tokenId : tokenToNode.keySet()) {
					if (tokenToNode.get(tokenId).data.equals(targetNode.data)) {
						targetToken = tokenId;
						break;
					}
				}
				
				if (targetToken != null) {
					id = targetNode.data + seq + sourceNode.data;

					errTrans = new IfThenElseExpr(new BinaryExpr(
										new BinaryExpr(token_nondet, BinaryOp.EQUAL, targetToken),
										BinaryOp.AND, new IdExpr(id)), targetToken,
										TOKEN_ERROR_STATE);
					
					id = sourceNode.data + observed;
					outputTrans[i] = new IfThenElseExpr(new IdExpr(id), TOKEN_OUTPUT_STATE, errTrans);
				} else {
					id = sourceNode.data + observed;
					outputTrans[i] = new IfThenElseExpr(new IdExpr(id), TOKEN_OUTPUT_STATE, TOKEN_ERROR_STATE);
				}
				System.out.println(outputTrans[i]);
			}
				i++;
		}
		
		// combine expressions (nesting)
		transExprs[len - 1] = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
				BinaryOp.EQUAL, tokens[len - 1]), outputTrans[len - 1], TOKEN_ERROR_STATE);
		
		for (i = len - 2; i >= 0; i--) {
			transExprs[i] = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
					BinaryOp.EQUAL, tokens[i]), outputTrans[i], transExprs[i + 1]);
		}
		
		return transExprs[0];
	}
	
	// build token-to-node, node-to-token, tokennode-dependency maps
	private void drawMaps() {
//		HashMap<VarDecl, ObservedTree> trees = sequentialTrees;
		tokens = new IdExpr[sequentialTrees.size()];
		count = 0;
		
//			System.out.println("============= drawing maps =============");
		for (VarDecl tree : sequentialTrees.keySet()) {
			tokens[count] = new IdExpr(prefix + (count + 1));
			ObservedTreeNode node = sequentialTrees.get(tree).root;
			tokenToNode.put(tokens[count], node);
			nodeToToken.put(node, tokens[count]);
			
			ObservedTreeNode root = sequentialTrees.get(tree).root;
			rootToLeavesMap.put(root, root.getAllLeafNodes());
			
//			System.out.println(count + " token-to-node: [" + tokens[count] + "] - " + tokenToNode.get(tokens[count]));
//			System.out.println(count + " node-to-token: [" + node + "] - " + nodeToToken.get(node));
//			System.out.println(count + " dependency: [" + node + "] >>> " + rootToLeavesMap.get(node));
			count++;
		}
	}
	
	public void setInIdList(List<String> inputs) {
		inList = inputs;
	}
}
package coverage;

import java.util.ArrayList;
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
	
	// dynamic tokens
	String prefix = "TOKEN_D";
	IdExpr[] tokens;
	int count;
	
	// token generator assistants
	// sequential trees (one token to one tree / root)
	HashMap<Node, HashMap<VarDecl, ObservedTree>> sequentialTrees;
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	HashMap<String, List<String>> tokenDependency = new HashMap<String, List<String>>();
	// token to tree node (root), Map<Token, Node>
	HashMap<IdExpr, String> tokenToNode = new HashMap<IdExpr, String>();
	// tree node (root) to token, Map<Node, Token>
	HashMap<String, IdExpr> nodeToToken = new HashMap<String, IdExpr>();
	
	public TokenAction(HashMap<Node, HashMap<VarDecl, ObservedTree>> seqTrees) {
		this.sequentialTrees = seqTrees;
		drawMaps();
	}
	
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<Obligation>();
		Obligation currentOb;
		Expr preInitExpr, preErrExpr, preFinalExpr;
		Expr transitionExpr;
		
		// initialization
		currentOb = new Obligation(token_first, true, 
							new IfThenElseExpr(token_init, token_nondet, TOKEN_INIT_STATE));
		obligations.add(currentOb);
		
		// transitions between dynamic tokens
		transitionExpr = transitions();
		preFinalExpr = new IfThenElseExpr(new BinaryExpr(new UnaryExpr(UnaryOp.PRE, token),
											BinaryOp.EQUAL, TOKEN_OUTPUT_STATE), TOKEN_OUTPUT_STATE, transitionExpr);
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
	
	private Expr transitions() {
		int len = tokens.length;
		Expr[] transExprs = new Expr[len];
		Expr[] outputTrans = new Expr[len];
		Expr errTrans;
		String observed = "_COMB_OBSERVED", seq = "_SEQ_USED_BY_";
		String id;
		int i = 0;
		
		for (IdExpr sourceToken : tokens) {
			String sourceNode = tokenToNode.get(sourceToken);
//			System.out.println("building transition equation for " + sourceToken.id + " :: " + sourceNode);
			for (String targetNode : tokenDependency.get(sourceNode)) {
//				System.out.println(sourceNode + " :: " + targetNode);
				id = sourceNode + seq + targetNode;
				errTrans = new IfThenElseExpr(new BinaryExpr(
						new BinaryExpr(token_nondet, BinaryOp.EQUAL, nodeToToken.get(targetNode)),
						BinaryOp.AND, new IdExpr(id)), nodeToToken.get(targetNode),
						TOKEN_ERROR_STATE);
				
				id = sourceNode + observed;
				outputTrans[i] = new IfThenElseExpr(new IdExpr(id), TOKEN_OUTPUT_STATE, errTrans);
//				System.out.println(outputTrans[i]);
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
		for (Node node : sequentialTrees.keySet()) {
			HashMap<VarDecl, ObservedTree> trees = sequentialTrees.get(node);
			tokens = new IdExpr[trees.size()];
			count = 0;
			
//			System.out.println("============= drawing maps =============");
			for (VarDecl tree : trees.keySet()) {
				tokens[count] = new IdExpr(prefix + (count + 1));
				tokenToNode.put(tokens[count], tree.id);
				nodeToToken.put(tree.id, tokens[count]);
				count++;
				
				ObservedTreeNode root = trees.get(tree).getroot();
				tokenDependency.put(tree.id, root.getAllLeaves());
				
//				System.out.println(count + " token-to-node: " + tokens[count] + " - " + tokenToNode.get(tokens[count]));
//				System.out.println(count + " node-to-token: " + tree.id + " - " + nodeToToken.get(tree.id));
//				System.out.println(count + " dependency: " + tree.id + " >>> " + tokenDependency.get(tree.id));
			}
			
			break; // assume only one node for one input
		}
	}
}
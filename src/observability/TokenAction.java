package observability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import coverage.Obligation;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import observability.tree.Tree;
import observability.tree.TreeNode;

public class TokenAction {
	// token states
	private final IdExpr token_first = new IdExpr("token_first"); 
	private final IdExpr token_init = new IdExpr("token_init");
	private final IdExpr token_next = new IdExpr("token_next");
	private final IdExpr token = new IdExpr("token");
	private final IdExpr token_nondet = new IdExpr("token_nondet");
	
	private final IdExpr TOKEN_INIT_STATE = new IdExpr("TOKEN_INIT_STATE");
	private final IdExpr TOKEN_ERROR_STATE = new IdExpr("TOKEN_ERROR_STATE");
	private final IdExpr TOKEN_OUTPUT_STATE = new IdExpr("TOKEN_OUTPUT_STATE");
	
	// dynamic tokens
	private String prefix = "TOKEN_D";
	private IdExpr[] tokens;
	
	// token generator assistants
	// sequential trees (one token to one tree / root)
	private Map<String, Tree> delayTrees;
	// relationship of tokens (in sequential trees), Map<Root, Leaves>
	private Map<TreeNode, List<TreeNode>> tokenDepTable = new HashMap<>();
	// token to tree node (root), Map<Token, Node>
	private Map<IdExpr, TreeNode> tokenToNode = new HashMap<>();
	// tree node (root) to token, Map<Node, Token>
	private Map<TreeNode, IdExpr> nodeToToken = new HashMap<>();
	
	public TokenAction(Map<String, Tree> delayTrees,
			Map<TreeNode, List<TreeNode>> tokenDepTable,
			Map<IdExpr, TreeNode> tokenToNode,
			Map<TreeNode, IdExpr> nodeToToken,
			IdExpr[] tokens) {
		this.delayTrees = delayTrees;
		this.tokenDepTable = tokenDepTable;
		this.tokenToNode = tokenToNode;
		this.nodeToToken = nodeToToken;
		this.tokens = tokens;
		
//		drawTokenMaps();
//		drawTokenDependantTable();
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
		
		// build transitions between dynamic tokens
		if (! delayTrees.keySet().isEmpty()) {
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
		
	private Expr transitions() {
		int len = tokens.length;
		Expr[] transExprs = new Expr[len];
		Expr[] outputTrans = new Expr[len];
		
		String observed = "_COMB_OBSERVED", seq = "_SEQ_USED_BY_";
		String id = "";
		int i = 0;
		
		for (IdExpr sourceToken : tokens) {
			IdExpr targetToken = null;
			TreeNode sourceNode = tokenToNode.get(sourceToken);

//			System.out.println("possible target of (" + sourceNode + "):\n\t" + tokenDepTable.get(sourceNode));
			
			if (tokenDepTable.get(sourceNode).isEmpty()) {
				id = sourceNode.rawId + observed;
				outputTrans[i] = new IfThenElseExpr(new IdExpr(id), TOKEN_OUTPUT_STATE, TOKEN_ERROR_STATE);
			} else {
				Expr[] condExpr = new Expr[tokenDepTable.get(sourceNode).size()];
				Expr[] elseExpr = new Expr[tokenDepTable.get(sourceNode).size()];
				Expr[] targetTk = new Expr[tokenDepTable.get(sourceNode).size()];
				
				int index = 0;
				for (TreeNode targetNode : tokenDepTable.get(sourceNode)) {
					targetToken = this.nodeToToken.get(targetNode);
					id = sourceNode.rawId + seq + targetNode.rawId;
					
					targetTk[index] = targetToken;
					condExpr[index++] = new BinaryExpr(
							new BinaryExpr(token_nondet, BinaryOp.EQUAL, targetToken),
							BinaryOp.AND, new IdExpr(id));
				}
				
				for (int j = 0; j < index; ++j) {
					if (j == 0) {
						elseExpr[j] = new IfThenElseExpr(condExpr[j], targetTk[j],
								TOKEN_ERROR_STATE);
					} else {
						elseExpr[j] = new IfThenElseExpr(condExpr[j], targetTk[j],
								elseExpr[j - 1]);
					}
				}
				id = sourceNode.rawId + observed;
				outputTrans[i] = new IfThenElseExpr(new IdExpr(id), TOKEN_OUTPUT_STATE, elseExpr[elseExpr.length - 1]);
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

}
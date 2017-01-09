package observability.nodecall;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import types.ExprTypeVisitor;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.NodeCallExpr;
import jkind.lustre.Program;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.VarDecl;
import jkind.lustre.visitors.AstMapVisitor;

/**
 * Update node calls for observability,
 * adding two more arguments to node calls:
 * 		token_nondet and token_init
 */
public class LustreUpdateNodeCallVisitor extends AstMapVisitor {
	private final ExprTypeVisitor exprTypeVisitor;
	private Map<String, Integer> delayMap;
	private int maxTokens = 0;
	
	private final String tokenNondet = "token_nondet";
	private final String tokenInit = "token_init";
	
	public static Program program(Program program, Map<String, Integer> delayMap) {
		return new LustreUpdateNodeCallVisitor(program, delayMap).visit(program);
	}

	private LustreUpdateNodeCallVisitor(Program program, 
							Map<String, Integer> delayMap) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.delayMap = delayMap;
		this.maxTokens = max();
	}

	@Override
	public Program visit(Program program) {
		// Only visit nodes
		List<Node> nodes = visitNodes(program.nodes);
		return new Program(program.location, program.types, 
				program.constants, nodes, program.main);
	}

	@Override
	public Node visit(Node node) {
		this.exprTypeVisitor.setNodeContext(node);
		// node calls could be in equations and assertions
		
		// translate equations
		List<Equation> equations = visitEquations(node.equations);
		// translate assertions
		List<Expr> assertions = visitAssertions(node.assertions);
		
		List<VarDecl> inputs = new ArrayList<>();
		
		inputs.addAll(node.inputs);
		
		if (delayMap.get(node.id) > 0) {
			// add local token definition if there is any
			SubrangeIntType subrange = new SubrangeIntType(BigInteger.valueOf(1), 
					BigInteger.valueOf(this.maxTokens));
			inputs.add(new VarDecl(tokenNondet, subrange));
			inputs.add(new VarDecl(tokenInit, NamedType.BOOL));
		}
		
		// return translate node
		return new Node(node.location, node.id, inputs, node.outputs, node.locals,
				equations, node.properties, assertions, null, null, null);
	}
	
	@Override
	public List<Expr> visitAssertions(List<Expr> exprs) {
		List<Expr> assertions = new ArrayList<>();
		
		for (Expr expr : exprs) {
			assertions.add(expr.accept(this));
		}
		
		return assertions;
	}
	
	@Override
	public Expr visit(NodeCallExpr expr) {
		List<Expr> args = new ArrayList<>();
		
		for (Expr arg : expr.args) {
			args.add(arg.accept(this));
		}
		
		if (this.delayMap.get(expr.node) > 0) {
			Expr token_nondet = new IdExpr(tokenNondet);
			Expr token_init = new IdExpr(tokenInit);
			
			args.add(token_nondet);
			args.add(token_init);
		}
		
		return new NodeCallExpr(expr.location, expr.node, args);
	}
	
	private int max() {
		int max = 0;
		
		for (String node : this.delayMap.keySet()) {
			max = Math.max(max, this.delayMap.get(node));
		}
		
		return max;
	}
	
}

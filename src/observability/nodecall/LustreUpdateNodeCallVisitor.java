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
		// Only visit equations
		List<Equation> equations = visitEquations(node.equations);
		List<VarDecl> inputs = new ArrayList<>();
		
		inputs.addAll(node.inputs);
//		System.out.println("Node ::: " + node.id);
		
		if (delayMap.get(node.id) > 0) {
			// add local token definition if there is any
			SubrangeIntType subrange = new SubrangeIntType(BigInteger.valueOf(1), 
					BigInteger.valueOf(this.maxTokens));
			inputs.add(new VarDecl("token_nondet", subrange));
			inputs.add(new VarDecl("token_init", NamedType.BOOL));
		}
		
//		System.out.println("inputs ::: " + inputs);
		// Get rid of e.realizabilityInputs
		return new Node(node.location, node.id, inputs, node.outputs, node.locals,
				equations, node.properties, node.assertions, null, null, null);
	}

	@Override
	public Expr visit(NodeCallExpr expr) {
		List<Expr> args = new ArrayList<>();
		
		for (Expr arg : expr.args) {
			args.add(arg.accept(this));
		}
		
		if (this.delayMap.get(expr.node) > 0) {
			Expr token_nondet = new IdExpr("token_nondet");
			Expr token_init = new IdExpr("token_init");
			
			args.add(token_nondet);
			args.add(token_init);
		}
		
//		System.out.println("updated: " + args.toString());
		
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

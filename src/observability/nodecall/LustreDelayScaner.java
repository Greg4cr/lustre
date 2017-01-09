package observability.nodecall;

import java.util.HashMap;
import java.util.Map;

import jkind.lustre.Equation;
import jkind.lustre.Node;
import jkind.lustre.Program;
import types.ExprTypeVisitor;

public class LustreDelayScaner {
	private final ExprTypeVisitor exprTypeVisitor;
	
	private LustreDelayScaner(Program program) {
		this.exprTypeVisitor = new ExprTypeVisitor(program);
	}
	
	public static Map<String, Integer> delaysInNode(Program program) {
		return new LustreDelayScaner(program).scan(program);
	}
	
	private Map<String, Integer> scan(Program program) {
		Map<String, Integer> delaysInNode = new HashMap<>();
		LustreDelayCounter counter = new LustreDelayCounter(this.exprTypeVisitor);
		
		for (Node node : program.nodes) {
			int delayNum = 0;
			for (Equation equation : node.equations) {
				delayNum += equation.expr.accept(counter);
			}
			delaysInNode.put(node.id, delayNum);
		}
		
//		System.out.println("<Node, DelayNumber> ::: " + delayInNode);
		return delaysInNode;
	}
}

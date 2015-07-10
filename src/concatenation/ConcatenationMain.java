package concatenation;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Program;
import lustre.LustreTrace;
import main.LustreMain;

public class ConcatenationMain {
	public static List<Expr> constraints = new ArrayList<Expr>();

	public static void main(String[] args) {
		Program program = LustreMain.getProgram("example.lus");

		addConstraints();

		TestConcatenation tc = new TestConcatenation(program);
		LustreTrace testCase = tc.generate();
		System.out.println("length: " + testCase.getLength());
		System.out.println(testCase);
	}

	// Get one constraint
	public static Expr getConstraint() {
		if (constraints.isEmpty()) {
			return null;
		}
		return constraints.remove(0);
	}

	// The set of constraints
	public static void addConstraints() {
		constraints.add(new BinaryExpr(new BinaryExpr(new IdExpr("b"),
				BinaryOp.EQUAL, new IntExpr(3)), BinaryOp.AND, new BinaryExpr(
				new IdExpr("c"), BinaryOp.EQUAL, new IntExpr(13))));
		constraints.add(new BinaryExpr(new BinaryExpr(new IdExpr("b"),
				BinaryOp.EQUAL, new IntExpr(5)), BinaryOp.AND, new BinaryExpr(
				new IdExpr("c"), BinaryOp.EQUAL, new IntExpr(15))));
		constraints.add(new BinaryExpr(new BinaryExpr(new IdExpr("b"),
				BinaryOp.EQUAL, new IntExpr(10)), BinaryOp.AND, new BinaryExpr(
				new IdExpr("c"), BinaryOp.EQUAL, new IntExpr(20))));
	}
}

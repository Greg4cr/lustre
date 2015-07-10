package concatenation;

import java.util.ArrayList;
import java.util.List;

import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.Program;
import main.LustreMain;

public class ConcatenationMain {
	public static List<Expr> constraints = new ArrayList<Expr>();

	public static void main(String[] args) {
		Program program = LustreMain.getProgram("microwave.greg.lus");

		addConstraints();

		TestConcatenation tc = new TestConcatenation(program);
		tc.generate();
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
		constraints
				.add(new IdExpr("DriveToSchool__StartCar__state = executing"));
		constraints.add(new IdExpr(
				"DriveToSchool__SelectStation__state = executing"));
		// The CD requires JKind to actually have an input be false :P
		constraints
				.add(new IdExpr(
						"DriveToSchool__SelectStation__if__7__ep2cp_IfBody__ep2cp_IfElseCase__CD__state = executing"));
		constraints.add(new IdExpr(
				"DriveToSchool__DriveUntilAtSchool__state = executing"));
		// Everything below this line hasn't been reachable yet
		constraints
				.add(new IdExpr(
						"DriveToSchool__DriveUntilAtSchool__while__10__ep2cp_WhileBody__ep2cp_WhileTrue__ep2cp_WhileAction__KeepDriving__HandleRain__state = executing"));
		constraints
				.add(new IdExpr(
						"DriveToSchool__DriveUntilAtSchool__while__10__ep2cp_WhileBody__ep2cp_WhileTrue__ep2cp_WhileAction__KeepDriving__HandleRain__if__11__ep2cp_IfBody__ep2cp_IfThenCase__Concurrence__12__Wipers__state = executing"));
	}
}

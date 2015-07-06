package coverage;

import java.util.List;

import types.ExprTypeVisitor;
import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;

public class LustreCoverage {
	public static Program program(Program program, Coverage coverage,
			Polarity polarity) {
		return new LustreCoverage(program, coverage, polarity).generate();
	}

	private final Program program;
	private final Coverage coverage;
	private final Polarity polarity;
	private final ExprTypeVisitor exprTypeVisitor;

	private LustreCoverage(Program program, Coverage coverage, Polarity polarity) {
		this.program = program;
		this.coverage = coverage;
		this.polarity = polarity;
		this.exprTypeVisitor = new ExprTypeVisitor(program);
	}

	private Program generate() {
		ProgramBuilder builder = new ProgramBuilder();
		builder.addConstants(this.program.constants)
				.addTypes(this.program.types).setMain(this.program.main);
		for (Node node : this.program.nodes) {
			builder.addNode(this.generate(node));
		}
		return builder.build();
	}

	// Generate obligations for a node
	private Node generate(Node node) {
		NodeBuilder builder = new NodeBuilder(node);

		this.exprTypeVisitor.setNodeContext(node);
		CoverageVisitor coverageVisitor = null;
		String coverageType = null;

		// Determine the coverage type
		if (coverage.equals(Coverage.MCDC)) {
			coverageVisitor = new MCDCVisitor(exprTypeVisitor);
			coverageType = "MCDC";
			System.out.println("------------Generating MC/DC obligations\n");
		} else if (coverage.equals(Coverage.CONDITION)) {
			coverageVisitor = new ConditionVisitor(exprTypeVisitor);
			coverageType = "CONDITION";
			System.out
					.println("------------Generating Condition Coverage obligations\n");
		} else if (coverage.equals(Coverage.BRANCH)) {
			coverageVisitor = new BranchVisitor(exprTypeVisitor);
			coverageType = "BRANCH";
			System.out
					.println("------------Generating Branch Coverage obligations\n");
		} else {
			throw new IllegalArgumentException("Unknown coverage: " + coverage);
		}

		// Start generating obligations
		int count = 0;
		for (Equation equation : node.equations) {
			String id = equation.lhs.get(0).id;

			// Concatenate IDs with more than one left-hand variables
			for (int i = 1; i < equation.lhs.size(); i++) {
				id += "_" + equation.lhs.get(i);
			}

			List<Obligation> obligations = equation.expr
					.accept(coverageVisitor);

			for (Obligation obligation : obligations) {
				// Skip if the expression's polarity is different from what
				// we need
				if (polarity.equals(Polarity.TRUE)
						&& !obligation.expressionPolarity) {
					continue;
				}
				if (polarity.equals(Polarity.FALSE)
						&& obligation.expressionPolarity) {
					continue;
				}

				String property = obligation.condition + "_"
						+ (obligation.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_" + coverageType + "_"
						+ (obligation.expressionPolarity ? "TRUE" : "FALSE")
						+ "_" + (count++);

				builder.addLocal(new VarDecl(property, NamedType.BOOL));
				builder.addEquation(new Equation(new IdExpr(property),
						new UnaryExpr(UnaryOp.NOT, obligation.obligation)));
				builder.addProperty(property);
			}
		}
		System.out.println("Number of Obligations: " + count + "\n");
		return builder.build();
	}
}

package coverage;

import java.math.BigInteger;
import java.util.List;

import enums.Coverage;
import enums.Polarity;
import main.LustreMain;
import types.ExprTypeVisitor;
import jkind.lustre.Constant;
import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.NamedType;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.SubrangeIntType;
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;

public final class LustreCoverage {
	// By default, use polarity ALL
	public static Program program(Program program, Coverage coverage) {
		return program(program, coverage, Polarity.ALL);
	}

	public static Program program(Program program, Coverage coverage,
			Polarity polarity) {
		// Remove XOR and boolean equality/inequality
		return new LustreCoverage(LustreCleanVisitor.program(program),
				coverage, polarity).generate();
	}

	private final Program program;
	private final Coverage coverage;
	private final Polarity polarity;
	private final ExprTypeVisitor exprTypeVisitor;
	private int count;
	private int upperbound = 0;
	
	private LustreCoverage(Program program, Coverage coverage, Polarity polarity) {
		this.program = program;
		this.coverage = coverage;
		this.polarity = polarity;
		this.exprTypeVisitor = new ExprTypeVisitor(program);
		this.count = 0;
	}

	private Program generate() {
		String coverageType = coverage.name();
		
		LustreMain.log("------------Generating " + coverageType
				+ " obligations");

		ProgramBuilder builder = new ProgramBuilder();
		builder.addConstants(this.program.constants)
				.addTypes(this.program.types).setMain(this.program.main);
		
		for (Node node : this.program.nodes) {
			builder.addNode(this.generate(node));
		}

		if (coverage == Coverage.OMCDC || coverage == Coverage.OBRANCH
				|| coverage == Coverage.OCONDITION
				|| coverage == Coverage.ODECISION) {
			// add more constants for observed coverage
			Type subrange = new SubrangeIntType(BigInteger.valueOf(-2), BigInteger.valueOf(upperbound));
			builder.addConstant(new Constant("TOKEN_INIT_STATE", subrange, new IntExpr(-2)));
			builder.addConstant(new Constant("TOKEN_ERROR_STATE", subrange, new IntExpr(-1)));
			builder.addConstant(new Constant("TOKEN_OUTPUT_STATE", subrange, new IntExpr(0)));
			
			for (int i = 1; i < upperbound + 1; i++) {
				builder.addConstant(new Constant("TOKEN_D" + i, subrange, new IntExpr(i)));
			}
		}
		
		LustreMain.log("Number of Obligations: " + count);
		return builder.build();
	}

	// Generate obligations for a node
	private Node generate(Node node) {
		LustreMain.log("Node: " + node.id);
		NodeBuilder builder = new NodeBuilder(node);

		CoverageVisitor coverageVisitor = null;
		ObservabilityCoverage observabilityCoverage = null;
		this.exprTypeVisitor.setNodeContext(node);

		// Determine the coverage type
		switch (coverage) {
		case MCDC:
			coverageVisitor = new MCDCVisitor(exprTypeVisitor);
			break;
		case BRANCH:
			coverageVisitor = new BranchVisitor(exprTypeVisitor);
			break;
		case CONDITION:
			coverageVisitor = new ConditionVisitor(exprTypeVisitor);
			break;
		case DECISION:
			coverageVisitor = new DecisionVisitor(exprTypeVisitor);
			break;
		case OMCDC:
		case OBRANCH:
		case OCONDITION:
		case ODECISION:
			observabilityCoverage = new ObservabilityCoverage(node, coverage,
								polarity, exprTypeVisitor);
			break;
		default:
			throw new IllegalArgumentException("Unknown coverage: " + coverage);
		}
		
		if (coverage.name().startsWith("O")) {
			// Start generating obligations
			// for OMCDC, OBRANCH, OCONDITION, ODECISION
			List<Obligation> obligations = observabilityCoverage.generate();
			upperbound = observabilityCoverage.getTokenRange();
			SubrangeIntType subrange = new SubrangeIntType(BigInteger.valueOf(1), 
					BigInteger.valueOf(upperbound));
			
			if (upperbound > 0) {
				// add local token definition if there is any
				builder.addInput(new VarDecl("token_nondet", subrange));
				builder.addInput(new VarDecl("token_init", NamedType.BOOL));
			}
			
			subrange = new SubrangeIntType(BigInteger.valueOf(-2), 
					BigInteger.valueOf(upperbound));
			builder.addLocal(new VarDecl("token", subrange));
			builder.addLocal(new VarDecl("token_first", subrange));
			builder.addLocal(new VarDecl("token_next", subrange));
			
			for (Obligation obligation : obligations) {
				count++;
				// Skip if the expression's polarity is different from what we need
				if (polarity.equals(Polarity.TRUE)
						&& !obligation.expressionPolarity) {
					continue;
				}
				if (polarity.equals(Polarity.FALSE)
						&& obligation.expressionPolarity) {
					continue;
				}
				
				String property = "";
				// keep the rest in original pattern.
				property = obligation.condition;
				if (property.startsWith("property_")) {
					builder.addProperty(property);
				}
				
				if (! property.contains("token")) {
					builder.addLocal(new VarDecl(property, NamedType.BOOL));
				}
				
				builder.addEquation(new Equation(new IdExpr(property),
												obligation.obligation));
			}
		} else {
			// Start generating obligations
			// for MCDC, BRANCH, CONDITION, DECISION
			for (Equation equation : node.equations) {
				String id = null;
				
				if (equation.lhs.isEmpty()) {
					id = "EMPTY";
				} else {
					id = equation.lhs.get(0).id;
				}
	
				// Concatenate IDs with more than one left-hand variables
				for (int i = 1; i < equation.lhs.size(); i++) {
					id += "_" + equation.lhs.get(i);
				}
				
				// obligations/expressions generated by visitors
				List<Obligation> obligations = equation.expr
						.accept(coverageVisitor);
				
				for (Obligation obligation : obligations) {
					// Skip if the expression's polarity is different from what we need
					if (polarity.equals(Polarity.TRUE)
							&& !obligation.expressionPolarity) {
						continue;
					}
					if (polarity.equals(Polarity.FALSE)
							&& obligation.expressionPolarity) {
						continue;
					}
	
					String property = "";
					// keep the rest in original pattern.
					property = obligation.condition + "_"
						+ (obligation.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_" + coverage.name() + "_"
						+ (obligation.expressionPolarity ? "TRUE" : "FALSE")
						+ "_" + (count++);
					
					builder.addProperty(property);
					
					builder.addEquation(new Equation(new IdExpr(property),
							new UnaryExpr(UnaryOp.NOT, obligation.obligation)));
				}
			}
		}
		
		return builder.build();
	}
}
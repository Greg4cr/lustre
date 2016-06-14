package coverage;

import java.util.ArrayList;
import java.util.HashMap;
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
import jkind.lustre.Type;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import jkind.lustre.builders.NodeBuilder;
import jkind.lustre.builders.ProgramBuilder;

public final class LustreCoverage {
	int upperbound = 0;
	HashMap<String, List<String>> delayMap = new HashMap<>();
	
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

		if (coverage.equals(Coverage.OMCDC)) {
			// add more constants for observed coverage
			Type type = new NamedType("subrange[-2," + upperbound + "] of int");
			builder.addConstant(new Constant("TOKEN_INIT_STATE", type, new IntExpr(-2)));
			builder.addConstant(new Constant("TOKEN_ERROR_STATE", type, new IntExpr(-1)));
			builder.addConstant(new Constant("TOKEN_OUTPUT_STATE", type, new IntExpr(-0)));
			
			for (int i = 1; i < upperbound + 1; i++) {
				builder.addConstant(new Constant("TOKEN_D" + i, type, new IntExpr(i)));
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
		case OMCDC: // for OMCDC coverage. Meng
			coverageVisitor = new OMCDCVisitor(exprTypeVisitor, program.nodes); 
			break;
		case OBRANCH:
			break;
		case OCONDITION:
			break;
		case ODECISION:
			break;
		default:
			throw new IllegalArgumentException("Unknown coverage: " + coverage);
		}
		
		// Start generating obligations
		// non-observed & comb_used_by obligations
		for (Equation equation : node.equations) {
			String id = null;
//			System.out.println("dealing with equation:\n\t" + equation.toString());
			
			if (equation.lhs.isEmpty()) {
				id = "EMPTY";
			} else {
				id = equation.lhs.get(0).id;
			}

			// Concatenate IDs with more than one left-hand variables
			for (int i = 1; i < equation.lhs.size(); i++) {
				id += "_" + equation.lhs.get(i);
			}
			
			if (coverage == coverage.OMCDC) {
				// A = B; or A = (not B);
				if (equation.expr instanceof IdExpr
						|| ((equation.expr instanceof UnaryExpr)
								&& ((UnaryExpr)equation.expr).expr instanceof IdExpr)) {
					((OMCDCVisitor) coverageVisitor).setIsDef(true);
				} else {
					((OMCDCVisitor) coverageVisitor).setIsDef(false);
				}
			}
			
			List<Obligation> obligations = equation.expr
					.accept(coverageVisitor);
			
			if (coverage == coverage.OMCDC) {
				// popular delay maps for observed coverage
				List<String> delayedItems = new ArrayList<>(); 
				delayedItems.addAll(((OMCDCVisitor)coverageVisitor).getDelayList());
				
				if (delayedItems != null && !delayedItems.isEmpty()) {
					delayMap.put(id, delayedItems);
					System.out.println("<" + id + ">\t" + delayMap.get(id));
					((OMCDCVisitor)coverageVisitor).resetDelayList();
				}
			}
			
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

				String property = "";
				if (coverage == coverage.OMCDC) {
					// name pattern for observed coverage. Meng
					property = obligation.condition + "_COMB_USED_BY_" + id;// + "_" + (count++);
					count++;
					builder.addEquation(new Equation(new IdExpr(property), 
								obligation.obligation));
				} else {
					// keep the rest in original pattern.
					property = obligation.condition + "_"
						+ (obligation.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_" + coverage.name() + "_"
						+ (obligation.expressionPolarity ? "TRUE" : "FALSE")
						+ "_" + (count++);
					
					builder.addEquation(new Equation(new IdExpr(property),
							new UnaryExpr(UnaryOp.NOT, obligation.obligation)));
				}

				builder.addLocal(new VarDecl(property, NamedType.BOOL));
				builder.addProperty(property);
			}
		}
		
		if (coverage == coverage.OMCDC) {
			// obligations for observed coverage only. Meng
			String property = "";
			
			System.out.println("******** Delays *******");
			for (String key : delayMap.keySet()) {
				System.out.println("\"" + key + "\" " + delayMap.get(key));
			}
			
			// set delay mapping
			((OMCDCVisitor)coverageVisitor).setDelayMap(delayMap);
			
			List<Obligation> obligations = ((OMCDCVisitor) coverageVisitor).generate();
			count += obligations.size();
			upperbound = ((OMCDCVisitor) coverageVisitor).getTokenRange();
			StringBuilder subrange = new StringBuilder();
			subrange.append("subrange");
			
			if (upperbound > 0) {
				// add local token definition if there is any
				subrange.append("[").append("1,").append(upperbound).append("]");
				builder.addInput(new VarDecl("token_nondet", new NamedType(subrange.toString() + " of int")));
				builder.addInput(new VarDecl("token_init", NamedType.BOOL));
				subrange.delete(subrange.indexOf("["), subrange.length());
			}
			subrange.append("[").append("-2,").append(upperbound).append("] of int");
			
			for (Obligation obligation : obligations) {
				property = obligation.condition;// + "_" + (count++);
				builder.addEquation(new Equation(new IdExpr(property), obligation.obligation));
				if (property.contains("token_")) {
					builder.addLocal(new VarDecl(property, new NamedType(subrange.toString())));
				} else if (!property.contains("token")){
					builder.addLocal(new VarDecl(property, NamedType.BOOL));
				}
				builder.addProperty(property);
			}
			
			// token, token_first, and token_next should be added in all cases.
			builder.addLocal(new VarDecl("token", new NamedType(subrange.toString())));
			builder.addLocal(new VarDecl("token_first", new NamedType(subrange.toString())));
			builder.addLocal(new VarDecl("token_next", new NamedType(subrange.toString())));
			
		}
		
		return builder.build();
	}
}

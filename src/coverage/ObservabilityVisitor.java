package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import enums.Coverage;
import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.Node;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;
import observability.AffectAtCaptureEquation;
import observability.CombObservedEquation;
import observability.ObservedCoverageObligation;
import observability.SequentialEquation;
import observability.TokenAction;
import observability.tree.ObservedTree;

/* Generate Observed Obligations for MC/DC, CONDITION, BRANCH, and DECISION coverage  */
public class ObservabilityVisitor extends ConditionVisitor {
	Node node;
	ObservabilityHelper obHelper;
	Coverage coverage; // OMCDC, OCONDITION, OBRANCH, ODECISION
	
	String addition = "_COMB_USED_BY_";
	String property = "";
	IdExpr parent;
	boolean isDef = false;
	HashMap<String, VarDecl> idList;
	List<String> properties = new ArrayList<String>();
	
	// variables for delays
	Boolean isDelay = false;
	Boolean assignToDelay = false;
	List<String> impactedByDelay = new ArrayList<>();
	HashMap<String, Integer> timesSeen = new HashMap<>();
	// root vs. first-level nodes
	HashMap<String, List<String>> delayMap = new HashMap<>();
	
	// delay dependency tree and reference dependency tree
	HashMap<VarDecl, ObservedTree> delayDependencyTrees = new HashMap<>();
	HashMap<VarDecl, ObservedTree> refDependencyTrees = new HashMap<>();
	HashMap<String, HashMap<String, Integer>> obligationMap = new HashMap<>();
		
	public ObservabilityVisitor(ExprTypeVisitor exprTypeVisitor, Node node) {
		super(exprTypeVisitor);
		this.node = node; 
		this.obHelper = new ObservabilityHelper(node);
		// default coverage: OMCDC
		this.coverage = Coverage.OMCDC;
	}
	
	public ObservabilityVisitor(ExprTypeVisitor exprTypeVisitor, 
									Node node, Coverage coverage) {
		super(exprTypeVisitor);
		this.node = node;
		this.obHelper = new ObservabilityHelper(node);
		this.coverage = coverage;
	}
	
	// main entrance to get observed obligations
	public List<Obligation> generate() {
		List<Obligation> obligations = new ArrayList<>();
		// need info from Obligation.arithExprByExpr 
		// and Obligation.arithExprById to build trees, 
		// which will be filled by the four coverage Visitors,
		// therefore, get non-observed obligations first.
		obligations.addAll(getCoverageObligation(exprTypeVisitor));

		obHelper.setDelayMap(delayMap);
		delayDependencyTrees = obHelper.buildSeqTrees();
		refDependencyTrees = obHelper.buildRefTrees();
		
		obligations.addAll(getCombObervedObligations());
		obligations.addAll(getSeqUsedByObligations());
		obligations.addAll(getTokenActions());
		obligations.addAll(getAffectAtCaptureObligations()); 
		obligations.addAll(getObligations());
		
		return obligations;
	}
	
	public int getTokenRange() {
		return obHelper.getTokenNumber();
	}
	
	public void setIsDef(boolean isDef) {
		this.isDef = isDef;
	}
	
	public void resetDelayList() {
		this.impactedByDelay.clear();
	}
	
	public List<String> getDelayList() {
		return this.impactedByDelay;
	}
	
	public void setDelayMap(HashMap<String, List<String>> delayMap) {
		this.delayMap = delayMap;
	}

	@Override
	public List<Obligation> visit(BinaryExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		
		List<Obligation> leftObs = expr.left.accept(this);
		List<Obligation> rightObs = expr.right.accept(this);
		
		// and
		// for one opr not be masked, the other one must be true
		if (expr.op.equals(BinaryOp.AND)) {
			for (Obligation leftOb : leftObs) {
				if (expr.left instanceof IdExpr 
						|| (expr.left instanceof UnaryExpr 
								&& ((UnaryExpr) expr.left).op.equals(UnaryOp.NOT))) {
					// A and subexpr, or
					// (not A) and subexpr
					leftOb.obligation = expr.right;
				} else {
					// nesting
					leftOb.obligation = new BinaryExpr(leftOb.obligation,
							BinaryOp.AND, expr.right);
				}
			}
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr 
						|| (expr.right instanceof UnaryExpr 
								&& ((UnaryExpr) expr.right).op.equals(UnaryOp.NOT))) {
					// subexpr and A, or
					// subexpr and (not A)
					rightOb.obligation = expr.left;
				} else {
					// nesting
					rightOb.obligation = new BinaryExpr(expr.left, BinaryOp.AND,
							rightOb.obligation);
				}
			}
			
		}
		// or
		// for one opr not be masked, the other one must be false
		else if (expr.op.equals(BinaryOp.OR)) {
			for (Obligation leftOb : leftObs) {
				if (expr.left instanceof IdExpr
						|| (expr.left instanceof UnaryExpr
								&& ((UnaryExpr) expr.left).op.equals(UnaryOp.NOT))) {
					// A or subexpr
					// (not A) or subexpr
					leftOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.right);
				} else {
					// nesting
					leftOb.obligation = new BinaryExpr(leftOb.obligation,
							BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
				}
			}
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr
						|| (expr.right instanceof UnaryExpr
								&& ((UnaryExpr) expr.right).op.equals(UnaryOp.NOT))) {
					// subexpr or A
					// subexpr or (not A)
					rightOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.left);
				} else {
					// nesting
					rightOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
							expr.left), BinaryOp.AND, rightOb.obligation);
				}
			}
		} 
		
		// >, >=, <, <=
		// +, -, *, /, div, %
		// ==, <>
		// opr is never masked
		else if (expr.op.equals(BinaryOp.GREATER)
				|| expr.op.equals(BinaryOp.GREATEREQUAL)
				|| expr.op.equals(BinaryOp.LESS)
				|| expr.op.equals(BinaryOp.LESSEQUAL)
				|| expr.op.equals(BinaryOp.PLUS)
				|| expr.op.equals(BinaryOp.MINUS)
				|| expr.op.equals(BinaryOp.MULTIPLY)
				|| expr.op.equals(BinaryOp.DIVIDE)
				|| expr.op.equals(BinaryOp.INT_DIVIDE)
				|| expr.op.equals(BinaryOp.MODULUS)
				// Equation of (bool_A = bool_B) has been translated into AND/OR equation
				// The EQUAL equations we meet here must be in forms of (int_A = num) or (num = int_B)
				// And the expression itself is a boolean expression and thus a condition
				// Similar to <>
				|| expr.op.equals(BinaryOp.EQUAL)
				|| expr.op.equals(BinaryOp.NOTEQUAL)) {
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BoolExpr(true);
			}
			for (Obligation rightOb : rightObs) {
				rightOb.obligation = new BoolExpr(true);
			}
		}
		
		// a => b
		// it can be treated as an OR expression (!a or b)
		else if (expr.op.equals(BinaryOp.IMPLIES)) {
			for (Obligation leftOb : leftObs) {
				if (expr.left instanceof IdExpr
						|| (expr.left instanceof UnaryExpr
								&& ((UnaryExpr) expr.left).op.equals(UnaryOp.NOT))) {
					leftOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.right);
				} else {
					leftOb.obligation = new BinaryExpr(leftOb.obligation,
							BinaryOp.AND, new UnaryExpr(UnaryOp.NOT, expr.right));
				}
			}
			
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr
						|| (expr.right instanceof UnaryExpr
								&& ((UnaryExpr) expr.right).op.equals(UnaryOp.NOT))) {
					rightOb.obligation = new UnaryExpr(UnaryOp.NOT, expr.left);
				} else {
					rightOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT,
							expr.left), BinaryOp.AND, rightOb.obligation);
				}
			}				
		}
		
		// expr_a -> expr_b
		else if (expr.op.equals(BinaryOp.ARROW)) {
			// prepare for ((not (...)) -> ...)
			if (expr.left instanceof UnaryExpr
					&& (((UnaryExpr)expr.left).op.equals(UnaryOp.NOT))
					&& ((UnaryExpr)expr.left).expr instanceof BinaryExpr) {
				BinaryExpr subexpr = ((BinaryExpr)((UnaryExpr)expr.left).expr);
				leftObs = this.visit(subexpr);
			}
			
			// prepare for (... -> (not (...)))
			if (expr.right instanceof UnaryExpr
					&& (((UnaryExpr)expr.right).op.equals(UnaryOp.NOT))
					&& ((UnaryExpr)expr.right).expr instanceof BinaryExpr) {
				BinaryExpr subexpr = ((BinaryExpr)((UnaryExpr)expr.right).expr);
				rightObs = this.visit(subexpr);
			}
			
			// generate obligations
			for (Obligation leftOb : leftObs) {
				leftOb.obligation = new BinaryExpr(new BoolExpr(true),
						BinaryOp.ARROW, new BoolExpr(false));
			}
			
			for (Obligation rightOb : rightObs) {
				if (expr.right instanceof IdExpr) {
					// ... -> A
					rightOb.obligation = new BinaryExpr(new BoolExpr(false),
							BinaryOp.ARROW, new BoolExpr(true));
				} else if (expr.right instanceof UnaryExpr
						&& ((UnaryExpr)expr.right).op.equals(UnaryOp.NOT)
						&& ((UnaryExpr)expr.right).expr instanceof IdExpr) {
					// ... -> (not A)
					rightOb.obligation = new BinaryExpr(new BoolExpr(false),
							BinaryOp.ARROW, new BoolExpr(true));
				} else if (expr.right instanceof UnaryExpr
						&& ((UnaryExpr)expr.right).op.equals(UnaryOp.PRE)) {
					// ... -> (pre A)
					// ... -> (pre ...)
					rightOb.obligation = new BoolExpr(false);
				} else {
					// expr.right is an instance of BinaryExpr
					// A = (not (C and D) -> (not (E or F)));
					// A = (C or D -> (E and F));
					// NOTE: not (...) has been processed
					// at the beginning of this scenario (... -> ...)
					rightOb.obligation = new BinaryExpr(new BoolExpr(false),
							BinaryOp.ARROW, rightOb.obligation);
				}
			}
		}
		
		// xor
		else if (expr.op.equals(BinaryOp.XOR)) {
			throw new IllegalArgumentException(
					"XOR should have been translated.");
		}
		
		obligations.addAll(leftObs);
		obligations.addAll(rightObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(IfThenElseExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		
		if (expr.cond instanceof IdExpr
				|| (expr.cond instanceof UnaryExpr
						&& ((UnaryExpr)expr.cond).expr instanceof IdExpr)) {
			// if (A) else subexpr, or if (not A) else subexpr
			setIsDef(true);
		} else {
			setIsDef(false);
		}
		obligations.addAll(expr.cond.accept(this));
		setIsDef(false);
		
		List<Obligation> thenObs = expr.thenExpr.accept(this);
		List<Obligation> elseObs = expr.elseExpr.accept(this);
		
		for (Obligation thenOb : thenObs) {
			
			if ((expr.thenExpr instanceof IdExpr)
					|| (expr.thenExpr instanceof UnaryExpr
						&& ((UnaryExpr)expr.thenExpr).op.equals(UnaryOp.NOT)
						&& ((UnaryExpr)expr.thenExpr).expr instanceof IdExpr)) {
				thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND, new BoolExpr(true));
			} else {
				thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND, thenOb.obligation);
			}
			
		}
		
		for (Obligation elseOb : elseObs) {
			if ((expr.elseExpr instanceof IdExpr)
					|| (expr.elseExpr instanceof UnaryExpr
							&& ((UnaryExpr)expr.elseExpr).op.equals(UnaryOp.NOT)
							&& ((UnaryExpr)expr.elseExpr).expr instanceof IdExpr)) {
				elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT, expr.cond), BinaryOp.AND, new BoolExpr(true));
			} else {
				elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT, expr.cond),
						BinaryOp.AND, elseOb.obligation);
			}
		}
		
		obligations.addAll(thenObs);
		obligations.addAll(elseObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(UnaryExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		List<Obligation> unaryObs = expr.expr.accept(this);
		
		for (Obligation unaryOb : unaryObs) {
			if (isDef) {
				unaryOb.obligation = new BoolExpr(true);
			} else {
				if (expr.op.equals(UnaryOp.PRE)) {
					unaryOb.obligation = new BoolExpr(false);
					impactedByDelay.add(expr.expr.toString());
				}
				else { // NOT
					// keep original value
				}
			}
		}
		
		obligations.addAll(unaryObs);
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(IdExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		
		if (isDef) {
			// definition, A = B or A = not B
			obligations.add(new Obligation(expr, true, new BoolExpr(true)));
		} else {
			obligations.add(new Obligation(expr, true, expr));
		}
		
		return obligations;
	}
	
	// get coverage obligations (without prefix NOTs), using original APIs
	// and populate obligationMap for coverage obligation generation
	private List<Obligation> getCoverageObligation(ExprTypeVisitor exprTypeVisitor) {
		CoverageVisitor coverageVisitor = null;
		List<Obligation> obligations = new ArrayList<>();
		
		switch (coverage) {
		case OMCDC:
			coverageVisitor = new MCDCVisitor(exprTypeVisitor);
			break;
		case OCONDITION:
			coverageVisitor = new ConditionVisitor(exprTypeVisitor);
			break;
		case OBRANCH:
			coverageVisitor = new BranchVisitor(exprTypeVisitor);
			break;
		case ODECISION:
			coverageVisitor = new DecisionVisitor(exprTypeVisitor);
			break;
		default:
			throw new IllegalArgumentException("Incorrect coverage: " + coverage);
		}
		
		for (Equation equation : node.equations) {
			List<Obligation> obs = equation.expr.accept(coverageVisitor);
			
			String id = null;
			HashMap<String, Integer> conditions = new HashMap<>();
			
			if (equation.lhs.isEmpty()) {
				id = "EMPTY";
			} else {
				id = equation.lhs.get(0).id;
			}

			// Concatenate IDs with more than one left-hand variables
			for (int i = 1; i < equation.lhs.size(); i++) {
				id += "_" + equation.lhs.get(i);
			}
			
			property = "";
			
			// count occurrence of each var (ob.condition)
			// in the equation for the usage of rename
			for (Obligation ob : obs) {
				if (conditions.containsKey(ob.condition)) {
					conditions.put(ob.condition, conditions.get(ob.condition) + 1);
				} else {
					conditions.put(ob.condition, 1);
				}
			}
			
			HashMap<String, Integer> handled = new HashMap<>();
			int i;
			
			// generate obligations
			for (Obligation ob : obs) {
				i = 0;
				
				int occ = conditions.get(ob.condition);
					String property = ob.condition;
				
					if (occ > 2) {
						if (handled.containsKey(ob.condition)) {
							i = handled.get(ob.condition) + 1;
						}
						property = property + "_" + (i / 2);
					}
					
					handled.put(ob.condition, i);
										
					property = property + "_"
							+ (ob.polarity ? "TRUE" : "FALSE") + "_AT_"
							+ id + "_" + coverage.name() + "_"
							+ (ob.polarity ? "TRUE" : "FALSE");
					
					Obligation currentOb = new Obligation(new IdExpr(property), true, ob.obligation);
					obligations.add(currentOb);
			}
			
			// populate map
			obligationMap.put(id, conditions);
		}
		
		return obligations;
	}
		
	// generate COMB_OBSERVED expressions
	private List<Obligation> getCombObervedObligations() {
		CombObservedEquation combObsEquation = new CombObservedEquation();
		combObsEquation.setSingleNodeList(obHelper.getSingleNodeList(refDependencyTrees));
		return combObsEquation.generate(refDependencyTrees, obHelper.getIdList());
		
	}
	// generate SEQ_USED_BY expressions
	private List<Obligation> getSeqUsedByObligations() {
		SequentialEquation delayDepdnEquation = new SequentialEquation();
		return delayDepdnEquation.generate(delayDependencyTrees);
	}
	
	// generate TOKEN Actions
	private List<Obligation> getTokenActions() {
		TokenAction tokenAction = new TokenAction(delayDependencyTrees);
		tokenAction.setInIdList(obHelper.getInStrList());
		
		boolean dynamic = (delayDependencyTrees.size() > 0);
		tokenAction.setHasDynamic(dynamic);
		
		return tokenAction.generate();
	}
	
	// generate affecting_at_capture expressions
	private List<Obligation> getAffectAtCaptureObligations() {
		AffectAtCaptureEquation affect = new AffectAtCaptureEquation(delayDependencyTrees,
				refDependencyTrees, delayMap, coverage);
		affect.setSingleNodeList(obHelper.getSingleNodeList(refDependencyTrees));
		affect.setSingleNodeTrees(obHelper.getSingleNodeTrees());
		return affect.generate();
	}
	
	// generate omcdc obligations for each expression
	private List<Obligation> getObligations() {
		ObservedCoverageObligation obligation = new ObservedCoverageObligation(obligationMap, coverage);
		return obligation.generate();
	}
}

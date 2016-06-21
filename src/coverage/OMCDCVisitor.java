package coverage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import enums.Polarity;
import types.ExprTypeVisitor;
import jkind.lustre.BinaryExpr;
import jkind.lustre.BinaryOp;
import jkind.lustre.BoolExpr;
import jkind.lustre.Equation;
import jkind.lustre.Expr;
import jkind.lustre.IdExpr;
import jkind.lustre.IfThenElseExpr;
import jkind.lustre.IntExpr;
import jkind.lustre.Node;
import jkind.lustre.Program;
import jkind.lustre.UnaryExpr;
import jkind.lustre.UnaryOp;
import jkind.lustre.VarDecl;

public class OMCDCVisitor extends ConditionVisitor {
	Node node;
	ObservedCoverageHelper obHelper;
	MCDCVisitor mcdcVisitor;
	
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
		
	public OMCDCVisitor(ExprTypeVisitor exprTypeVisitor, Node node) {
		super(exprTypeVisitor);
		this.node = node;
		this.obHelper = new ObservedCoverageHelper(node);
	}
	
	// main entrance to get OMCDC obligations
	public List<Obligation> generate() {
		obHelper.setDelayMap(delayMap);
		delayDependencyTrees = obHelper.buildSeqTrees();
		refDependencyTrees = obHelper.buildRefTrees();
		
		List<Obligation> obligations = new ArrayList<>();
		
//		obligations.addAll(getMCDCObligation(exprTypeVisitor)); // done
//		obligations.addAll(getCombObervedObligations());  // done
//		obligations.addAll(getSeqUsedByObligations());
//		obligations.addAll(getTokenActions());	// done
//		// FIXME
		obligations.addAll(getAffectAtCaptureObligations()); // bugs to fix
//		obligations.addAll(getObligations());
		
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
		
//		System.out.println("for expression: " + expr.toString());
//		System.out.println("expr.left :: " + expr.left + "; expr.right :: " + expr.right);
//		System.out.println("class of left :: " + expr.left.getClass() + "; class of right :: " + expr.right.getClass());
//		System.out.println("leftObs :: " + leftObs + "; rightObs :: " + rightObs);
		
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
//			System.out.println("ARROW:\t" + expr.toString());
			
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
//			System.out.println("thenOb >>>>> " + thenOb.toString());
			thenOb.obligation = new BinaryExpr(expr.cond, BinaryOp.AND, thenOb.obligation);
		}
		
		for (Obligation elseOb : elseObs) {
//			System.out.println("elseOb >>>>> " + elseOb.toString());
			elseOb.obligation = new BinaryExpr(new UnaryExpr(UnaryOp.NOT, expr.cond),
					BinaryOp.AND, elseOb.obligation);
		}
		
		obligations.addAll(thenObs);
		obligations.addAll(elseObs);
		
		return obligations;
	}
	
	@Override
	public List<Obligation> visit(UnaryExpr expr) {
		List<Obligation> obligations = new ArrayList<>();
		List<Obligation> unaryObs = expr.expr.accept(this);
		
//		System.out.println("unary.expr :: " + unaryObs.toString());
		
		for (Obligation unaryOb : unaryObs) {
			if (isDef) {
				unaryOb.obligation = new BoolExpr(true);
			} else {
				if (expr.op.equals(UnaryOp.PRE)) {
					unaryOb.obligation = new BoolExpr(false);
					System.out.println("add " + expr.expr.toString());
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
	
	// get MCDC obligations (without prefix NOTs), using original APIs
	private List<Obligation> getMCDCObligation(ExprTypeVisitor exprTypeVisitor) {
		mcdcVisitor = new MCDCVisitor(exprTypeVisitor);
		List<Obligation> obligations = new ArrayList<>();
		for (Equation equation : node.equations) {
			List<Obligation> obs = equation.expr.accept(mcdcVisitor);
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
			
			String property;
			for (Obligation ob : obs) {
				property = ob.condition + "_"
						+ (ob.polarity ? "TRUE" : "FALSE") + "_AT_"
						+ id + "_MCDC_"
						+ (ob.expressionPolarity ? "TRUE" : "FALSE");
				properties.add(property);
				Obligation currentOb = new Obligation(new IdExpr(property), false, ob.obligation);
				obligations.add(currentOb);
			}
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
		
		if (delayDependencyTrees.size() > 0) {
			tokenAction.setHasDynamic(true);
		} else {
			tokenAction.setHasDynamic(false);
		}
		return tokenAction.generate();
	}
	
	// generate affecting_at_capture expressions
	private List<Obligation> getAffectAtCaptureObligations() {
		AffectAtCaptureEquation affect = new AffectAtCaptureEquation(delayDependencyTrees,
				refDependencyTrees);
		affect.setSingleNodeList(obHelper.getSingleNodeList(refDependencyTrees));
		affect.setSingleNodeTrees(obHelper.getSingleNodeTrees());
		return affect.generate();
	}
	
	// generate omcdc obligations for each expression
	private List<Obligation> getObligations() {
		OMCDCObligation obligation = new OMCDCObligation(delayDependencyTrees,
				refDependencyTrees);
		return obligation.generate();
	}
}

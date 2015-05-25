package blog.engine.pbvi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blog.DBLOGUtil;
import blog.bn.BayesNetVar;
import blog.bn.DerivedVar;
import blog.bn.RandFuncAppVar;
import blog.engine.onlinePF.inverseBucket.UBT;
import blog.model.ArgSpec;
import blog.model.EqualityFormula;
import blog.model.BuiltInTypes;
import blog.model.DecisionEvidenceStatement;
import blog.model.DecisionFunction;
import blog.model.Evidence;
import blog.model.FuncAppTerm;
import blog.model.RandomFunction;
import blog.model.SkolemConstant;
import blog.model.SymbolEvidenceStatement;
import blog.model.Term;
import blog.model.ValueEvidenceStatement;

/**
 * This is a wrapper for evidence 
 * to represent lifted actions and observations. 
 * For now, it'll just be time independent.
 */
public class LiftedEvidence {
	private boolean debug = false;
	private static Term emptyTimestep;
	
	static {
		emptyTimestep = BuiltInTypes.REAL.getCanonicalTerm(Float.NaN);
	}
	
	// The original ground evidence and terms that can be replaced via lifting
	private Evidence originalGroundEvidence;
	private Set<Object> originalTerms;
	private Term originalTimestep;
	
	private Evidence evidence; // The part of the evidence without any non guaranteed objects
	private LiftedProperties prevLiftedProperties; // This is the history of lifted properties before this evidence.
	private LiftedProperties liftedProperties; // This includes the current and historical lifted properties.

	public LiftedEvidence(Evidence evidence) {
		this(evidence, null);
	}
	
	/**
	 * If there's no lifted history given, this evidence will have its timestep replaced by 0 
	 * (representing any t so that it's time agnostic).
	 * 
	 * Otherwise, if lifted history is given, 
	 * evidence containing ground objects are replaced by LiftedProperties
	 * 
	 * @param evidence
	 */
	public LiftedEvidence(Evidence evidence, LiftedProperties liftedHistory) {
		liftedProperties = new LiftedProperties();
		originalGroundEvidence = evidence;

		int timestep = 0;
		Set<? extends BayesNetVar> evidenceVars = evidence.getEvidenceVars();
		Evidence newEvidence = new Evidence();
		if (evidenceVars.size() > 0) {
			for (BayesNetVar var : evidenceVars) {
				timestep = DBLOGUtil.getTimestepIndex(var);
				if (timestep >= 0) break;
			}
			
			if (timestep < 0) {
				System.out.println("Evidence has no timestep? " + evidence);
				new Exception().printStackTrace();
				System.exit(0);
			}
		}
		Term toReplace = BuiltInTypes.TIMESTEP.getCanonicalTerm(BuiltInTypes.TIMESTEP.getGuaranteedObject(timestep));	
		originalTimestep = toReplace;
		if (liftedHistory == null)
			evidence = evidence.replace(toReplace, emptyTimestep);
		
		Collection<ValueEvidenceStatement> ves = evidence.getValueEvidence();
		Collection<DecisionEvidenceStatement> des = evidence.getDecisionEvidence(); //TODO
		Collection<Object> statements = new ArrayList<Object>(ves);
		statements.addAll(des);
		
		Set<DerivedVar> liftedVars = new HashSet<DerivedVar>();
		
		if (UBT.liftedPbvi) {
			for (Object stmt : statements) {
				BayesNetVar var = null; 
				Object value = null; 

				if (stmt instanceof ValueEvidenceStatement) {
					var = ((ValueEvidenceStatement) stmt).getObservedVar();
					value = ((ValueEvidenceStatement) stmt).getObservedValue();
				} else if (stmt instanceof DecisionEvidenceStatement) {
					var = ((DecisionEvidenceStatement) stmt).getObservedVar();
					value = ((DecisionEvidenceStatement) stmt).getObservedValue();
				} else {
					System.err.println("LiftedEvidence: " + stmt + " not a ValueEvidenceStatement or DecisionEvidenceStatement");
					System.exit(1);
				}

				if (var instanceof DerivedVar) {
					ArgSpec argSpec = ((DerivedVar) var).getArgSpec();
					if (argSpec instanceof EqualityFormula) {
						argSpec = ((EqualityFormula) argSpec).getTerm1();
						liftedVars.add((DerivedVar) var);
						continue;
					} 
					if (!(argSpec instanceof FuncAppTerm)) {
						System.out.println("not a func app" + argSpec);
						continue;
					}

					FuncAppTerm term = (FuncAppTerm) argSpec;
					RandomFunction function = null;
					if (term.getFunction() instanceof blog.model.RandomFunction)
						function = (RandomFunction) term.getFunction();
					else if (term.getFunction() instanceof blog.model.DecisionFunction) {
						DecisionFunction f = (DecisionFunction) term.getFunction();
						function = new RandomFunction(f.getName(), Arrays.asList(f.getArgTypes()), f.getRetType(), null);
					} else
						continue;
					
					// Search for non-guaranteed symbols
					ArgSpec[] args = term.getArgs();
					if (args.length == 0) {
						args = new ArgSpec[1];
						args[0] = term; 
					}
					List<Object> newArgs = new ArrayList<Object>();
					boolean hasNgo = false;
					for (ArgSpec arg : args) {
						newArgs.add(arg);
						if (!(arg instanceof FuncAppTerm)) continue;
						FuncAppTerm fat = (FuncAppTerm) arg;
						if (!(fat.getFunction() instanceof SkolemConstant)) continue;
						hasNgo = true;
						liftedProperties.addObject(fat);
					}
					if (hasNgo) {
						// NOTE: the following RandFuncAppVar is not "valid" in the sense that
						// its arguments contain symbols rather than their corresponding objects.
						// TODO: change liftedProperties to use Terms instead of Bayes net vars
						RandFuncAppVar newVar = new RandFuncAppVar(function, newArgs);

						liftedProperties.addProperty(newVar, value);
						liftedVars.add((DerivedVar) var);
					}
				}
			}
		}

		for (ValueEvidenceStatement stmt : ves) {
			if (liftedHistory != null && liftedVars.contains(stmt.getObservedVar())) continue;
			newEvidence.addValueEvidence(stmt);
		}

		for (DecisionEvidenceStatement stmt : des) {
			if (liftedHistory != null && liftedVars.contains(stmt.getObservedVar())) continue;
			newEvidence.addDecisionEvidence(stmt);
		}
		for (SymbolEvidenceStatement stmt : evidence.getSymbolEvidence()) {
			if (liftedHistory != null) continue;//liftedVars.contains(stmt.getObservedVar())) continue;
			newEvidence.addSymbolEvidence(stmt);
		}
		
		if (UBT.liftedPbvi) {
			prevLiftedProperties = liftedHistory;
			originalTerms = new HashSet<Object>(liftedProperties.getObjects());
			liftedProperties.add(liftedHistory);
		}

		newEvidence.compile();
		this.evidence = newEvidence;
	}
	
	/**
	 * Returns the grounded version of the evidence.
	 * The belief should contain enough information to ground the evidence.
	 * 
	 * For instance, if this evidence says take(x), this function will find a ground
	 * term in the belief to substitute in for x.
	 * 
	 * If we are not using lifted pbvi, just return the evidence with the time corrected.
	 * @param b
	 * @return
	 */
	public Evidence getEvidence(Belief b) {
		int timestep = b.getTimestep();
		Term replace = BuiltInTypes.TIMESTEP.getCanonicalTerm(BuiltInTypes.TIMESTEP.getGuaranteedObject(timestep));
		Evidence grounded = originalGroundEvidence.replace(originalTimestep, replace);
		if (!UBT.liftedPbvi) {
			return grounded;
		}
		Map<Object, Object> subst = getSubstitution(b.getEvidenceHistory(), true);
		
		if (subst == null) {
			
			//if (debug) {
				//System.out.println("SUBSITUTION IS NULL");
				//System.out.println("b.getEvidenceHistory(): " + b.getEvidenceHistory());
				//System.out.println("prevLiftedProperties: " + prevLiftedProperties);
				//System.out.println("Originalterms: " + originalTerms);
			//}
			return null;
		}
		
		return grounded.replace(subst);
	}
	
	public Evidence getOrigGroundProps(){
		return originalGroundEvidence;
	}
	
	/**
	 * Find a mapping from all terms in the original terms of this evidence to
	 * terms used in otherProperties so that substituting all terms in (prev)LiftedProperties
	 * with the terms in otherProperties would generate the same properties in
	 * otherProperties.
	 * 
	 * excludeThisEvidence is a flag that controls whether to include this evidence as
	 * properties to compare or only compare the related historical properties.
	 * 
	 * @param otherProperties
	 * @return
	 */
	public Map<Object, Object> getSubstitution(LiftedProperties otherProperties, boolean excludeThisEvidence) {
		LiftedProperties toCompare = null;
		if (excludeThisEvidence)
			toCompare = prevLiftedProperties;
		else
			toCompare = liftedProperties;
		Map<Object, Object> rtn = toCompare.findNgoSubstitution(originalTerms, otherProperties);
		Map<Object, Object> rtn2 = toCompare.findNgoSubstitution(originalTerms, otherProperties);
		
		return rtn;
	}
	
	@Deprecated
	public Evidence getEvidence(int timestep) {
		Term replace = BuiltInTypes.TIMESTEP.getCanonicalTerm(BuiltInTypes.TIMESTEP.getGuaranteedObject(timestep));
		Evidence grounded = evidence.replace(emptyTimestep, replace);
		return grounded;
	}
	
	
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof LiftedEvidence))
			return false;
		LiftedEvidence otherEvidence = (LiftedEvidence) other;
		if (!UBT.liftedPbvi) {
			return this.evidence.equals((otherEvidence.evidence));
		}
		boolean x = this.evidence.equals(otherEvidence.evidence);
		boolean y = (this.getSubstitution(otherEvidence.getLiftedProperties(), false) != null);
		return x && y;
	}
	
	@Override
	public int hashCode() {
		return this.evidence.hashCode();
	}
	
	@Override
	public String toString() {
		return this.evidence.toString() + " Lifted: " + this.liftedProperties.toString();
	}

	public Evidence getStoredEvidence() {
		return originalGroundEvidence;
	}

	public LiftedProperties getLiftedProperties() {
		return liftedProperties;
	}
}

package blog.engine.pbvi;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blog.bn.BasicVar;
import blog.bn.BayesNetVar;
import blog.bn.DerivedVar;
import blog.engine.onlinePF.absyn.PolicyModel;
import blog.engine.onlinePF.inverseBucket.UBT;
import blog.model.ArgSpec;
import blog.model.DecisionEvidenceStatement;
import blog.model.FuncAppTerm;
import blog.model.SkolemConstant;
import blog.model.Term;
import blog.world.AbstractPartialWorld;

public class FiniteStatePolicy extends PolicyModel {
	private boolean debug = false;
	private AlphaVector alpha;
	private LiftedEvidence action;
	private Map<LiftedEvidence, FiniteStatePolicy> successors;
	private Map<LiftedEvidence, String> notes;
	private int id;
	
	public FiniteStatePolicy(LiftedEvidence action, Map<LiftedEvidence, FiniteStatePolicy> successors) {
		this.id = nextPolicyId;
		nextPolicyId++;
		this.action = action;
		this.successors = new HashMap<LiftedEvidence, FiniteStatePolicy>();
		for (LiftedEvidence e : successors.keySet()) {
			setNextPolicy(e, successors.get(e));
		}
		this.notes = new HashMap<LiftedEvidence, String>();
	}
	
	public AlphaVector getAlphaVector() { 
		return alpha;
	}
	
	public void setAlphaVector(AlphaVector alpha) { 
		this.alpha = alpha; 
	}
	
	public String toString() {
		String s = "action: " + action + "\n";
		s += successors;
		return s;
	}
	
	public LiftedEvidence getAction() {
		return action;
	}
	
	//works only for a tree right now
	public String toDotString(String name) {
		Map<FiniteStatePolicy, String> included = new HashMap<FiniteStatePolicy, String>();
		return toDotStringHelper(name + "_" + id, included);
	}
	
	private String toDotStringHelper(String name, Map<FiniteStatePolicy, String> included) {
		included.put(this, name);
		String result = name + " [label=\"" + action + "\"];\n";
		int i = 0;
		for (LiftedEvidence o : successors.keySet()) {
			String evidenceString = "";
			Collection valueEvidence = o.getStoredEvidence().getValueEvidence();
			for (Object v : valueEvidence) {
				if (v.toString().contains("observable_")) continue;
				evidenceString += v.toString() + "\\n";
			}
			Collection symbolEvidence = o.getStoredEvidence().getSymbolEvidence();
			for (Object s : symbolEvidence) {
				evidenceString += s.toString() + "\\n";
			}
			evidenceString += o.getLiftedProperties();
			
			FiniteStatePolicy contingentPolicy = successors.get(o);
			String nextName = name + "_" + i;
			if (included.containsKey(contingentPolicy)) {
				nextName = included.get(contingentPolicy);
			} else {
				result += contingentPolicy.toDotStringHelper(nextName, included);
			}
			result = result + name + " -> " + nextName + " [label=\"" + evidenceString +  " " + getNote(o) + "\"];\n";
			
			i++;
		}
		
		return result;
	}
	
	public boolean isLeafPolicy() {
		return successors.isEmpty();
	}

	public void setNextPolicy(LiftedEvidence obs,
			FiniteStatePolicy nextPolicy) {
		if (nextPolicy == null) {
			System.err.println("You can't set next policy to null");
			new Exception().printStackTrace();
			//System.exit(0);
		}
		successors.put(obs, nextPolicy);
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof FiniteStatePolicy)) {
			return false;
		}
		FiniteStatePolicy p = (FiniteStatePolicy) other;
		if (!p.getAction().equals(this.getAction()))
			return false;
		return this.successors.equals(p.successors);
	}
	
	//after hashcode is queried, prevent further changes
	private Integer setHashCode = null;
	
	@Override
	public int hashCode() {
		if (setHashCode == null)
			setHashCode =  action.hashCode() ^ successors.hashCode();
		return setHashCode;
	}
	
	public boolean merge(FiniteStatePolicy policy) {
		 if (!action.equals(policy.action)) return false;
		 for (LiftedEvidence o : successors.keySet()) {
			 if (policy.successors.containsKey(o) &&
					 !successors.get(o).equals(policy.successors.get(o))) {
				 return false;
			 }
		 }
		 
		 for (LiftedEvidence o : policy.successors.keySet()) {
			 setNextPolicy(o, policy.getNextPolicy(o));
			 addObsNote(o, "merged");
		 }
		 this.alpha = null;
		 return true;
	}
	
	public FiniteStatePolicy getNextPolicy(LiftedEvidence o) {
		if (UBT.liftedPbvi) {
			for (LiftedEvidence policyObservation : successors.keySet()) {
				if (o.equals(policyObservation)) {
					return successors.get(policyObservation);
				}
			}
			return null;
		}
		return successors.get(o);
	}
	
	public void addObsNote(LiftedEvidence obs, String note) {
		notes.put(obs, note);
	}
	
	public String getNote(LiftedEvidence obs) {
		String note = notes.get(obs);
		if (note == null) {
			return "";
		}
		return note;
	}

	public int getID() {
		return id;
	}
	
	private static int nextPolicyId = 0;


	public Set<LiftedEvidence> getNextEvidences() {
		return this.successors.keySet();
	}

	public FiniteStatePolicy getSomeNextPolicy() {
		for (FiniteStatePolicy policy : this.successors.values())
			return policy;
		return null;
	}
	
}

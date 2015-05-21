package blog.engine.pbvi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import blog.engine.onlinePF.inverseBucket.UBT;
import blog.model.Evidence;

public class FiniteStatePolicyEvaluator {
	private OUPOMDPModel pomdp;
	private double gamma;
	private Map<Evidence, Integer> numMissingObs;
	
	public FiniteStatePolicyEvaluator(OUPOMDPModel pomdp) {
		this.pomdp = pomdp;
		this.gamma = pomdp.getGamma();
	}
	
	public Map<Evidence, Integer> getLastMissingObs() {
		return numMissingObs;
	}

	private void addMissingObs(Evidence nextObs) {
		if (!numMissingObs.containsKey(nextObs))
			numMissingObs.put(nextObs, 0);
		numMissingObs.put(nextObs, numMissingObs.get(nextObs) + 1);
	}
	
	public Double eval(Belief b, FiniteStatePolicy p, int numTrials) {
		return eval(b, p, numTrials, 0);
	}
	
	public Double eval(Belief b, FiniteStatePolicy p, int numTrials, int numTrialsToPrint) {
		Double value = 0D;
		int totalCount = 0;
		/*List<TimedParticle> particles = b.getParticleFilter().particles;
		for (TimedParticle particle : particles) {
			State s = new State((AbstractPartialWorld) particle.curWorld, particle.getTimestep());
			Double v = eval(s, p, numTrials, numTrialsToPrint);
			if (v == null) return v;
			//value += v * b.getCount(s);
			//totalCount += b.getCount(s);
			value += v;
			totalCount += 1;
		}*/
		for (State s : b.getStates()) {
			Double predictedValue = p.getAlphaVector().getValue(s);
			if (numTrialsToPrint > 0 && predictedValue != null)
				System.out.println("Predicted Value for state to evaluate: " + predictedValue);
			Double v = eval(s, p, numTrials, numTrialsToPrint);
			if (numTrialsToPrint > 0)
				System.out.println("Evaluated Value for state: " + v);
			value += v * b.getCount(s);
			totalCount += b.getCount(s);
		}
		
		return value/totalCount;
	}
	
	public String getMissingObs() {
		String result = "";
		for (Evidence o : numMissingObs.keySet()) {
			result += o + " " + numMissingObs.get(o) + "\n";
		}
		return result;
	}
	
	public Double eval(State state, FiniteStatePolicy p, int numTrials) {
		return eval(state, p, numTrials, 0);
	}
	
	public Double eval(State state, FiniteStatePolicy p, int numTrials, int numTrialsToPrint) {
		return eval(state, p, numTrials, numTrialsToPrint, false);
	}
	
	public Double eval(State state, FiniteStatePolicy p, int numTrials, int numTrialsToPrint,
			boolean stopAtKnownAlphaValue) {
		numMissingObs = new HashMap<Evidence, Integer>();
		int numPathsPrinted = 0;
		Belief initState = Belief.getSingletonBelief(state, 1, pomdp);
		double accumulatedValue = 0;
		for (int i = 0; i < numTrials; i++) {
			Belief curState = initState;
			FiniteStatePolicy curPolicy = p;
			double curValue = 0D;
			double discount = 1;
			List<Evidence> curPath = new ArrayList<Evidence>();
			LiftedProperties policyHistory = new LiftedProperties();
			
			if (UBT.liftedPbvi) {
				Set<Object> existing = curPolicy.getAction().getLiftedProperties().getObjects();
				for (Object e : existing)
					policyHistory.addObject(e);
			}
				
			
			while (curPolicy != null) {
				if (curState.ended()) break;
				LiftedEvidence nextAction = curPolicy.getAction();
				Evidence groundedAction = nextAction.getEvidence(curState);
				if (groundedAction == null) {
					curValue = -10000D;
					break;
				}
				curPath.add(groundedAction);
				curState = curState.sampleNextBelief(groundedAction);
				Evidence nextObs = curState.getLatestEvidence();
				
				if (stopAtKnownAlphaValue) {
					Double alphaValue = curPolicy.getAlphaVector().getValue(curState);
					if (alphaValue != null)
						return alphaValue;
				}
				
				if (!curPolicy.isLeafPolicy()) {
					curPath.add(nextObs);
					LiftedEvidence evidenceToMatch = new LiftedEvidence(nextObs, curState.getEvidenceHistory());
					FiniteStatePolicy nextPolicy = curPolicy.getNextPolicy(evidenceToMatch);
					
					// There is no matching evidence in the policy.
					// Find a random applicable next policy.
					if (nextPolicy == null && !curState.ended()) { 
						if (!UBT.liftedPbvi) {
							nextPolicy = curPolicy.getApplicableNextPolicy(new LiftedEvidence(nextObs, policyHistory), curState);
						} 
						if (nextPolicy != null) {
							addMissingObs(nextObs);
						} else {
							System.out.println("no next policy for evidence " + evidenceToMatch);
							System.out.println("action just taken is " + nextAction + " groundedAction: " + groundedAction);
							System.out.println("policy successors are ");
							for (LiftedEvidence e : curPolicy.getNextEvidences())
								System.out.println(e);
							curValue = -10000D;
							break;
						}
						//curPolicy.debug = true;
						//LiftedEvidence x = curPolicy.getMatchingEvidence(liftedEvidence, policyHistory, curState);
						//System.out.println(x + " *** " + liftedEvidence);
					} 
					
					curPolicy = nextPolicy;
				} else {
					curPolicy = null;
				}
				curValue += discount * curState.getLatestReward();
				discount = discount * gamma;
			}
			if (numPathsPrinted < numTrialsToPrint) {
				System.out.println("Value: " + curValue + ", Path: " + curPath);
				if (UBT.liftedPbvi)
					System.out.println("history, policyHistory: " + curState.getEvidenceHistory() + ", " + policyHistory);
				numPathsPrinted++;
			}
			accumulatedValue += curValue;
		}
		return accumulatedValue/numTrials;
	}
	
	public Double eval(Belief b, DotToPolicy p, int numTrials) {
		Double value = 0D;
		int totalCount = 0;
		for (State s : b.getStates()) {
			Double v = eval(s, p, numTrials);
			if (v == null) return v;
			value += v * b.getCount(s);
			totalCount += b.getCount(s);
		}
		System.out.println("Total count " + totalCount);
		return value/totalCount;
	}
	
	public Double eval(State state, DotToPolicy p, int numTrials) {
		numMissingObs = new HashMap<Evidence, Integer>();
		Belief initState = Belief.getSingletonBelief(state, 1, pomdp);
		//System.out.println(initState);
		double accumulatedValue = 0;
		
		for (int i = 0; i < numTrials; i++) {
			Belief curState = initState;
			double curValue = 0D;
			double discount = 1;
			int iter = 0;
			p.resetSim();
			while (true) {
				if (curState.ended()) break;
				LiftedEvidence nextAction = p.getAction(pomdp.getActions(curState));
				//System.out.println(nextAction);
				curState = curState.sampleNextBelief(nextAction);		
				Evidence nextObs = curState.getLatestEvidence();
				boolean nextAvailable = p.advancePolicy(nextObs);
				curValue += discount * curState.getLatestReward();
				discount = discount * gamma;
				iter++;
				if (!nextAvailable) break;
			}

			accumulatedValue += curValue;
		}
		//System.out.println("Accum value " + accumulatedValue);
		return accumulatedValue/numTrials;
	}

}

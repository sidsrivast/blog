package blog.engine.pbvi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import blog.bn.RandFuncAppVar;
import blog.common.Util;
import blog.model.FuncAppTerm;
import blog.model.RandomFunction;

public class LiftedProperties {
	public boolean debug = false;
	private Set<Object> ngos;
	private Map<RandFuncAppVar, Object> properties;
	private Map<Object, Set<RandFuncAppVar>> objToProperties;
	private Map<Object[], Set<Object[]>> objsToConstraints;
	private Map<Object, Set<Object>> objToContrainedObjs;
	
	private Map<Object, Set<Object[]>> objToPropLoc;
	private Map<RandomFunction, Set<ArrayList<Object[]>>> propToNgoPos;
	
	
	
	
	public LiftedProperties() {
		ngos = new HashSet<Object>();
		properties = new HashMap<RandFuncAppVar, Object>();
		objToProperties = new HashMap<Object, Set<RandFuncAppVar>>();
		objsToConstraints = new HashMap<Object[], Set<Object[]>>(); 
		objToContrainedObjs = new HashMap<Object, Set<Object>>(); 
		objToPropLoc = new HashMap<Object, Set<Object[]>>();
		propToNgoPos = new HashMap<RandomFunction, Set<ArrayList<Object[]>>>();
	}
	
	public void addObject(Object obj) {
		ngos.add(obj);
		objToProperties.put(obj, new HashSet<RandFuncAppVar>());
		objToContrainedObjs.put(obj, new HashSet<Object>());
		objToPropLoc.put(obj, new HashSet<Object[]>());
	}
	
	public Set<Object> getObjects() {
		return Collections.unmodifiableSet(ngos);
	}
	
	public void addProperty(RandFuncAppVar var, Object value) {
		properties.put(var, value);
		if(!propToNgoPos.keySet().contains(var.func())){
			propToNgoPos.put(var.func(), new HashSet< ArrayList<Object[]>>());
		}
		Object[] args = var.args();
		
		int ind = 0;
		//index of a
		ArrayList<Object[]> ngoPos = new ArrayList<Object[]>();
		
		for (Object a : args) {
			if(ngos.contains(a)){
				//is a is an object in our world
				Object[] propLoc = {var.func(), ind, value};
				objToPropLoc.get(a).add(propLoc);
				Object[] ngoPosElement = {ind, a, value}; 
				ngoPos.add(ngoPosElement);
			}
			
			int ind2 = 0;
			//index of b
			for (Object b: args){
				if (a!=b){
					if(objToContrainedObjs.containsKey(a) && objToContrainedObjs.containsKey(b)){
						//if a and b are objects in our world
						if(!objToContrainedObjs.get(a).contains(b)){
							objToContrainedObjs.get(a).add(b);
						}
						Object[] pair = {a,b};
						//obj pair
						if(!objsToConstraints.keySet().contains(pair)){
							objsToConstraints.put(pair, new HashSet<Object[]>());
						}
						Object [] constraint = {var.func(),ind,ind2};
						if(!objsToConstraints.get(pair).contains(constraint)){
							objsToConstraints.get(pair).add(constraint);
						}
					}
				}
				ind2++;
				
			}
			if (objToProperties.containsKey(a)) {
				objToProperties.get(a).add(var);
			}
			ind++;
		}
		propToNgoPos.get(var.func()).add(ngoPos);
		
	}
	

	
	public Map<Object, Object> findNgoSubstitution(Set<Object> ngos, LiftedProperties other) {
		Map<Object, Set<Object>> cand = getCandidates(other, ngos);	
		
		/*
		System.out.println("-----");
		System.out.println(ngos);
		System.out.println(new HashSet<Object>(other.ngos));
		System.out.println(getRelevantProperties(ngos));
		System.out.println(new HashMap<RandFuncAppVar, Object>(other.properties));
			
	
		System.out.println("Candidates:");
		System.out.println(cand);
		System.out.println("original:");
		System.out.println(findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				getRelevantProperties(ngos), 
				new HashMap<RandFuncAppVar, Object>(other.properties), 
				new HashMap<Object, Object>()));
		
		
		System.out.println("new:");
		System.out.println(findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				getRelevantProperties(ngos), 
				new HashMap<RandFuncAppVar, Object>(other.properties), 
				new HashMap<Object, Object>(),
				cand,
				objToContrainedObjs,
				objsToConstraints,
				new HashSet<Map<Object, Object>>(),
				0
				));
		
		*/
		return  sampler(findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				getRelevantProperties(ngos), 
				new HashMap<RandFuncAppVar, Object>(other.properties), 
				new HashMap<Object, Object>(),
				cand,
				objToContrainedObjs,
				objsToConstraints,
				new HashSet<Map<Object, Object>>(),
				0
				));
		/*return findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				getRelevantProperties(ngos), 
				new HashMap<RandFuncAppVar, Object>(other.properties), 
				new HashMap<Object, Object>());		
				*/
	}
	
	// TODO: Right now, assuming not more than one ngo in a var
	private Map<RandFuncAppVar, Object> getRelevantProperties(
			Set<Object> ngos) {
		Map<RandFuncAppVar, Object> result = new HashMap<RandFuncAppVar, Object>();
		for (Object ngo : ngos) {
			Set<RandFuncAppVar> leftSideProperties = objToProperties.get(ngo);
			if (leftSideProperties == null) {
				//System.out.println(ngos);
				//System.out.println(this);
				continue;
			}
			for (RandFuncAppVar var : leftSideProperties) {
				result.put(var, properties.get(var));
			}
		}
		
		return result;
	}
	
	public Map<Object, Object> findNgoBijection(LiftedProperties other) {
		Map<Object, Set<Object>> cand = getCandidates(other, ngos);
		
		Set<Map<Object, Object>> solutionset = 
				findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				properties, 
				other.properties, 
				new HashMap<Object, Object>(),
				cand,
				objToContrainedObjs,
				objsToConstraints,
				new HashSet<Map<Object, Object>>(),
				0
				);
		for(Map<Object, Object> mapping : solutionset){
			Set<Object> collisioncheck = new HashSet<Object>();
			for(Object ngo: mapping.keySet()){
				collisioncheck.add(mapping.get(ngo));
			}
			if(collisioncheck.size() == mapping.keySet().size()){
				return mapping;
			}
		}
		return null;
	}

	public Map<Object, Object> findNgoSubstitution(LiftedProperties other) {
		Map<Object, Set<Object>> cand = getCandidates(other, ngos);
		
		/*
		System.out.println("-----type2");
		System.out.println(ngos);
		System.out.println(new HashSet<Object>(other.ngos));
		System.out.println(getRelevantProperties(ngos));
		System.out.println(new HashMap<RandFuncAppVar, Object>(other.properties));
		
				
		
		System.out.println("Candidates:");
		System.out.println(cand);
		
		System.out.println("new:");
		System.out.println(findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				properties, 
				other.properties, 
				new HashMap<Object, Object>(),
				cand,
				objToContrainedObjs,
				objsToConstraints,
				new HashSet<Map<Object, Object>>(),
				0
				));
		*/
		return sampler(findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				properties, 
				other.properties, 
				new HashMap<Object, Object>(),
				cand,
				objToContrainedObjs,
				objsToConstraints,
				new HashSet<Map<Object, Object>>(),
				0
				));
				
		
		
		
		/*return findNgoSubstitution(new HashSet<Object>(ngos), 
				new HashSet<Object>(other.ngos), 
				properties, 
				other.properties, 
				new HashMap<Object, Object>());
				*/
	}
	
	private Map<Object, Object> sampler(Set<Map<Object, Object>> solutionset){
		if(solutionset == null){
			return null;
		}
		int size = solutionset.size();
		if(size == 0){
			return null;
		}
		int item = new Random().nextInt(size);
		int i = 0;
		for(Map<Object, Object> solution : solutionset)
		{
		    if (i == item)
		        return solution;
		    i = i + 1;
		}
		return null;
	}
	
	private Map<Object, Set<Object>> getCandidates(LiftedProperties other, Set<Object> ngos) {
		Map<Object, Set<Object>> candidates = new HashMap<Object, Set<Object>>();
		for(Object obj : ngos){
			candidates.put(obj, new HashSet<Object>());
			if(!objToPropLoc.keySet().contains(obj)){
				for(Object ngo : other.ngos){
					candidates.get(obj).add(ngo);
				}
				continue;
			}
			if(objToPropLoc.get(obj).isEmpty()){
				for(Object ngo : other.ngos){
					candidates.get(obj).add(ngo);
				}
				continue;
			}
			int i = 0;
			
			for(Object[] objLocations : objToPropLoc.get(obj)){
				Set<Object> cands = new HashSet<Object>();
				if(!other.propToNgoPos.containsKey(objLocations[0])){
					continue;
				}
				for(ArrayList<Object[]> otherobjLocations : other.propToNgoPos.get(objLocations[0])){
					for(Object[] otherLocation : otherobjLocations){
						if(otherLocation[0] == objLocations[1] && otherLocation[2] == objLocations[2]){
							if(i == 0){
								if(!candidates.get(obj).contains(otherLocation[1])){
									candidates.get(obj).add(otherLocation[1]);
								}
							} else {
								if(!cands.contains(otherLocation[1])){
									cands.add(otherLocation[1]);
								}
							}
						}
					//candidates.get(obj).add(otherobjLocations.get((Integer) objLocations[1])[1]);
					}
				}
				if(i != 0){
					candidates.get(obj).retainAll(cands);
				}
				i = i+1;
			}
		}
		return candidates;
	}
	
	
	
	private Set<Map<Object, Object>> findNgoSubstitution(
			Set<Object> myNgos,
			Set<Object> otherNgos,
			Map<RandFuncAppVar, Object> myProperties,
			Map<RandFuncAppVar, Object> otherProperties,
			Map<Object, Object> partialSolution,
			Map<Object, Set<Object>> candidates,
			Map<Object, Set<Object>> objToContrainedObjs,
			Map<Object[], Set<Object[]>> objsToConstraints,
			Set<Map<Object, Object>> solutionSet,
			int iter
			){
		//System.out.println();
		//System.out.println("candidates");
		//System.out.println(candidates);
		Map<Object, Object> solution = null;
		if (myNgos.size() > otherNgos.size()) return null;
		if (myNgos.size() == partialSolution.keySet().size()){
			solutionSet.add(partialSolution);
			return solutionSet;
		}
		int mrv = 1000000;
		Object currentNGO = null;
		
		//Selects the ngo with the minimum remaining values
		for (Object ngo : myNgos){
			if(!partialSolution.keySet().contains(ngo)){
				int candsize = candidates.get(ngo).size();
				if(candsize < mrv){
					mrv = candsize;
					currentNGO = ngo;
				}
			}
		}
		
		//iterate over potential mapping for that ngo
		for (Object val : candidates.get(currentNGO)){
			Map<Object, Object> newPartialSolution = new HashMap<Object, Object>(partialSolution);
			//add to the existing mapping
			newPartialSolution.put(currentNGO, val);
			//prune off candidates
			Map<Object, Set<Object>> inferredCandidates = inference(newPartialSolution, candidates, currentNGO, objToContrainedObjs, objsToConstraints);
			//check consistency
			if(isConsistent(inferredCandidates, myNgos)){
				solutionSet = 
						findNgoSubstitution(
						myNgos,
						otherNgos,
						myProperties,
						otherProperties,
						newPartialSolution,
						inferredCandidates,
						objToContrainedObjs,
						objsToConstraints,
						solutionSet,
						iter + 1);				
			}
			
		}
		return solutionSet;		
	}
	
	private Set<Map<Object, Object>> findNgoBijection(
			Set<Object> myNgos,
			Set<Object> otherNgos,
			Map<RandFuncAppVar, Object> myProperties,
			Map<RandFuncAppVar, Object> otherProperties,
			Map<Object, Object> partialSolution,
			Map<Object, Set<Object>> candidates,
			Map<Object, Set<Object>> objToContrainedObjs,
			Map<Object[], Set<Object[]>> objsToConstraints,
			Set<Map<Object, Object>> solutionSet,
			int iter
			){
		//System.out.println();
		//System.out.println("candidates");
		//System.out.println(candidates);
		Map<Object, Object> solution = null;
		if (myNgos.size() != otherNgos.size()) return null;
		if (myNgos.size() == partialSolution.keySet().size()){
			solutionSet.add(partialSolution);
			return solutionSet;
		}
		int mrv = 1000000;
		Object currentNGO = null;
		for (Object ngo : myNgos){
			if(!partialSolution.keySet().contains(ngo)){
				int candsize = candidates.get(ngo).size();
				if(candsize < mrv){
					mrv = candsize;
					currentNGO = ngo;
				}
			}
		}
		//System.out.println("currentNGO");
		//System.out.println(currentNGO);
		for (Object val : candidates.get(currentNGO)){
			Map<Object, Object> newPartialSolution = new HashMap<Object, Object>(partialSolution);
			newPartialSolution.put(currentNGO, val);
			Map<Object, Set<Object>> inferredCandidates = inference(newPartialSolution, candidates, currentNGO, objToContrainedObjs, objsToConstraints);
			if(isConsistent(inferredCandidates, myNgos)){
				solutionSet = 
						findNgoSubstitution(
						myNgos,
						otherNgos,
						myProperties,
						otherProperties,
						newPartialSolution,
						inferredCandidates,
						objToContrainedObjs,
						objsToConstraints,
						solutionSet,
						iter + 1);				
			}
			
		}
		return solutionSet;		
	}
	
	
	private Map<Object, Set<Object>> inference(
			Map<Object, Object> partialSol, 
			Map<Object, Set<Object>> candidates, 
			Object var,
			Map<Object, Set<Object>> objToContrainedObjs,
			Map<Object[], Set<Object[]>> objsToConstraints
			){	
		Map<Object, Set<Object>> newCandidates = new HashMap<Object, Set<Object>>(candidates);
		newCandidates.remove(var);
		newCandidates.put(var, new HashSet<Object>());
		newCandidates.get(var).add(partialSol.get(var));
		if(!objToContrainedObjs.containsKey(var)){
			return newCandidates;
		}
		for(Object neighbor : objToContrainedObjs.get(var)){
			newCandidates.remove(neighbor);
			newCandidates.put(neighbor, new HashSet<Object>());
			Object[] edge = {var, neighbor};
			for(Object[] constraint : objsToConstraints.get(edge)){
				for(ArrayList<Object[]> args : propToNgoPos.get(constraint[0])){
					if(args.get((Integer) constraint[1]) == partialSol.get(var)){
						if(candidates.get(neighbor).contains(args.get((Integer) constraint[2]))){
							newCandidates.get(neighbor).add(args.get((Integer) constraint[2]));
						}
					}
				}
			}
		}
		return newCandidates;
	}
	
	private boolean isConsistent(Map<Object, Set<Object>> candidates, Set<Object> myNgos){
		boolean ret = true;
		for (Object obj : myNgos){
			if(candidates.get(obj).size() == 0){
				ret = false;
			}
		}
		return ret;
	}
			
			
	@Override
	public String toString() {
		String s = "";
		for (Object ngo1 : ngos) {
			for (Object ngo2 : ngos) {
				if (ngo1.hashCode() == ngo2.hashCode() && !ngo1.equals(ngo2)) {
					FuncAppTerm ngo1Term = (FuncAppTerm) ngo1;
					FuncAppTerm ngo2Term = (FuncAppTerm) ngo2;
					ngo1Term.equalsDebug(ngo2Term);
				}
					
			}
		}
		return s + ngos + " " + properties.toString();
	}
	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof LiftedProperties)) return false;
		return findNgoSubstitution((LiftedProperties) other) != null;
	}

	public void add(LiftedProperties other) {
		for (Object ngo : other.ngos) {
			this.addObject(ngo);
		}
		for (RandFuncAppVar var : other.properties.keySet()) {
			this.addProperty(var, other.properties.get(var));
		}	
	}
	
	public LiftedProperties replace(Object old, Object n) {
		LiftedProperties result = new LiftedProperties();
		for (Object ngo : ngos) {
			result.addObject(ngo);
		}
		for (RandFuncAppVar var : properties.keySet()) {
			Object[] args = var.args().clone();
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals(old))
					args[i] = n;
			}
			result.addProperty(new RandFuncAppVar(var.func(), args), properties.get(var));
		}
		return result;
	}
}

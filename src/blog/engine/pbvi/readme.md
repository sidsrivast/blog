/*Author: Paul Ruan */


#Classes in src/blog/engine/pbvi
##OUPBVI
- entry point
- runs algorithm

 
##OUPOMDPModel
- contains methods like getActions, getQueries, getInitialBelief
 
##Belief
- state distribution representation
    - stateCounts: object maps from state to number of particles with that state
- has a particle filter object to allow for belief propagation
    - for instance, b.sampleNextBelief(a) samples a next belief given an action a
- beliefsAfterAction(a) returns an ActionPropagated object 
    – represents a set of possible next beliefs
 

##State
- a wrapper around AbstractPartialWorld
- used as AlphaVector keys
 

##FiniteStatePolicy
- policy representation
- main components are: action, successors (observation -> next policy)
- toDotString returns a string in dot format
 

#Main steps in execution
1. OUPBVI main
    1. generates OUPOMDPModel from BLOG model
    2. calls run to compute policy
2. run
    1. selects set of beliefs (maxNormBeliefExpansion)
    2. runs singleBackup for t-steps
    3. evaluates policy (uses FiniteStatePolicyEvaluator)
3. singlebackup
    1. calls singleBackupForBelief to produce new policy for belief
    2. involves policy evaluation step that’s done in evalPolicyDFS
    3. merges resulting policies


#Lifted version
The above main steps are still the same. 
The difference is only in the representation of observations and actions, 
and an additional information in the belief.


##LiftedEvidence
- a wrapper around Evidence
    - Evidence is the class that represents ground actions and observations
    - arrest(Suspect0, @2) = True)
    - isNervous(Suspect0, @1) = False and Number_Suspect(@1) = 1
- initialized by taking an Evidence object and a LiftedProperties object
    - can think of the given LiftedProperties object as the “history”, Evidence as “current”
    - the given LiftedProperties object is kept as prevLiftedProperties
    - liftedProperties: prevLiftedProperties + properties extracted from the given Evidence
        - this means that liftedProperties contain more than needed for this action/obs
        - originalTerms: set of terms used in the original Evidence
            - provides a way to know what properties are relevant
        - TODO: look into not storing repeated lifted properties
    - evidence: any obs/actions not containing any variables/non-guaranteed objects
- getEvidence
    - takes in a belief and returns an Evidence object equivalent to this 
    LiftedEvidence object but with all the variables replaced with ground terms


##LiftedProperties
- ngos: terms representing non-guaranteed objects; variables
- properties: mapping RandFuncAppVar to their values
- objToProperties: mapping from ngo to set of properties that contain them
- findNgoSubstitution: find substitution (map from term to another term) from this set of lifted properties to another
    - TODO: use objToProperties on line 100 [Oliver]
- getRelevantProperties: given a set of variables we care about, filter out relevant properties
    - TODO: getRelevantProperties in LiftedProperties
        - BFS algorithm using objToProperties [Richard]
        - only supports properties with a single variable right now
- provides a representation for
    - arrest(x, @2) = True such that isNervous(x, @1)
    - isNervous(x, @1) = False and NumberSuspect(@t) = 1
    - TODO: make the lifted properties time agnostic (see below) [Oliver]
    - replace @j  with prev^n(t); where n = t-j//t is the current timestep


##Change in Belief Class
- maintains a LiftedProperties object representing set of objects it has seen
- see sampleNextBelief and beliefsAfterAction for how it is maintained


#TODO: Lifted alpha vectors
- want the keys of the alpha vectors to be lifted
    - need history of object properties
    - equivalent states with different histories may have different expected 
    values for a policy because the policy is dependent on the history
    - instead of isNervous(Suspect0, t) and isGuilty(Suspect0, t) = True => 10 
    want isNervous(x, t - 1) and isNervous(x, t) and isGuilty(x, t) => 10
- we can do it by
    - add lifted properties to State objects
    - or lifted properties to AbstractPartialWorld objects
    - need to modify equals (and hashCode)

- Write a wrapper around abstract partial world to provide substitution methods 
(no need of lifting the states when used as keys for alpha vectors)

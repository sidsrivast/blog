/*Author: Paul Ruan */

Classes in src/blog/engine/pbvi
OUPBVI
- entry point
- runs algorithm
 
OUPOMDPModel
- contains methods like getActions, getQueries, getInitialBelief
 
Belief
- state distribution representation
  o stateCounts  object maps from state to number of particles with that state
- has a particle filter object to allow for belief propagation
  o for instance, b.sampleNextBelief(a) samples a next belief given an action a
- beliefsAfterAction(a) returns an ActionPropagated object – represents a set of possible next beliefs
 
State
- a wrapper around AbstractPartialWorld
- used as AlphaVector keys
 
FiniteStatePolicy
- policy representation
- main components are: action, successors (observation -> next policy)
- toDotString returns a string in dot format
 
Main steps in execution
1. OUPBVI main
  a. generates OUPOMDPModel from BLOG model
  b. calls run to compute policy
2. run
  a. selects set of beliefs (maxNormBeliefExpansion)
  b. runs singleBackup for t-steps
  c. evaluate policy (uses FiniteStatePolicyEvaluator)
3. singlebackup
  a. calls singleBackupForBelief to produce new policy for belief
  b. involves policy evaluation step that’s done in evalPolicyDFS
  c. merges resulting policies

package blog.engine.pbvi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import blog.DBLOGUtil;
import blog.bn.BasicVar;
import blog.bn.NumberVar;
import blog.bn.RandFuncAppVar;
import blog.engine.onlinePF.inverseBucket.UBT;
import blog.model.BuiltInTypes;
import blog.model.POP;
import blog.model.RandomFunction;
import blog.model.Type;
import blog.world.AbstractPartialWorld;
import blog.world.DefaultPartialWorld;

public class State {
	private AbstractPartialWorld world;
	private LiftedProperties liftedProperties;
	private int timestep;

	public State(AbstractPartialWorld w, int t) {
		world = w;
		timestep = t;
		//zeroTimestep();
	}

	public AbstractPartialWorld getWorld() {
		return world;
	}

	public int getTimestep() {
		return timestep;
	}

	public void zeroTimestep(int zero) {
		Timer.start("zeroTimestep");
		Set<BasicVar> basicVars = new HashSet<BasicVar>(world.getChangedVars());
		for (BasicVar var : basicVars) {
			int timestep = DBLOGUtil.getTimestepIndex(var);
			if (timestep == -1 || timestep == zero) continue;
			Object value = world.getValue(var);
			Object[] args = var.args();
			BasicVar newVar = null;
			if (var instanceof RandFuncAppVar) {
				RandFuncAppVar funcAppVar = (RandFuncAppVar) var;
				RandomFunction f = funcAppVar.func();
				List newArgs = zeroTimestepArg(args, f.getArgTypes(), zero);
				newVar = new RandFuncAppVar(f, newArgs);
			} 
			
			if (var instanceof NumberVar) {
				NumberVar numVar = (NumberVar) var;
				POP pop = numVar.pop();
				List newArgs = zeroTimestepArg(args, pop.getArgTypes(), zero);
				newVar = new NumberVar(pop, newArgs);
			}
			if (newVar == null) {
				System.err.println("Cannot zero: " + var);
				System.exit(0);
			}
			world.setValue(newVar, value);
			world.setValue(var, null);
		}

		this.timestep = zero;
		Timer.record("zeroTimestep");
	}

	private List zeroTimestepArg(Object[] args, Type[] argTypes, int zero) {
		List newArgs = new ArrayList();
		for (int i = 0; i < argTypes.length; i++) {
			if (argTypes[i].equals(BuiltInTypes.TIMESTEP)) {
				newArgs.add(BuiltInTypes.TIMESTEP.getGuaranteedObject(zero));
			} else {
				newArgs.add(args[i]);
			}
		}
		return newArgs;
	}

	public void initLiftedProperties(LiftedProperties properties) {
		this.liftedProperties = properties;
	}

	private AbstractPartialWorld zeroWorld;
	
	public AbstractPartialWorld getZeroWorld() {
		if (zeroWorld == null) {
			AbstractPartialWorld thisWorld = (DefaultPartialWorld) ((DefaultPartialWorld) world).clone();
			State zeroState = new State(thisWorld, timestep);
			zeroState.zeroTimestep(0);
			zeroWorld = zeroState.world;
		}
		return zeroWorld;
	}
	
	@Override
	public int hashCode() {
		return this.getZeroWorld().innerStatehashCode(0);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof State))
			return false;
		State s = (State) o;
		boolean equality = false;
		Timer.start("STATE_COMPARISON");
		
		if (s.liftedProperties != null && liftedProperties != null) {
			if (!s.liftedProperties.equals(liftedProperties))
				return false;
		} else if (s.liftedProperties == null && liftedProperties == null) {
			
		} else {
			Exception e = new Exception();
			e.printStackTrace();
			System.err.println("Should not be comparing a state with lifted properties " +
					"with another with none");
			System.err.println("s.liftedProperties" + s.liftedProperties);
			System.err.println("liftedProperties" + liftedProperties);
			System.exit(1);
		}
		
		AbstractPartialWorld thisWorld = world;
		AbstractPartialWorld toCompareWorld = s.world;
		
		if (timestep != s.timestep) {
			toCompareWorld = s.getZeroWorld();
			thisWorld = getZeroWorld();
		}
		equality = toCompareWorld.innerStateEquals(thisWorld, 0);
		Timer.record("STATE_COMPARISON");
		return equality;
		
	}
	
	@Override
	public String toString() {
		if (UBT.liftedPbvi) {
			return "State: " + world.toString() + ", Lifted Properties: " + liftedProperties;
		}
		return "State: " + world.toString();
	}

	public LiftedProperties getLiftedProperties() {
		return liftedProperties;
	}
}

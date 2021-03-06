/*
 * Copyright (c) 2005, Regents of the University of California
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in
 *   the documentation and/or other materials provided with the
 *   distribution.  
 *
 * * Neither the name of the University of California, Berkeley nor
 *   the names of its contributors may be used to endorse or promote
 *   products derived from this software without specific prior 
 *   written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package blog.sample;

import java.util.Properties;

import blog.common.Util;
import blog.model.Evidence;
import blog.model.Model;
import blog.model.Queries;
import blog.world.PartialWorld;

/**
 * An object that stochastically generates a sequence of samples, which are
 * possible worlds of a given BLOG model. A Sampler object may generate
 * independent samples, or it may maintain internal state (e.g., a Markov chain
 * sampler). The samples generated may also have associated weights, as in
 * likelihood weighting.
 * 
 * <p>
 * A concrete Sampler subclass should have a constructor that takes two
 * arguments: a blog.Model object and a java.util.Properties object. The
 * properties object specifies configuration parameters for the sampler.
 */
public abstract class Sampler {
  // Samples with log weight <= NEGLIGIBLE_LOG_WEIGHT are ignored.
  public static final double NEGLIGIBLE_LOG_WEIGHT = -10000;

  /**
   * Creates a Sampler object for the given BLOG model.
   */
  public Sampler(Model model) {
    this.model = model;
  }

  /** Make new sampler instance of named class. */
  public static Sampler make(String samplerClassName, Model model,
      Properties properties) {
    return (Sampler) Util.makeInstance_NE(samplerClassName, new Class[] {
        Model.class, Properties.class }, new Object[] { model, properties });
  }

  /**
   * Prepares this sampler to sample from the distribution conditioned on the
   * given evidence, returning PartialWorld objects that are complete enough to
   * answer the given queries. Also clears the internal state of this sampler so
   * that the next sample generated will be independent of all previous ones.
   * 
   * <p>
   * The default implementation just sets the <code>evidence</code> and
   * <code>queries</code> member variables.
   * 
   * @param queries
   *          List of Query objects
   */
  public void initialize(Evidence evidence, Queries queries) {
    this.evidence = evidence;
    this.queries = queries;
  }

  /**
   * Generates the next sample (world), and possibly assigns it a weight.
   * 
   * @throws IllegalStateException
   *           if <code>initialize</code> has not been called on this Sampler
   */
  public abstract void nextSample();

  /**
   * Sets the partial world to be extended (thus, the instance is modified!) as
   * a basis when a sample is being generated from scratch (as likelihood
   * weighting samplers do, for example), as opposed to when it is being
   * generated from, or being a modification of, the previous sample (as MH
   * samplers do, for example). This is useful for incremental sampling, such as
   * the one in the particles of a particle filter, that already have a current
   * partial world.
   */
  public abstract void setBaseWorld(PartialWorld world);

  /**
   * Returns the world generated by the most recent call to
   * <code>nextSample</code>. The returned PartialWorld object may be modified
   * by the next call to <code>nextSample</code>.
   * 
   * @throws IllegalStateException
   *           if <code>nextSample</code> has not been called, or if
   *           <code>initialize</code> has been called since the last call to
   *           <code>nextSample</code>
   */
  public abstract PartialWorld getLatestWorld();

  /**
   * Returns the logWeight for the world generated by the most recent call to
   * <code>nextSample</code>. The default implementation returns 0.0.
   * 
   * FIXME (cberzan): This is API is unnecessarily stateful. The latest weight
   * should not be kept in the sampler at all. It should probably be returned
   * by nextSample() in some way.
   * 
   * @throws IllegalStateException
   *           if <code>nextSample</code> has not been called, or if
   *           <code>initialize</code> has been called since the last call to
   *           <code>nextSample</code>
   * 
   */
  public double getLatestLogWeight() {
    return 0.0;
  }

  /**
   * Prints statistics about the internal activities of this sampler. The
   * default implementation does nothing.
   */
  public void printStats() {
  }

  /**
   * BLOG model for which this sampler generates partial worlds.
   */
  protected Model model;

  /**
   * Evidence specified so far (usually by the last call to
   * <code>initialize</code>, but extending classes may have methods to increase
   * it), or null if <code>initialize</code> has not been called.
   */
  protected Evidence evidence;

  public AfterSamplingListener afterSamplingListener = null;

  /**
   * List of Query objects specified so far (usually by the last call to
   * <code>initialize</code>, but extending classes may have methods to increase
   * it) or null if <code>initialize</code> has not been called.
   */
  protected Queries queries;
}

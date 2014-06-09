/*
 * Copyright (c) 2005, 2006, Regents of the University of California
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

package blog.model;

import java.io.PrintStream;
import java.util.Collections;

import ve.Factor;
import blog.Main;
import blog.bn.BasicVar;
import blog.common.Histogram;
import blog.common.Util;
import blog.world.PartialWorld;

public class FormulaQuery extends ArgSpecQuery {

  public FormulaQuery(Formula formula) {
    super(formula);

    if (Main.outputPath() != null) {
      outputFile = Main.filePrintStream(Main.outputPath() + "-trial" + trialNum
          + ".data");
    }
  }

  public Formula formula() {
    return (Formula) getArgSpec();
  }

  public void printResults(PrintStream s) {
    s.println("Probability of " + getArgSpec() + " is " + trueProb());
  }

  public void logResults(int numSamples) {
    if (outputFile != null) {
      outputFile.println("\t" + numSamples + "\t" + trueProb());
    }
  }

  public void updateStats(PartialWorld world, double logweight) {
    if (probTrue >= 0) {
      throw new IllegalStateException(
          "Can't update states: posterior already specified.");
    }

    if (((Formula) getArgSpec()).isTrue(world)) {
      trueSum = Util.logSum(logweight, trueSum);
    }
    totalSum = Util.logSum(logweight, totalSum);
  }

  public void setPosterior(Factor posterior) {
    if (!posterior.getRandomVars().contains((BasicVar) variable)) {
      throw new IllegalArgumentException("Query variable " + variable
          + " not covered by factor on " + posterior.getRandomVars());
    }
    if (posterior.getRandomVars().size() > 1) {
      throw new IllegalArgumentException("Answer to query on " + variable
          + " should be factor on " + "that variable alone, not "
          + posterior.getRandomVars());
    }

    probTrue = posterior.getPotential().getValue(
        Collections.singletonList(Boolean.TRUE));
  }

  public void zeroOut() {
    double result = trueProb();
    runningProbSum += result;
    runningProbSumSquares += (result * result);
    trialNum++;

    if ((outputFile != null) && (trialNum != Main.numTrials())) {
      outputFile = Main.filePrintStream(Main.outputPath() + "-trial" + trialNum
          + ".data");
    }

    trueSum = 0;
    totalSum = 0;
    probTrue = -1;
  }

  private double trueProb() {
    if (probTrue >= 0) {
      return probTrue;
    }
    return Math.exp(trueSum - totalSum);
  }

  // CAREFUL: zeroOut() must be called before using this method
  public void printVarianceResults(PrintStream s) {
    double mean = runningProbSum / trialNum;
    s.println("Mean of " + getArgSpec() + " query results is " + mean);
    s.println("Std dev of " + getArgSpec() + " query results is "
        + Math.sqrt(runningProbSumSquares / trialNum - (mean * mean)));
  }

  public Histogram getHistogram() {
    histogram.clear();
    double logt = trueSum - totalSum;
    histogram.increaseWeight(Boolean.TRUE, logt);
    histogram.increaseWeight(Boolean.FALSE, Math.log(1 - Math.exp(logt)));
    return histogram;
  }

  private double trueSum = Double.NEGATIVE_INFINITY;
  private double totalSum = Double.NEGATIVE_INFINITY;
  private double probTrue = -1;

  private double runningProbSum = Double.NEGATIVE_INFINITY;
  private double runningProbSumSquares = Double.NEGATIVE_INFINITY;
  private int trialNum = 0;
}

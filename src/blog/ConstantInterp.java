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

package blog;

import java.util.*;

import common.Util;

/**
 * Implementation of FunctionInterp for constants (zero-ary functions). In this
 * case, the interpretation is specified by just a single value.
 */
public class ConstantInterp extends AbstractFunctionInterp {
	/**
	 * Expects a single parameter, namely the function value.
	 */
	public ConstantInterp(List params) {
		if (params.size() != 1) {
			throw new IllegalArgumentException(
					"ConstantInterp expects a single parameter.");
		}

		value = params.get(0);
	}

	public Object getValue(List args) {
		if (!args.isEmpty()) {
			throw new IllegalArgumentException("ConstantInterp expects no arguments.");
		}
		return value;
	}

	public Set getInverseTuples(Object v) {
		if (value.equals(v)) {
			return Collections.singleton(Collections.EMPTY_LIST);
		}
		return Collections.EMPTY_SET;
	}

	public boolean equals(Object o) {
		if (!(o instanceof ConstantInterp))
			return false;
		return Util.equalsOrBothNull(value, ((ConstantInterp) o).value);
	}

	public int hashCode() {
		return value.hashCode();
	}

	private Object value;
}

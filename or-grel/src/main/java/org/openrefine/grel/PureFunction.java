/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

package org.openrefine.grel;

import java.util.Properties;

/**
 * Function whose value only depends on the values of its arguments and not of the global state which can be accessed
 * via the context.
 * 
 * Most functions fall into this category: only a few (cross, facetCount…) rely on the context.
 * 
 * @author Antonin Delpeuch
 */
public abstract class PureFunction implements Function {

    /**
     * Computes the value of the function on the given arguments
     * 
     * @param bindings
     *            the evaluation context
     * @param args
     *            the values of the arguments
     * @return the return value of the function
     */
    @Override
    public final Object call(Properties bindings, Object[] args) {
        return call(args);
    }

    /**
     * Computes the value of the function on the given arguments
     * 
     * @param bindings
     *            the evaluation context
     * @param args
     *            the values of the arguments
     * @return the return value of the function
     */
    public abstract Object call(Object[] args);
}

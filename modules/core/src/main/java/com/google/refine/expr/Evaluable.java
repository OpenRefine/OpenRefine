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

package com.google.refine.expr;

import java.util.Optional;
import java.util.Properties;
import java.util.Set;

/**
 * Interface for evaluable expressions in any arbitrary language.
 */
public interface Evaluable {

    /**
     * Evaluate this expression in the given environment (bindings).
     * 
     * @param bindings
     * @return
     */
    public Object evaluate(Properties bindings);

    /**
     * Returns an approximation of the names of the columns this expression depends on. This approximation is designed
     * to be safe: if a set of column names is returned, then the expression does not read any other column than the
     * ones mentioned, regardless of the data it is executed on.
     *
     * @param baseColumn
     *            the name of the column this expression is based on (none if the expression is not evaluated on a
     *            particular column)
     * @return {@link Optional#empty()} if the columns could not be isolated: in this case, the expression might depend
     *         on all columns in the project. Note that this is different from returning an empty set, which means that
     *         the expression is constant.
     */
    public default Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        return Optional.empty();
    }

}

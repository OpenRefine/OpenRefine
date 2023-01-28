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

package org.openrefine.expr;

import java.io.Serializable;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Interface for evaluable expressions in any arbitrary language.
 */
public interface Evaluable extends Serializable {

    /**
     * Evaluate this expression in the given environment (bindings).
     */
    public Object evaluate(Properties bindings);

    /**
     * Returns the source string which generated this expression. This does not include the language prefix, which can
     * be obtained by {@link #getLanguagePrefix()}.
     */
    public String getSource();

    /**
     * @return the language prefix used to generate this evaluable.
     */
    public String getLanguagePrefix();

    /**
     * @return true when the evaluable can be computed fully (and quickly) from the local context (the given row or
     *         record). If it relies on any other information (external web service, aggregation over the project), then
     *         it should return false, indicating that the return value may need to be cached by the caller.
     */
    public default boolean isLocal() {
        return false;
    }

    /**
     * Returns the names of the columns this expression depends on.
     *
     * @param baseColumn
     *            the name of the column this expression is based on (null if none)
     * @return null if the columns could not be isolated: in this case, the expression might depend on all columns in
     *         the project.
     */
    public default Set<String> getColumnDependencies(String baseColumn) {
        return null;
    }

    /**
     * Translates this expression by simultaneously substituting column names as the supplied map specifies.
     * <p>
     * This is only possible if the extraction of column dependencies with {@link #getColumnDependencies(String)}
     * succeeds (return a non-null value).
     *
     * @param substitutions
     *            a map specifying new names for some columns. If a column name is not present in the map, it is assumed
     *            that the column is not renamed.
     * @return a new expression with updated column names.
     */
    public default Evaluable renameColumnDependencies(Map<String, String> substitutions) {
        return null;
    }

    /**
     * @return the source prefixed by the language prefix
     */
    public default String getFullSource() {
        return getLanguagePrefix() + ":" + getSource();
    }
}

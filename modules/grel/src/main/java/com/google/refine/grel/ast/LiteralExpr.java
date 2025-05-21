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

package com.google.refine.grel.ast;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.databind.node.TextNode;

import com.google.refine.expr.Evaluable;

/**
 * An abstract syntax tree node encapsulating a literal value.
 */
public class LiteralExpr extends GrelExpr {

    final protected Object _value;
    final protected String _source;

    /**
     * @deprecated use the version of the constructor which supplies the full source of the literal
     */
    @Deprecated(since = "3.10")
    public LiteralExpr(Object value) {
        _value = value;
        _source = null;
    }

    public LiteralExpr(Object value, String source) {
        _value = value;
        _source = source;
    }

    protected Object getValue() {
        return _value;
    }

    @Override
    public Object evaluate(Properties bindings) {
        return _value;
    }

    @Override
    public Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        return Optional.of(Collections.emptySet());
    }

    @Override
    public Evaluable renameColumnDependencies(Map<String, String> substitutions) {
        return this;
    }

    @Override
    public String toString() {
        if (_source != null) {
            return _source;
        } else {
            return _value instanceof String ? new TextNode((String) _value).toString() : _value.toString();
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(_source);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LiteralExpr other = (LiteralExpr) obj;
        // ignore _value on purpose because it is entirely determined from _source
        return Objects.equals(_source, other._source);
    }
}

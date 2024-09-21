/*

Copyright 2010,2011. Google Inc.
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

import java.util.Properties;

import clojure.java.api.Clojure;
import clojure.lang.IFn;

/**
 * A parser for expressions written in Clojure.
 */
public class ClojureParser implements LanguageSpecificParser {

    // final IFn READ_STRING = Clojure.var("clojure.core", "read-string");
//        final IFn REQUIRE = Clojure.var("clojure.core", "require");
//        Object ignored = REQUIRE.invoke(Clojure.read("clojure.set"));
    final IFn EVAL = Clojure.var("clojure.core", "eval");

    @Override
    public Evaluable parse(String source, String languagePrefix) throws ParsingException {
        try {
            // Although declared to return an Object, in our case we know it will be an IFn of arity 6
            IFn fn = (IFn) EVAL.invoke(Clojure.read("(fn [value cell cells row rowIndex value1 value2] " + source + ")"));

            return new Evaluable() {

                private IFn _fn;

                public Evaluable init(IFn fn) {
                    _fn = fn;
                    return this;
                }

                @Override
                public Object evaluate(Properties bindings) {
                    try {
                        return _fn.invoke(
                                bindings.get("value"),
                                bindings.get("cell"),
                                bindings.get("cells"),
                                bindings.get("row"),
                                bindings.get("rowIndex"),
                                bindings.get("value1"),
                                bindings.get("value2"));
                    } catch (Exception e) {
                        return new EvalError(e.getMessage());
                    }
                }

                @Override
                public String getSource() {
                    return source;
                }

                @Override
                public String getLanguagePrefix() {
                    return languagePrefix;
                }
            }.init(fn);
        } catch (Exception e) {
            throw new ParsingException(e.getMessage());
        }
    }
}

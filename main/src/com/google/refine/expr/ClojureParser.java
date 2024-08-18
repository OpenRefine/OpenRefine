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

import java.io.StringReader;
import java.util.Properties;

import clojure.lang.IFn;
import clojure.lang.RT;

/**
 * A parser for expressions written in Clojure.
 */
public class ClojureParser implements LanguageSpecificParser {

    @Override
    public Evaluable parse(String s) throws ParsingException {
        try {
//                    RT.load("clojure/core"); // Make sure RT is initialized
            Object foo = RT.CURRENT_NS; // Make sure RT is initialized
            IFn fn = (IFn) clojure.lang.Compiler.load(new StringReader(
                    "(fn [value cell cells row rowIndex] " + s + ")"));

            // TODO: We should to switch from using Compiler.load
            // because it's technically an internal interface
//                    Object code = CLOJURE_READ_STRING.invoke(
//                            "(fn [value cell cells row rowIndex] " + s + ")"
//                            );

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
                                bindings.get("rowIndex"));
                    } catch (Exception e) {
                        return new EvalError(e.getMessage());
                    }
                }
            }.init(fn);
        } catch (Exception e) {
            throw new ParsingException(e.getMessage());
        }
    }
}

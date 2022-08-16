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

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.refine.grel.Parser;

import clojure.lang.IFn;
import clojure.lang.RT;

abstract public class MetaParser {

    static public class LanguageInfo {

        @JsonProperty("name")
        final public String name;
        @JsonIgnore
        final public LanguageSpecificParser parser;
        @JsonProperty("defaultExpression")
        final public String defaultExpression;

        LanguageInfo(String name, LanguageSpecificParser parser, String defaultExpression) {
            this.name = name;
            this.parser = parser;
            this.defaultExpression = defaultExpression;
        }
    }

    static final protected Map<String, LanguageInfo> s_languages = new HashMap<String, LanguageInfo>();

    // TODO: We should switch from using the internal compiler class
//    final static private Var CLOJURE_READ_STRING = RT.var("clojure.core", "read-string");
//    final static private Var CLOJURE_EVAL = RT.var("clojure.core", "eval");

    static {
        registerLanguageParser("grel", "General Refine Expression Language (GREL)", new LanguageSpecificParser() {

            @Override
            public Evaluable parse(String s) throws ParsingException {
                return parseGREL(s);
            }
        }, "value");

        registerLanguageParser("clojure", "Clojure", new LanguageSpecificParser() {

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
        }, "value");
    }

    /**
     * languagePrefix will be stored in the meta model as an identifier. so be careful when change it as it will break
     * the backward compatibility for the old project
     * 
     * @param languagePrefix
     * @param name
     * @param parser
     * @param defaultExpression
     */
    static public void registerLanguageParser(String languagePrefix, String name, LanguageSpecificParser parser, String defaultExpression) {
        s_languages.put(languagePrefix, new LanguageInfo(name, parser, defaultExpression));
    }

    static public LanguageInfo getLanguageInfo(String languagePrefix) {
        return s_languages.get(languagePrefix.toLowerCase());
    }

    static public Set<String> getLanguagePrefixes() {
        return s_languages.keySet();
    }

    /**
     * Parse an expression that might have a language prefix into an Evaluable. Expressions without valid prefixes or
     * without any prefix are assumed to be GREL expressions.
     * 
     * @param s
     * @return
     * @throws ParsingException
     */
    static public Evaluable parse(String s) throws ParsingException {
        String language = "grel";

        int colon = s.indexOf(':');
        if (colon >= 0) {
            language = s.substring(0, colon).toLowerCase();
            if ("gel".equals(language)) {
                language = "grel";
            }
        }

        LanguageInfo info = s_languages.get(language.toLowerCase());
        if (info != null) {
            return info.parser.parse(s.substring(colon + 1));
        } else {
            return parseGREL(s);
        }
    }

    static protected Evaluable parseGREL(String s) throws ParsingException {
        Parser parser = new Parser(s);

        return parser.getExpression();
    }
}

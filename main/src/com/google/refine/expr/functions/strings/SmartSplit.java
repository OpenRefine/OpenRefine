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

package com.google.refine.expr.functions.strings;

import java.io.IOException;
import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.Function;

import au.com.bytecode.opencsv.CSVParser;
import com.google.refine.grel.FunctionDescription;

public class SmartSplit implements Function {

    static final protected CSVParser s_tabParser = new CSVParser(
            '\t',
            CSVParser.DEFAULT_QUOTE_CHARACTER,
            CSVParser.DEFAULT_ESCAPE_CHARACTER,
            CSVParser.DEFAULT_STRICT_QUOTES,
            CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
            false);

    static final protected CSVParser s_commaParser = new CSVParser(
            ',',
            CSVParser.DEFAULT_QUOTE_CHARACTER,
            CSVParser.DEFAULT_ESCAPE_CHARACTER,
            CSVParser.DEFAULT_STRICT_QUOTES,
            CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
            false);

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1 && args.length <= 2) {
            CSVParser parser = null;

            Object v = args[0];
            String s = v.toString();

            if (args.length > 1) {
                String sep = args[1].toString();
                parser = new CSVParser(
                        sep,
                        CSVParser.DEFAULT_QUOTE_CHARACTER,
                        CSVParser.DEFAULT_ESCAPE_CHARACTER,
                        CSVParser.DEFAULT_STRICT_QUOTES,
                        CSVParser.DEFAULT_IGNORE_LEADING_WHITESPACE,
                        false);
            }

            if (parser == null) {
                int tab = s.indexOf('\t');
                if (tab >= 0) {
                    parser = s_tabParser;
                } else {
                    parser = s_commaParser;
                }
            }

            try {
                return parser.parseLine(s);
            } catch (IOException e) {
                return new EvalError(EvalErrorMessage.error(ControlFunctionRegistry.getFunctionName(this), e.getMessage()));
            }
        }
        return new EvalError(EvalErrorMessage.expects_one_or_two_strings(ControlFunctionRegistry.getFunctionName(this)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.str_smart_split();
    }

    @Override
    public String getParams() {
        return "string s, optional string sep";
    }

    @Override
    public String getReturns() {
        return "array";
    }
}

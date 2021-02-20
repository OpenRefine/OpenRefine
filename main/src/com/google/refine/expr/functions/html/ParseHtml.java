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

package com.google.refine.expr.functions.html;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.functions.xml.ParseXml;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class ParseHtml implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object o1 = args[0];
            if (o1 != null && o1 instanceof String) {
                return new ParseXml().call(bindings,args,"html");
            }
        }
        return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects a single String as an argument");
    }


    @Override
    public String getDescription() {
        return "Given a cell full of HTML-formatted text, parseHtml() simplifies HTML tags (such as by removing ' /' at the end of self-closing tags), closes any unclosed tags, and inserts linebreaks and indents for cleaner code. A cell cannot store the output of parseHtml() unless you convert it with toString(): for example, value.parseHtml().toString().";
    }
    
    @Override
    public String getParams() {
        return "string s";
    }
    
    @Override
    public String getReturns() {
        return "HTML object";
    }
}


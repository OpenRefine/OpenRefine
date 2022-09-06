/*

Copyright 2010,2011 Google Inc.
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

package com.google.refine.expr.functions.xml;

import java.util.Properties;

import com.google.refine.grel.EvalErrorMessage;
import com.google.refine.grel.FunctionDescription;
import org.jsoup.nodes.Element;

import com.google.refine.expr.functions.Type;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class XmlText implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object o1 = args[0];
            if (o1 != null && o1 instanceof Element) {
                Element e1 = (Element) o1;
                return e1.text();

            } else {
                // new Type().call(bindings, args) + "' and failed as the first parameter is not an XML or HTML Element.
                // Please first use parseXml() or parseHtml() and select(query) prior to using this function");
                return new EvalError(EvalErrorMessage.xml_text_cannot_work_with_and_failed(ControlFunctionRegistry.getFunctionName(this),
                        new Type().call(bindings, args)));
            }
        }
        // Type().call(bindings, args) + "' and expects a single XML or HTML element as an argument");
        return new EvalError(EvalErrorMessage.xml_text_cannot_work_with_and_expects(ControlFunctionRegistry.getFunctionName(this),
                new Type().call(bindings, args)));
    }

    @Override
    public String getDescription() {
        return FunctionDescription.xml_xmltext();
    }

    @Override
    public String getParams() {
        return "Element e";
    }

    @Override
    public String getReturns() {
        return "String text";
    }
}

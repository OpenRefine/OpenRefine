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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class Unescape implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 2) {
            Object o1 = args[0];
            Object o2 = args[1];
            if (o1 != null && o2 != null && o1 instanceof String && o2 instanceof String) {
                String s = (String) o1;
                String mode = ((String) o2).toLowerCase();
                if ("html".equals(mode)) {
                    return StringEscapeUtils.unescapeHtml(s);
                } else if ("xml".equals(mode)) {
                    return StringEscapeUtils.unescapeXml(s);
                } else if ("csv".equals(mode)) {
                    return StringEscapeUtils.unescapeCsv(s);
                } else if ("javascript".equals(mode)) {
                    return StringEscapeUtils.unescapeJavaScript(s);
                } else if ("url".equals(mode)) {
                    try {
                        return URLDecoder.decode(s,"UTF-8");
                    } catch (UnsupportedEncodingException e) {}
                } else {
                    return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " does not recognize mode '" + mode + "'.");
                }
            }
        }
        return null;
    }
    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Unescapes all escaped parts of the string depending on the given escaping mode.");
        writer.key("params"); writer.value("string s, string mode ['html','xml','csv','url','javascript']");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}

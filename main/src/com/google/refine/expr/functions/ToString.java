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

package com.google.refine.expr.functions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.json.JSONException;
import org.json.JSONWriter;

import com.google.refine.grel.Function;

public class ToString implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length >= 1) {
            Object o1 = args[0];
            if (o1 != null) {
                if (o1 instanceof Calendar || o1 instanceof Date) {
                    DateFormat formatter = null;
                    if (args.length == 2) {
                        Object o2 = args[1];
                        if (o2 != null && o2 instanceof String) {
                            formatter = new SimpleDateFormat((String) o2);
                        }
                    }
                    if (formatter == null) {
                        formatter = DateFormat.getDateInstance();
                    }
                    return formatter.format(o1 instanceof Date ? ((Date) o1) : ((Calendar) o1).getTime());
                } else {
                    return (o1 instanceof String) ? o1 : o1.toString();
                }
            }
        }
        return null;
    }

    
    @Override
    public void write(JSONWriter writer, Properties options)
        throws JSONException {
    
        writer.object();
        writer.key("description"); writer.value("Returns o converted to a string");
        writer.key("params"); writer.value("o, string format (optional)");
        writer.key("returns"); writer.value("string");
        writer.endObject();
    }
}

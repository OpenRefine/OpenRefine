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

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;
import com.google.refine.util.ParsingUtilities;

public class ToJson implements Function {

    @Override
    /**
     * Transfor an array of objects into a json array format string
     * @param args the user input
     *        args[0] is an array of objects
     * @return string representing array of objects in json array format
     */
    public Object call(Properties bindings, Object[] args) {
        String error_message = " accepts an array of objects";
        Object result = new EvalError(ControlFunctionRegistry.getFunctionName(this) + error_message);
        if (args.length == 1 && args[0] instanceof Object[]) {
            Object[] array = (Object[]) args[0];
            try{
                result = ParsingUtilities.mapper.writeValueAsString(array);
                String result_str = (String) result;
                // add whitespace based on formatting
                result_str = result_str.replace(",",", ").replace("[","[ ").replace("]"," ]").replace("[  ]","[ ]");
                result = (Object) result_str;
            }catch(JsonProcessingException e){
            	e.printStackTrace();
            }
        }
        return (Object) result;
    }

    @Override
    public String getDescription() {
        return "Takes an array of any value type (string, number, date, boolean, another array, null) and gives a string of the array following the json array format (https://www.json.org/json-en.html). You can convert numbers to strings with rounding, using an optional string format. See https://docs.oracle.com/javase/8/docs/api/java/util/Formatter.html. You can also convert dates to strings using date parsing syntax. See https://docs.openrefine.org/manual/grelfunctions/#date-functions.";
    }
    
    @Override
    public String getParams() {
        return "object o, string format (optional)";
    }
    
    @Override
    public String getReturns() {
        return "string";
    }
}

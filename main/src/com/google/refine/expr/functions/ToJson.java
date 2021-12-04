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
     * Transfer an object into a json format string
     * @param args the user input
     *        args[0] is the object
     * @return string representing an object in json array format
     */
    public Object call(Properties bindings, Object[] args) {
        String error_message = " accepts an object";
        Object result = new EvalError(ControlFunctionRegistry.getFunctionName(this) + error_message);
        if (args.length == 1 && args[0] instanceof Object[]) {
            Object obj = args[0];
            try{
                result = ParsingUtilities.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            }catch(JsonProcessingException e){
            	e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public String getDescription() {
        return "Takes an object and gives a string of this object in json array format (https://www.json.org/json-en.html). ";
    }
    
    @Override
    public String getParams() {
        return "object o";
    }
    
    @Override
    public String getReturns() {
        return "string";
    }
}

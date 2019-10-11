/*

Copyright 2019, Owen Stephens
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
    * Neither the name of the copyright holder nor the names of its
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.google.refine.expr.EvalError;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class GetFields implements Function {

    @Override
    public Object call(Properties bindings, Object[] args) {
        if (args.length == 1) {
            Object v = args[0];
            if (v instanceof ObjectNode){
                ObjectNode o = (ObjectNode)v;
                Iterator<String> fieldNames = o.fieldNames();
                ArrayList<String> fields = new ArrayList<String>();
                while (fieldNames.hasNext()) {
                    String field = fieldNames.next();
                    fields.add(field);
                }
                return fields;
            } else {
                return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects an Object with fields");
            }
        } else {
            return new EvalError(ControlFunctionRegistry.getFunctionName(this) + " expects single Object");
        }
    }

    @Override
    public String getDescription() {
        return 
            "Returns an array of the field/property names available at the top level of object o";
    }
    
    @Override
    public String getParams() {
        return "o, an Object with fields";
    }
    
    @Override
    public String getReturns() {
        return "Array";
    }
}

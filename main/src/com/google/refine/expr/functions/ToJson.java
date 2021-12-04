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
     * Has an optional format argument, can parse an array of numbers or dates in the given format
     * @param args the user input
     *        args[0] is an array of objects
     *        args[1] is optional, it is a string which indicates the format
     * @return string representing array of objects in json array format
     */
    public Object call(Properties bindings, Object[] args) {
        String error_message = " accepts an array of objects and one optional format argument";
        Object result = new EvalError(ControlFunctionRegistry.getFunctionName(this) + error_message);
        if (args.length >= 1 && args[0] instanceof Object[]) {
            Object[] array = (Object[]) args[0];
            int arraySize = array.length;
            String format = null;
            // get format
            if(args.length == 2 && args[1] instanceof String){
                format = (String) args[1];
            }
            // parse format
            boolean[] formatFlag = new boolean[arraySize];
            for (int i = 0; i < arraySize; i++){
                Object element = array[i];
                if (element instanceof OffsetDateTime && format != null) {
                    OffsetDateTime odt = (OffsetDateTime) element;
                    element = (Object) odt.format(DateTimeFormatter.ofPattern(format));
                    formatFlag[i] = true;
                }
                if (element instanceof Number && format != null) {
                    element = String.format(format, (Number) element);
                    formatFlag[i] = true;
                }
                array[i] = element;
            }
            try{
                result = ParsingUtilities.mapper.writeValueAsString(array);
                String[] elements = ((String) result).split(",");
                for(int i = 0; i < arraySize; i++){
                    if(formatFlag[i]){
                        elements[i] = elements[i].replace("\"","");
                    }
                }
                result = String.join(", ", elements);
            }catch(JsonProcessingException e){
            	e.printStackTrace();
            }
        }
        return result;
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

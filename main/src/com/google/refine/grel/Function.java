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

package com.google.refine.grel;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
package com.google.refine.expr.functions;

import java.util.Properties;

import org.codehaus.jackson.JsonNode;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.expr.HasFields;
import com.google.refine.grel.ControlFunctionRegistry;
import com.google.refine.grel.Function;

public class GrelFunctions {
    public static void register(ControlFunctionRegistry registry) {
        registry.registerFunction(new ForEachItem());
    }

    private static class ForEachItem implements Function {

        @Override
        public Object call(Properties bindings, Object[] args) {
            if (args.length != 3) {
                return new EvalError("forEachItem: Wrong number of arguments");
            }
            Object obj = args[0];
            String keyName = args[1].toString();
            String valueName = args[2].toString();
            if (!(obj instanceof JsonNode)) {
                return new EvalError("forEachItem: Argument is not a JSON object");
            }
            JsonNode json = (JsonNode) obj;
            if (!json.isObject()) {
                return new EvalError("forEachItem: Argument is not a JSON object");
            }
            for (String fieldName : ExpressionUtils.getFieldsInJsonObject(json)) {
                JsonNode fieldNode = json.get(fieldName);
                bindings.put(keyName, fieldName);
                bindings.put(valueName, fieldNode);
                if (fieldNode.isObject() && !(fieldNode instanceof HasFields)) {
                    return new EvalError("forEachItem: Cannot iterate over object that does not implement HasFields");
                }
                ExpressionUtils.evaluate(args[2], bindings);
            }
            return null;
        }

        @Override
        public String getDescription() {
            return "Iterates over each item in a JSON object, assigning the string value of the key to the key variable and the value for the item to the value variable and then executing the GREL expression.";
        }
    }
}
/**
 * Interface for functions. When a function is called, its arguments have already been evaluated down into non-error
 * values.
 */
public interface Function {

    public Object call(Properties bindings, Object[] args);

    @JsonProperty("description")
    public String getDescription();

    @JsonProperty("params")
    @JsonInclude(Include.NON_EMPTY)
    default public String getParams() {
        return "";
    }

    @JsonProperty("returns")
    public String getReturns();
}

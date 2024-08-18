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

package com.google.refine.clustering.knn;

import java.util.Properties;

import com.google.refine.expr.Evaluable;
import com.google.refine.expr.MetaParser;
import com.google.refine.expr.ParsingException;

public class UserDefinedDistance implements SimilarityDistance {

    private Evaluable eval;
    private Properties bindings;

    public UserDefinedDistance(String expression) throws ParsingException {
        eval = MetaParser.parse("grel:" + expression);
        bindings = new Properties();

        bindings.put("true", true);
        bindings.put("false", false);
        bindings.put("PI", Math.PI);
    }

    @Override
    public double compute(String a, String b) {
        if (a == null || b == null) {
            throw new IllegalArgumentException("Input strings cannot be null");
        }
        if (a.isEmpty() || b.isEmpty()) {
            throw new IllegalArgumentException("Input strings cannot be empty");
        }

        bindings.put("value1", a);
        bindings.put("value2", b);

        Object result = eval.evaluate(bindings);

        return convertToDouble(result);
    }

    public static double convertToDouble(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("The object is null");
        }

        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        } else if (obj instanceof String) {
            try {
                return Double.parseDouble((String) obj);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("The string cannot be converted to a double", e);
            }
        } else {
            throw new IllegalArgumentException("The object type cannot be converted to a double");
        }
    }
}

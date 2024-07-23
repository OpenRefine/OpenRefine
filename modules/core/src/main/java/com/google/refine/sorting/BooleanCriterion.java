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

package com.google.refine.sorting;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.ExpressionUtils;

public class BooleanCriterion extends Criterion {

    final static protected EvalError s_error = new EvalError("Not a boolean");

    @Override
    public KeyMaker createKeyMaker() {
        return new KeyMaker() {

            @Override
            protected Object makeKey(Object value) {
                if (ExpressionUtils.isNonBlankData(value)) {
                    if (value instanceof Boolean) {
                        return value;
                    } else if (value instanceof String) {
                        return Boolean.parseBoolean((String) value);
                    } else {
                        return s_error;
                    }
                }
                return null;
            }

            @Override
            public int compareKeys(Object key1, Object key2) {
                return ((Boolean) key1).compareTo((Boolean) key2);
            }
        };
    }

    @Override
    public String getValueType() {
        return "boolean";
    }
}

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

package com.google.refine.grel.controls;

import java.util.Properties;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Control;
import com.google.refine.grel.ControlEvalError;
import com.google.refine.grel.ControlFunctionRegistry;

abstract class IsTest implements Control {

    @Override
    public String checkArguments(Evaluable[] args) {
        if (args.length != 1) {
            return ControlEvalError.expects_one_arg(ControlFunctionRegistry.getControlName(this));
        }
        return null;
    }

    @Override
    public Object call(Properties bindings, Evaluable[] args) {
        Object o;
        try {
            o = args[0].evaluate(bindings);
        } catch (Exception e) {
            o = new EvalError(e.toString());
        }
        return test(o);
    }

    @Override
    public String getParams() {
        return "expression o";
    }

    @Override
    public String getReturns() {
        return "boolean";
    }

    abstract protected boolean test(Object v);
}

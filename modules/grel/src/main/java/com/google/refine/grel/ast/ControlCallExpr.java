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

package com.google.refine.grel.ast;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import com.google.refine.expr.EvalError;
import com.google.refine.expr.Evaluable;
import com.google.refine.grel.Control;

/**
 * An abstract syntax tree node encapsulating a control call, such as "if".
 */
public class ControlCallExpr extends GrelExpr {

    final protected Evaluable[] _args;
    final protected Control _control;
    final protected String _controlName;

    /**
     * @deprecated use the version that supplies the name under which the control was invoked
     */
    @Deprecated
    public ControlCallExpr(Evaluable[] args, Control c) {
        _args = args;
        _control = c;
        _controlName = _control.getClass().getSimpleName();
    }

    /**
     * @param args
     *            the arguments of the control
     * @param c
     *            the control itself
     * @param controlName
     *            the name with which the control was referred to
     */
    public ControlCallExpr(Evaluable[] args, Control c, String controlName) {
        _args = args;
        _control = c;
        _controlName = controlName;
    }

    @Override
    public Object evaluate(Properties bindings) {
        try {
            return _control.call(bindings, _args);
        } catch (Exception e) {
            return new EvalError(e.toString());
        }
    }

    @Override
    public final Optional<Set<String>> getColumnDependencies(Optional<String> baseColumn) {
        Set<String> dependencies = new HashSet<>();
        for (Evaluable ev : _args) {
            Optional<Set<String>> deps = ev.getColumnDependencies(baseColumn);
            if (deps.isEmpty()) {
                return Optional.empty();
            }
            dependencies.addAll(deps.get());
        }
        return Optional.of(dependencies);
    }

    @Override
    public Optional<Evaluable> renameColumnDependencies(Map<String, String> substitutions) {
        Evaluable[] translatedArgs = new Evaluable[_args.length];
        for (int i = 0; i != _args.length; i++) {
            Optional<Evaluable> translatedArg = _args[i].renameColumnDependencies(substitutions);
            if (translatedArg.isEmpty()) {
                return Optional.empty();
            } else {
                translatedArgs[i] = translatedArg.get();
            }
        }
        return Optional.of(new ControlCallExpr(translatedArgs, _control, _controlName));
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();

        for (Evaluable ev : _args) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(ev.toString());
        }

        return _controlName + "(" + sb.toString() + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(_args);
        result = prime * result + Objects.hash(_control, _controlName);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ControlCallExpr other = (ControlCallExpr) obj;
        return Arrays.equals(_args, other._args) && Objects.equals(_control, other._control)
                && Objects.equals(_controlName, other._controlName);
    }

}

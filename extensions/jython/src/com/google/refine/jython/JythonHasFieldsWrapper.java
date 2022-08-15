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

package com.google.refine.jython;

import java.util.Properties;

import org.python.core.Py;
import org.python.core.PyObject;

import com.google.refine.expr.HasFields;

public class JythonHasFieldsWrapper extends PyObject {

    private static final long serialVersionUID = -1275353513262385099L;

    public HasFields _obj;

    private Properties _bindings;

    public JythonHasFieldsWrapper(HasFields obj, Properties bindings) {
        _obj = obj;
        _bindings = bindings;
    }

    @Override
    public PyObject __finditem__(PyObject key) {
        String k = (String) key.__tojava__(String.class);
        return __findattr_ex__(k);
    }

    @Override
    public PyObject __findattr_ex__(String name) {
        Object v = _obj.getField(name, _bindings);
        if (v != null) {
            if (v instanceof PyObject) {
                return (PyObject) v;
            } else if (v instanceof HasFields) {
                return new JythonHasFieldsWrapper((HasFields) v, _bindings);
            } else if (Py.getAdapter().canAdapt(v)) {
                return Py.java2py(v);
            } else {
                return new JythonObjectWrapper(v);
            }
        } else {
            return null;
        }
    }
}

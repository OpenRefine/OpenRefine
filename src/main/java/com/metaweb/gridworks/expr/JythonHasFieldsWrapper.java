package com.metaweb.gridworks.expr;

import java.util.Properties;

import org.python.core.Py;
import org.python.core.PyObject;

public class JythonHasFieldsWrapper extends PyObject {
    private static final long serialVersionUID = -1275353513262385099L;

    public HasFields _obj;

    private Properties _bindings;

    public JythonHasFieldsWrapper(HasFields obj, Properties bindings) {
        _obj = obj;
        _bindings = bindings;
    }

    public PyObject __finditem__(PyObject key) {
        String k = (String) key.__tojava__(String.class);
        Object v = _obj.getField(k, _bindings);
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

package com.metaweb.gridworks.expr;

import org.python.core.PyObject;

public class JythonObjectWrapper extends PyObject {
    private static final long serialVersionUID = -6608115027151667441L;
    
    public Object  _obj;
	
	public JythonObjectWrapper(Object obj) {
		_obj = obj;
	}
	
	public String toString() {
		return _obj.getClass().getSimpleName();
	}
}

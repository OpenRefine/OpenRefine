package com.metaweb.gridworks.expr;

import org.python.core.PyObject;

public class JythonObjectWrapper extends PyObject {
	public Object  _obj;
	
	public JythonObjectWrapper(Object obj) {
		_obj = obj;
	}
	
	public String toString() {
		return _obj.getClass().getSimpleName();
	}
}

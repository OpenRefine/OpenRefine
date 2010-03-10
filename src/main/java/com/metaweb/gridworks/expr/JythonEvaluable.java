package com.metaweb.gridworks.expr;

import java.util.Enumeration;
import java.util.Properties;

import org.python.core.PyException;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

public class JythonEvaluable implements Evaluable {

	private String _code;
	private static final String _functionName = "temp";
    private static PythonInterpreter _engine = new PythonInterpreter();


    public JythonEvaluable(String s) {
    	// indent and create a function out of the code
    	String[] lines = s.split("\r\n|\r|\n");
    	StringBuilder function = new StringBuilder();
    	function.append("def " + _functionName + "():");
    	for (int i = 0; i < lines.length; i++) {
    		function.append("\n    " + lines[i]);    		
    	}
    	_code = function.toString();
    }
    
	public Object evaluate(Properties bindings) {
		try {
			for (Enumeration<Object> e = bindings.keys() ; e.hasMoreElements() ;) {
				Object k = e.nextElement();
				Object v = bindings.get(k);
				if (v instanceof HasFields) {
					_engine.set((String)k, new JythonHasFieldsWrapper((HasFields) v, bindings));
				} else {
					_engine.set((String) k, v);
				}
			}
			_engine.exec(_code);
			
			Object result = _engine.eval(_functionName + "()");

			// unwrap the underlying Java objects
			if (result instanceof JythonObjectWrapper) {
		        result = ((JythonObjectWrapper) result)._obj;
			} else if (result instanceof JythonHasFieldsWrapper) {
	            result = ((JythonHasFieldsWrapper) result)._obj;
			}
			
			return result;
		} catch (PyException e) {
			return new EvalError(e.toString());
		}
	}

}

package com.metaweb.gridworks.expr;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyNone;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;

public class JythonEvaluable implements Evaluable {
	private static final String s_functionName = "___temp___";
	
    private static PythonInterpreter _engine; 
    static {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("mac os x")) {
            File libPath = new File("lib/jython");
            if (libPath.getAbsolutePath().contains("/Gridworks.app/Contents/Resources/")) {
                Properties props = new Properties();
                props.setProperty("python.path", libPath.getAbsolutePath());
                
                PythonInterpreter.initialize(System.getProperties(),props, new String[] { "" });
            }
        }
        _engine = new PythonInterpreter();
    }

    public JythonEvaluable(String s) {
        // indent and create a function out of the code
        String[] lines = s.split("\r\n|\r|\n");
        
        StringBuffer sb = new StringBuffer(1024);
        sb.append("def ");
        sb.append(s_functionName);
        sb.append("(value, cell, cells, row, rowIndex):");
        for (int i = 0; i < lines.length; i++) {
            sb.append("\n  ");
            sb.append(lines[i]);
        }

        _engine.exec(sb.toString());
    }
    
    public Object evaluate(Properties bindings) {
        try {
            // call the temporary PyFunction directly
            Object result = ((PyFunction)_engine.get(s_functionName)).__call__(
                new PyObject[] {
                    Py.java2py( bindings.get("value") ),
                    new JythonHasFieldsWrapper((HasFields) bindings.get("cell"), bindings),
                    new JythonHasFieldsWrapper((HasFields) bindings.get("cells"), bindings),
                    new JythonHasFieldsWrapper((HasFields) bindings.get("row"), bindings),
                    Py.java2py( bindings.get("rowIndex") )
                }
            );

            return unwrap(result);
        } catch (PyException e) {
            return new EvalError(e.getMessage());
        }
    }
    
    protected Object unwrap(Object result) {
        if (result != null) {
            if (result instanceof JythonObjectWrapper) {
                return ((JythonObjectWrapper) result)._obj;
            } else if (result instanceof JythonHasFieldsWrapper) {
                return ((JythonHasFieldsWrapper) result)._obj;
            } else if (result instanceof PyString) {
                return ((PyString) result).asString();
            } else if (result instanceof PyObject) {
                return unwrap((PyObject) result);
            }
        }
        
        return result;	    
    }
    
    protected Object unwrap(PyObject po) {
        if (po instanceof PyNone) {
            return null;
        } else if (po.isNumberType()) {
            return po.asDouble();
        } else if (po.isSequenceType()) {
            Iterator<PyObject> i = po.asIterable().iterator();
            
            List<Object> list = new ArrayList<Object>();
            while (i.hasNext()) {
                list.add(unwrap((Object) i.next()));
            }
            
            return list.toArray();
        } else {
            return po;
        }
    }
}

package com.metaweb.gridlock;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Properties;

import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * A utility class for encapsulating a Javascript object that can
 * then be pretty-printed out through an IndentWriter.
 * 
 * @author dfhuynh
 */
public class JSObject extends Properties {
    private static final long serialVersionUID = 5864375136126385719L;

    static public void writeJSObject(IndentWriter writer, JSObject jso) throws IOException, JSONException {
        writer.println("{");
        writer.indent();
        {
            Enumeration e = jso.propertyNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                Object value = jso.get(name);
                
                writer.print("'");
                writer.print(name + "' : ");
                writeObject(writer, value);
                
                if (e.hasMoreElements()) {
                	writer.println(",");
                } else {
                	writer.println();
                }
            }
        }
        writer.unindent();
        writer.print("}");
    }
    
    static public void writeCollection(IndentWriter writer, Collection c) throws IOException, JSONException {
    	writer.println("[");
    	writer.indent();
        {
            Iterator i = c.iterator();
            while (i.hasNext()) {
                writeObject(writer, i.next());
                if (i.hasNext()) {
                	writer.println(",");
                } else {
                	writer.println();
                }
            }
        }
        writer.unindent();
        writer.print("]");
    }
    
    static public void writeJSONObject(IndentWriter writer, JSONObject no) throws IOException, JSONException {
        writer.println("{");
        writer.indent();
        {
        	String[] names = no.getNames(no);
			for (int i = 0; i < names.length; i++) {
				String name = names[i];
				Object value = no.get(name);
				
				writer.print("'");
                writer.print(name + "' : ");
                writeObject(writer, value);
                
                if (i < names.length - 1) {
                	writer.println(",");
                } else {
                	writer.println();
                }
			}
        }
        writer.unindent();
        writer.print("}");
    }
    
    static public void writeJSONArray(IndentWriter writer, JSONArray na) throws IOException, JSONException {
    	writer.println("[");
    	writer.indent();
        {
			int count = na.length();
			for (int i = 0; i < count; i++) {
				Object element = na.get(i);
				
                writeObject(writer, element);
                if (i < count - 1) {
                	writer.println(",");
                } else {
                	writer.println();
                }
            }
        }
        writer.unindent();
        writer.print("]");
    }
    
    static public void writeObject(IndentWriter writer, Object o) throws IOException, JSONException {
	    if (o == null) {
	    	writer.print("null");
	    } else if (o instanceof Boolean) {
        	writer.print(((Boolean) o).booleanValue() ? "true" : "false");
	    } else if (o instanceof Number) {
        	writer.print(((Number) o).toString());
        	
        } else if (o instanceof Collection) {
        	writeCollection(writer, (Collection) o);
        } else if (o instanceof JSONArray) {
        	writeJSONArray(writer, (JSONArray) o);
        } else if (o instanceof JSObject) {
            writeJSObject(writer, (JSObject) o);
        } else if (o instanceof JSONObject) {
            writeJSONObject(writer, (JSONObject) o);
            
        } else {
        	writer.print("\"" + StringEscapeUtils.escapeJavaScript(o.toString()) + "\"");
        }
    }
}

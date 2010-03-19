package com.metaweb.gridworks.expr;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;

public class ExpressionUtils {
    static public Properties createBindings(Project project) {
        Properties bindings = new Properties();
        
        bindings.put("true", true);
        bindings.put("false", false);
        
        bindings.put("project", project);
        
        return bindings;
    }
    
    static public void bind(Properties bindings, Row row, int rowIndex, Cell cell) {
        bindings.put("row", row);
        bindings.put("rowIndex", rowIndex);
        bindings.put("cells", row.getField("cells", bindings));
        
        if (cell == null) {
            bindings.remove("cell");
            bindings.remove("value");
        } else {
            bindings.put("cell", cell);
            if (cell.value == null) {
                bindings.remove("value");
            } else {
                bindings.put("value", cell.value);
            }
        }
    }
    
    static public boolean isError(Object o) {
        return o != null && o instanceof EvalError;
    }
    /*
    static public boolean isBlank(Object o) {
        return o == null || (o instanceof String && ((String) o).length() == 0);
    }
    */
    static public boolean isNonBlankData(Object o) {
        return 
            o != null && 
            !(o instanceof EvalError) &&
            (!(o instanceof String) || ((String) o).length() > 0);
    }

    static public boolean isTrue(Object o) {
        return o != null && 
            (o instanceof Boolean ? 
                ((Boolean) o).booleanValue() : 
                Boolean.parseBoolean(o.toString()));
    }
    
    static public boolean sameValue(Object v1, Object v2) {
    	if (v1 == null) {
    		return (v2 == null) || (v2 instanceof String && ((String) v2).length() == 0);
    	} else if (v2 == null) {
    		return (v1 == null) || (v1 instanceof String && ((String) v1).length() == 0);
    	} else {
    		return v1.equals(v2);
    	}
    }
    
    static public boolean isStorable(Object v) {
        return v == null ||
            v instanceof Number ||
            v instanceof String ||
            v instanceof Boolean ||
            v instanceof Date ||
            v instanceof Calendar ||
            v instanceof EvalError;
    }
    
    static public Serializable wrapStorable(Object v) {
        return isStorable(v) ? 
            (Serializable) v : 
            new EvalError(v.getClass().getSimpleName() + " value not storable");
    }
    
    @SuppressWarnings("unchecked")
	static public List<Object> toObjectList(Object v) {
    	return (List<Object>) v;
    }
    
    @SuppressWarnings("unchecked")
	static public Collection<Object> toObjectCollection(Object v) {
    	return (Collection<Object>) v;
    }
}

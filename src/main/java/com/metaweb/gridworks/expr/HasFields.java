package com.metaweb.gridworks.expr;

import java.util.Properties;

/**
 * Interface for objects that have named fields, which can be retrieved using the 
 * dot notation or the bracket notation, e.g., cells.Country, cells["Type of Disaster"].
 */
public interface HasFields {
    public Object getField(String name, Properties bindings);
}

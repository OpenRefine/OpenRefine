package com.metaweb.gridworks.gel;

import java.util.Properties;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.Evaluable;

/**
 * Interface of GEL controls such as if, forEach, forNonBlank, with. A control can
 * decide which part of the code to execute and can affect the environment bindings.
 * Functions, on the other hand, can't do either.
 */
public interface Control extends Jsonizable {
    public Object call(Properties bindings, Evaluable[] args);
    
    public String checkArguments(Evaluable[] args);
}

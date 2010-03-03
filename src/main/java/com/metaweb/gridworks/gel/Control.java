package com.metaweb.gridworks.gel;

import java.util.Properties;

import com.metaweb.gridworks.Jsonizable;
import com.metaweb.gridworks.expr.Evaluable;

public interface Control extends Jsonizable {
    public Object call(Properties bindings, Evaluable[] args);
    
    public String checkArguments(Evaluable[] args);
}

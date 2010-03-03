package com.metaweb.gridworks.gel;

import java.util.Properties;

import com.metaweb.gridworks.Jsonizable;

public interface Function extends Jsonizable {
    public Object call(Properties bindings, Object[] args);
}

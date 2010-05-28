package com.metaweb.gridworks.gel;

import java.util.Properties;

import com.metaweb.gridworks.Jsonizable;

/**
 * Interface for functions. When a function is called, its arguments have already
 * been evaluated down into non-error values.
 */
public interface Function extends Jsonizable {
    public Object call(Properties bindings, Object[] args);
}

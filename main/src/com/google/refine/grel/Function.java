package com.google.refine.grel;

import java.util.Properties;

import com.google.refine.Jsonizable;

/**
 * Interface for functions. When a function is called, its arguments have already
 * been evaluated down into non-error values.
 */
public interface Function extends Jsonizable {
    public Object call(Properties bindings, Object[] args);
}

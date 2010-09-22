package com.google.refine.expr;

/**
 * Interface for objects each of which is a list of HasFields objects of the
 * same kind (e.g., list of cells). Its getField method thus returns either
 * another HasFieldsList object or an array or java.util.List of objects. 
 */
public interface HasFieldsList extends HasFields {
    public int length();
    
    public HasFields get(int index);
    
    public HasFieldsList getSubList(int from, int to);
}

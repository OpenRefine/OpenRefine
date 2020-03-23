package com.google.refine.util;

/**
 * Thrown when there is an error on a project lookup.
 */
public class LookupException extends Exception {

    /**
     * Default LookupException format exception.
     */
    public LookupException() { super(); }

    /**
     * LookupException exception.
     *
     * @param message error message
     */
    public LookupException(String message) { super(message); }
}
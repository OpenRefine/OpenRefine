package com.metaweb.gridworks.expr;

// Taken from http://icecube.wisc.edu/~dglo/software/calparse/index.html
// Copyright Dave Glowacki. Released under the BSD license.

/**
 * Thrown when an invalid date is encountered in <tt>CalendarParser</tt>.
 */
public class CalendarParserException extends Exception {

    private static final long serialVersionUID = 7195725880623801198L;

    /**
     * Default date format exception.
     */
    public CalendarParserException() { super(); }
    
    /**
     * Date format exception.
     *
     * @param str error message
     */
    public CalendarParserException(String str) { super(str); }
}

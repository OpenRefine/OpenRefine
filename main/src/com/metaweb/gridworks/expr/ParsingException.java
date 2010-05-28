/**
 * 
 */
package com.metaweb.gridworks.expr;

public class ParsingException extends Exception {
    private static final long serialVersionUID = 155004505172098755L;

    public ParsingException(String message) {
        super(message);
    }
    
    public ParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
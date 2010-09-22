package com.google.refine.importers;

/**
 * Exception thrown by importers.  Typically contains a nested exception
 * indicating the underlying cause of the problem.
 */
public class ImportException extends Exception {

	private static final long serialVersionUID = 7077314805989174181L;

	public ImportException(String message, Throwable cause) {
        super(message, cause);
    }

}

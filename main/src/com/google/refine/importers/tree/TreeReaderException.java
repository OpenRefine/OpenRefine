package com.google.refine.importers.tree;


/**
 * An Exception from the TreeReader interface methods.
 */
public class TreeReaderException extends Exception {

    private static final long serialVersionUID = 1L;

    public TreeReaderException(String message, Throwable cause) {
        super(message, cause);
    }

    public TreeReaderException(String message) {
        super(message);
    }

    public TreeReaderException(Throwable cause) {
        super(cause);
    }

}

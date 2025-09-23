
package com.google.refine.exporters;

/**
 * Exception thrown by exporters to indicate a failure or error condition during export to distinguish them from
 * completely unexpected/unhandled errors. These might include things like row or column limits exceeded. Exceptions
 * thrown during the export which are not instances or subclasses of this will cause HTTP 500 server error.
 */
public class ExporterException extends RuntimeException {

    public ExporterException(String message) {
        super(message);
    }
}

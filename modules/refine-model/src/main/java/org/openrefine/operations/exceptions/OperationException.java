
package org.openrefine.operations.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Exception thrown by an operation when it fails to apply.
 * 
 * @author Antonin Delpeuch
 *
 */
public class OperationException extends Exception {

    private static final long serialVersionUID = 1L;
    protected final String code;

    public OperationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public OperationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    /**
     * A code characterizing the type of error encountered.
     * 
     * @return
     */
    @JsonProperty("code")
    public String getCode() {
        return code;
    }

    /**
     * A human-readable message describing the error.
     */
    @JsonProperty("message")
    @Override
    public String getMessage() {
        return super.getMessage();
    }
}

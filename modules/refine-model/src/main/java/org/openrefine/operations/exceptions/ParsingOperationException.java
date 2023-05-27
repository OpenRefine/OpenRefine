
package org.openrefine.operations.exceptions;

import org.openrefine.expr.ParsingException;

/**
 * An exception thrown when a {@link ParsingException} is thrown from an expression relied on by the operation.
 */
public class ParsingOperationException extends OperationException {

    private static final long serialVersionUID = 206243747651627150L;

    public ParsingOperationException(ParsingException cause) {
        super("parsing", cause.getMessage(), cause);
    }

}

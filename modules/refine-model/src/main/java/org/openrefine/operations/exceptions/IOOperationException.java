
package org.openrefine.operations.exceptions;

import java.io.IOException;

/**
 * Thrown by exceptions when they encounter an IO error while retrieving the statically computed data they require.
 */
public class IOOperationException extends OperationException {

    private static final long serialVersionUID = 8439161706781825026L;

    public IOOperationException(IOException e) {
        super("io", e.getMessage(), e);
    }

}

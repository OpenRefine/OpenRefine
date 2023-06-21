
package org.openrefine.operations.exceptions;

/**
 * Exception thrown by operations while computing their associated change data, when they want the problem to halt all
 * computations and report a message to the user.
 */
public class ChangeDataFetchingException extends RuntimeException {

    private static final long serialVersionUID = -3284333413657491359L;

    boolean offerResuming;

    /**
     * Constructor
     * 
     * @param message
     *            the error message to report to the user
     * @param resumptionSupported
     *            whether or not it makes sense to start a new process to complete the existing change data. If not, the
     *            user will only have the option to cancel the process and the associated operation.
     */
    public ChangeDataFetchingException(String message, boolean resumptionSupported) {
        super(message);
    }
}

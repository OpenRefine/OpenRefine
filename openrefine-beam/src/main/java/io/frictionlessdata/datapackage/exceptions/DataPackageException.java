package io.frictionlessdata.datapackage.exceptions;

/**
 *
 *
 */
public class DataPackageException extends Exception {

    /**
     * Creates a new instance of <code>DataPackageException</code> without
     * detail message.
     */
    public DataPackageException() {
    }

    /**
     * Constructs an instance of <code>DataPackageException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public DataPackageException(String msg) {
        super(msg);
    }
}
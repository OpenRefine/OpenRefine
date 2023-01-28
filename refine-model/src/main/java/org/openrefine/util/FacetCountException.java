
package org.openrefine.util;

/**
 * Exception thrown while accessing the facet counts from an expression.
 * 
 *
 */
public class FacetCountException extends Exception {

    private static final long serialVersionUID = -7028339501993354529L;

    /**
     * FacetCountException
     *
     * @param message
     *            error message
     */
    public FacetCountException(String message) {
        super(message);
    }
}

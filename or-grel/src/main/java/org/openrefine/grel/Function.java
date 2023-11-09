
package org.openrefine.grel;

import java.util.Properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Interface for functions. When a function is called, its arguments have already been evaluated down into non-error
 * values.
 */
public interface Function {

    /**
     * Computes the value of the function on the given arguments
     * 
     * @param bindings
     *            the evaluation context
     * @param args
     *            the values of the arguments
     * @return the return value of the function
     */
    public Object call(Properties bindings, Object[] args);

    @JsonProperty("description")
    public String getDescription();

    @JsonProperty("params")
    @JsonInclude(Include.NON_EMPTY)
    default public String getParams() {
        return "";
    }

    @JsonProperty("returns")
    public String getReturns();
}

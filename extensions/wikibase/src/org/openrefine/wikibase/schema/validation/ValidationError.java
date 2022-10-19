
package org.openrefine.wikibase.schema.validation;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A sign that a schema is incomplete or incorrect, which is described by an error message, as well as a list of steps
 * to reach the place where the problem is, from the root of the schema.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ValidationError {

    private final List<PathElement> path;
    private final String message;

    public ValidationError(List<PathElement> path, String message) {
        this.path = path;
        this.message = message;
    }

    @JsonProperty("path")
    List<PathElement> getPath() {
        return path;
    }

    @JsonProperty("message")
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ValidationError [path=" + path + ", message=" + message + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(message, path);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ValidationError other = (ValidationError) obj;
        return Objects.equals(message, other.message) && Objects.equals(path, other.path);
    }

}

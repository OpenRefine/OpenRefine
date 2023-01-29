
package org.openrefine.overlay;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * An overlay model that is unknown to the current OpenRefine instance, but might be interpretable by another instance
 * (for instance, a later version of OpenRefine, or using an extension).
 * 
 * This class holds the JSON serialization of the overlay model, in the interest of being able to serialize it later,
 * hence avoiding to discard it and lose metadata.
 * 
 *
 */
public class UnknownOverlayModel implements OverlayModel {

    // Map storing the JSON serialization of the operation in an agnostic way
    private Map<String, Object> properties;

    @JsonCreator
    public UnknownOverlayModel() {
        properties = new HashMap<>();
    }

    @JsonAnySetter
    public void setAttribute(String key, Object value) {
        properties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getAttributes() {
        return properties;
    }
}


package org.openrefine.wikibase.schema.entityvalues;

import org.wikidata.wdtk.datamodel.implementation.SnakImpl;
import org.wikidata.wdtk.datamodel.implementation.ValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A tweaked version of {@link SnakImpl} that serializes the full property (not just its PID), so that we can also get
 * the label for that property and display it in the UI without having to query the remote server.
 * 
 * @author Antonin Delpeuch
 *
 */
public class FullyPropertySerializingValueSnak extends ValueSnakImpl {

    public FullyPropertySerializingValueSnak(PropertyIdValue property, Value value) {
        super(property, value);
    }

    @JsonProperty("full_property")
    public PropertyIdValue getFullPropertyId() {
        return getPropertyId();
    }
}

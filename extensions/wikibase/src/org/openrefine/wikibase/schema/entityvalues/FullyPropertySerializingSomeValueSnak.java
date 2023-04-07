
package org.openrefine.wikibase.schema.entityvalues;

import org.wikidata.wdtk.datamodel.implementation.SomeValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FullyPropertySerializingSomeValueSnak extends SomeValueSnakImpl {

    public FullyPropertySerializingSomeValueSnak(PropertyIdValue property) {
        super(property);
    }

    @JsonProperty("full_property")
    public PropertyIdValue getFullPropertyId() {
        return getPropertyId();
    }
}

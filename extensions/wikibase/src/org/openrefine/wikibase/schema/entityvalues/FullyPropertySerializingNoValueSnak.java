
package org.openrefine.wikibase.schema.entityvalues;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.wikidata.wdtk.datamodel.implementation.NoValueSnakImpl;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class FullyPropertySerializingNoValueSnak extends NoValueSnakImpl {

    public FullyPropertySerializingNoValueSnak(PropertyIdValue property) {
        super(property);
    }

    @JsonProperty("full_property")
    public PropertyIdValue getFullPropertyId() {
        return getPropertyId();
    }
}


package org.openrefine.wikibase.schema.entityvalues;

import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.DatatypeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.LexemeIdValue;
import org.wikidata.wdtk.datamodel.interfaces.SenseIdValue;

public class SuggestedSenseIdValue extends SuggestedEntityIdValue implements SenseIdValue {

    private SenseIdValue parsedId;

    public SuggestedSenseIdValue(String id, String siteIRI, String label) {
        super(id, siteIRI, label);
        EntityIdValue parsed = EntityIdValueImpl.fromId(id, siteIRI);
        if (parsed instanceof SenseIdValue) {
            parsedId = (SenseIdValue) parsed;
        } else {
            throw new IllegalArgumentException(String.format("Invalid id for a form: %s", id));
        }
    }

    @Override
    public LexemeIdValue getLexemeId() {
        return parsedId.getLexemeId();
    }

    @Override
    public String getEntityType() {
        return DatatypeIdValue.DT_SENSE;
    }

}

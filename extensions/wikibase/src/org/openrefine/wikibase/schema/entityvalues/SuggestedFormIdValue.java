
package org.openrefine.wikibase.schema.entityvalues;

import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.FormIdValue;
import org.wikidata.wdtk.datamodel.interfaces.LexemeIdValue;

public class SuggestedFormIdValue extends SuggestedEntityIdValue implements FormIdValue {

    private FormIdValue parsedId;

    public SuggestedFormIdValue(String id, String siteIRI, String label) {
        super(id, siteIRI, label);
        EntityIdValue parsed = EntityIdValueImpl.fromId(id, siteIRI);
        if (parsed instanceof FormIdValue) {
            parsedId = (FormIdValue) parsed;
        } else {
            throw new IllegalArgumentException(String.format("Invalid id for a form: %s", id));
        }
    }

    @Override
    public String getEntityType() {
        return ET_FORM;
    }

    @Override
    public LexemeIdValue getLexemeId() {
        return parsedId.getLexemeId();
    }

}

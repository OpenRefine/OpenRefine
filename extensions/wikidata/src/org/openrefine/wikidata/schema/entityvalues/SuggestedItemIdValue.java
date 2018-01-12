package org.openrefine.wikidata.schema.entityvalues;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class SuggestedItemIdValue extends SuggestedEntityIdValue implements ItemIdValue {

    public SuggestedItemIdValue(String id, String siteIRI, String label) {
        super(id, siteIRI, label);
    }

    @Override
    public String getEntityType() {
        return ET_ITEM;
    }

}

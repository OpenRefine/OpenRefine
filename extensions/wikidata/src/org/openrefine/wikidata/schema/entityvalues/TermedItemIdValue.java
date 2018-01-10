package org.openrefine.wikidata.schema.entityvalues;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class TermedItemIdValue extends TermedEntityIdValue implements ItemIdValue {

    public TermedItemIdValue(String id, String siteIRI, String label) {
        super(id, siteIRI, label);
    }

    @Override
    public String getEntityType() {
        return ET_ITEM;
    }

}

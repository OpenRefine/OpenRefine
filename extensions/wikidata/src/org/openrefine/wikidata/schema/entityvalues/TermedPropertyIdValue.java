package org.openrefine.wikidata.schema.entityvalues;

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

public class TermedPropertyIdValue extends TermedEntityIdValue implements PropertyIdValue {

    public TermedPropertyIdValue(String id, String siteIRI, String label) {
        super(id, siteIRI, label);
    }

    @Override
    public String getEntityType() {
        return ET_PROPERTY;
    }
}

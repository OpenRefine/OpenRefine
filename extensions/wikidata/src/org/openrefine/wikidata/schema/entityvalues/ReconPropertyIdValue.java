package org.openrefine.wikidata.schema.entityvalues;

import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;

import com.google.refine.model.Recon;

public class ReconPropertyIdValue extends ReconEntityIdValue implements PropertyIdValue {

    public ReconPropertyIdValue(Recon recon, String cellValue) {
        super(recon, cellValue);
    }

    @Override
    public String getEntityType() {
        return ET_PROPERTY;
    }
}

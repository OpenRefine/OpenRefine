package org.openrefine.wikidata.schema.entityvalues;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.google.refine.model.Recon;

public class ReconItemIdValue extends ReconEntityIdValue implements ItemIdValue {

    public ReconItemIdValue(Recon recon, String cellValue) {
        super(recon, cellValue);
    }

    @Override
    public String getEntityType() {
        return ET_ITEM;
    }

}

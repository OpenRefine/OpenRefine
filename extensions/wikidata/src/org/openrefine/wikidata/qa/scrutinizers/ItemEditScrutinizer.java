package org.openrefine.wikidata.qa.scrutinizers;

import java.util.List;

import org.openrefine.wikidata.schema.ItemUpdate;

public abstract class ItemEditScrutinizer extends EditScrutinizer {

    @Override
    public void scrutinize(List<ItemUpdate> edit) {
        for(ItemUpdate update : edit) {
            if(!update.isNull()) {
                scrutinize(update);
            }
        }
    }

    public abstract void scrutinize(ItemUpdate update);  
}

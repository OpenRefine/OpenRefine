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
        batchIsFinished();
    }

    /**
     * Method to be overridden by subclasses to scrutinize 
     * an individual item update.
     * @param update
     */
    public abstract void scrutinize(ItemUpdate update);
    
    /**
     * Method to be overridden by subclasses to emit warnings
     * once a batch has been completely analyzed.
     */
    public void batchIsFinished() {
        ;
    }
}

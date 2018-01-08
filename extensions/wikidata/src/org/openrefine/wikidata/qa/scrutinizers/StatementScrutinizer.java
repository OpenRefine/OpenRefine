package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class StatementScrutinizer extends ItemEditScrutinizer {

    @Override
    public void scrutinize(ItemUpdate update) {
        for(Statement statement : update.getAddedStatements()) {
            scrutinizeAdded(statement);
        }
    }

    public abstract void scrutinizeAdded(Statement statement);
    
    public abstract void scrutinizeDeleted(Statement statement);
}

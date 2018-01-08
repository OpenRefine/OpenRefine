package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

public abstract class StatementGroupScrutinizer extends ItemEditScrutinizer {

    @Override
    public void scrutinize(ItemUpdate update) {
        for(StatementGroup statementGroup : update.getAddedStatementGroups()) {
            scrutinizeAdded(statementGroup);
        }
        
    }

    public abstract void scrutinizeAdded(StatementGroup statementGroup);

    public abstract void scrutinizeDeleted(StatementGroup statementGroup);
}

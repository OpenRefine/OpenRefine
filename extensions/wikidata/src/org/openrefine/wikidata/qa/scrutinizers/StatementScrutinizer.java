package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class StatementScrutinizer extends ItemEditScrutinizer {

    @Override
    public void scrutinize(ItemUpdate update) {
        EntityIdValue currentEntityId = update.getItemId();
        for(Statement statement : update.getAddedStatements()) {
            scrutinize(statement, currentEntityId, true);
        }
        for (Statement statement : update.getDeletedStatements()) {
            scrutinize(statement, currentEntityId, false);
        }
    }

    /**
     * The method that should be overridden by subclasses, implementing
     * the checks on one statement
     * @param statement: the statement to scrutinize
     * @param entityId: the id of the entity on which this statement is made or removed
     * @param added: whether this statement was added or deleted
     */
    public abstract void scrutinize(Statement statement, EntityIdValue entityId, boolean added);
}

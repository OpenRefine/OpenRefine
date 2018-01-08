package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.schema.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

/**
 * A scrutinizer that inspects new items
 * @author antonin
 */
public class NewItemScrutinizer extends ItemEditScrutinizer {

    @Override
    public void scrutinize(ItemUpdate update) {
        if (update.isNew()) {
            info("new-item-created");
            
            if (update.getLabels().isEmpty() && update.getAliases().isEmpty()) {
                important("new-item-without-labels-or-aliases");
            }
            
            if (update.getDescriptions().isEmpty()) {
                warning("new-item-without-descriptions");
            }
            
            if (! update.getDeletedStatements().isEmpty()) {
                warning("new-item-with-deleted-statements");
            }
            
            // Try to find a "instance of" or "subclass of" claim
            boolean typeFound = false;
            for(StatementGroup group : update.getAddedStatementGroups()) {
                String pid = group.getProperty().getId();
                if ("P31".equals(pid) || "P279".equals(pid)) {
                    typeFound = true;
                    break;
                }
            }
            if (!typeFound) {
                warning("new-item-without-P31-or-P279");
            }
        }
    }

}

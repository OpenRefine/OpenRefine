package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

/**
 * A scrutinizer that inspects new items.
 * 
 * @author Antonin Delpeuch
 */
public class NewItemScrutinizer extends ItemUpdateScrutinizer {
    
    public static final String noLabelType = "new-item-without-labels-or-aliases";
    public static final String noDescType = "new-item-without-descriptions";
    public static final String deletedStatementsType = "new-item-with-deleted-statements";
    public static final String noTypeType = "new-item-without-P31-or-P279";
    public static final String newItemType = "new-item-created";

    @Override
    public void scrutinize(ItemUpdate update) {
        if (update.isNew()) {
            info(newItemType);
            
            if (update.getLabels().isEmpty() && update.getAliases().isEmpty()) {
                QAWarning issue = new QAWarning(
                     noLabelType,
                     null,
                     QAWarning.Severity.CRITICAL,
                     1);
                issue.setProperty("example_entity", update.getItemId());
                addIssue(issue);
            }
            
            if (update.getDescriptions().isEmpty()) {
                QAWarning issue = new QAWarning(
                     noDescType,
                     null,
                     QAWarning.Severity.WARNING,
                     1);
                issue.setProperty("example_entity", update.getItemId());
                addIssue(issue);
            }
            
            if (!update.getDeletedStatements().isEmpty()) {
                QAWarning issue = new QAWarning(
                     deletedStatementsType,
                     null,
                     QAWarning.Severity.WARNING,
                     1);
                issue.setProperty("example_entity", update.getItemId());
                addIssue(issue);
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
                QAWarning issue = new QAWarning(
                        noTypeType,
                        null,
                        QAWarning.Severity.WARNING,
                        1);
                   issue.setProperty("example_entity", update.getItemId());
                   addIssue(issue);
            }
        }
    }

}

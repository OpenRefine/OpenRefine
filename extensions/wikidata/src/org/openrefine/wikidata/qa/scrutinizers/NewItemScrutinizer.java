package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
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
                QAWarning issue = new QAWarning(
                     "new-item-without-labels-or-aliases",
                     null,
                     QAWarning.Severity.CRITICAL,
                     1);
                issue.setProperty("example_entity", update.getItemId());
                addIssue(issue);
            }
            
            if (update.getDescriptions().isEmpty()) {
                QAWarning issue = new QAWarning(
                     "new-item-without-descriptions",
                     null,
                     QAWarning.Severity.WARNING,
                     1);
                issue.setProperty("example_entity", update.getItemId());
                addIssue(issue);
            }
            
            if (! update.getDeletedStatements().isEmpty()) {
                QAWarning issue = new QAWarning(
                     "new-item-with-deleted-statements",
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
                        "new-item-without-P31-or-P279",
                        null,
                        QAWarning.Severity.WARNING,
                        1);
                   issue.setProperty("example_entity", update.getItemId());
                   addIssue(issue);
            }
        }
    }

}

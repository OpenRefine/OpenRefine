
package org.openrefine.wikibase.qa.scrutinizers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.StatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public class MultiValueScrutinizer extends EditScrutinizer {

    public static final String new_type = "multi-valued-property-is-required-for-new-item";
    public static final String existing_type = "multi-valued-property-is-required-for-existing-item";
    public String multiValueConstraintQid;

    @Override
    public boolean prepareDependencies() {
        multiValueConstraintQid = getConstraintsRelatedId("multi_value_constraint_qid");
        return _fetcher != null && multiValueConstraintQid != null;
    }

    @Override
    public void scrutinize(ItemEdit update) {
        scrutinizeStatementEdit(update);
    }

    @Override
    public void scrutinize(MediaInfoEdit update) {
        scrutinizeStatementEdit(update);
    }

    public void scrutinizeStatementEdit(StatementEntityEdit update) {
        Map<PropertyIdValue, Integer> propertyCount = new HashMap<>();

        for (Statement statement : update.getAddedStatements()) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            List<Statement> statementList = _fetcher.getConstraintsByType(pid, multiValueConstraintQid);
            if (propertyCount.containsKey(pid)) {
                propertyCount.put(pid, propertyCount.get(pid) + 1);
            } else if (!statementList.isEmpty()) {
                propertyCount.put(pid, 1);
            }
        }

        if (update.isNew()) {
            for (PropertyIdValue pid : propertyCount.keySet()) {
                if (propertyCount.get(pid) == 1) {
                    QAWarning issue = new QAWarning(new_type, pid.getId(), QAWarning.Severity.WARNING, 1);
                    issue.setProperty("property_entity", pid);
                    issue.setProperty("example_entity", update.getEntityId());
                    addIssue(issue);
                }
            }
        } else {
            for (PropertyIdValue pid : propertyCount.keySet()) {
                if (propertyCount.get(pid) == 1) {
                    QAWarning issue = new QAWarning(existing_type, pid.getId(), QAWarning.Severity.INFO, 1);
                    issue.setProperty("property_entity", pid);
                    issue.setProperty("example_entity", update.getEntityId());
                    addIssue(issue);
                }
            }
        }

    }
}

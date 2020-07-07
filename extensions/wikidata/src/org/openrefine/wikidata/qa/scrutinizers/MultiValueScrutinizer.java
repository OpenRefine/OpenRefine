package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiValueScrutinizer extends EditScrutinizer {

    public static final String new_type = "multi-valued-property-is-required-for-new-item";
    public static final String existing_type = "multi-valued-property-is-required-for-existing-item";
    public static String MULTI_VALUE_CONSTRAINT_QID = "Q21510857";

    @Override
    public void scrutinize(ItemUpdate update) {
        Map<PropertyIdValue, Integer> propertyCount = new HashMap<>();

        for (Statement statement : update.getAddedStatements()) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            List<Statement> statementList = _fetcher.getConstraintsByType(pid, MULTI_VALUE_CONSTRAINT_QID);
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
                    issue.setProperty("example_entity", update.getItemId());
                    addIssue(issue);
                }
            }
        } else {
            for (PropertyIdValue pid : propertyCount.keySet()) {
                if (propertyCount.get(pid) == 1) {
                    QAWarning issue = new QAWarning(existing_type, pid.getId(), QAWarning.Severity.INFO, 1);
                    issue.setProperty("property_entity", pid);
                    issue.setProperty("example_entity", update.getItemId());
                    addIssue(issue);
                }
            }
        }

    }
}

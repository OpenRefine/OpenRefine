package org.openrefine.wikidata.qa.scrutinizers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

/**
 * A scrutinizer that checks for properties using the same value
 * on different items.
 * 
 * @author antonin
 *
 */
public class DistinctValuesScrutinizer extends StatementScrutinizer {
    
    private Map<PropertyIdValue, Map<Value, EntityIdValue>> _seenValues;
    
    public DistinctValuesScrutinizer() {
        _seenValues = new HashMap<>();
    }

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
        if (_fetcher.hasDistinctValues(pid)) {
            Value mainSnakValue = statement.getClaim().getMainSnak().getValue();
            Map<Value, EntityIdValue> seen = _seenValues.get(pid);
            if (seen == null) {
                seen = new HashMap<Value, EntityIdValue>();
                _seenValues.put(pid, seen);
            }
            if (seen.containsKey(mainSnakValue)) {
                EntityIdValue otherId = seen.get(mainSnakValue);
                QAWarning issue = new QAWarning(
                        "identical-values-for-distinct-valued-property",
                        pid.getId(),
                        QAWarning.Severity.IMPORTANT,
                        1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("item1_entity", entityId);
                issue.setProperty("item2_entity", otherId);
                addIssue(issue);
            } else {
                seen.put(mainSnakValue, entityId);
            }
        }
    }

}

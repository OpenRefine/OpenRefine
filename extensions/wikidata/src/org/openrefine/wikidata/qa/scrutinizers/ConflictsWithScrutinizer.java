package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ConflictsWithScrutinizer extends EditScrutinizer {

    public static final String type = "having-conflicts-with-statements";

    @Override
    public void scrutinize(ItemUpdate update) {
        Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()){
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            Value value = statement.getClaim().getMainSnak().getValue();
            Set<Value> values;
            if (value != null) {
                if (propertyIdValueValueMap.containsKey(pid)) {
                    values = propertyIdValueValueMap.get(pid);
                } else {
                    values = new HashSet<>();
                }

                values.add(value);
                propertyIdValueValueMap.put(pid, values);
            }
        }

        for(PropertyIdValue propertyId : propertyIdValueValueMap.keySet()){
            Map<PropertyIdValue, List<Value>> conflictingPropertyMap = _fetcher.getParamConflictsWith(propertyId);
            for (PropertyIdValue conflictingPid : conflictingPropertyMap.keySet()) {
                if (propertyIdValueValueMap.containsKey(conflictingPid) && raiseWarning(propertyIdValueValueMap, conflictingPid, conflictingPropertyMap)) {
                    QAWarning issue = new QAWarning(type, propertyId.getId()+conflictingPid.getId(), QAWarning.Severity.WARNING, 1);
                    issue.setProperty("property_entity", propertyId);
                    issue.setProperty("added_property_entity", conflictingPid);
                    issue.setProperty("example_entity", update.getItemId());
                    addIssue(issue);
                }
            }
        }
    }

    private boolean raiseWarning(Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap, PropertyIdValue conflictingPid, Map<PropertyIdValue, List<Value>> conflictingPropertyMap) {
        if (conflictingPropertyMap.get(conflictingPid).isEmpty()){
            return true;
        }

        for (Value value : conflictingPropertyMap.get(conflictingPid)) {
            if (propertyIdValueValueMap.get(conflictingPid).contains(value)){
                return true;
            }
        }

        return false;
    }
}

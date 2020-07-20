package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ItemRequiresScrutinizer extends EditScrutinizer {

    public static final String newItemRequireValuesType = "new-item-requires-property-to-have-certain-values";
    public static final String newItemRequirePropertyType = "new-item-requires-certain-other-statement";
    public static final String existingItemRequireValuesType = "existing-item-requires-property-to-have-certain-values";
    public static final String existingItemRequirePropertyType = "existing-item-requires-certain-other-statement";
    public static String ITEM_REQUIRES_CONSTRAINT_QID = "Q21503247";
    public static String ITEM_REQUIRES_PROPERTY_PID = "P2306";
    public static String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";

    class ItemRequiresConstraint {
        final PropertyIdValue itemRequiresPid;
        final List<Value> itemList;

        ItemRequiresConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            PropertyIdValue pid = null;
            this.itemList = new ArrayList<>();
            for(SnakGroup group : specs) {
                for (Snak snak : group.getSnaks()) {
                    if (group.getProperty().getId().equals(ITEM_REQUIRES_PROPERTY_PID)){
                        pid = (PropertyIdValue) snak.getValue();
                    }
                    if (group.getProperty().getId().equals(ITEM_OF_PROPERTY_CONSTRAINT_PID)){
                        this.itemList.add(snak.getValue());
                    }
                }
            }
            this.itemRequiresPid = pid;
        }
    }

    @Override
    public void scrutinize(ItemUpdate update) {
        Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()) {
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

        for (PropertyIdValue propertyId : propertyIdValueValueMap.keySet()) {
            List<Statement> constraintDefinitions = _fetcher.getConstraintsByType(propertyId, ITEM_REQUIRES_CONSTRAINT_QID);
            for (Statement statement : constraintDefinitions) {
                ItemRequiresConstraint constraint = new ItemRequiresConstraint(statement);
                PropertyIdValue itemRequiresPid = constraint.itemRequiresPid;
                List<Value> itemList = constraint.itemList;
                if (!propertyIdValueValueMap.containsKey(itemRequiresPid)) {
                    QAWarning issue = new QAWarning(update.isNew() ? newItemRequirePropertyType : existingItemRequirePropertyType, propertyId.getId() + itemRequiresPid.getId(), update.isNew() ? QAWarning.Severity.WARNING : QAWarning.Severity.INFO, 1);
                    issue.setProperty("property_entity", propertyId);
                    issue.setProperty("added_property_entity", itemRequiresPid);
                    issue.setProperty("example_entity", update.getItemId());
                    addIssue(issue);
                } else if (raiseWarning(propertyIdValueValueMap, itemRequiresPid, itemList)) {
                    QAWarning issue = new QAWarning(update.isNew() ? newItemRequireValuesType : existingItemRequireValuesType, propertyId.getId() + itemRequiresPid.getId(), update.isNew() ? QAWarning.Severity.WARNING : QAWarning.Severity.INFO, 1);
                    issue.setProperty("property_entity", propertyId);
                    issue.setProperty("added_property_entity", itemRequiresPid);
                    issue.setProperty("example_entity", update.getItemId());
                    addIssue(issue);
                }
            }
        }
    }

    private boolean raiseWarning(Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap, PropertyIdValue itemRequiresPid, List<Value> itemList) {
        if (itemList.isEmpty()){
            return false;
        }

        for (Value value : itemList) {
            if (propertyIdValueValueMap.get(itemRequiresPid).contains(value)){
                return false;
            }
        }

        return true;
    }
}


package org.openrefine.wikibase.qa.scrutinizers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.StatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

public class ItemRequiresScrutinizer extends EditScrutinizer {

    public static final String newItemRequireValuesType = "new-item-requires-property-to-have-certain-values";
    public static final String newItemRequirePropertyType = "new-item-requires-certain-other-statement";
    public static final String existingItemRequireValuesType = "existing-item-requires-property-to-have-certain-values";
    public static final String existingItemRequirePropertyType = "existing-item-requires-certain-other-statement";
    public String itemRequiresConstraintQid;
    public String itemRequiresPropertyPid;
    public String itemOfPropertyConstraintPid;

    class ItemRequiresConstraint {

        final PropertyIdValue itemRequiresPid;
        final List<Value> itemList;

        ItemRequiresConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            PropertyIdValue pid = null;
            this.itemList = new ArrayList<>();
            for (SnakGroup group : specs) {
                for (Snak snak : group.getSnaks()) {
                    if (!(snak instanceof ValueSnak)) {
                        continue;
                    }
                    if (group.getProperty().getId().equals(itemRequiresPropertyPid)) {
                        pid = (PropertyIdValue) ((ValueSnak) snak).getValue();
                    }
                    if (group.getProperty().getId().equals(itemOfPropertyConstraintPid)) {
                        this.itemList.add(((ValueSnak) snak).getValue());
                    }
                }
            }
            this.itemRequiresPid = pid;
        }
    }

    @Override
    public boolean prepareDependencies() {
        itemRequiresConstraintQid = getConstraintsRelatedId("item_requires_statement_constraint_qid");
        itemRequiresPropertyPid = getConstraintsRelatedId("property_pid");
        itemOfPropertyConstraintPid = getConstraintsRelatedId("item_of_property_constraint_pid");
        return _fetcher != null && itemRequiresConstraintQid != null
                && itemRequiresPropertyPid != null && itemOfPropertyConstraintPid != null;
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
        Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()) {
            Snak mainSnak = statement.getClaim().getMainSnak();
            PropertyIdValue pid = mainSnak.getPropertyId();
            Set<Value> values;
            if (mainSnak instanceof ValueSnak) {
                Value value = ((ValueSnak) mainSnak).getValue();
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
            List<Statement> constraintDefinitions = _fetcher.getConstraintsByType(propertyId, itemRequiresConstraintQid);
            for (Statement statement : constraintDefinitions) {
                ItemRequiresConstraint constraint = new ItemRequiresConstraint(statement);
                PropertyIdValue itemRequiresPid = constraint.itemRequiresPid;
                List<Value> itemList = constraint.itemList;
                if (!propertyIdValueValueMap.containsKey(itemRequiresPid)) {
                    QAWarning issue = new QAWarning(update.isNew() ? newItemRequirePropertyType : existingItemRequirePropertyType,
                            propertyId.getId() + itemRequiresPid.getId(),
                            update.isNew() ? QAWarning.Severity.WARNING : QAWarning.Severity.INFO, 1);
                    issue.setProperty("property_entity", propertyId);
                    issue.setProperty("added_property_entity", itemRequiresPid);
                    issue.setProperty("example_entity", update.getEntityId());
                    addIssue(issue);
                } else if (raiseWarning(propertyIdValueValueMap, itemRequiresPid, itemList)) {
                    QAWarning issue = new QAWarning(update.isNew() ? newItemRequireValuesType : existingItemRequireValuesType,
                            propertyId.getId() + itemRequiresPid.getId(),
                            update.isNew() ? QAWarning.Severity.WARNING : QAWarning.Severity.INFO, 1);
                    issue.setProperty("property_entity", propertyId);
                    issue.setProperty("added_property_entity", itemRequiresPid);
                    issue.setProperty("example_entity", update.getEntityId());
                    addIssue(issue);
                }
            }
        }
    }

    private boolean raiseWarning(Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap, PropertyIdValue itemRequiresPid,
            List<Value> itemList) {
        if (itemList.isEmpty()) {
            return false;
        }

        for (Value value : itemList) {
            if (propertyIdValueValueMap.get(itemRequiresPid).contains(value)) {
                return false;
            }
        }

        return true;
    }
}

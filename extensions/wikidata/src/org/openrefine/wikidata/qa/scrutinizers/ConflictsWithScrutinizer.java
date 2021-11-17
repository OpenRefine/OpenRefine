package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.TermedStatementEntityUpdate;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConflictsWithScrutinizer extends EditScrutinizer {

    public static final String type = "having-conflicts-with-statements";
    public String conflictsWithConstraintQid;
    public String conflictsWithPropertyPid;
    public String itemOfPropertyConstraintPid;

    class ConflictsWithConstraint {
        final PropertyIdValue conflictingPid;
        final List<Value> itemList;

        ConflictsWithConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            PropertyIdValue pid = null;
            this.itemList = new ArrayList<>();
            for(SnakGroup group : specs) {
                for (Snak snak : group.getSnaks()) {
                    if (group.getProperty().getId().equals(conflictsWithPropertyPid) && snak instanceof ValueSnak) {
                        pid = (PropertyIdValue) ((ValueSnak)snak).getValue();
                    }
                    if (group.getProperty().getId().equals(itemOfPropertyConstraintPid) && snak instanceof ValueSnak) {
                        this.itemList.add(((ValueSnak)snak).getValue());
                    }
                }
            }
            this.conflictingPid = pid;
        }
    }

    @Override
    public boolean prepareDependencies() {
        conflictsWithConstraintQid = getConstraintsRelatedId("conflicts_with_constraint_qid");
        conflictsWithPropertyPid = getConstraintsRelatedId("property_pid");
        itemOfPropertyConstraintPid = getConstraintsRelatedId("item_of_property_constraint_pid");

        return _fetcher != null && conflictsWithConstraintQid != null
                && conflictsWithPropertyPid != null && itemOfPropertyConstraintPid != null;
    }

    @Override
    public void scrutinize(TermedStatementEntityUpdate update) {
        Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()){
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            Value value = null;
            Snak mainSnak = statement.getClaim().getMainSnak();
            if (mainSnak instanceof ValueSnak) {
                value = ((ValueSnak)mainSnak).getValue();
            }
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
            List<Statement> statementList = _fetcher.getConstraintsByType(propertyId, conflictsWithConstraintQid);
            for (Statement statement : statementList) {
                ConflictsWithConstraint constraint = new ConflictsWithConstraint(statement);
                PropertyIdValue conflictingPid = constraint.conflictingPid;
                List<Value> itemList = constraint.itemList;
                if (propertyIdValueValueMap.containsKey(conflictingPid) && raiseWarning(propertyIdValueValueMap, conflictingPid, itemList)) {
                    QAWarning issue = new QAWarning(type, propertyId.getId()+conflictingPid.getId(), QAWarning.Severity.WARNING, 1);
                    issue.setProperty("property_entity", propertyId);
                    issue.setProperty("added_property_entity", conflictingPid);
                    issue.setProperty("example_entity", update.getItemId());
                    addIssue(issue);
                }
            }

        }
    }

    private boolean raiseWarning(Map<PropertyIdValue, Set<Value>> propertyIdValueValueMap, PropertyIdValue conflictingPid, List<Value> itemList) {
        if (itemList.isEmpty()){
            return true;
        }

        for (Value value : itemList) {
            if (propertyIdValueValueMap.get(conflictingPid).contains(value)){
                return true;
            }
        }

        return false;
    }
}

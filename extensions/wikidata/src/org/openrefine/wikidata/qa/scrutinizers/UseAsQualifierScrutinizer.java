package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UseAsQualifierScrutinizer extends EditScrutinizer {

    public static final String type = "values-should-not-be-used-as-qualifier";
    public static String ONE_OF_QUALIFIER_VALUE_PROPERTY_CONSTRAINT = "Q52712340";
    public static String ALLOWED_AS_QUALIFIER_PROPERTY_PID = "P2306";
    public static String ITEM_OF_PROPERTY_CONSTRAINT_PID = "P2305";

    class UseAsQualifierConstraint {
        final PropertyIdValue allowedQualifierPid;
        final List<ItemIdValue> itemList;
        UseAsQualifierConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            PropertyIdValue pid = null;
            this.itemList = new ArrayList<>();
            for(SnakGroup group : specs) {
                for (Snak snak : group.getSnaks()) {
                    if (group.getProperty().getId().equals(ALLOWED_AS_QUALIFIER_PROPERTY_PID)){
                        pid = (PropertyIdValue) snak.getValue();
                    }
                    if (group.getProperty().getId().equals(ITEM_OF_PROPERTY_CONSTRAINT_PID)){
                        this.itemList.add((ItemIdValue) snak.getValue());
                    }
                }
            }
            this.allowedQualifierPid = pid;
        }
    }

    @Override
    public void scrutinize(ItemUpdate update) {
        for (Statement statement : update.getAddedStatements()) {
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            Map<PropertyIdValue, List<ItemIdValue>> qualifiersMap = new HashMap<>();
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            for(SnakGroup group : specs) {
                PropertyIdValue qualifierPid = Datamodel.makeWikidataPropertyIdValue(group.getProperty().getId());
                List<ItemIdValue> itemList;
                for (Snak snak : group.getSnaks()) {
                    if (qualifiersMap.containsKey(qualifierPid)){
                        itemList = qualifiersMap.get(qualifierPid);
                    }else {
                        itemList = new ArrayList<>();
                    }
                    itemList.add((ItemIdValue) snak.getValue());
                    qualifiersMap.put(qualifierPid, itemList);
                }
            }

            List<Statement> constraintDefinitions = _fetcher.getConstraintsByType(pid, ONE_OF_QUALIFIER_VALUE_PROPERTY_CONSTRAINT);
            for (Statement constraintStatement : constraintDefinitions) {
                UseAsQualifierConstraint constraint = new UseAsQualifierConstraint(constraintStatement);
                if (qualifiersMap.containsKey(constraint.allowedQualifierPid)) {
                    for (ItemIdValue value : qualifiersMap.get(constraint.allowedQualifierPid)) {
                        if (!constraint.itemList.contains(value)) {
                            QAWarning issue = new QAWarning(type, pid.getId()+constraint.allowedQualifierPid.getId(), QAWarning.Severity.WARNING, 1);
                            issue.setProperty("property_entity", pid);
                            issue.setProperty("added_property_entity", constraint.allowedQualifierPid);
                            issue.setProperty("example_entity", update.getItemId());
                            addIssue(issue);
                        }
                    }
                }
            }

        }
    }

}

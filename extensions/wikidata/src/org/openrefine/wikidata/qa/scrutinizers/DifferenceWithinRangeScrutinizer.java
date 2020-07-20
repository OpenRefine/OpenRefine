package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DifferenceWithinRangeScrutinizer extends EditScrutinizer {

    public static final String type = "difference-of-the-properties-is-not-within-the-specified-range";
    public static String DIFFERENCE_WITHIN_RANGE_CONSTRAINT_QID = "Q21510854";
    public static String DIFFERENCE_WITHIN_RANGE_CONSTRAINT_PID = "P2306";
    public static String MINIMUM_VALUE_PID = "P2313";
    public static String MAXIMUM_VALUE_PID = "P2312";

    class DifferenceWithinRangeConstraint {
        PropertyIdValue lowerPropertyIdValue;
        QuantityValue minRangeValue, maxRangeValue;

        DifferenceWithinRangeConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> lowerValueProperty = findValues(specs, DIFFERENCE_WITHIN_RANGE_CONSTRAINT_PID);
                List<Value> minValue = findValues(specs, MINIMUM_VALUE_PID);
                List<Value> maxValue = findValues(specs, MAXIMUM_VALUE_PID);
                if (!lowerValueProperty.isEmpty()) {
                    lowerPropertyIdValue = (PropertyIdValue) lowerValueProperty.get(0);
                }
                if (!minValue.isEmpty()) {
                    minRangeValue = (QuantityValue) minValue.get(0);
                }
                if (!maxValue.isEmpty()) {
                    maxRangeValue = (QuantityValue) maxValue.get(0);
                }
            }
        }
    }

    @Override
    public void scrutinize(ItemUpdate update) {
        Map<PropertyIdValue, Value> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()){
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            Value value = statement.getClaim().getMainSnak().getValue();
            propertyIdValueValueMap.put(pid, value);
        }

        for(PropertyIdValue propertyId : propertyIdValueValueMap.keySet()){
            List<Statement> statementList = _fetcher.getConstraintsByType(propertyId, DIFFERENCE_WITHIN_RANGE_CONSTRAINT_QID);
            if (!statementList.isEmpty()){
                DifferenceWithinRangeConstraint constraint = new DifferenceWithinRangeConstraint(statementList.get(0));
                PropertyIdValue lowerPropertyId = constraint.lowerPropertyIdValue;
                QuantityValue minRangeValue = constraint.minRangeValue;
                QuantityValue maxRangeValue = constraint.maxRangeValue;

                if (propertyIdValueValueMap.containsKey(lowerPropertyId)){
                    Value startingValue = propertyIdValueValueMap.get(lowerPropertyId);
                    Value endingValue = propertyIdValueValueMap.get(propertyId);
                    if (startingValue instanceof TimeValue && endingValue instanceof TimeValue){
                        TimeValue lowerDate = (TimeValue)startingValue;
                        TimeValue upperDate = (TimeValue)endingValue;

                        long differenceInYears = upperDate.getYear() - lowerDate.getYear();
                        long differenceInMonths = upperDate.getMonth() - lowerDate.getMonth();
                        long differenceInDays = upperDate.getMonth() - lowerDate.getMonth();

                        if (minRangeValue != null && (differenceInYears < minRangeValue.getNumericValue().longValue()
                                || differenceInYears == 0 && differenceInMonths < 0
                                || differenceInYears == 0 && differenceInMonths == 0 && differenceInDays < 0)){
                            QAWarning issue = new QAWarning(type, propertyId.getId(), QAWarning.Severity.WARNING, 1);
                            issue.setProperty("source_entity", lowerPropertyId);
                            issue.setProperty("target_entity", propertyId);
                            issue.setProperty("min_value", minRangeValue.getNumericValue());
                            if (maxRangeValue != null) {
                                issue.setProperty("max_value", maxRangeValue.getNumericValue());
                            } else {
                                issue.setProperty("max_value", null);
                            }
                            issue.setProperty("example_entity", update.getItemId());
                            addIssue(issue);
                        }

                        if (maxRangeValue != null && differenceInYears > maxRangeValue.getNumericValue().longValue()){
                            QAWarning issue = new QAWarning(type, propertyId.getId(), QAWarning.Severity.WARNING, 1);
                            issue.setProperty("source_entity", lowerPropertyId);
                            issue.setProperty("target_entity", propertyId);
                            if (minRangeValue != null) {
                                issue.setProperty("min_value", minRangeValue.getNumericValue());
                            } else {
                                issue.setProperty("min_value", null);
                            }
                            issue.setProperty("max_value", maxRangeValue.getNumericValue());
                            issue.setProperty("example_entity", update.getItemId());
                            addIssue(issue);
                        }
                    }
                }

            }
        }

    }
}

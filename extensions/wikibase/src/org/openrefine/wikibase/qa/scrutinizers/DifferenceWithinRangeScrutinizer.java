
package org.openrefine.wikibase.qa.scrutinizers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.updates.ItemEdit;
import org.openrefine.wikibase.updates.MediaInfoEdit;
import org.openrefine.wikibase.updates.StatementEntityEdit;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

public class DifferenceWithinRangeScrutinizer extends EditScrutinizer {

    public static final String type = "difference-of-the-properties-is-not-within-the-specified-range";
    public String differenceWithinRangeConstraintQid;
    public String differenceWithinRangeConstraintPid;
    public String minimumValuePid;
    public String maximumValuePid;

    class DifferenceWithinRangeConstraint {

        PropertyIdValue lowerPropertyIdValue;
        QuantityValue minRangeValue, maxRangeValue;

        DifferenceWithinRangeConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> lowerValueProperty = findValues(specs, differenceWithinRangeConstraintPid);
                List<Value> minValue = findValues(specs, minimumValuePid);
                List<Value> maxValue = findValues(specs, maximumValuePid);
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
    public boolean prepareDependencies() {
        differenceWithinRangeConstraintQid = getConstraintsRelatedId("difference_within_range_constraint_qid");
        differenceWithinRangeConstraintPid = getConstraintsRelatedId("property_pid");
        minimumValuePid = getConstraintsRelatedId("minimum_value_pid");
        maximumValuePid = getConstraintsRelatedId("maximum_value_pid");
        return _fetcher != null && differenceWithinRangeConstraintQid != null && differenceWithinRangeConstraintPid != null
                && minimumValuePid != null && maximumValuePid != null;
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
        Map<PropertyIdValue, Value> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()) {
            Snak mainSnak = statement.getClaim().getMainSnak();
            if (mainSnak instanceof ValueSnak) {
                PropertyIdValue pid = mainSnak.getPropertyId();
                Value value = ((ValueSnak) mainSnak).getValue();
                propertyIdValueValueMap.put(pid, value);
            }
        }

        for (PropertyIdValue propertyId : propertyIdValueValueMap.keySet()) {
            List<Statement> statementList = _fetcher.getConstraintsByType(propertyId, differenceWithinRangeConstraintQid);
            if (!statementList.isEmpty()) {
                DifferenceWithinRangeConstraint constraint = new DifferenceWithinRangeConstraint(statementList.get(0));
                PropertyIdValue lowerPropertyId = constraint.lowerPropertyIdValue;
                QuantityValue minRangeValue = constraint.minRangeValue;
                QuantityValue maxRangeValue = constraint.maxRangeValue;

                if (propertyIdValueValueMap.containsKey(lowerPropertyId)) {
                    Value startingValue = propertyIdValueValueMap.get(lowerPropertyId);
                    Value endingValue = propertyIdValueValueMap.get(propertyId);
                    if (startingValue instanceof TimeValue && endingValue instanceof TimeValue) {
                        TimeValue lowerDate = (TimeValue) startingValue;
                        TimeValue upperDate = (TimeValue) endingValue;

                        long differenceInYears = upperDate.getYear() - lowerDate.getYear();
                        long differenceInMonths = upperDate.getMonth() - lowerDate.getMonth();
                        long differenceInDays = upperDate.getMonth() - lowerDate.getMonth();

                        if (minRangeValue != null && (differenceInYears < minRangeValue.getNumericValue().longValue()
                                || differenceInYears == 0 && differenceInMonths < 0
                                || differenceInYears == 0 && differenceInMonths == 0 && differenceInDays < 0)) {
                            QAWarning issue = new QAWarning(type, propertyId.getId(), QAWarning.Severity.WARNING, 1);
                            issue.setProperty("source_entity", lowerPropertyId);
                            issue.setProperty("target_entity", propertyId);
                            issue.setProperty("min_value", minRangeValue.getNumericValue());
                            if (maxRangeValue != null) {
                                issue.setProperty("max_value", maxRangeValue.getNumericValue());
                            } else {
                                issue.setProperty("max_value", null);
                            }
                            issue.setProperty("example_entity", update.getEntityId());
                            addIssue(issue);
                        }

                        if (maxRangeValue != null && differenceInYears > maxRangeValue.getNumericValue().longValue()) {
                            QAWarning issue = new QAWarning(type, propertyId.getId(), QAWarning.Severity.WARNING, 1);
                            issue.setProperty("source_entity", lowerPropertyId);
                            issue.setProperty("target_entity", propertyId);
                            if (minRangeValue != null) {
                                issue.setProperty("min_value", minRangeValue.getNumericValue());
                            } else {
                                issue.setProperty("min_value", null);
                            }
                            issue.setProperty("max_value", maxRangeValue.getNumericValue());
                            issue.setProperty("example_entity", update.getEntityId());
                            addIssue(issue);
                        }
                    }
                }

            }
        }

    }
}

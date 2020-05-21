package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.updates.ItemUpdate;
import org.wikidata.wdtk.datamodel.interfaces.*;

import java.util.HashMap;
import java.util.Map;

public class DifferenceWithinRangeScrutinizer extends EditScrutinizer {

    public static final String type = "difference-of-the-properties-is-not-within-the-specified-range";

    @Override
    public void scrutinize(ItemUpdate update) {
        Map<PropertyIdValue, Value> propertyIdValueValueMap = new HashMap<>();
        for (Statement statement : update.getAddedStatements()){
            PropertyIdValue pid = statement.getClaim().getMainSnak().getPropertyId();
            Value value = statement.getClaim().getMainSnak().getValue();
            propertyIdValueValueMap.put(pid, value);
        }

        for(PropertyIdValue propertyId : propertyIdValueValueMap.keySet()){
            if (_fetcher.hasDiffWithinRange(propertyId)){
                PropertyIdValue lowerPropertyId = _fetcher.getLowerPropertyId(propertyId);
                QuantityValue minRangeValue = _fetcher.getMinimumValue(propertyId);
                QuantityValue maxRangeValue = _fetcher.getMaximumValue(propertyId);

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

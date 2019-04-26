package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Set;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public class RestrictedValuesScrutinizer extends SnakScrutinizer {
    
    public static String type = "forbidden-value";

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        PropertyIdValue pid = snak.getPropertyId();
        Value value = snak.getValue();
        
        Set<Value> allowedValues = _fetcher.allowedValues(pid);
        Set<Value> disallowedValues = _fetcher.disallowedValues(pid);
        if((allowedValues != null && !allowedValues.contains(value)) ||
           (disallowedValues != null && disallowedValues.contains(value))) {
            QAWarning issue = new QAWarning(type, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
            issue.setProperty("property_entity", pid);
            issue.setProperty("example_value_entity", value);
            issue.setProperty("example_subject_entity", entityId);
            addIssue(issue);
        }
    }
}

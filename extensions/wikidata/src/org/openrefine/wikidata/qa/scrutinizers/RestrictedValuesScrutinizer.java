package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class RestrictedValuesScrutinizer extends SnakScrutinizer {
    
    public static String type = "forbidden-value";
    public static String ALLOWED_VALUES_CONSTRAINT_QID = "Q21510859";
    public static String ALLOWED_VALUES_CONSTRAINT_PID = "P2305";

    public static String DISALLOWED_VALUES_CONSTRAINT_QID = "Q52558054";
    public static String DISALLOWED_VALUES_CONSTRAINT_PID = "P2305";

    class AllowedValueConstraint {
        Set<Value> allowedValues;
        AllowedValueConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = _fetcher.findValues(specs, ALLOWED_VALUES_CONSTRAINT_PID);
                allowedValues = new HashSet<>(properties);
            }
        }
    }

    class DisallowedValueConstraint {
        Set<Value> disallowedValues;
        DisallowedValueConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = _fetcher.findValues(specs, DISALLOWED_VALUES_CONSTRAINT_PID);
                disallowedValues = new HashSet<>(properties);
            }
        }
    }

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

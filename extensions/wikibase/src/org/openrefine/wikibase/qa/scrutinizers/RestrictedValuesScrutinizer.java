
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class RestrictedValuesScrutinizer extends SnakScrutinizer {

    public static final String type = "forbidden-value";
    public String allowedValuesConstraintQid;
    public String allowedValuesConstraintPid;

    public String disallowedValuesConstraintQid;
    public String disallowedValuesConstraintPid;

    @Override
    public boolean prepareDependencies() {
        allowedValuesConstraintQid = getConstraintsRelatedId("one_of_constraint_qid");
        allowedValuesConstraintPid = getConstraintsRelatedId("item_of_property_constraint_pid");
        disallowedValuesConstraintQid = getConstraintsRelatedId("none_of_constraint_qid");
        disallowedValuesConstraintPid = getConstraintsRelatedId("item_of_property_constraint_pid");
        return _fetcher != null && allowedValuesConstraintQid != null && allowedValuesConstraintPid != null
                && disallowedValuesConstraintQid != null && disallowedValuesConstraintPid != null;
    }

    class AllowedValueConstraint {

        Set<Value> allowedValues;

        AllowedValueConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = findValues(specs, allowedValuesConstraintPid);
                allowedValues = properties.stream().collect(Collectors.toSet());
            }
        }
    }

    class DisallowedValueConstraint {

        Set<Value> disallowedValues;

        DisallowedValueConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = findValues(specs, disallowedValuesConstraintPid);
                disallowedValues = properties.stream().collect(Collectors.toSet());
            }
        }
    }

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (!added) {
            return;
        }
        PropertyIdValue pid = snak.getPropertyId();
        Value value = null;
        if (snak instanceof ValueSnak) {
            value = ((ValueSnak) snak).getValue();
        }
        List<Statement> allowedValueConstraintDefinitions = _fetcher.getConstraintsByType(pid, allowedValuesConstraintQid);
        List<Statement> disallowedValueConstraintDefinitions = _fetcher.getConstraintsByType(pid, disallowedValuesConstraintQid);
        Set<Value> allowedValues = null, disallowedValues = null;
        if (!allowedValueConstraintDefinitions.isEmpty()) {
            AllowedValueConstraint constraint = new AllowedValueConstraint(allowedValueConstraintDefinitions.get(0));
            allowedValues = constraint.allowedValues;
        }
        if (!disallowedValueConstraintDefinitions.isEmpty()) {
            DisallowedValueConstraint constraint = new DisallowedValueConstraint(disallowedValueConstraintDefinitions.get(0));
            disallowedValues = constraint.disallowedValues;
        }
        if ((allowedValues != null && !allowedValues.contains(value)) ||
                (disallowedValues != null && disallowedValues.contains(value))) {
            QAWarning issue = new QAWarning(type, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
            issue.setProperty("property_entity", pid);
            issue.setProperty("example_value_entity", value);
            issue.setProperty("example_subject_entity", entityId);
            addIssue(issue);
        }
    }
}

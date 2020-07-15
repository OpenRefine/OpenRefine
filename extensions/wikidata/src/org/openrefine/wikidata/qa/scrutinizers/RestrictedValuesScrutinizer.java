package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.manifests.constraints.NoneOfConstraint;
import org.openrefine.wikidata.manifests.constraints.OneOfConstraint;
import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

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
        OneOfConstraint oneOfConstraint = constraints.getOneOfConstraint();
        NoneOfConstraint noneOfConstraint = constraints.getNoneOfConstraint();
        if (oneOfConstraint == null) return false;
        if (noneOfConstraint == null) return false;
        allowedValuesConstraintQid = oneOfConstraint.getQid();
        allowedValuesConstraintPid = oneOfConstraint.getItemOfPropertyConstraint();
        disallowedValuesConstraintQid = noneOfConstraint.getQid();
        disallowedValuesConstraintPid = noneOfConstraint.getItemOfPropertyConstraint();
        return allowedValuesConstraintQid != null && allowedValuesConstraintPid != null && disallowedValuesConstraintQid != null && disallowedValuesConstraintPid != null;
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
        PropertyIdValue pid = snak.getPropertyId();
        Value value = snak.getValue();
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

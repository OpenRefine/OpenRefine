
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Scrutinizer checking for units and bounds in quantities.
 * 
 * @author Antonin Delpeuch
 *
 */
public class QuantityScrutinizer extends SnakScrutinizer {

    public static final String boundsDisallowedType = "bounds-disallowed";
    public static final String integerConstraintType = "values-should-be-integers";
    public static final String invalidUnitType = "invalid-unit";
    public static final String noUnitProvidedType = "no-unit-provided";

    public String noBoundsConstraintQid;
    public String integerValuedConstraintQid;

    public String allowedUnitsConstraintQid;
    public String allowedUnitsConstraintPid;

    @Override
    public boolean prepareDependencies() {
        noBoundsConstraintQid = getConstraintsRelatedId("no_bounds_constraint_qid");
        integerValuedConstraintQid = getConstraintsRelatedId("integer_constraint_qid");
        allowedUnitsConstraintQid = getConstraintsRelatedId("allowed_units_constraint_qid");
        allowedUnitsConstraintPid = getConstraintsRelatedId("item_of_property_constraint_pid");
        return _fetcher != null && noBoundsConstraintQid != null && integerValuedConstraintQid != null
                && allowedUnitsConstraintQid != null && allowedUnitsConstraintPid != null;
    }

    class AllowedUnitsConstraint {

        Set<ItemIdValue> allowedUnits;

        AllowedUnitsConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = findValues(specs, allowedUnitsConstraintPid);
                allowedUnits = properties.stream()
                        .map(e -> e == null ? null : (ItemIdValue) e)
                        .collect(Collectors.toSet());
            }
        }
    }

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (!added) {
            return;
        }
        if (snak instanceof ValueSnak && ((ValueSnak) snak).getValue() instanceof QuantityValue && added) {
            PropertyIdValue pid = snak.getPropertyId();
            QuantityValue value = (QuantityValue) ((ValueSnak) snak).getValue();

            if (!_fetcher.getConstraintsByType(pid, noBoundsConstraintQid).isEmpty()
                    && (value.getUpperBound() != null || value.getLowerBound() != null)) {
                QAWarning issue = new QAWarning(boundsDisallowedType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                addIssue(issue);
            }
            if (!_fetcher.getConstraintsByType(pid, integerValuedConstraintQid).isEmpty() && value.getNumericValue().scale() > 0) {
                QAWarning issue = new QAWarning(integerConstraintType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                addIssue(issue);
            }
            List<Statement> statementList = _fetcher.getConstraintsByType(pid, allowedUnitsConstraintQid);
            Set<ItemIdValue> allowedUnits = null;
            if (!statementList.isEmpty()) {
                AllowedUnitsConstraint allowedUnitsConstraint = new AllowedUnitsConstraint(statementList.get(0));
                allowedUnits = allowedUnitsConstraint.allowedUnits;
            }
            ItemIdValue currentUnit = null;
            if (value.getUnitItemId() != null) {
                currentUnit = value.getUnitItemId();
            }
            if (allowedUnits != null &&
                    !allowedUnits.contains(currentUnit)) {
                String issueType = currentUnit == null ? noUnitProvidedType : invalidUnitType;
                QAWarning issue = new QAWarning(issueType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                if (currentUnit != null) {
                    issue.setProperty("unit_entity", value.getUnitItemId());
                }
                addIssue(issue);
            }
        }
    }

}

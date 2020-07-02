package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.Value;

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

    public static String NO_BOUNDS_CONSTRAINT_QID = "Q51723761";
    public static String INTEGER_VALUED_CONSTRAINT_QID = "Q52848401";

    public static String ALLOWED_UNITS_CONSTRAINT_QID = "Q21514353";
    public static String ALLOWED_UNITS_CONSTRAINT_PID = "P2305";

    class AllowedUnitsConstraint {
        Set<ItemIdValue> allowedUnits;
        AllowedUnitsConstraint(Statement statement) {
            List<SnakGroup> specs = statement.getClaim().getQualifiers();
            if (specs != null) {
                List<Value> properties = _fetcher.findValues(specs, ALLOWED_UNITS_CONSTRAINT_PID);
                allowedUnits = properties.stream()
                        .map(e -> e == null ? null : (ItemIdValue) e)
                        .collect(Collectors.toSet());
            }
        }
    }

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (snak.getValue() instanceof QuantityValue && added) {
            PropertyIdValue pid = snak.getPropertyId();
            QuantityValue value = (QuantityValue)snak.getValue();

            if(!_fetcher.getConstraintsByType(pid, NO_BOUNDS_CONSTRAINT_QID).isEmpty() && (value.getUpperBound() != null || value.getLowerBound() != null)) {
                QAWarning issue = new QAWarning(boundsDisallowedType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                addIssue(issue);
            }
            if(!_fetcher.getConstraintsByType(pid, INTEGER_VALUED_CONSTRAINT_QID).isEmpty() && value.getNumericValue().scale() > 0) {
                QAWarning issue = new QAWarning(integerConstraintType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                addIssue(issue);
            }
            List<Statement> statementList = _fetcher.getConstraintsByType(pid, ALLOWED_UNITS_CONSTRAINT_QID);
            Set<ItemIdValue> allowedUnits = null;
            if (!statementList.isEmpty()) {
                AllowedUnitsConstraint allowedUnitsConstraint = new AllowedUnitsConstraint(statementList.get(0));
                allowedUnits = allowedUnitsConstraint.allowedUnits;
            }
            ItemIdValue currentUnit = null;
            if (value.getUnitItemId() != null) {
                currentUnit = value.getUnitItemId();
            }
            if(allowedUnits != null &&
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

package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Set;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;

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

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (QuantityValue.class.isInstance(snak.getValue()) && added) {
            PropertyIdValue pid = snak.getPropertyId();
            QuantityValue value = (QuantityValue)snak.getValue();
            
            if(!_fetcher.boundsAllowed(pid) && (value.getUpperBound() != null || value.getLowerBound() != null)) {
                QAWarning issue = new QAWarning(boundsDisallowedType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                addIssue(issue);
            }
            if(_fetcher.integerValued(pid) && value.getNumericValue().scale() > 0) {
                QAWarning issue = new QAWarning(integerConstraintType, pid.getId(), QAWarning.Severity.IMPORTANT, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_value", value.getNumericValue().toString());
                issue.setProperty("example_item_entity", entityId);
                addIssue(issue);
            }
            Set<ItemIdValue> allowedUnits = _fetcher.allowedUnits(pid);
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

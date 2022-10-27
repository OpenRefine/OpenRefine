
package org.openrefine.wikibase.qa.scrutinizers;

import org.openrefine.wikibase.qa.QAWarning;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.List;

public class EntityTypeScrutinizer extends SnakScrutinizer {

    public final static String type = "invalid-entity-type";
    public String allowedEntityTypesQid;
    public String wikibaseItemQid;
    public String itemOfPropertyConstraint;

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (!added) {
            return;
        }
        PropertyIdValue pid = snak.getPropertyId();
        List<Statement> statementList = _fetcher.getConstraintsByType(pid, allowedEntityTypesQid);
        if (!statementList.isEmpty()) {
            List<SnakGroup> constraint = statementList.get(0).getClaim().getQualifiers();
            boolean isUsable = true;
            if (constraint != null) {
                isUsable = findValues(constraint, itemOfPropertyConstraint).contains(
                        Datamodel.makeWikidataItemIdValue(wikibaseItemQid));
            }
            if (!isUsable) {
                QAWarning issue = new QAWarning(type, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_entity", entityId);
                addIssue(issue);
            }
        }
    }

    @Override
    public boolean prepareDependencies() {
        allowedEntityTypesQid = getConstraintsRelatedId("allowed_entity_types_constraint_qid");
        wikibaseItemQid = getConstraintsRelatedId("wikibase_item_qid");
        itemOfPropertyConstraint = getConstraintsRelatedId("item_of_property_constraint_pid");
        return _fetcher != null && allowedEntityTypesQid != null
                && wikibaseItemQid != null && itemOfPropertyConstraint != null;
    }
}

package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

import java.util.List;


public class EntityTypeScrutinizer extends SnakScrutinizer {
    
    public final static String type = "invalid-entity-type";
    public static String ALLOWED_ENTITY_TYPES_QID = "Q52004125";
    public static String ALLOWED_ITEM_TYPE_QID = "Q29934200";
    public static String ALLOWED_ENTITY_TYPES_PID = "P2305";

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        PropertyIdValue pid = snak.getPropertyId();
        List<Statement> statementList = _fetcher.getConstraintsByType(pid, ALLOWED_ENTITY_TYPES_QID);
        if(!statementList.isEmpty()) {
            List<SnakGroup> constraint = statementList.get(0).getClaim().getQualifiers();
            boolean isUsable = true;
            if (constraint != null) {
                isUsable = findValues(constraint, ALLOWED_ENTITY_TYPES_PID).contains(
                        Datamodel.makeWikidataItemIdValue(ALLOWED_ITEM_TYPE_QID));
            }
            if (!isUsable) {
                QAWarning issue = new QAWarning(type, null, QAWarning.Severity.WARNING, 1);
                issue.setProperty("property_entity", pid);
                issue.setProperty("example_entity", entityId);
                addIssue(issue);
            }
        }
    }
}

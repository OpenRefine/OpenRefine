package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;


public class EntityTypeScrutinizer extends SnakScrutinizer {
    
    public final static String type = "invalid-entity-type";

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        PropertyIdValue pid = snak.getPropertyId();
        if(!_fetcher.usableOnItems(pid)) {
            QAWarning issue = new QAWarning(type, null, QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", entityId);
            addIssue(issue);
        }
    }
}

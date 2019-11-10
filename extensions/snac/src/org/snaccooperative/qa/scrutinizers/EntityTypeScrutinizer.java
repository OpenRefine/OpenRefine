package org.snaccooperative.qa.scrutinizers;

import org.snaccooperative.qa.QAWarning;
import org.SNAC.wdtk.datamodel.interfaces.EntityIdValue;
import org.SNAC.wdtk.datamodel.interfaces.PropertyIdValue;
import org.SNAC.wdtk.datamodel.interfaces.Snak;


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

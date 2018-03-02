package org.openrefine.wikidata.qa.scrutinizers;

import org.openrefine.wikidata.qa.QAWarning;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;

/**
 * A scrutinizer that checks for self-referential statements.
 * These statements are flagged by Wikibase as suspicious.
 * 
 * @author antonin
 *
 */
public class SelfReferentialScrutinizer extends SnakScrutinizer {
    
    public static final String type = "self-referential-statements";

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (entityId.equals(snak.getValue())) {
            QAWarning issue = new QAWarning(
                 type, null,
                 QAWarning.Severity.WARNING, 1);
            issue.setProperty("example_entity", entityId);
            addIssue(issue);
        }
    }

}

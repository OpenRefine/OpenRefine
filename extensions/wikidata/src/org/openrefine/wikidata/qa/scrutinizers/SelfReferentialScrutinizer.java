package org.openrefine.wikidata.qa.scrutinizers;

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

    @Override
    public void scrutinize(Snak snak, EntityIdValue entityId, boolean added) {
        if (entityId.equals(snak.getValue())) {
            warning("self-referential-statements");
        }
    }

}

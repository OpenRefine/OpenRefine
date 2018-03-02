package org.openrefine.wikidata.qa.scrutinizers;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A scrutinizer checking for unsourced statements
 * 
 * @author antonin
 *
 */
public class UnsourcedScrutinizer extends StatementScrutinizer {
    
    public static final String type = "unsourced-statements";

    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        if(statement.getReferences().isEmpty() && added) {
            warning(type);
        }
    }

}

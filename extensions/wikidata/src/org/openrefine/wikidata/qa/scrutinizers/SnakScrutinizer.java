package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Iterator;

import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class SnakScrutinizer extends StatementScrutinizer {
    
    public abstract void scrutinizeAdded(Snak snak);

    public abstract void scrutinizeDeleted(Snak snak);
    
    @Override
    public void scrutinizeAdded(Statement statement) {
        // Main snak
        scrutinizeAdded(statement.getClaim().getMainSnak());
        
        // Qualifiers
        scrutinizeSnakSet(statement.getClaim().getAllQualifiers(), true);
        
        // References
        for(Reference ref : statement.getReferences()) {
            scrutinizeSnakSet(ref.getAllSnaks(), true);
        }
    }

    @Override
    public void scrutinizeDeleted(Statement statement) {
        // Main snak
        scrutinizeDeleted(statement.getClaim().getMainSnak());
        
        // Qualifiers
        scrutinizeSnakSet(statement.getClaim().getAllQualifiers(), false);
        
        // References
        for(Reference ref : statement.getReferences()) {
            scrutinizeSnakSet(ref.getAllSnaks(), false);
        }
    }
    
    private void scrutinizeSnakSet(Iterator<Snak> snaks, boolean add) {
        while(snaks.hasNext()) {
            Snak snak = snaks.next();
            if (add) {
                scrutinizeAdded(snak);
            } else {
                scrutinizeDeleted(snak);
            }
        }
    }
}

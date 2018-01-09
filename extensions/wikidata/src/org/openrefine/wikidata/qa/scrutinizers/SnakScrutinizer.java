package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Iterator;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

public abstract class SnakScrutinizer extends StatementScrutinizer {
    
    public abstract void scrutinize(Snak snak, boolean added);
    
    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        // Main snak
        scrutinize(statement.getClaim().getMainSnak(), added);
        
        // Qualifiers
        scrutinizeSnakSet(statement.getClaim().getAllQualifiers(), added);
        
        // References
        for(Reference ref : statement.getReferences()) {
            scrutinizeSnakSet(ref.getAllSnaks(), added);
        }
    }
    
    private void scrutinizeSnakSet(Iterator<Snak> snaks, boolean added) {
        while(snaks.hasNext()) {
            Snak snak = snaks.next();
            scrutinize(snak, added);
        }
    }
}

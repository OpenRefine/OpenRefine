package org.openrefine.wikidata.qa.scrutinizers;

import java.util.Iterator;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.Statement;

/**
 * A scrutinizer that inspects snaks individually, no matter whether they
 * appear as main snaks, qualifiers or references.
 * 
 * @author antonin
 *
 */
public abstract class SnakScrutinizer extends StatementScrutinizer {
    
    /**
     * This is the method that subclasses should override to implement their checks.
     * @param snak: the snak to inspect
     * @param entityId: the item on which it is going to (dis)appear
     * @param added: whether this snak is going to be added or deleted
     */
    public abstract void scrutinize(Snak snak, EntityIdValue entityId, boolean added);
    
    @Override
    public void scrutinize(Statement statement, EntityIdValue entityId, boolean added) {
        // Main snak
        scrutinize(statement.getClaim().getMainSnak(), entityId, added);
        
        // Qualifiers
        scrutinizeSnakSet(statement.getClaim().getAllQualifiers(), entityId, added);
        
        // References
        for(Reference ref : statement.getReferences()) {
            scrutinizeSnakSet(ref.getAllSnaks(), entityId, added);
        }
    }
    
    private void scrutinizeSnakSet(Iterator<Snak> snaks, EntityIdValue entityId, boolean added) {
        while(snaks.hasNext()) {
            Snak snak = snaks.next();
            scrutinize(snak, entityId, added);
        }
    }
}

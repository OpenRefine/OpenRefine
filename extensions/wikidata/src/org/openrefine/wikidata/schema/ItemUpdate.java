package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;

import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;


/**
 * A class to plan an update of an item, after evaluating the statements
 * but before fetching the current content of the item (this is why it does not
 * extend StatementsUpdate).
 * 
 * @author antonin
 */
public class ItemUpdate {
    private ItemIdValue qid;
    private List<Statement> addedStatements;
    private List<Statement> deletedStatements;
    
    public ItemUpdate(ItemIdValue qid) {
        this.qid = qid;
        this.addedStatements = new ArrayList<Statement>();
        this.deletedStatements = new ArrayList<Statement>();
    }
    
    public void addStatement(Statement s) {
        addedStatements.add(s);
    }
    
    public void deleteStatement(Statement s) {
        deletedStatements.add(s);
    }
    
    public void addStatements(List<Statement> l) {
        addedStatements.addAll(l);
    }
    
    public void deleteStatements(List<Statement> l) {
        deletedStatements.addAll(l);
    }
    
    public ItemIdValue getItemId() {
        return qid;
    }
    
    public List<Statement> getAddedStatements() {
        return addedStatements;
    }
    
    public List<Statement> getDeletedStatements() {
        return deletedStatements;
    }

    /**
     * Merges all the changes in other into this instance.
     * @param other: the other change that should be merged
     */
    public void merge(ItemUpdate other) {
        addStatements(other.getAddedStatements());
        deleteStatements(other.getDeletedStatements());
    }

    public boolean isNull() {
        return addedStatements.isEmpty() && deletedStatements.isEmpty();
    }
}

package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ValueVisitor;

/**
 * A placeholder for the Qid of a new item, which
 * also remembers from which reconciled cell it was
 * generated. This allows us to make sure that we will
 * create only one item per cell marked as "new".
 * 
 * @author antonin
 */
public class NewEntityIdValue implements ItemIdValue {
    
    private final int rowId;
    private final int colId;
    
    /**
     * Creates a new entity id corresponding to the
     * cell designated by the indices.
     * 
     * @param rowId
     *          the index of the row for the cell
     * @param colId
     *          the index of the column for the cell
     */
    public NewEntityIdValue(int rowId, int colId) {
        this.rowId = rowId;
        this.colId = colId;
    }
    
    public int getRowId() {
        return rowId;
    }
    
    public int getColId() {
        return colId;
    }
    
    /**
     * Equality check is important when we gather
     * all ItemUpdates related to an ItemId.
     */
    @Override
    public boolean equals(Object other) {
        if (other == null ||
            !NewEntityIdValue.class.isInstance(other)) {
            return false;
        }
        final NewEntityIdValue otherNew = (NewEntityIdValue)other;
        return (rowId == otherNew.getRowId() &&
                colId == otherNew.getColId());
    }
    
    @Override
    public int hashCode() {
        int hash = 3;
        hash = 41*hash + rowId;
        hash = 41*hash + colId;
        return hash;
    }
    
    @Override
    public String getIri() {
            return getSiteIri() + getId();
    }

    @Override
    public <T> T accept(ValueVisitor<T> valueVisitor) {
            return valueVisitor.visit(this);
    }

    @Override
    public String getEntityType() {
            return ET_ITEM;
    }

    @Override
    public String getId() {
            return "Q0";
    }

    @Override
    public String getSiteIri() {
            return EntityIdValue.SITE_LOCAL;
    }

}

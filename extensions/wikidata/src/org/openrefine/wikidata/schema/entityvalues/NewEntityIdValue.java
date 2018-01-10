package org.openrefine.wikidata.schema.entityvalues;

/**
 * A placeholder for the Qid of a new item, which
 * also remembers from which reconciled cell it was
 * generated. This allows us to make sure that we will
 * create only one item per cell marked as "new".
 * 
 * @author antonin
 */
public class NewEntityIdValue extends TermedItemIdValue {
    
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
     * @param label
     *          the value of the cell
     */
    public NewEntityIdValue(int rowId, int colId, String siteIRI, String label) {
        super("Q0", siteIRI, label);
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
}

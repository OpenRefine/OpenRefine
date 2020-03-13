package org.openrefine.history.dag;

import java.util.List;

import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;

/**
 * Exception indicating that a DAG slice cannot be applied to 
 * a given state (given by the list of column metadata at that stage).
 * 
 * @author Antonin Delpeuch
 *
 */
public class IncompatibleSliceException extends Exception {

    private static final long serialVersionUID = -8875879446276064143L;
    
    private DagSlice slice;
    private ColumnModel columns;

    public IncompatibleSliceException(DagSlice slice, ColumnModel columns) {
        this.slice = slice;
        this.columns = columns;
    }
    
    /**
     * @return
     *    the slice which failed to apply
     */
    public DagSlice getSlice() {
        return slice;
    }
    
    /**
     * @return
     *    the columns the slice could not be applied to
     */
    public ColumnModel getColumnModel() {
        return columns;
    }
    
    @Override
    public String getMessage() {
       return String.format("Slice %s cannot be applied to columns %s", slice, columns); 
    }
}

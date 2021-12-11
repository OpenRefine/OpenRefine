package org.openrefine.model;

import java.io.Serializable;
import java.util.List;

/**
 * A function applied to a row, returning a list of new rows to replace it.
 * Implementations should be stateless. If a state is required,
 * use {@link RowScanMapper}. If multiple rows or no rows can be returned,
 * use {@link RowFlatMapper}.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface RowFlatMapper extends Serializable {
    
    public List<Row> call(long rowId, Row row);
    
    /**
     * Returns a flat mapper which applies one of the two mappers provided depending
     * on the outcome of the filter. If the filter evaluates to true, the positive
     * mapper is evaluated, otherwise the negative one is used.
     * 
     * @param filter the filter to use for the disjunction
     * @param positive what to do if the filter evaluates to true
     * @param negative what to do otherwise
     * @return the conditional mapper
     */
    public static RowFlatMapper conditionalMapper(RowFilter filter, RowFlatMapper positive, RowFlatMapper negative) {
        return new RowFlatMapper() {
            
            private static final long serialVersionUID = 7178778933601927979L;

            @Override
            public List<Row> call(long rowId, Row row) {
                if (filter.filterRow(rowId, row)) {
                    return positive.call(rowId, row);
                } else {
                    return negative.call(rowId, row);
                }
            }
            
        };
    }
}


package org.openrefine.model;

import java.io.Serializable;

/**
 * A function applied to a row, returning a new row to replace it. Implementations should be stateless. If a state is
 * required, use {@link RowScanMapper}.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface RowMapper extends Serializable {

    public Row call(long rowId, Row row);

    /**
     * A row mapper which returns unchanged rows.
     */
    public static RowMapper IDENTITY = new RowMapper() {

        private static final long serialVersionUID = 5980234797287459615L;

        @Override
        public Row call(long rowId, Row row) {
            return row;
        }

    };

    /**
     * Returns a mapper which applies one of the two mappers provided depending on the outcome of the filter. If the
     * filter evaluates to true, the positive mapper is evaluated, otherwise the negative one is used.
     * 
     * @param filter
     *            the filter to use for the disjunction
     * @param positive
     *            what to do if the filter evaluates to true
     * @param negative
     *            what to do otherwise
     * @return the conditional mapper
     */
    public static RowMapper conditionalMapper(RowFilter filter, RowMapper positive, RowMapper negative) {
        return new RowMapper() {

            private static final long serialVersionUID = 7178778933601927979L;

            @Override
            public Row call(long rowId, Row row) {
                if (filter.filterRow(rowId, row)) {
                    return positive.call(rowId, row);
                } else {
                    return negative.call(rowId, row);
                }
            }

        };
    }
}

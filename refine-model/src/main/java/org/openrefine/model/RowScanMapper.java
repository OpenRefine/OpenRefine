
package org.openrefine.model;

import java.io.Serializable;

/**
 * A row mapper which is able to update a state as it reads rows. Only use this if {@link RowMapper} cannot be used.
 * 
 * @author Antonin Delpeuch
 *
 * @param <S>
 */
public interface RowScanMapper<S> extends Serializable {

    /**
     * Produces the state associated with a given row.
     * 
     * @param rowId
     *            the index of the row
     * @param row
     *            the row to map to the corresponding state
     * @return
     */
    public S feed(long rowId, Row row);

    /**
     * Combines two states produced by rows. This needs to be associative and unital (for the unit provided below).
     * 
     * @param left
     * @param right
     * @return
     */
    public S combine(S left, S right);

    /**
     * The initial state of the mapper.
     */
    public S unit();

    /**
     * Given the accumulated state of all the previous rows (the list of states produced by feed and merged using
     * combine), transform the current row to a new one.
     */
    public Row map(S state, long rowId, Row row);

    /**
     * Restricts a {@link RowScanMapper} to only aggregate its state on filtered rows, and only apply the map function
     * to those filtered rows. A stateless mapper is applied to the other rows.
     * 
     * @param <S>
     *            the internal state of the mapper
     * @param filter
     *            the row filter used to determine which mapper to apply to a given row
     * @param rowScanMapper
     *            the mapper for rows selected by the filter
     * @param negativeMapper
     *            the mapper for rows not selected by the filter
     * @return
     */
    public static <S> RowScanMapper<S> conditionalMapper(RowFilter filter, RowScanMapper<S> rowScanMapper, RowMapper negativeMapper) {
        return new RowScanMapper<S>() {

            private static final long serialVersionUID = -3179605285331078257L;

            @Override
            public S feed(long rowId, Row row) {
                if (filter.filterRow(rowId, row)) {
                    return rowScanMapper.feed(rowId, row);
                } else {
                    return rowScanMapper.unit();
                }
            }

            @Override
            public S combine(S left, S right) {
                return rowScanMapper.combine(left, right);
            }

            @Override
            public S unit() {
                return rowScanMapper.unit();
            }

            @Override
            public Row map(S state, long rowId, Row row) {
                if (filter.filterRow(rowId, row)) {
                    return rowScanMapper.map(state, rowId, row);
                } else {
                    return negativeMapper.call(rowId, row);
                }
            }

        };
    }
}

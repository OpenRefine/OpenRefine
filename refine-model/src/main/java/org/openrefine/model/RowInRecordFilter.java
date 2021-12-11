
package org.openrefine.model;

import java.util.List;

/**
 * Row filter which has also access to the spanning record. This is needed when filtering by expressions which refer to
 * the record via the `row.record` variable.
 * 
 * When used as a {@link RowFilter} this just passes `null` as a record.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class RowInRecordFilter implements RowFilter, RecordFilter {

    private static final long serialVersionUID = -7174059662391981636L;

    private final boolean anyRow;

    /**
     * @param anyRow
     *            true if any matching row makes the record match, false if all rows are required.
     */
    public RowInRecordFilter(boolean anyRow) {
        this.anyRow = anyRow;
    }

    /**
     * Determines if a row matches the filter.
     * 
     * @param rowIndex
     *            the global index of the row (from the entire grid)
     * @param row
     *            the row itself
     * @param record
     *            the enclosing record
     * @return true if the row matches the filter
     */
    public abstract boolean filterRow(long rowIndex, Row row, Record record);

    @Override
    public boolean filterRecord(Record record) {
        List<Row> rows = record.getRows();
        for (int i = 0; i < rows.size(); i++) {
            Row row = rows.get(i);
            long rowId = record.getStartRowId() + i;
            if (filterRow(rowId, row, record) == anyRow) {
                return anyRow;
            }
        }
        return !anyRow;
    }

    @Override
    public boolean filterRow(long rowIndex, Row row) {
        return filterRow(rowIndex, row, null);
    }

    public static RowInRecordFilter fromRowFilter(RowFilter rowFilter) {
        return new RowInRecordFilter(true) {

            private static final long serialVersionUID = -2272861290097135775L;

            @Override
            public boolean filterRow(long rowIndex, Row row, Record record) {
                return rowFilter.filterRow(rowIndex, row);
            }

        };
    }

    public static final RowInRecordFilter ANY_ROW_IN_RECORD = new RowInRecordFilter(true) {

        private static final long serialVersionUID = 3807800088433931083L;

        @Override
        public boolean filterRow(long rowIndex, Row row, Record record) {
            return true;
        }

    };

}

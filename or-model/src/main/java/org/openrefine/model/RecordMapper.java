
package org.openrefine.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A function applied to a record, returning a list of rows to replace it. They do not need to form a single record.
 * 
 * @author Antonin Delpeuch
 *
 */
public interface RecordMapper extends Serializable {

    /**
     * Maps a record to a list of rows.
     * 
     * @param record
     * @return
     */
    public List<Row> call(Record record);

    /**
     * If every record is mapped to a list with the same number of rows as what the record contains, set this boolean to
     * true. This helps the underlying implementation avoid recomputing row ids.
     */
    public default boolean preservesRowCount() {
        return false;
    }

    /**
     * Applies a row mapper on all rows in the record.
     */
    public static RecordMapper rowWiseRecordMapper(RowMapper mapper) {
        return new RecordMapper() {

            private static final long serialVersionUID = -3973310131665435325L;

            @Override
            public List<Row> call(Record record) {
                List<Row> rows = record.getRows();
                List<Row> results = new ArrayList<>(rows.size());
                for (int i = 0; i != rows.size(); i++) {
                    results.add(mapper.call(record.getStartRowId() + i, rows.get(i)));
                }
                return results;
            }

            @Override
            public boolean preservesRowCount() {
                return true;
            }

        };
    }

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
    public static RecordMapper conditionalMapper(RecordFilter filter, RecordMapper positive, RecordMapper negative) {
        return new RecordMapper() {

            private static final long serialVersionUID = -382102889800400246L;

            @Override
            public List<Row> call(Record record) {
                if (filter.filterRecord(record)) {
                    return positive.call(record);
                } else {
                    return negative.call(record);
                }
            }

        };
    }
}

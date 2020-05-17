
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

    public List<Row> call(Record record);

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

        };
    }
}

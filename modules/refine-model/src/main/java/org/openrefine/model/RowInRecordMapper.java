
package org.openrefine.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Like {@link RowMapper} except that the enclosing {@link Record} is also available. This defines a
 * {@link RecordMapper} which maps each row accordingly.
 * 
 * When used as a {@link RowMapper}, the record argument is set to null.
 * 
 * This class is useful when performing a row-wise operation in records mode, as the enclosing record must be available
 * in the expression language.
 * 
 *
 */
public abstract class RowInRecordMapper implements RowMapper, RecordMapper {

    private static final long serialVersionUID = -2751707283001756980L;

    /**
     * Maps a row, in the context of a record.
     * 
     * @param record
     *            the record enclosing the row to map
     * @param rowId
     *            the global index of the row in the entire grid
     * @param row
     *            the row itself
     */
    public abstract Row call(Record record, long rowId, Row row);

    /**
     * Is this mapper guaranteed to preserve the records structure, both as a record mapper and a row mapper?
     */
    @Override
    public abstract boolean preservesRecordStructure();

    @Override
    public List<Row> call(Record record) {
        List<Row> rows = record.getRows();
        List<Row> results = new ArrayList<>(rows.size());
        for (int i = 0; i != rows.size(); i++) {
            results.add(call(record, record.getStartRowId() + i, rows.get(i)));
        }
        return results;
    }

    @Override
    public boolean preservesRowCount() {
        return true;
    }

    @Override
    public Row call(long rowId, Row row) {
        return call(null, rowId, row);
    }

    public static RowInRecordMapper fromRowMapper(RowMapper rowMapper) {
        return new RowInRecordMapper() {

            private static final long serialVersionUID = -985327238622556721L;

            @Override
            public Row call(Record record, long rowId, Row row) {
                return rowMapper.call(rowId, row);
            }

            @Override
            public boolean preservesRecordStructure() {
                return rowMapper.preservesRecordStructure();
            }

        };
    }

    public final static RowInRecordMapper IDENTITY = fromRowMapper(RowMapper.IDENTITY);

}

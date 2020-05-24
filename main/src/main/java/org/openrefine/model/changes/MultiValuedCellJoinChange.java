
package org.openrefine.model.changes;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.expr.ExpressionUtils;
import org.openrefine.history.Change;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;

/**
 * Within a record, joins the non-blank cells of a column into the first cell, with the specified separator. The
 * keyColumnName can be used to specify which column should be treated as record key (although this parameter has never
 * been exposed in the UI as of 2020-05).
 * 
 * @author Antonin Delpeuch
 *
 */
public class MultiValuedCellJoinChange implements Change {

    private final String columnName;
    private final String keyColumnName;
    private final String separator;

    @JsonCreator
    public MultiValuedCellJoinChange(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("separator") String separator) {
        this.columnName = columnName;
        this.keyColumnName = keyColumnName;
        this.separator = separator;
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

    @JsonProperty("keyColumnName")
    public String getKeyColumnName() {
        return keyColumnName;
    }

    @JsonProperty("separator")
    public String getSeparator() {
        return separator;
    }

    @Override
    public GridState apply(GridState projectState) throws DoesNotApplyException {
        ColumnModel columnModel = projectState.getColumnModel();
        int columnIdx = columnModel.getColumnIndexByName(columnName);
        if (columnIdx == -1) {
            throw new DoesNotApplyException(
                    String.format("Column '%s' does not exist", columnName));
        }
        int keyColumnIdx = keyColumnName == null ? 0 : columnModel.getColumnIndexByName(keyColumnName);
        if (keyColumnIdx == -1) {
            throw new DoesNotApplyException(
                    String.format("Key column '%s' does not exist", keyColumnName));
        }
        if (keyColumnIdx != columnModel.getKeyColumnIndex()) {
            projectState = projectState.withColumnModel(columnModel.withKeyColumnIndex(keyColumnIdx));
        }
        return projectState.mapRecords(
                recordMapper(columnIdx, separator),
                columnModel);
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    protected static RecordMapper recordMapper(int columnIdx, String separator) {
        return new RecordMapper() {

            private static final long serialVersionUID = -5684754503934565526L;

            @Override
            public List<Row> call(Record record) {
                List<Row> rows = record.getRows();

                // Join the non-blank cell values
                StringBuffer sb = new StringBuffer();
                for (int i = 0; i != rows.size(); i++) {
                    Object value = rows.get(i).getCellValue(columnIdx);
                    if (ExpressionUtils.isNonBlankData(value)) {
                        if (sb.length() > 0) {
                            sb.append(separator);
                        }
                        sb.append(value.toString());
                    }
                }

                // Compute the new rows
                List<Row> newRows = new ArrayList<>(rows.size());
                String joined = sb.toString();
                newRows.add(rows.get(0).withCell(columnIdx, new Cell(joined.isEmpty() ? null : joined, null)));
                for (int i = 1; i < rows.size(); i++) {
                    Row row = rows.get(i).withCell(columnIdx, null);
                    // Only add rows if they are not entirely blank after removing the joined value
                    if (row.getCells().stream().anyMatch(c -> c != null && ExpressionUtils.isNonBlankData(c.getValue()))) {
                        newRows.add(row);
                    }
                }

                return newRows;
            }

        };
    }

}

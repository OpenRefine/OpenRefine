
package org.openrefine.model.changes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.openrefine.history.Change;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.GridState;
import org.openrefine.model.Record;
import org.openrefine.model.RecordMapper;
import org.openrefine.model.Row;
import org.openrefine.model.changes.ColumnSplitChange.CellValueSplitter;
import org.openrefine.model.changes.ColumnSplitChange.Mode;

/**
 * Splits the value of a cell and spreads the splits on the following rows, while respecting the record structure. The
 * keyColumnName can be used to specify which column should be treated as record key (although this parameter has never
 * been exposed in the UI as of 2020-05).
 * 
 * @author Antonin Delpeuch
 *
 */
public class MultiValuedCellSplitChange implements Change {

    private final String columnName;
    private final String keyColumnName;
    private final Mode mode;
    private final String separator;
    private final Boolean regex;
    private final int[] fieldLengths;
    private final CellValueSplitter splitter;

    @JsonCreator
    public MultiValuedCellSplitChange(
            @JsonProperty("columnName") String columnName,
            @JsonProperty("keyColumnName") String keyColumnName,
            @JsonProperty("mode") Mode mode,
            @JsonProperty("separator") String separator,
            @JsonProperty("regex") Boolean regex,
            @JsonProperty("fieldLengths") int[] fieldLengths) {
        this.columnName = columnName;
        this.keyColumnName = keyColumnName;
        this.mode = mode;
        this.separator = separator;
        this.regex = regex;
        this.fieldLengths = fieldLengths;
        this.splitter = CellValueSplitter.construct(mode, separator, regex, fieldLengths, null);
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }

    @JsonProperty("columnName")
    public String getKeyColumnName() {
        return keyColumnName;
    }

    @JsonProperty("mode")
    public Mode getMode() {
        return mode;
    }

    @JsonProperty("separator")
    @JsonInclude(Include.NON_NULL)
    public String getSeparator() {
        return separator;
    }

    @JsonProperty("regex")
    @JsonInclude(Include.NON_NULL)
    public Boolean getRegex() {
        return regex;
    }

    @JsonProperty("fieldLengths")
    @JsonInclude(Include.NON_NULL)
    public int[] getFieldLengths() {
        return fieldLengths;
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
        return projectState.mapRecords(recordMapper(columnIdx, splitter), columnModel);
    }

    @Override
    public boolean isImmediate() {
        return true;
    }

    protected static RecordMapper recordMapper(int columnIdx, CellValueSplitter splitter) {
        return new RecordMapper() {

            private static final long serialVersionUID = -6481651649785541256L;

            @Override
            public List<Row> call(Record record) {
                List<String> splits = Collections.emptyList();
                List<Row> origRows = record.getRows();
                List<Row> newRows = new ArrayList<>(origRows.size());
                int rowSize = 0;

                for (int i = 0; i != origRows.size(); i++) {
                    Row row = origRows.get(i);
                    rowSize = row.getCells().size();

                    if (row.isCellBlank(columnIdx)) {
                        if (!splits.isEmpty()) {
                            // We have space to add an accumulated split value
                            String splitValue = splits.remove(0);
                            newRows.add(row.withCell(columnIdx, new Cell(splitValue, null)));
                        } else {
                            // We leave the row as it is
                            newRows.add(row);
                        }
                    } else {
                        // We have a non-empty value to split

                        // We need to exhaust the queue of splits to insert first
                        for (String splitValue : splits) {
                            Row newRow = new Row(Collections.nCopies(rowSize, (Cell) null));
                            newRows.add(newRow.withCell(columnIdx, new Cell(splitValue, null)));
                        }
                        splits = Collections.emptyList();

                        // Split the current value
                        Serializable cellValue = origRows.get(i).getCellValue(columnIdx);
                        if (!(cellValue instanceof String)) {
                            newRows.add(row);
                        } else {
                            // we use a linked list because we pop elements one by one later on
                            splits = new LinkedList<>(splitter.split((String) cellValue));
                            Cell firstSplit = null;
                            if (!splits.isEmpty()) {
                                firstSplit = new Cell(splits.remove(0), null);
                            }
                            newRows.add(row.withCell(columnIdx, firstSplit));
                        }
                    }
                }

                //  Exhaust any remaining splits at the end
                for (String splitValue : splits) {
                    Row newRow = new Row(Collections.nCopies(rowSize, (Cell) null));
                    newRows.add(newRow.withCell(columnIdx, new Cell(splitValue, null)));
                }

                return newRows;
            }

        };
    }

}

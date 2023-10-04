
package org.openrefine.model;

import org.apache.commons.lang3.Validate;
import org.openrefine.expr.Evaluable;
import org.openrefine.operations.exceptions.MissingColumnException;
import org.openrefine.overlay.OverlayModel;
import org.openrefine.util.ColumnDependencyException;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Translates column-dependent metadata to a reduced column model, which is tailored to the dependencies of a particular
 * expression or operation. <br>
 * This is used to provide some isolation around operators like {@link RowMapper} or
 * {@link org.openrefine.model.changes.RowChangeDataProducer}: if they declare depending only on certain columns, this
 * class can be used to feed them with reduced {@link Row}s or {@link Record}s, which guarantee that they cannot rely on
 * information from any other column.
 */
public class ColumnMapper implements Serializable {

    protected final List<ColumnId> dependencies;
    protected final ColumnModel newColumnModel;
    protected final List<Integer> columnIndices;
    protected final ColumnModel reducedColumnModel;

    /**
     * Constructor.
     *
     * @param dependencies
     *            the list of columns depended on, or null if any columns can be potentially relied on
     * @param newColumnModel
     *            the column model to be reduced, which must contain all the columns dependended on. All the columns not
     *            depended on will be stripped from the resulting column model (which can be obtained via
     *            {@link #getReducedColumnModel()}).
     */
    public ColumnMapper(List<ColumnId> dependencies, ColumnModel newColumnModel) {
        this.dependencies = dependencies == null ? null : dependencies.stream().distinct().collect(Collectors.toList());
        this.newColumnModel = newColumnModel;
        this.columnIndices = dependencies == null ? null
                : this.dependencies.stream()
                        .map(newColumnModel::getRequiredColumnIndex)
                        .collect(Collectors.toList());
        this.reducedColumnModel = dependencies == null ? null
                : new ColumnModel(columnIndices.stream()
                        .map(newColumnModel::getColumnByIndex)
                        .collect(Collectors.toList()),
                        columnIndices.indexOf(newColumnModel.getKeyColumnIndex()),
                        newColumnModel.hasRecords() && columnIndices.contains(newColumnModel.getKeyColumnIndex()));
    }

    public Row translateRow(Row row) {
        if (dependencies == null) {
            return row;
        } else {
            return new Row(columnIndices.stream().map(row::getCell).collect(Collectors.toList()));
        }
    }

    public IndexedRow translateIndexedRow(IndexedRow indexedRow) {
        if (dependencies == null) {
            return indexedRow;
        } else {
            return new IndexedRow(indexedRow.getIndex(), translateRow(indexedRow.getRow()));
        }
    }

    public List<IndexedRow> translateIndexedRowBatch(List<IndexedRow> next) {
        if (dependencies == null) {
            return next;
        } else {
            return next.stream().map(this::translateIndexedRow).collect(Collectors.toList());
        }
    }

    public Record translateRecord(Record record) {
        if (dependencies == null) {
            return record;
        } else {
            // when mapping records we require that the key column index is included
            Validate.isTrue(columnIndices.contains(newColumnModel.getKeyColumnIndex()),
                    "key column not included as a dependency while we are mapping records");
            return new Record(record.getStartRowId(), record.getRows().stream().map(
                    row -> new Row(columnIndices.stream().map(row::getCell).collect(Collectors.toList()))).collect(Collectors.toList()));
        }
    }

    public List<Record> translateRecordBatch(List<Record> batch) {
        if (dependencies == null) {
            return batch;
        } else {
            return batch.stream().map(this::translateRecord).collect(Collectors.toList());
        }
    }

    public int translateColumnIndex(int columnIndex) {
        if (dependencies == null) {
            return columnIndex;
        } else {
            return columnIndices.indexOf(columnIndex);
        }
    }

    public Map<String, OverlayModel> translateOverlays(Map<String, OverlayModel> overlayModels) {
        // TODO add method on OverlayModel to translate the overlay up to a column rename
        // and implement this.
        return overlayModels;
    }

    public ColumnModel getReducedColumnModel() {
        if (dependencies == null) {
            return newColumnModel;
        } else {
            return reducedColumnModel;
        }
    }

    public List<ColumnId> getDependencies() {
        return dependencies;
    }
}


package org.openrefine.importers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnMetadata;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowMapper;
import org.openrefine.sorting.SortingConfig;
import org.openrefine.util.JSONUtilities;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * An importer which reads each line of a file as a row with a single string-valued cell containing the line.
 * 
 * @author Antonin Delpeuch
 *
 */
public class LineBasedImporter extends LineBasedImporterBase {

    @Override
    protected RowMapper getRowMapper(ObjectNode options) {
        return RowMapper.IDENTITY;
    }

    @Override
    protected int getColumnCount(GridState rawCells, RowMapper rowMapper, ObjectNode options) {
        return 1;
    }

    @Override
    protected int getPassesNeededToComputeColumnCount(ObjectNode options) {
        return 0;
    }

    @Override
    protected GridState postTransform(GridState parsed, ObjectNode options) {
        int linesPerRow = JSONUtilities.getInt(options, "linesPerRow", 1);
        if (linesPerRow == 1) {
            return parsed;
        } else {
            // we do not have an efficient way to read multiple lines for a given row,
            // so we resort to loading everything in memory
            List<Row> newRows = new ArrayList<>();
            List<Cell> currentCells = new ArrayList<>();
            for (IndexedRow row : parsed.iterateRows(RowFilter.ANY_ROW)) {
                currentCells.add(row.getRow().getCell(0));
                if (currentCells.size() >= linesPerRow) {
                    newRows.add(new Row(currentCells));
                    currentCells = new ArrayList<>();
                }
            }
            if (!currentCells.isEmpty()) {
                while (currentCells.size() < linesPerRow) {
                    currentCells.add(null);
                }
                newRows.add(new Row(currentCells));
            }
            List<ColumnMetadata> columns = new ArrayList<>();
            for (int i = 0; i != linesPerRow; i++) {
                columns.add(new ColumnMetadata("Column " + (i + 1)));
            }
            ColumnModel columnModel = new ColumnModel(columns);
            return parsed.getDatamodelRunner().create(columnModel, newRows, Collections.emptyMap());
        }
    }

    @Override
    public ObjectNode createParserUIInitializationData(DatamodelRunner runner,
            ImportingJob job, List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(runner, job, fileRecords, format);
        JSONUtilities.safePut(options, "linesPerRow", 1); // number of lines per row
        return options;
    }

}

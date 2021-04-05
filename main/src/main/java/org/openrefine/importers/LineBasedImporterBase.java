
package org.openrefine.importers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openrefine.ProjectMetadata;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.util.JSONUtilities;

public abstract class LineBasedImporterBase extends HDFSImporter {

    protected LineBasedImporterBase(DatamodelRunner runner) {
        super(runner);
    }

    /**
     * Method to be overridden by subclasses to define how each row should be parsed. The row mapper takes a row with
     * only a single string cell as argument and returns a parsed row.
     * 
     * The mapper may return rows of different lengths: in any case the maximum should not exceed the count returned by
     * getColumnCount.
     * 
     * @param options
     *            the importing options
     * @return
     */
    protected abstract RowMapper getRowMapper(ObjectNode options);

    /**
     * Method to be overridden by subclasses to determine the total number of columns of the project.
     * 
     * By default this computes the maximum of the row lengths returned by the row mapper. If this number is known in
     * advance from the importing options, it is worth overriding the method to avoid a pass on the dataset.
     * 
     * @param rawCells
     *            the lines of the text file represented as a grid
     * @param rowMapper
     *            the row mapper to be applied to this grid to obtain the final grid
     * @param options
     *            the parsing options
     * @return
     */
    protected int getColumnCount(GridState rawCells, RowMapper rowMapper, ObjectNode options) {
        return countMaxColumnNb(rawCells, rowMapper);
    }

    @Override
    public GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource, String archiveFileName, String sparkURI,
            long limit, ObjectNode options)
            throws Exception {
        int ignoreLines = Math.max(JSONUtilities.getInt(options, "ignoreLines", -1), 0);
        int headerLines = Math.max(JSONUtilities.getInt(options, "headerLines", 0), 0);
        int skipDataLines = Math.max(JSONUtilities.getInt(options, "skipDataLines", 0), 0);
        String[] optionColumnNames = JSONUtilities.getStringArray(options, "columnNames");
        boolean includeFileSources = JSONUtilities.getBoolean(options, "includeFileSources", false);
        boolean includeArchiveFileName = JSONUtilities.getBoolean(options, "includeArchiveFileName", false);
        boolean guessCellValueTypes = JSONUtilities.getBoolean(options, "guessCellValueTypes", false);
        boolean trimStrings = JSONUtilities.getBoolean(options, "trimStrings", false);
        boolean storeBlankCellsAsNulls = JSONUtilities.getBoolean(options, "storeBlankCellsAsNulls", true);

        long limit2 = JSONUtilities.getLong(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }

        RowMapper rowMapper = getRowMapper(options);
        GridState rawCells = limit2 > 0 ? runner.loadTextFile(sparkURI, limit2 + ignoreLines + headerLines + skipDataLines)
                : runner.loadTextFile(sparkURI);

        // Compute the maximum number of cells in the entire grid
        int maxColumnNb = getColumnCount(rawCells, rowMapper, options);

        // Parse column names
        List<String> columnNames = new ArrayList<>();

        if (optionColumnNames.length > 0) {
            for (int i = 0; i != optionColumnNames.length; i++) {
                ImporterUtilities.appendColumnName(columnNames, i, optionColumnNames[i]);
            }
            headerLines = 0;
        } else if (headerLines > 0) {
            List<IndexedRow> firstLines = rawCells.getRows(ignoreLines, headerLines);
            for (int i = 0; i < firstLines.size(); i++) {
                IndexedRow headerLine = firstLines.get(i);
                Row mappedRow = rowMapper.call(headerLine.getIndex(), headerLine.getRow());
                for (int j = 0; j != mappedRow.getCells().size(); j++) {
                    Serializable cellValue = mappedRow.getCellValue(j);
                    ImporterUtilities.appendColumnName(columnNames, j, cellValue == null ? "" : cellValue.toString());
                }
            }
        }

        ColumnModel columnModel = ImporterUtilities.setupColumns(columnNames);
        while (columnModel.getColumns().size() < maxColumnNb) {
            columnModel = ImporterUtilities.expandColumnModelIfNeeded(columnModel, columnModel.getColumns().size());
        }
        GridState grid = rawCells.dropRows(ignoreLines + headerLines + skipDataLines)
                .mapRows(rowMapperWithPadding(rowMapper, maxColumnNb), columnModel);

        if (trimStrings || guessCellValueTypes || storeBlankCellsAsNulls) {
            grid = grid.mapRows(cellValueCleaningMapper(guessCellValueTypes, trimStrings, storeBlankCellsAsNulls), columnModel);
        }
        if (includeFileSources) {
            grid = TabularParserHelper.prependColumn("File", fileSource, grid);
        }
        if (includeArchiveFileName) {
            grid = TabularParserHelper.prependColumn("Archive", archiveFileName, grid);
        }
        return grid;
    }

    /**
     * Makes sure a row mapper always returns rows of the same size.
     * 
     * We do not simply express this as two consecutive maps because the intermediate grid size would be invalid (not
     * having as many cells in each rows as there are columns).
     * 
     * @param mapper
     *            the original row mapper
     * @param nbColumns
     *            the number of columns
     * @return the modified mapper
     */
    protected static RowMapper rowMapperWithPadding(RowMapper mapper, int nbColumns) {
        return new RowMapper() {

            private static final long serialVersionUID = 1L;

            @Override
            public Row call(long rowId, Row row) {
                return mapper.call(rowId, row).padWithNull(nbColumns);
            }

        };
    }

    protected static int countMaxColumnNb(GridState grid, RowMapper rowMapper) {
        RowAggregator<Integer> aggregator = new RowAggregator<Integer>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Integer sum(Integer first, Integer second) {
                return Math.max(first, second);
            }

            @Override
            public Integer withRow(Integer state, long rowId, Row row) {
                return Math.max(state, rowMapper.call(rowId, row).getCells().size());
            }

        };

        return grid.aggregateRows(aggregator, 0);
    }

    protected static Cell cleanCellValue(Cell cell, boolean guessTypes, boolean trim, boolean blankToNull) {
        if (cell == null || cell.value == null) {
            return null;
        }
        String cellValue = cell.value.toString();
        if (trim) {
            cellValue = cellValue.trim();
        }
        if (blankToNull && !ExpressionUtils.isNonBlankData(cellValue)) {
            return null;
        }
        if (guessTypes) {
            return new Cell(ImporterUtilities.parseCellValue(cellValue), cell.recon);
        } else {
            return new Cell(cellValue, cell.recon);
        }
    }

    protected static RowMapper cellValueCleaningMapper(boolean guessTypes, boolean trim, boolean blankToNull) {
        return new RowMapper() {

            private static final long serialVersionUID = -1274112816995044699L;

            @Override
            public Row call(long rowId, Row row) {
                List<Cell> cells = row.getCells().stream()
                        .map(c -> cleanCellValue(c, guessTypes, trim, blankToNull))
                        .collect(Collectors.toList());
                return new Row(cells);
            }

        };
    }

}

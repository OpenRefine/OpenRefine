
package org.openrefine.importers;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.openrefine.ProjectMetadata;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.expr.ExpressionUtils;
import org.openrefine.importing.EncodingGuesser;
import org.openrefine.importing.ImportingFileRecord;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.Cell;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowMapper;
import org.openrefine.util.JSONUtilities;

/**
 * Base class for importers which work by reading files line by line, and mapping a line of the file to a row of the
 * corresponding project.
 * 
 * @author Antonin Delpeuch
 *
 */
public abstract class LineBasedImporterBase extends URIImporter {

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
     * If you override this then you should also override {@link #getPassesNeededToComputeColumnCount(ObjectNode)} which
     * predicts how many scans of the dataset will be required to run this method.
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

    /**
     * Returns the number of passes done on the dataset when computing the column count. Override this if you override
     * {@link #getColumnCount(GridState, RowMapper, ObjectNode)} so that progress reporting is appropriately adapted.
     *
     * @param options
     * @return
     */
    protected int getPassesNeededToComputeColumnCount(ObjectNode options) {
        return 1;
    }

    /**
     * Hook to let the subclass perform any other transformation on the grid after its parsing as a line-based file.
     * 
     * By default, it returns the grid unchanged.
     * 
     * @param parsed
     *            the parsed grid
     * @param options
     *            the parsing options
     * @return the final grid state
     */
    protected GridState postTransform(GridState parsed, ObjectNode options) {
        return parsed;
    }

    @Override
    public ObjectNode createParserUIInitializationData(DatamodelRunner runner,
            ImportingJob job, List<ImportingFileRecord> fileRecords, String format) {
        ObjectNode options = super.createParserUIInitializationData(runner, job, fileRecords, format);
        EncodingGuesser.guessInitialEncoding(fileRecords, options);
        JSONUtilities.safePut(options, "ignoreLines", -1); // number of blank lines at the beginning to ignore
        JSONUtilities.safePut(options, "headerLines", 1); // number of header lines

        JSONUtilities.safePut(options, "skipDataLines", 0); // number of initial data lines to skip
        JSONUtilities.safePut(options, "storeBlankRows", true);
        JSONUtilities.safePut(options, "storeBlankCellsAsNulls", true);
        JSONUtilities.safePut(options, "includeArchiveFileName", false);
        return options;
    }

    @Override
    public GridState parseOneFile(DatamodelRunner runner, ProjectMetadata metadata, ImportingJob job,
            String fileSource, String archiveFileName, String sparkURI, long limit, ObjectNode options, MultiFileReadingProgress progress)
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

        // Predict the number of passes we are going to do on the file
        int passes = 2; // corresponds to the initial indexing of the grid (1 pass) and of
        // the saving of the grid in the workspace (another pass)

        // add any other passes which might be required by the importer
        passes += getPassesNeededToComputeColumnCount(options);

        MultiFileReadingProgress scaledProgress = new ScaledProgress(progress, passes);

        Charset charset = Charset.defaultCharset();
        String encoding = JSONUtilities.getString(options, "encoding", null);
        if (encoding != null) {
            charset = Charset.forName(encoding);
        }

        RowMapper rowMapper = getRowMapper(options);
        GridState rawCells;
        if (limit2 > 0) {
            rawCells = runner.loadTextFile(sparkURI, scaledProgress, charset, limit2 + ignoreLines + headerLines + skipDataLines);
        } else {
            rawCells = runner.loadTextFile(sparkURI, scaledProgress, charset);
        }

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
        if (ignoreLines + headerLines + skipDataLines > 0) {
            rawCells = rawCells.dropRows(ignoreLines + headerLines + skipDataLines);
        }
        GridState grid = rawCells.mapRows(rowMapperWithPadding(rowMapper, maxColumnNb), columnModel);

        if (trimStrings || guessCellValueTypes || storeBlankCellsAsNulls) {
            grid = grid.mapRows(cellValueCleaningMapper(guessCellValueTypes, trimStrings, storeBlankCellsAsNulls), columnModel);
        }
        if (includeFileSources) {
            grid = TabularParserHelper.prependColumn("File", fileSource, grid);
        }
        if (includeArchiveFileName) {
            grid = TabularParserHelper.prependColumn("Archive", archiveFileName, grid);
        }
        return postTransform(grid, options);
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

    /**
     * Counts the maximum number of cells in a row returned by a {@link RowMapper}, by evaluating it on the entire grid.
     * 
     * @param grid
     * @param rowMapper
     * @return
     */
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

    /**
     * Utility class to report progress in a scaled fashion, when we know how many passes on the file we will need to
     * do.
     * 
     * @author Antonin Delpeuch
     *
     */
    protected class ScaledProgress implements MultiFileReadingProgress {

        private MultiFileReadingProgress parent;
        private int factor;

        protected ScaledProgress(MultiFileReadingProgress parent, int factor) {
            this.parent = parent;
            this.factor = factor;
        }

        @Override
        public void startFile(String fileSource) {
            parent.startFile(fileSource);
        }

        @Override
        public void readingFile(String fileSource, long bytesRead) {
            parent.readingFile(fileSource, bytesRead / factor);
        }

        @Override
        public void endFile(String fileSource, long bytesRead) {
            parent.endFile(fileSource, bytesRead / factor);
        }
    }

}

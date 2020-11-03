package org.openrefine.importers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.openrefine.ProjectMetadata;
import org.openrefine.browsing.facets.RowAggregator;
import org.openrefine.importing.ImportingJob;
import org.openrefine.model.ColumnModel;
import org.openrefine.model.DatamodelRunner;
import org.openrefine.model.GridState;
import org.openrefine.model.IndexedRow;
import org.openrefine.model.Row;
import org.openrefine.model.RowFilter;
import org.openrefine.model.RowMapper;
import org.openrefine.util.JSONUtilities;

import com.fasterxml.jackson.databind.node.ObjectNode;

public class LineBasedImporter extends HDFSImporter {

    public LineBasedImporter(DatamodelRunner runner) {
        super(runner);
    }

    protected RowMapper getRowMapper(ObjectNode options) {
        return RowMapper.IDENTITY;
    }
    
    @Override
    public GridState parseOneFile(ProjectMetadata metadata, ImportingJob job, String fileSource,
                String sparkURI, long limit, ObjectNode options) throws Exception {
        int ignoreLines = Math.max(JSONUtilities.getInt(options, "ignoreLines", -1), 0);
        int headerLines = Math.max(JSONUtilities.getInt(options, "headerLines", 0), 0);
        int skipDataLines = Math.max(JSONUtilities.getInt(options, "skipDataLines", 0), 0);
        String[] optionColumnNames = JSONUtilities.getStringArray(options, "columnNames");
        long limit2 = JSONUtilities.getLong(options, "limit", -1);
        if (limit > 0) {
            if (limit2 > 0) {
                limit2 = Math.min(limit, limit2);
            } else {
                limit2 = limit;
            }
        }
        
        RowMapper rowMapper = getRowMapper(options);
        GridState rawCells = runner.loadTextFile(sparkURI);
        
        // Compute the maximum number of cells in the entire grid
        int maxColumnNb = countMaxColumnNb(rawCells, rowMapper);
        
        // Parse column names
        List<String> columnNames = new ArrayList<>();
        
        if (optionColumnNames.length > 0) {
            for (int i = 0; i != optionColumnNames.length; i++) {
                ImporterUtilities.appendColumnName(columnNames, i, optionColumnNames[i]);
            }
        } else if (headerLines > 0) {
            int numTake = ignoreLines + headerLines;
            
            List<IndexedRow> firstLines = rawCells.getRows(ignoreLines, numTake);
            for(int i = 0; i < firstLines.size(); i++) {
                IndexedRow headerLine = firstLines.get(i);
                Row mappedRow = rowMapper.call(headerLine.getIndex(), headerLine.getRow());
                for(int j = 0; j != mappedRow.getCells().size(); j++) {
                    Serializable cellValue = mappedRow.getCellValue(j);
                    ImporterUtilities.appendColumnName(columnNames, j, cellValue == null ? "" : cellValue.toString());
                }
            }           
        }
        
        ColumnModel columnModel = ImporterUtilities.setupColumns(columnNames);
        while (columnModel.getColumns().size() < maxColumnNb) {
        	columnModel = ImporterUtilities.expandColumnModelIfNeeded(columnModel, columnModel.getColumns().size());
        }
        return rawCells.removeRows(removeFirstRows(ignoreLines + headerLines + skipDataLines))
                .mapRows(rowMapperWithPadding(rowMapper, maxColumnNb), columnModel);
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
    
    /**
     * Makes sure a row mapper always returns rows of the same size.
     * 
     * We do not simply express this as two consecutive maps because
     * the intermediate grid size would be invalid (not having as many cells
     * in each rows as there are columns).
     * 
     * @param mapper the original row mapper
     * @param nbColumns the number of columns
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
     * Returns true for the first n rows.
     * 
     * @param n the number of rows to remove
     * @return 
     */
    protected static RowFilter removeFirstRows(int n) {
        return new RowFilter() {

            private static final long serialVersionUID = 7373072483613980130L;

            @Override
            public boolean filterRow(long rowIndex, Row row) {
                return rowIndex < n;
            }
            
        };
    }

}

package org.openrefine.wikidata.schema;

import org.apache.commons.lang.Validate;
import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Row;

/**
 * A class holding all the necessary information about
 * the context in which a schema expression is evaluated.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ExpressionContext {
    private String baseIRI;
    private int rowId;
    private Row row;
    private ColumnModel columnModel;
    private QAWarningStore warningStore;
    
    /**
     * Builds an expression context to evaluate a schema on a row
     * @param baseIRI
     *      the siteIRI of the schema
     * @param rowId
     *      the id of the row currently visited
     * @param row
     *      the row itself
     * @param columnModel
     *      lets us access cells by column name
     * @param warningStore
     *      where to store the issues encountered when
     *      evaluating (can be set to null if these issues should be ignored)
     */
    public ExpressionContext(
            String baseIRI,
            int rowId,
            Row row,
            ColumnModel columnModel,
            QAWarningStore warningStore) {
        Validate.notNull(baseIRI);
        this.baseIRI = baseIRI;
        this.rowId = rowId;
        Validate.notNull(row);
        this.row = row;
        Validate.notNull(columnModel);
        this.columnModel = columnModel;
        this.warningStore = warningStore;
    }
    
    public String getBaseIRI() {
        return baseIRI;
    }
    /**
     * Retrieves a cell in the current row, by column name.
     * If the column does not exist, null is returned.
     * 
     * @param name
     *     the name of the column to retrieve the cell from
     * @return
     *     the cell
     */
    public Cell getCellByName(String name) {
        Column column = columnModel.getColumnByName(name);
        if (column != null) {
            int idx = column.getCellIndex();
            return row.getCell(idx);
        } else {
            return null;
        }
    }
    
    public int getRowId() {
        return rowId;
    }
    
    public void addWarning(QAWarning warning) {
        if (warningStore != null) {
            warningStore.addWarning(warning);
        }
    }
}

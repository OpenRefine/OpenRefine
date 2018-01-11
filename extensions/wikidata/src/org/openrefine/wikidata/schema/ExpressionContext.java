package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.qa.QAWarning;
import org.openrefine.wikidata.qa.QAWarningStore;

import com.google.refine.model.Cell;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Row;

/**
 * A class holding all the necessary information about
 * the context in which a schema expression is evaluated.
 * 
 * @author antonin
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
     * @param baseIRI: the siteIRI of the schema
     * @param rowId: the id of the row currently visited
     * @param row: the row itself
     * @param columnModel: lets us access cells by column name
     * @param warningStore: where to store the issues encountered when
     * evaluating (can be set to null if these issues should be ignored)
     */
    public ExpressionContext(
            String baseIRI,
            int rowId,
            Row row,
            ColumnModel columnModel,
            QAWarningStore warningStore) {
        this.baseIRI = baseIRI;
        this.rowId = rowId;
        this.row = row;
        this.columnModel = columnModel;
        this.warningStore = warningStore;
    }
    
    public String getBaseIRI() {
        return baseIRI;
    }
    
    public int getCellIndexByName(String name) {
        return columnModel.getColumnByName(name).getCellIndex();
    }
    
    public Cell getCellByName(String name) {
        int idx = getCellIndexByName(name);
        return row.getCell(idx);
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

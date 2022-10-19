/*******************************************************************************
 * MIT License
 * 
 * Copyright (c) 2018 Antonin Delpeuch
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/

package org.openrefine.wikibase.schema;

import java.util.Map;

import org.apache.commons.lang.Validate;
import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarningStore;

import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.ColumnModel;
import com.google.refine.model.Row;

/**
 * A class holding all the necessary information about the context in which a schema expression is evaluated.
 * 
 * @author Antonin Delpeuch
 *
 */
public class ExpressionContext {

    private String baseIRI;
    private Map<String, String> entityTypeIRIs;
    private String mediaWikiApiEndpoint;
    private int rowId;
    private Row row;
    private ColumnModel columnModel;
    private QAWarningStore warningStore;

    /**
     * Builds an expression context to evaluate a schema on a row
     * 
     * @param baseIRI
     *            the siteIRI of the schema
     * @param entityTypeBaseIRIs
     *            the siteIRI for specific entity types, falling back on the baseIRI otherwise
     * @param mediaWikiApiEndpoint
     *            the MediaWiki API endpoint of the Wikibase
     * @param rowId
     *            the id of the row currently visited
     * @param row
     *            the row itself
     * @param columnModel
     *            lets us access cells by column name
     * @param warningStore
     *            where to store the issues encountered when evaluating (can be set to null if these issues should be
     *            ignored)
     */
    public ExpressionContext(
            String baseIRI,
            Map<String, String> entityTypeBaseIRIs,
            String mediaWikiApiEndpoint,
            int rowId,
            Row row,
            ColumnModel columnModel,
            QAWarningStore warningStore) {
        Validate.notNull(baseIRI);
        this.baseIRI = baseIRI;
        this.entityTypeIRIs = entityTypeBaseIRIs;
        this.mediaWikiApiEndpoint = mediaWikiApiEndpoint;
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

    public String getBaseIRIForEntityType(String entityType) {
        return entityTypeIRIs.getOrDefault(entityType, baseIRI);
    }

    public String getMediaWikiApiEndpoint() {
        return mediaWikiApiEndpoint;
    }

    /**
     * Retrieves a cell in the current row, by column name. If the column does not exist, null is returned.
     * 
     * @param name
     *            the name of the column to retrieve the cell from
     * @return the cell
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

    public ColumnModel getColumnModel() {
        return columnModel;
    }
}

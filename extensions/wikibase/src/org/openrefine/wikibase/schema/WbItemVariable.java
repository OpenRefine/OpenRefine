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

import org.openrefine.wikibase.qa.QAWarning;
import org.openrefine.wikibase.qa.QAWarning.Severity;
import org.openrefine.wikibase.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikibase.schema.exceptions.QAWarningException;
import org.openrefine.wikibase.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.implementation.EntityIdValueImpl;
import org.wikidata.wdtk.datamodel.interfaces.EntityIdValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.refine.model.Cell;
import com.google.refine.model.Recon.Judgment;

/**
 * An item that depends on a reconciled value in a column.
 * 
 * @author Antonin Delpeuch
 *
 */
public class WbItemVariable extends WbVariableExpr<ItemIdValue> {

    public static final String INVALID_ITEM_ID_FORMAT_WARNING_TYPE = "invalid-item-id-format";

    @JsonCreator
    public WbItemVariable() {

    }

    /**
     * Constructs a variable and sets the column it is bound to. Mostly used as a convenience method for testing.
     * 
     * @param columnName
     *            the name of the column the expression should draw its value from
     */
    public WbItemVariable(String columnName) {
        setColumnName(columnName);
    }

    @Override
    public ItemIdValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException, QAWarningException {
        if (cell.recon != null
                && (Judgment.Matched.equals(cell.recon.judgment) || Judgment.New.equals(cell.recon.judgment))) {
            if (cell.recon.identifierSpace == null || !cell.recon.identifierSpace.equals(ctxt.getBaseIRI())) {
                QAWarning warning = new QAWarning("invalid-identifier-space", null, QAWarning.Severity.INFO, 1);
                warning.setProperty("example_cell", cell.value.toString());
                ctxt.addWarning(warning);
                throw new SkipSchemaExpressionException();
            }

            // Check that the id is of the correct format
            if (Judgment.Matched.equals(cell.recon.judgment)) {
                try {
                    EntityIdValue id = EntityIdValueImpl.fromId(cell.recon.match.id, cell.recon.identifierSpace);
                    if (!(id instanceof ItemIdValue)) {
                        QAWarning warning = new QAWarning(INVALID_ITEM_ID_FORMAT_WARNING_TYPE, "", Severity.CRITICAL, 1);
                        warning.setProperty("example", cell.recon.match.id);
                        throw new QAWarningException(warning);
                    }
                } catch (IllegalArgumentException e) {
                    QAWarning warning = new QAWarning(WbEntityVariable.INVALID_ENTITY_ID_FORMAT_WARNING_TYPE, "", Severity.CRITICAL, 1);
                    warning.setProperty("example", cell.recon.match.id);
                    throw new QAWarningException(warning);
                }
            }
            return new ReconItemIdValue(cell.recon, cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }

    @Override
    public boolean equals(Object other) {
        return equalAsVariables(other, WbItemVariable.class);
    }
}

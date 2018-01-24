package org.openrefine.wikidata.schema;


import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon.Judgment;

/**
 * An item that depends on a reconciled value in a column.
 * 
 * @author antonin
 *
 */
public class WbItemVariable extends WbVariableExpr<ItemIdValue> {

    @Override
    public ItemIdValue fromCell(Cell cell, ExpressionContext ctxt) throws SkipSchemaExpressionException {
        if (cell.recon != null
                && (Judgment.Matched.equals(cell.recon.judgment) ||
                    Judgment.New.equals(cell.recon.judgment))) {
            return new ReconItemIdValue(cell.recon, cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }
}

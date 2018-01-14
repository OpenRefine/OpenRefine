package org.openrefine.wikidata.schema;


import org.openrefine.wikidata.schema.entityvalues.ReconItemIdValue;
import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;
import com.google.refine.model.Recon.Judgment;

public class WbItemVariable extends WbItemExpr {
    /* An item that depends on a reconciled value in a column */
    
    private String columnName;
    
    @JsonCreator
    public WbItemVariable(
            @JsonProperty("columnName") String columnName) {
        this.columnName = columnName;
    }

    @Override
    public ItemIdValue evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException {
        Cell cell = ctxt.getCellByName(getColumnName());
        if (cell != null && cell.recon != null
                && (Judgment.Matched.equals(cell.recon.judgment) ||
                    Judgment.New.equals(cell.recon.judgment))) {
            return new ReconItemIdValue(cell.recon, cell.value.toString());
        }
        throw new SkipSchemaExpressionException();
    }

    @JsonProperty("columnName")
    public String getColumnName() {
        return columnName;
    }
}

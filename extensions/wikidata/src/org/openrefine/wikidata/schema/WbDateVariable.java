package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.google.refine.model.Cell;


public class WbDateVariable extends WbDateExpr {
    
    private String columnName;
    
    @JsonCreator
    public WbDateVariable(
            @JsonProperty("columnName") String columnName) {
        this.columnName = columnName;
    }

    @Override
    public TimeValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException {
        Cell cell = ctxt.getCellByName(columnName);
        if (cell != null) {
            try {
                // TODO accept parsed dates (without converting them to strings)
                return WbDateConstant.parse(cell.value.toString());
            } catch (ParseException e) {
            }
        }
        throw new SkipStatementException();
    }

    public String getColumnName() {
        return columnName;
    }
}

package org.openrefine.wikidata.schema;

import java.text.ParseException;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

import com.google.refine.model.Cell;


public class WbDateVariable extends WbVariableExpr<TimeValue> {

    @Override
    public TimeValue fromCell(Cell cell, ExpressionContext ctxt)
            throws SkipSchemaExpressionException {
        try {
            // TODO accept parsed dates (without converting them to strings)
            return WbDateConstant.parse(cell.value.toString());
        } catch (ParseException e) {
            throw new SkipSchemaExpressionException();
        }
    }
}

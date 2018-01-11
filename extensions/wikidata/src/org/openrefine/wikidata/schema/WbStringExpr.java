package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.exceptions.SkipSchemaExpressionException;
import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public abstract class WbStringExpr extends WbValueExpr {
    public abstract StringValue evaluate(ExpressionContext ctxt) throws SkipSchemaExpressionException;
}

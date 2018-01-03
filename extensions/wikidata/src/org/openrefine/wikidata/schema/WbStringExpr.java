package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.StringValue;

public abstract class WbStringExpr extends WbValueExpr {
    public abstract StringValue evaluate(ExpressionContext ctxt) throws SkipStatementException;
}

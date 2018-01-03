package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.ExpressionContext;
import org.openrefine.wikidata.schema.WbValueExpr;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;


public abstract class WbItemExpr extends WbValueExpr {
    public abstract ItemIdValue evaluate(ExpressionContext ctxt) throws SkipStatementException;
}

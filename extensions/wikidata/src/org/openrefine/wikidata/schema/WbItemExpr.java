package org.openrefine.wikidata.schema;

import org.openrefine.wikidata.schema.ExpressionContext;
import org.openrefine.wikidata.schema.WbValueExpr;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public interface WbItemExpr extends WbValueExpr {
    public ItemIdValue evaluate(ExpressionContext ctxt);
}

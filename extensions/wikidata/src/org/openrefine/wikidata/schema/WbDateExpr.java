package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.TimeValue;

public abstract class WbDateExpr extends WbValueExpr {

    @Override
    public abstract TimeValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException;
}

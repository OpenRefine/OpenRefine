package org.openrefine.wikidata.schema;

import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;

public abstract class WbLocationExpr extends WbValueExpr {
    @Override
    public abstract GlobeCoordinatesValue evaluate(ExpressionContext ctxt)
            throws SkipStatementException;
}

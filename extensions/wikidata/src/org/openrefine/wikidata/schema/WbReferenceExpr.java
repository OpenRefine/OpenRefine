package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;

import org.openrefine.wikidata.utils.JacksonJsonizable;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Reference;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class WbReferenceExpr extends JacksonJsonizable {
    private List<WbSnakExpr> snakExprs;
    
    @JsonCreator
    public WbReferenceExpr(
            @JsonProperty("snaks") List<WbSnakExpr> snakExprs) {
        this.snakExprs = snakExprs;
    }
    
    public Reference evaluate(ExpressionContext ctxt) throws SkipStatementException {
        List<SnakGroup> snakGroups = new ArrayList<SnakGroup>();
        for (WbSnakExpr expr : getSnaks()) {
            List<Snak> snakList = new ArrayList<Snak>(1);
            snakList.add(expr.evaluate(ctxt));
            snakGroups.add(Datamodel.makeSnakGroup(snakList));
        }
        return Datamodel.makeReference(snakGroups);
    }

    public List<WbSnakExpr> getSnaks() {
        return snakExprs;
    }

}

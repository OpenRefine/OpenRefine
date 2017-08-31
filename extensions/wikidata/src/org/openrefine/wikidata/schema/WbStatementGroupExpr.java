package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.Datamodel;
import org.wikidata.wdtk.datamodel.interfaces.Claim;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Snak;
import org.wikidata.wdtk.datamodel.interfaces.SnakGroup;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;


public class WbStatementGroupExpr extends BiJsonizable {
    public static final String jsonType = "wbstatementgroupexpr";
    
    private WbPropExpr propertyExpr;
    private List<WbStatementExpr> claimExprs;
    
    public WbStatementGroupExpr(WbPropExpr propertyExpr, List<WbStatementExpr> claimExprs) {
        this.propertyExpr = propertyExpr;
        this.claimExprs = claimExprs;
    }
    
    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        
        writer.key("property");
        propertyExpr.write(writer, options);
        writer.key("statements");
        writer.array();
        for(WbStatementExpr expr : claimExprs) {
            expr.write(writer, options);
        }
        writer.endArray();
    }
    
    public static WbStatementGroupExpr fromJSON(JSONObject obj) throws JSONException {
        JSONObject propertyObj = obj.getJSONObject("property");
        JSONArray claimsArr = obj.getJSONArray("statements");
        List<WbStatementExpr> claimExprs = new ArrayList<WbStatementExpr>();
        for (int i = 0; i != claimsArr.length(); i++) {
            claimExprs.add(WbStatementExpr.fromJSON(claimsArr.getJSONObject(i)));
        }
        return new WbStatementGroupExpr(
                WbPropExpr.fromJSON(propertyObj),
                claimExprs);
    }

    public StatementGroup evaluate(ExpressionContext ctxt, ItemIdValue subject) {
        PropertyIdValue propertyId = propertyExpr.evaluate(ctxt);
        List<Statement> statements = new ArrayList<Statement>(claimExprs.size());
        for(WbStatementExpr expr : claimExprs) {
            statements.add(expr.evaluate(ctxt, subject, propertyId));
        }
        // List<SnakGroup> groupedQualifiers = groupSnaks(qualifiers);
        return Datamodel.makeStatementGroup(statements);
    }
    
    public String getJsonType() {
        return jsonType;
    }
}

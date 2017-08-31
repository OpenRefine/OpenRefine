package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;


public class WbItemStatementsExpr extends WbChangeExpr {
    
    public static final String jsonType = "wbitemstatements";

    private WbItemExpr subjectExpr;
    private List<WbStatementGroupExpr> statementGroupExprs;
    
    public WbItemStatementsExpr(WbItemExpr subjectExpr, List<WbStatementGroupExpr> statementGroupExprs) {
        this.subjectExpr = subjectExpr;
        this.statementGroupExprs = statementGroupExprs;
    }
    
    @Override
    public void writeFields(JSONWriter writer, Properties options)
            throws JSONException {
        writer.key("subject");
        subjectExpr.write(writer, options);
        writer.key("statementGroups");
        writer.array();
        for(WbStatementGroupExpr expr : statementGroupExprs) {
            expr.write(writer, options);
        }
        writer.endArray();
    }
    
    public static WbItemStatementsExpr fromJSON(JSONObject obj) throws JSONException {
        JSONObject subjectObj = obj.getJSONObject("subject");
        JSONArray statementsArr = obj.getJSONArray("statementGroups");
        List<WbStatementGroupExpr> statementExprs = new ArrayList<WbStatementGroupExpr>();
        for (int i = 0; i != statementsArr.length(); i++) {
            statementExprs.add(WbStatementGroupExpr.fromJSON(statementsArr.getJSONObject(i)));
        }
        return new WbItemStatementsExpr(
                WbItemExpr.fromJSON(subjectObj),
                statementExprs);
    }
    
    public List<StatementGroup> evaluate(ExpressionContext ctxt) {
        List<StatementGroup> results = new ArrayList<StatementGroup>(statementGroupExprs.size());
        ItemIdValue subjectId = subjectExpr.evaluate(ctxt);
        for(WbStatementGroupExpr expr : statementGroupExprs) {
            results.add(expr.evaluate(ctxt, subjectId));
        }
        return results;
    }
    
    public String getJsonType() {
        return jsonType;
    }
}

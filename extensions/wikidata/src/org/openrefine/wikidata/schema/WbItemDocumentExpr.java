package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.wikidata.wdtk.datamodel.helpers.ItemDocumentBuilder;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;


public class WbItemDocumentExpr extends BiJsonizable {
    
    public static final String jsonType = "wbitemdocument";

    private WbItemExpr subjectExpr;
    private List<WbStatementGroupExpr> statementGroupExprs;
    
    public WbItemDocumentExpr(WbItemExpr subjectExpr, List<WbStatementGroupExpr> statementGroupExprs) {
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
    
    public static WbItemDocumentExpr fromJSON(JSONObject obj) throws JSONException {
        JSONObject subjectObj = obj.getJSONObject("subject");
        JSONArray statementsArr = obj.getJSONArray("statementGroups");
        List<WbStatementGroupExpr> statementExprs = new ArrayList<WbStatementGroupExpr>();
        for (int i = 0; i != statementsArr.length(); i++) {
            statementExprs.add(WbStatementGroupExpr.fromJSON(statementsArr.getJSONObject(i)));
        }
        return new WbItemDocumentExpr(
                WbItemExpr.fromJSON(subjectObj),
                statementExprs);
    }
    
    public ItemUpdate evaluate(ExpressionContext ctxt) throws SkipStatementException {
        ItemIdValue subjectId = subjectExpr.evaluate(ctxt);
        ItemUpdate update = new ItemUpdate(subjectId);
        for(WbStatementGroupExpr expr : statementGroupExprs) {
            for(Statement s : expr.evaluate(ctxt, subjectId).getStatements()) {
                update.addStatement(s);
            }
        }
        return update;
    }
    
    public String getJsonType() {
        return jsonType;
    }
}

package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.openrefine.wikidata.schema.ExpressionContext;

public class WikibaseSchema implements OverlayModel {

    final static Logger logger = LoggerFactory.getLogger("RdfSchema");
	
    final protected List<WbChangeExpr> changeExprs = new ArrayList<WbChangeExpr>();
    
    protected String baseUri = "http://www.wikidata.org/entity/";

    @Override
    public void onBeforeSave(Project project) {
    }
    
    @Override
    public void onAfterSave(Project project) {
    }
    
   @Override
    public void dispose(Project project) {

    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public WikibaseSchema(){
    	
    }
    
    
    public String getBaseUri() {
        return baseUri;
    }

    public List<WbChangeExpr> getChangeExpressions() {
        return changeExprs;
    }
    
    public List<StatementGroup> evaluate(ExpressionContext ctxt) {
        List<StatementGroup> result = new ArrayList<StatementGroup>();
        for (WbChangeExpr changeExpr : changeExprs) {
            WbItemStatementsExpr expr = (WbItemStatementsExpr)changeExpr;
            
            try {
                result.addAll(expr.evaluate(ctxt));
            } catch (SkipStatementException e) {
                continue;
            }
        }
        return result;
    }
    
    public List<StatementGroup> evaluate(Project project) {
        List<StatementGroup> result = new ArrayList<StatementGroup>();
        for (Row row : project.rows)  {
            ExpressionContext ctxt = new ExpressionContext(baseUri, row, project.columnModel);
            result.addAll(evaluate(ctxt));
        }
        return result;
    }

    static public WikibaseSchema reconstruct(JSONObject o) throws JSONException {
        JSONArray changeArr = o.getJSONArray("changes");
        WikibaseSchema schema = new WikibaseSchema();
        for (int i = 0; i != changeArr.length(); i++) {
            WbChangeExpr changeExpr = WbItemStatementsExpr.fromJSON(changeArr.getJSONObject(i));
            schema.changeExprs.add(changeExpr);
        }
        return schema;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("changes");
        writer.array();
        for (WbChangeExpr changeExpr : changeExprs) {
            changeExpr.write(writer, options);
        }
        writer.endArray();
        writer.endObject();
    }
    
    static public WikibaseSchema load(Project project, JSONObject obj) throws Exception {
        return reconstruct(obj);
    }
}
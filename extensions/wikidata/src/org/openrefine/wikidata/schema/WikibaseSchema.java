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
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;

import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import org.openrefine.wikidata.schema.ExpressionContext;

public class WikibaseSchema implements OverlayModel {

    final static Logger logger = LoggerFactory.getLogger("RdfSchema");
	
    final protected List<WbItemDocumentExpr> itemDocumentExprs = new ArrayList<WbItemDocumentExpr>();
    
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

    public List<WbItemDocumentExpr> getItemDocumentExpressions() {
        return itemDocumentExprs;
    }
    
    public List<ItemDocument> evaluate(ExpressionContext ctxt) {
        List<ItemDocument> result = new ArrayList<ItemDocument>();
        for (WbItemDocumentExpr expr : itemDocumentExprs) {
            
            try {
                result.add(expr.evaluate(ctxt));
            } catch (SkipStatementException e) {
                continue;
            }
        }
        return result;
    }
    
    public List<ItemDocument> evaluate(Project project) {
        List<ItemDocument> result = new ArrayList<ItemDocument>();
        for (Row row : project.rows)  {
            ExpressionContext ctxt = new ExpressionContext(baseUri, row, project.columnModel);
            result.addAll(evaluate(ctxt));
        }
        return result;
    }

    static public WikibaseSchema reconstruct(JSONObject o) throws JSONException {
        JSONArray changeArr = o.getJSONArray("itemDocuments");
        WikibaseSchema schema = new WikibaseSchema();
        for (int i = 0; i != changeArr.length(); i++) {
            WbItemDocumentExpr changeExpr = WbItemDocumentExpr.fromJSON(changeArr.getJSONObject(i));
            schema.itemDocumentExprs.add(changeExpr);
        }
        return schema;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.object();
        writer.key("itemDocuments");
        writer.array();
        for (WbItemDocumentExpr changeExpr : itemDocumentExprs) {
            changeExpr.write(writer, options);
        }
        writer.endArray();
        writer.endObject();
    }
    
    static public WikibaseSchema load(Project project, JSONObject obj) throws Exception {
        return reconstruct(obj);
    }
}
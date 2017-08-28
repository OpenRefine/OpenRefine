package org.openrefine.wikidata.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/*
import org.deri.grefine.rdf.ResourceNode.RdfType;
import org.deri.grefine.rdf.app.ApplicationContext;
import org.deri.grefine.rdf.vocab.PrefixExistException;
import org.deri.grefine.rdf.vocab.Vocabulary;
import org.deri.grefine.rdf.vocab.VocabularyIndexException; */
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.refine.model.OverlayModel;
import com.google.refine.model.Project;

public class WikibaseSchema implements OverlayModel {

    final static Logger logger = LoggerFactory.getLogger("RdfSchema");
	
    final protected List<WbChangeExpr> changeExprs = new ArrayList<WbChangeExpr>();
    
    protected String baseUri;

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

    static public WikibaseSchema reconstruct(JSONObject o) throws JSONException {
        JSONArray changeArr = o.getJSONArray("changes");
        WikibaseSchema schema = new WikibaseSchema();
        for (int i = 0; i != changeArr.length(); i++) {
            WbChangeExpr changeExpr = WbClaimExpr.fromJSON(changeArr.getJSONObject(i));
            schema.changeExprs.add(changeExpr);
        }
        return schema;
    }

    @Override
    public void write(JSONWriter writer, Properties options)
            throws JSONException {
        writer.array();
        for (WbChangeExpr changeExpr : changeExprs) {
            changeExpr.write(writer, options);
        }
        writer.endArray();
    }
    
    static public WikibaseSchema load(Project project, JSONObject obj) throws Exception {
        return reconstruct(obj);
    }
}
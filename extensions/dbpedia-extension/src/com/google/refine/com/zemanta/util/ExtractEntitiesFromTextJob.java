package com.google.refine.com.zemanta.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.ProjectManager;
import com.google.refine.model.ReconCandidate;
import com.google.refine.preference.PreferenceStore;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

import com.zemanta.api.Zemanta;

/***
 * What does this do?
 * 1. API key is needed
 * 2. GUI to enter API key is needed (like to register service on which to extend data)
 * store API key into settings
 * 3. Takes text as an input, returns entities contained in the text
 * 4. returns JSON
 *  
 * @author mateja
 *
 */

public class ExtractEntitiesFromTextJob {
    static public class DataExtension {
            final public Object[][] data;
            
            public DataExtension(Object[][] data) {
                this.data = data;
            }
    }
    static public class ColumnInfo {
            final public String name;
            final public String expectedType;
            
            protected ColumnInfo(String name, String expectedType) {
                this.name = name;
                this.expectedType = expectedType;
            }
    }
        
    final public JSONObject         extension;
    final public int                columnCount;
    final public List<ColumnInfo>   columns = new ArrayList<ColumnInfo>();
    public List<String>             entityTypesFilter = null;
        
    public ExtractEntitiesFromTextJob(JSONObject obj) throws JSONException {
            this.extension = obj;

            String columnType = "";
            String columnName = "";

            if(obj.has("types") && !obj.isNull("types")) {
                entityTypesFilter = JSONUtilities.toStringList(extension.getJSONArray("types"));
                                
                for(int i = 0; i < entityTypesFilter.size(); i++){
                    columnType = entityTypesFilter.get(i);
                        String parts[] = columnType.split("/");
                        columnName = parts[parts.length-1] + " [" + columnType + "]";                    
                        columns.add(new ColumnInfo(columnName,columnType));
                }
            }
            this.columnCount = columns.size();
        }
            
    public Map<String, ExtractEntitiesFromTextJob.DataExtension> extend (
        Set<String> texts,
        Map<String, ReconCandidate> reconCandidateMap
        ) throws Exception {
            
            Map<String, ExtractEntitiesFromTextJob.DataExtension> map = new HashMap<String, ExtractEntitiesFromTextJob.DataExtension>();
            
            PreferenceStore ps  =  ProjectManager.singleton.getPreferenceStore();                                
            String apiKey = (String) ps.get("zemanta-api-key");
            final String API_SERVICE_URL = "http://api.zemanta.com/services/rest/0.0/";
            
            if(apiKey != null) {
                Zemanta zem = new Zemanta(apiKey, API_SERVICE_URL);
                HashMap<String, String> parameters = new HashMap<String, String>();
                parameters.put("method", "zemanta.suggest_markup");
                parameters.put("api_key", apiKey);
                parameters.put("format", "json");
    
                String text = "";
                for(Iterator<String> singleText = texts.iterator(); singleText.hasNext();) {
                   text = singleText.next();
                    if(text != null) {
                        parameters.put("text", text);
                        String raw = zem.getRawData(parameters); //zemanta api calls
                        JSONObject result = ParsingUtilities.evaluateJsonStringToObject(raw);
                        
                        System.out.println("Raw results: " + raw);
                        if(result != null && result.has("status")) {
                            if(result.get("status").equals("ok")) {
                                ExtractEntitiesFromTextJob.DataExtension ext =  extractRowsWithEntities(reconCandidateMap, text, result);
    
                                if(ext != null) {
                                        map.put(text, ext);
                                }
                            }
                            else {
                                System.out.println("Request to Zemanta API failed.");  
                            }
                        }
                    }            
                } 
            } 
            
            return map;
    
    }

    protected ExtractEntitiesFromTextJob.DataExtension extractRowsWithEntities(Map<String, 
            ReconCandidate> reconCandidateMap, String text, JSONObject result) throws JSONException {
        
        Object[][] data = null;
            
        JSONObject markup = result.getJSONObject("markup");
        
        if(markup.has("links")) {
                            
            JSONArray links = markup.getJSONArray("links");
            int maxRows = getMaxRows(links);
            int rowIndexForEntityType[] = new int[entityTypesFilter.size()];

            for(int ri = 0; ri < entityTypesFilter.size(); ri++) {
                rowIndexForEntityType[ri] = 0;
            }
            
            data = new Object[maxRows][columnCount];
            int column = 0;

    
            for (int link = 0; link < links.length(); link++) {
                JSONObject o = links.getJSONObject(link);
                String name = o.getString("anchor");
                String id = "";
                
                boolean targetFound = false;
                
                if(o.has("target")) {
                    JSONArray targets = o.getJSONArray("target");
                    
                    for(int t = 0; (t < targets.length() && !targetFound); t++) {
                            JSONObject target = targets.getJSONObject(t);
                            String target_type = target.getString("type");

                            //returns only wikipedia target (if it exists)!
                            if(target_type.equals("wikipedia")) {
                                    name = target.getString("title");
                                    id = target.getString("url"); //remove en.wikipedia.org/wiki
                                    targetFound = true;
                            }
                    }
                }
                
                double score = 0.0;
                score = JSONUtilities.getDouble(o, "confidence", 0.0);

                String types[] = {};
                if(o.has("entity_type")) {
                    types = JSONUtilities.getStringArray(o,"entity_type");
                }
                
                //entity has no type defined, check if it is enabled in filter
                if(entityTypesFilter.contains("unknown") && types.length == 0) {
                        column = entityTypesFilter.indexOf("unknown");
                        ReconCandidate rc = new ReconCandidate(id, name, types, score);
                        reconCandidateMap.put(text, rc);

                        data[rowIndexForEntityType[column]][column] = rc;
                        rowIndexForEntityType[column]++;
                } 
                else {
                    for(int t = 0; t < types.length; t++){
                         if(entityTypesFilter.contains(types[t])){
                             column = entityTypesFilter.indexOf(types[t]);
                             String [] entityTypes = {types[t]};
                             ReconCandidate rc = new ReconCandidate(id, name, entityTypes, score);
                             reconCandidateMap.put(text, rc);
                             
                             data[rowIndexForEntityType[column]][column] = rc;
                             rowIndexForEntityType[column]++;
                         }   
                    }
                }              
            }
        }

        return new DataExtension(data);
    }

    protected int getMaxRows(JSONArray links)
            throws JSONException {
        
        int maxRows = 0;
        int typeIndex = 0;
        int[] maxRowsPerType = new int[entityTypesFilter.size()];
        
        for(int j = 0; j < entityTypesFilter.size(); j++) {
            maxRowsPerType[j] = 0;
        }
        
        for(int i = 0; i < links.length(); i++) {
            JSONObject o = links.getJSONObject(i);
            String types[] = {};
            
            if(o.has("entity_type")) {
                types = JSONUtilities.getStringArray(o,"entity_type");

                if(entityTypesFilter.contains("unknown") && (types.length == 0)) {
                    typeIndex = entityTypesFilter.indexOf("unknown");

                    maxRowsPerType[typeIndex]++;
                    if (maxRowsPerType[typeIndex] > maxRows) {
                        maxRows = maxRowsPerType[typeIndex];
                    } 
                }
                else {
                    for (int t = 0; t < types.length; t++ ) {
                        typeIndex = entityTypesFilter.indexOf(types[t]);
                        if(typeIndex != -1) {
                            maxRowsPerType[typeIndex]++;
                            if (maxRowsPerType[typeIndex] > maxRows) {
                                maxRows = maxRowsPerType[typeIndex];
                            } 
                        }
                    }
                }    
           }
        }
        return maxRows;
    }

}

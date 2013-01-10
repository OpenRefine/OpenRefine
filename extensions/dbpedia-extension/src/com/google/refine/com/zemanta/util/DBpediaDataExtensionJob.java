/*

Copyright 2010, Google Inc.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
copyright notice, this list of conditions and the following disclaimer
in the documentation and/or other materials provided with the
distribution.
    * Neither the name of Google Inc. nor the names of its
contributors may be used to endorse or promote products derived from
this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,           
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY           
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/

/**
 * 
 */
package com.google.refine.com.zemanta.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.refine.com.zemanta.DBpediaType;
import com.google.refine.model.ReconCandidate;
import com.google.refine.util.JSONUtilities;
import com.google.refine.util.ParsingUtilities;

public class DBpediaDataExtensionJob {
    static public class DataExtension {
        final public Object[][] data;
        
        public DataExtension(Object[][] data) {
            this.data = data;
        }
    }
    
    static public class ColumnInfo {
        final public List<String> names;
        final public List<String> path;
        final public DBpediaType expectedType;
        
        protected ColumnInfo(List<String> names, List<String> path, DBpediaType expected) {
            this.names = names;
            this.path = path;
            this.expectedType = expected;
        }
    }
    
    final public JSONObject         extension;
    final public int                columnCount;
    final public List<ColumnInfo>   columns = new ArrayList<ColumnInfo>();
    
    public DBpediaDataExtensionJob(JSONObject obj) throws JSONException {
        this.extension = obj;
        this.columnCount = (obj.has("properties") && !obj.isNull("properties")) ?
                countColumns(obj.getJSONArray("properties"), columns, new ArrayList<String>(), new ArrayList<String>()) : 0;
    }
    
    
    protected void extractRecordsFromJSON(JSONArray triples, HashMap<String, JSONArray> extractedResults)
            throws JSONException {
        
        HashMap<String,HashMap<String, JSONArray>> tempResults = new HashMap<String, HashMap<String, JSONArray>>();
        boolean skipNonEnglishTriplet = false;
        
        for(int i=0; i< triples.length(); i++) {
            
            JSONObject obj = triples.getJSONObject(i);
            skipNonEnglishTriplet = false;
            
            String key = obj.getJSONObject("obj").getString("value");
            String name = "";
            JSONObject result;
            
           
            if(obj.has("label")) {
                    //filter language
                    name = obj.getJSONObject("label").getString("value");
                    result = obj.getJSONObject("subj"); //result
            }
            else {
                    JSONObject subj = obj.getJSONObject("subj");
                    name = subj.getString("value");
                    if(subj.has("xml:lang")) {                        
                        if(!subj.getString("xml:lang").equals("en")) {
                            skipNonEnglishTriplet = true;
                        }
                    }
                    //return no id if type is literal
                    if(subj.has("type")) {
                        if(subj.get("type").equals("literal")) {
                            subj = ParsingUtilities.evaluateJsonStringToObject("{value: '', type: 'literal'}");
                        }
                    }
                    result = subj;
            }
            
            //skip all languages except English
            //TODO: make language selection configurable
            if(!skipNonEnglishTriplet) { 
                String type = obj.getJSONObject("prop").getString("value");
                result.put("name", name);
                
                if(tempResults.containsKey(key)){
                    
                    HashMap<String, JSONArray> existingPropertyRows = tempResults.get(key);
                    
                    if(existingPropertyRows.containsKey(type)) {
                        JSONArray oldRows = existingPropertyRows.get(type);
                        oldRows.put(result);
                    }
                    else {
                        JSONArray newRows = new JSONArray();
                        newRows.put(result);
                        existingPropertyRows.put(type, newRows);
                    }
                    
                }
                else { //completely new record
                    HashMap<String, JSONArray> propertyRows = new HashMap<String, JSONArray>();
                    JSONArray rows = new JSONArray();
                    rows.put(result);
                    propertyRows.put(type, rows);
                    tempResults.put(key, propertyRows);
                }
            }
                           
        }
        
        for(Iterator<String> iterator = tempResults.keySet().iterator(); iterator.hasNext();) {
            String key = iterator.next(); //original row recon id
            HashMap<String, JSONArray> allPropRows = tempResults.get(key);
            JSONArray resultsPerRecord = new JSONArray();
            
            for(Iterator<String> propIterator = allPropRows.keySet().iterator(); propIterator.hasNext();){
                String propID = propIterator.next();
                JSONObject rowsPerPropertyObj = new JSONObject();
                rowsPerPropertyObj.put("property", propID);
                rowsPerPropertyObj.put("rows", allPropRows.get(propID));
                resultsPerRecord.put(rowsPerPropertyObj);
            }
            
            extractedResults.put(key, resultsPerRecord);
        }
        
    }
    
    protected DBpediaDataExtensionJob.DataExtension collectResultsPerRecord(String id,
            JSONArray results, 
            Map<String, ReconCandidate> reconCandidateMap
            ) throws JSONException, UnsupportedEncodingException {
        
        int maxRows = countMaxRows(results);
        int column = 0;
        Object[][] data = new Object[maxRows][columnCount];
        
        for(int i = 0; i < results.length(); i++) {
            JSONObject rowsPerPropertyObj = results.getJSONObject(i);
            
            JSONArray rows = rowsPerPropertyObj.getJSONArray("rows");
            column = getColumn(rowsPerPropertyObj);
 
            for(int row = 0; row < rows.length(); row++) {
                JSONObject o = rows.getJSONObject(row);
                String resultID = o.getString("value");
                String name = o.getString("name");
                String type = rowsPerPropertyObj.getString("property");
                
                //TODO: remove this types hack... eventually
                JSONArray types = new JSONArray();
                types.put(type);
                rowsPerPropertyObj.put("type", types);
               
                ReconCandidate rc = new ReconCandidate(
                        resultID,
                        name,
                        JSONUtilities.getStringArray(rowsPerPropertyObj, "type"),
                        100
                    );
                
                reconCandidateMap.put(id, rc);
                data[row][column] = rc;
                
            }
        }
        
        return new DataExtension(data);
    }
    
    protected int getColumn(JSONObject rowsPerPropertyObj) throws JSONException {

        int column = 0;
        String propertyID = rowsPerPropertyObj.getString("property");
        
        for(int i = column; i < columnCount; i++) {
            ColumnInfo ci = columns.get(i);
            for(int j = 0; j < ci.path.size(); j++) {
                if(propertyID.equals(ci.path.get(j))){
                    column = i;
                    
                }
            }
        }
        
        return column;

    }

    protected int countMaxRows(JSONArray results) throws JSONException {
        
        int maxRows = 0;
        
        for(int i = 0; i < results.length(); i++) {
            JSONObject prop = results.getJSONObject(i);
            if((prop != null) && (prop.has("rows"))) {
                if(prop.getJSONArray("rows").length() > maxRows) {
                    maxRows = prop.getJSONArray("rows").length();
                }
            }
        }
        
        return maxRows;
    }

    public Map<String, DBpediaDataExtensionJob.DataExtension> extend(
        Set<String> ids,
        Map<String, ReconCandidate> reconCandidateMap
    ) throws Exception {

        Map<String, DBpediaDataExtensionJob.DataExtension> map = new HashMap<String, DBpediaDataExtensionJob.DataExtension>();
        InputStream is = null;
        try {
            if(extension.getJSONArray("properties").length() > 0) {
                String query = formulateQuery(ids, extension);  
                is = doSparqlRead(query);
                String s = ParsingUtilities.inputStreamToString(is);
        
                JSONArray properties = extension.getJSONArray("properties");
                if(properties.length() > 0) {
                                    
                    JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s).getJSONObject("results");
                    if (o.has("bindings")) {
                        
                        JSONArray bindings = o.getJSONArray("bindings");        
                                 
                        HashMap<String,JSONArray> extractedResults = new HashMap<String,JSONArray>();                
                        extractRecordsFromJSON(bindings, extractedResults);
                        
                        for (Iterator<String> it = ids.iterator(); it.hasNext();){
                            String id = it.next();
                            DBpediaDataExtensionJob.DataExtension ext = null;
                            
                            if(extractedResults.containsKey(id)) {
                                JSONArray results = extractedResults.get(id);
                                ext = collectResultsPerRecord(id, results, reconCandidateMap);                         
                                if (ext != null) {                        
                                    map.put(id, ext);
                                }
                            }
                            
                        }
                    }
                }
            }
        }finally {
            if(is != null)
                is.close();
        }
            
            return map;
    }

    static protected InputStream doSparqlRead(String query) throws IOException {

        String connString ="http://dbpedia.org/sparql";
        connString += "?query=" + ParsingUtilities.encode(query) + "&format=json";
        URL url = new URL(connString);
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        connection.setConnectTimeout(5000);
        connection.setDoOutput(true);
        connection.connect();
        
        return connection.getInputStream();
    }
    
    static protected String formulateQuery(Set<String> ids, JSONObject node) {
        String sparqlQuery = "";
        try {
            
            sparqlQuery = "PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> ";
            sparqlQuery += "SELECT ?obj ?prop ?subj ?label ";
            sparqlQuery += "WHERE { ?obj ?prop ?subj . ";
            sparqlQuery += "OPTIONAL {?subj rdfs:label ?label . ";
            sparqlQuery += "FILTER (LANG(?label) = 'en') }"; 
            
            sparqlQuery += "FILTER ( ?prop IN (";
            sparqlQuery += formulateSubqueryProperties(node.getJSONArray("properties"));
            sparqlQuery += ")) ";
            
            sparqlQuery += "FILTER ( ?obj IN (";
            sparqlQuery += formulateSubqueryObj(ids);
            sparqlQuery += ")) ";
            
            sparqlQuery += "}";
                
        } catch (JSONException e) {
            sparqlQuery = "";
            e.printStackTrace();
        }
        
        return sparqlQuery;


    }

    static protected String formulateSubqueryProperties(JSONArray properties) throws JSONException {
        String subQuery = "";
        int last = properties.length() - 1;
        
        for(int i = 0; i < properties.length(); i++) {
            JSONObject prop = properties.getJSONObject(i);
            subQuery += "<";
            subQuery += prop.getString("id");
            subQuery += ">";
            if(i < last) {
                subQuery += ",";
            }
            
        }

        return subQuery;
    }

    static protected String formulateSubqueryObj(Set<String> ids) {
        String subQuery = "";
        String id = "";
        
        for(Iterator<String> entityIDs = ids.iterator(); entityIDs.hasNext();) {
           id = entityIDs.next();
            if(id != null) {
                subQuery += "<" + id + ">";
                if(entityIDs.hasNext()) {
                    subQuery +=", ";
                }
            }            
        }
        
        return subQuery;
    }
    
    
    static protected int countColumns(JSONObject obj, List<ColumnInfo> columns, List<String> names, List<String> path) throws JSONException {
        String name = obj.getString("name");
        
        List<String> names2 = null;
        List<String> path2 = null;
        if (columns != null) {
            names2 = new ArrayList<String>(names);
            names2.add(name);
            
            path2 = new ArrayList<String>(path);
            path2.add(obj.getString("id"));
        }
        
        if (obj.has("properties") && !obj.isNull("properties")) {
            boolean included = (obj.has("included") && obj.getBoolean("included"));
            if (included && columns != null) {
                JSONObject expected = obj.getJSONObject("expected");
                
                columns.add(new ColumnInfo(names2, path2,
                    new DBpediaType(expected.getString("id"), expected.getString("name"))));
            }
            
            return (included ? 1 : 0) + 
                countColumns(obj.getJSONArray("properties"), columns, names2, path2);
        } else {
            if (columns != null) {
                columns.add(new ColumnInfo(names2, path2,
                    new DBpediaType(obj.getString("id"), obj.getString("name"))));
            }
            return 1;
        }
    }
    
    static protected int countColumns(JSONArray a, List<ColumnInfo> columns, List<String> names, List<String> path) throws JSONException {
        int c = 0;
        int l = a.length();
        for (int i = 0; i < l; i++) {
            c += countColumns(a.getJSONObject(i), columns, names, path);
        }
        return c;
    }
}
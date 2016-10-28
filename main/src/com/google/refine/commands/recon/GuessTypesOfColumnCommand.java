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

package com.google.refine.commands.recon;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;

import com.google.refine.commands.Command;
import com.google.refine.expr.ExpressionUtils;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

public class GuessTypesOfColumnCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");
            String serviceUrl = request.getParameter("service");
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            
            Column column = project.columnModel.getColumnByName(columnName);
            if (column == null) {
                writer.key("code"); writer.value("error");
                writer.key("message"); writer.value("No such column");
            } else {
                    List<TypeGroup> typeGroups = guessTypes(project, column, serviceUrl);
                    
                    writer.key("code"); writer.value("ok");
                    writer.key("types"); writer.array();         
                    
                    for (TypeGroup tg : typeGroups) {
                        writer.object();
                        writer.key("id"); writer.value(tg.id);
                        writer.key("name"); writer.value(tg.name);
                        writer.key("score"); writer.value(tg.score);
                        writer.key("count"); writer.value(tg.count);
                        writer.endObject();
                    }
                    
                    writer.endArray();
            }
            
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    final static int SAMPLE_SIZE = 10;
    
    /**
     * Run relevance searches for the first n cells in the given column and
     * count the types of the results. Return a sorted list of types, from most
     * frequent to least. 
     * 
     * @param project
     * @param column
     * @return
     * @throws JSONException, IOException 
     */
    protected List<TypeGroup> guessTypes(Project project, Column column, String serviceUrl)
            throws JSONException, IOException {
        Map<String, TypeGroup> map = new HashMap<String, TypeGroup>();
        
        int cellIndex = column.getCellIndex();
        
        List<String> samples = new ArrayList<String>(SAMPLE_SIZE);
        Set<String> sampleSet = new HashSet<String>();
        
        for (Row row : project.rows) {
            Object value = row.getCellValue(cellIndex);
            if (ExpressionUtils.isNonBlankData(value)) {
                String s = value.toString().trim();
                if (!sampleSet.contains(s)) {
                    samples.add(s);
                    sampleSet.add(s);
                    if (samples.size() >= SAMPLE_SIZE) {
                        break;
                    }
                }
            }
        }
        
        StringWriter stringWriter = new StringWriter();
        try {
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            jsonWriter.object();
            for (int i = 0; i < samples.size(); i++) {
                jsonWriter.key("q" + i);
                jsonWriter.object();
                
                jsonWriter.key("query"); jsonWriter.value(samples.get(i));
                jsonWriter.key("limit"); jsonWriter.value(3);
                
                jsonWriter.endObject();
            }
            jsonWriter.endObject();
        } catch (JSONException e) {
            logger.error("Error constructing query", e);
        }
        
        String queriesString = stringWriter.toString();
        try {
            URL url = new URL(serviceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            {
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                connection.setConnectTimeout(30000);
                connection.setDoOutput(true);
                
                DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
                try {
                    String body = "queries=" + ParsingUtilities.encode(queriesString);
                    
                    dos.writeBytes(body);
                } finally {
                    dos.flush();
                    dos.close();
                }
                
                connection.connect();
            }

            if (connection.getResponseCode() >= 400) {
                InputStream is = connection.getErrorStream();
                throw new IOException("Failed  - code:" 
                        + Integer.toString(connection.getResponseCode()) 
                        + " message: " + is == null ? "" : ParsingUtilities.inputStreamToString(is));
            } else {
                InputStream is = connection.getInputStream();
                try {
                    String s = ParsingUtilities.inputStreamToString(is);
                    JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);

                    for (int i = 0; i < samples.size(); i++) {
                        String key = "q" + i;
                        if (!o.has(key)) {
                            continue;
                        }

                        JSONObject o2 = o.getJSONObject(key);
                        if (!(o2.has("result"))) {
                            continue;
                        }

                        JSONArray results = o2.getJSONArray("result");
                        int count = results.length();

                        for (int j = 0; j < count; j++) {
                            JSONObject result = results.getJSONObject(j);
                            double score = 1.0 / (1 + j); // score by each result's rank

                            JSONArray types = result.getJSONArray("type");
                            int typeCount = types.length();

                            for (int t = 0; t < typeCount; t++) {
                                Object type = types.get(t);
                                String typeID;
                                String typeName;

                                if (type instanceof String) {
                                    typeID = typeName = (String) type;
                                } else {
                                    typeID = ((JSONObject) type).getString("id");
                                    typeName = ((JSONObject) type).getString("name");
                                }

                                double score2 = score * (typeCount - t) / typeCount;
                                if (map.containsKey(typeID)) {
                                    TypeGroup tg = map.get(typeID);
                                    tg.score += score2;
                                    tg.count++;
                                } else {
                                    map.put(typeID, new TypeGroup(typeID, typeName, score2));
                                }
                            }
                        }
                    }
                } finally {
                    is.close();
                }
            }
        } catch (IOException e) {
            logger.error("Failed to guess cell types for load\n" + queriesString, e);
            throw e;
        }
        
        List<TypeGroup> types = new ArrayList<TypeGroup>(map.values());
        Collections.sort(types, new Comparator<TypeGroup>() {
            @Override
            public int compare(TypeGroup o1, TypeGroup o2) {
                int c = Math.min(SAMPLE_SIZE, o2.count) - Math.min(SAMPLE_SIZE, o1.count);
                if (c != 0) {
                    return c;
                }
                return (int) Math.signum(o2.score / o2.count - o1.score / o1.count);
            }
        });
        
        return types;
    }
    
    static protected class TypeGroup {
        String id;
        String name;
        int count;
        double score;
        
        TypeGroup(String id, String name, double score) {
            this.id = id;
            this.name = name;
            this.score = score;
            this.count = 1;
        }
    }
}

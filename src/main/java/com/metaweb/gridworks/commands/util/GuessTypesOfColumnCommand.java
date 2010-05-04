package com.metaweb.gridworks.commands.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLConnection;
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
import org.json.JSONObject;
import org.json.JSONWriter;

import com.metaweb.gridworks.commands.Command;
import com.metaweb.gridworks.expr.ExpressionUtils;
import com.metaweb.gridworks.model.Column;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.ParsingUtilities;

public class GuessTypesOfColumnCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            
            Column column = project.columnModel.getColumnByName(columnName);
            if (column == null) {
                writer.key("code"); writer.value("error");
                writer.key("message"); writer.value("No such column");
            } else {
                try {
                    writer.key("code"); writer.value("ok");
                    writer.key("types"); writer.array();
                    
                    List<TypeGroup> typeGroups = guessTypes(project, column);
                    for (TypeGroup tg : typeGroups) {
                        writer.object();
                        writer.key("id"); writer.value(tg.id);
                        writer.key("name"); writer.value(tg.name);
                        writer.key("score"); writer.value(tg.score);
                        writer.key("count"); writer.value(tg.count);
                        writer.endObject();
                    }
                    
                    writer.endArray();
                } catch (Exception e) {
                    writer.key("code"); writer.value("error");
                }
            }
            
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
    
    final static int s_sampleSize = 20;
    
    /**
     * Run relevance searches for the first n cells in the given column and
     * count the types of the results. Return a sorted list of types, from most
     * frequent to least. 
     * 
     * @param project
     * @param column
     * @return
     */
    protected List<TypeGroup> guessTypes(Project project, Column column) {
        Map<String, TypeGroup> map = new HashMap<String, TypeGroup>();
        
        int cellIndex = column.getCellIndex();
        
        List<String> samples = new ArrayList<String>(s_sampleSize);
        Set<String> sampleSet = new HashSet<String>();
        
        for (Row row : project.rows) {
            Object value = row.getCellValue(cellIndex);
            if (ExpressionUtils.isNonBlankData(value)) {
                String s = value.toString().trim();
                if (!sampleSet.contains(s)) {
                    samples.add(s);
                    sampleSet.add(s);
                    if (samples.size() >= s_sampleSize) {
                        break;
                    }
                }
            }
        }
        
        try {
            StringWriter stringWriter = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(stringWriter);
            
            jsonWriter.object();
            for (int i = 0; i < samples.size(); i++) {
                jsonWriter.key("q" + i + ":search");
                jsonWriter.object();
                
                jsonWriter.key("query"); jsonWriter.value(samples.get(i));
                jsonWriter.key("limit"); jsonWriter.value(3);
                
                jsonWriter.endObject();
            }
            jsonWriter.endObject();
            
            StringBuffer sb = new StringBuffer(1024);
            sb.append("http://api.freebase.com/api/service/search?queries=");
            sb.append(ParsingUtilities.encode(stringWriter.toString()));
            
            URL url = new URL(sb.toString());
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(5000);
            connection.connect();
            
            InputStream is = connection.getInputStream();
            try {
                String s = ParsingUtilities.inputStreamToString(is);
                JSONObject o = ParsingUtilities.evaluateJsonStringToObject(s);
                
                for (int i = 0; i < samples.size(); i++) {
                    String key = "q" + i + ":search";
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
                            JSONObject type = types.getJSONObject(t);
                            String id = type.getString("id");
                            if (id.equals("/common/topic") ||
                                id.equals("/base/ontologies/ontology_instance") ||
                                (id.startsWith("/base/") && id.endsWith("/topic")) ||
                                id.startsWith("/user/") ||
                                id.startsWith("/freebase/")
                            ) {
                                continue;
                            }
                            
                            if (map.containsKey(id)) {
                                TypeGroup tg = map.get(id);
                                tg.score += score;
                                tg.count++;
                            } else {
                                map.put(id, new TypeGroup(id, type.getString("name"), score));
                            }
                        }
                    }
                }
            } finally {
                is.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        List<TypeGroup> types = new ArrayList<TypeGroup>(map.values());
        Collections.sort(types, new Comparator<TypeGroup>() {
            public int compare(TypeGroup o1, TypeGroup o2) {
                int c = Math.min(s_sampleSize, o2.count) - Math.min(s_sampleSize, o1.count);
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

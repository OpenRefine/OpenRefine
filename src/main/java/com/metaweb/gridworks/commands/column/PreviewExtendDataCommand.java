package com.metaweb.gridworks.commands.column;

import java.io.IOException;
import java.util.ArrayList;
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
import com.metaweb.gridworks.model.Cell;
import com.metaweb.gridworks.model.Project;
import com.metaweb.gridworks.model.ReconCandidate;
import com.metaweb.gridworks.model.Row;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob;
import com.metaweb.gridworks.util.ParsingUtilities;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob.ColumnInfo;
import com.metaweb.gridworks.util.FreebaseDataExtensionJob.DataExtension;

public class PreviewExtendDataCommand extends Command {
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            Project project = getProject(request);
            String columnName = request.getParameter("columnName");
            
            String rowIndicesString = request.getParameter("rowIndices");
            if (rowIndicesString == null) {
                respond(response, "{ \"code\" : \"error\", \"message\" : \"No row indices specified\" }");
                return;
            }
            
            String jsonString = request.getParameter("extension");
            JSONObject json = ParsingUtilities.evaluateJsonStringToObject(jsonString);
            
            JSONArray rowIndices = ParsingUtilities.evaluateJsonStringToArray(rowIndicesString);
            int length = rowIndices.length();
            int cellIndex = project.columnModel.getColumnByName(columnName).getCellIndex();
            
            List<String> topicNames = new ArrayList<String>();
            List<String> topicGuids = new ArrayList<String>();
            Set<String> guids = new HashSet<String>();
            for (int i = 0; i < length; i++) {
                int rowIndex = rowIndices.getInt(i);
                if (rowIndex >= 0 && rowIndex < project.rows.size()) {
                    Row row = project.rows.get(rowIndex);
                    Cell cell = row.getCell(cellIndex);
                    if (cell != null && cell.recon != null && cell.recon.match != null) {
                        topicNames.add(cell.recon.match.topicName);
                        topicGuids.add(cell.recon.match.topicGUID);
                        guids.add(cell.recon.match.topicGUID);
                    } else {
                        topicNames.add(null);
                        topicGuids.add(null);
                        guids.add(null);
                    }
                }
            }
            
            Map<String, ReconCandidate> reconCandidateMap = new HashMap<String, ReconCandidate>();
            FreebaseDataExtensionJob job = new FreebaseDataExtensionJob(json);
            Map<String, DataExtension> map = job.extend(guids, reconCandidateMap);
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONWriter writer = new JSONWriter(response.getWriter());
            writer.object();
            writer.key("code"); writer.value("ok");
            writer.key("columns");
                writer.array();
                for (ColumnInfo info : job.columns) {
                    writer.object();
                    writer.key("names");
                        writer.array();
                        for (String name : info.names) {
                            writer.value(name);
                        }
                        writer.endArray();
                    writer.key("path");
                        writer.array();
                        for (String id : info.path) {
                            writer.value(id);
                        }
                        writer.endArray();
                    writer.endObject();
                }
                writer.endArray();
            
            writer.key("rows");
                writer.array();
                for (int r = 0; r < topicNames.size(); r++) {
                    String guid = topicGuids.get(r);
                    String topicName = topicNames.get(r);
                    
                    if (guid != null && map.containsKey(guid)) {
                        DataExtension ext = map.get(guid);
                        boolean first = true;
                        
                        if (ext.data.length > 0) {
                            for (Object[] row : ext.data) {
                                writer.array();
                                if (first) {
                                    writer.value(topicName);
                                    first = false;
                                } else {
                                    writer.value(null);
                                }
                                
                                for (Object cell : row) {
                                    if (cell != null && cell instanceof ReconCandidate) {
                                        ReconCandidate rc = (ReconCandidate) cell;
                                        writer.object();
                                        writer.key("id"); writer.value(rc.topicID);
                                        writer.key("name"); writer.value(rc.topicName);
                                        writer.endObject();
                                    } else {
                                        writer.value(cell);
                                    }
                                }
                                
                                writer.endArray();
                            }
                            continue;
                        }
                    }
                    
                    writer.array();
                    if (guid != null) {
                        writer.object();
                        writer.key("id"); writer.value("/guid/" + guid.substring(1));
                        writer.key("name"); writer.value(topicName);
                        writer.endObject();
                    } else {
                        writer.value("<not reconciled>");
                    }
                    writer.endArray();
                }
                writer.endArray();
                
            writer.endObject();
        } catch (Exception e) {
            respondException(response, e);
        }
    }
}

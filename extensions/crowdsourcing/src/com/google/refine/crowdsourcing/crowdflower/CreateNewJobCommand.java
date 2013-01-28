package com.google.refine.crowdsourcing.crowdflower;

//TODO: does it make sense to implement an importer to import data *from* CF?

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.refine.browsing.Engine;
import com.google.refine.browsing.FilteredRows;
import com.google.refine.browsing.RowVisitor;
import com.google.refine.commands.Command;
import com.google.refine.crowdsourcing.CrowdsourcingUtil;
import com.google.refine.model.Cell;
import com.google.refine.model.Column;
import com.google.refine.model.Project;
import com.google.refine.model.Row;
import com.google.refine.util.ParsingUtilities;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zemanta.crowdflower.client.CrowdFlowerClient;

public class CreateNewJobCommand extends Command{
    static final Logger logger = LoggerFactory.getLogger("crowdflower_createnewjob");
    protected List<Integer> _cell_indeces;
    
    
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        try {
            
            JSONObject extension = new JSONObject(request.getParameter("extension")); 

            Project project = getProject(request);
            Engine engine = getEngine(request, project);

            String apiKey = (String) CrowdsourcingUtil.getPreference("crowdflower.apikey"); 
            Object defTimeout = CrowdsourcingUtil.getPreference("crowdflower.defaultTimeout");
            String defaultTimeout = (defTimeout != null) ? (String)defTimeout : "1500";
            
            CrowdFlowerClient cf_client = new CrowdFlowerClient(apiKey, Integer.valueOf(defaultTimeout));
            
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Content-Type", "application/json");
            
            JSONObject job = new JSONObject();
            JSONObject result = new JSONObject();
            
            if(extension.has("new_job") && extension.getBoolean("new_job")) {
                String job_title = extension.getString("title");
                String job_instructions = extension.getString("instructions");

                //new empty job with title and instructions
                job = createNewEmptyJob(cf_client, job_title, job_instructions);
                
                if(job.getString("status").equals("ERROR")) {
                    result = job;
                }
            }
            else {
                if(extension.has("job_id")) {
                    job.put("job_id", extension.getString("job_id"));
                }
                //ERROR
                else { 
                    result.put("status", "ERROR");
                    result.put("message", "No job id specified.");
                }
            }
         
            if(result.has("status")) {
                generateErrorResponse(response, result);
            }
            else {

                //update job with cml - in this case no data is uploaded, because mapping between columns and CF fields is neede
                if(extension.has("cml") && !extension.getString("cml").equals("")) {
                    
                    JSONObject obj_tmp = updateJobCML(job.getString("job_id"), extension.getString("cml"), cf_client);
                    
                    if(obj_tmp.get("status").equals("ERROR")) {
                        generateErrorResponse(response, obj_tmp);
                    } else {
                        generateResponse(response, obj_tmp);
                    }
                    
                } else {
                
                    //job either exist or it is created, time to upload data
                    if(extension.getBoolean("upload")) {
                        
                        logger.info("Generating objects for upload....");
                        StringBuffer data = generateObjectsForUpload(extension, project, engine);             
                        String msg = cf_client.bulkUploadJSONToExistingJob(job.getString("job_id"), data.toString());
                        JSONObject obj = ParsingUtilities.evaluateJsonStringToObject(msg);
                        
                        result.put("status", obj.get("status"));
    
                        if(obj.has("response") && !obj.isNull("response")) {
                            //upload succeeded
                            JSONObject rspn = obj.getJSONObject("response");
                            result.put("job_id", rspn.get("id"));                        
                            generateResponse(response, result);
                        } else {
                            result.put("message", obj.get("message"));
                            generateErrorResponse(response, result);
                        }
                    }
                    //no data to upload - just return status ok
                    else {
                        logger.info("Just empty job, without data");
                        result = job;
                        result.put("status", "OK");
                        generateResponse(response, result);
                    } 

                }
 
            }
        } catch(Exception e) {
            logger.error(e.getLocalizedMessage(),e);
        }
        
    }


    private void generateResponse(HttpServletResponse response, JSONObject data)
            throws IOException, JSONException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("job_id"); writer.value(data.get("job_id"));    
        } catch(Exception e){
            logger.error("Generating response failed.");
        }
        finally {
            writer.endObject();
            w.flush();
            w.close();
        }
    }
    
    private void generateErrorResponse(HttpServletResponse response, JSONObject data)
            throws IOException, JSONException {
        Writer w = response.getWriter();
        JSONWriter writer = new JSONWriter(w);
        try {
            writer.object();
            writer.key("status"); writer.value(data.get("status"));
            writer.key("message"); writer.value(data.get("message"));    
        } catch(Exception e){
            logger.error("Generating ERROR response failed.");
        }
        finally {
            writer.endObject();
            w.flush();
            w.close();
        }
    }


    private StringBuffer generateObjectsForUpload(JSONObject extension, Project project, Engine engine)
            throws JSONException {
        _cell_indeces = new ArrayList<Integer>();
        
        //if it is an existing job with existing data!, get field-column mappings
        
        JSONArray column_names = extension.getJSONArray("column_names"); 
            
        for (int i=0; i < column_names.length(); i++) {
            Column col = project.columnModel.getColumnByName(column_names.getJSONObject(i).getString("name"));
            _cell_indeces.add(col.getCellIndex());
        }

        FilteredRows rows = engine.getAllFilteredRows();
        List<Integer> rows_indeces = new ArrayList<Integer>();
        
        
        //for each row create JSON object
        rows.accept(project, new RowVisitor() {

            List<Integer> _rowIndices;
            
            public RowVisitor init(List<Integer> rowIndices) {
                _rowIndices = rowIndices;
                return this;
            }
            
            @Override
            public void start(Project project) {
                // nothing to do 
            }

            @Override
            public void end(Project project) {
                // nothing to do
                
            }
            
            @Override
            public boolean visit(Project project, int rowIndex, Row row) {
                //check cells in any of the selected columns
                
                for (int k = 0; k < _cell_indeces.size(); k ++) {
                
                    Cell cell = row.getCell(_cell_indeces.get(k));
                    if(cell != null) { //as soon a value is encountered in any of selected columns, add row index
                        _rowIndices.add(rowIndex);
                        break;
                    }
                }
                return false;
            }}.init(rows_indeces));
        
        
        StringBuffer bf = new StringBuffer();
        
        for(Iterator<Integer> it = rows_indeces.iterator(); it.hasNext();) {
            int row_index = it.next();
            
            JSONObject obj =  new JSONObject();
            for (int c=0; c < column_names.length(); c++) {
                int cell_index = _cell_indeces.get(c);
                
                //TODO: if job already has data, can we use safe_name to pass field?
                String key = column_names.getJSONObject(c).getString("safe_name");
                Object value = project.rows.get(row_index).getCellValue(cell_index);
                
                if(value != null) {
                    obj.put(key, value.toString());
                } else {
                    obj.put(key, "");
                }
                
            } 
            
           bf.append(obj.toString()); 
            
        }
        return bf;
    }


    private JSONObject createNewEmptyJob(CrowdFlowerClient cf_client, String title, String instructions)
            throws JSONException {


        String response_msg = cf_client.createNewJobWithoutData(title, instructions);
        JSONObject obj =  ParsingUtilities.evaluateJsonStringToObject(response_msg);
        JSONObject result = new JSONObject();
        
        result.put("status", obj.getString("status"));
        
        if(obj.has("response") && !obj.isNull("response")) {
            JSONObject obj2 = obj.getJSONObject("response");
            
            if(obj2.has("id") && !obj2.isNull("id")) {
                result.put("job_id", obj2.get("id"));
            } 
            else {
                result.put("message", "No job id was retruned by CrowdFlower service");
            }
        }
        else {
            result.put("message", obj.getJSONObject("error").get("message"));
        }

        return result;

    }

    private JSONObject updateJobCML(String job_id, String cml, CrowdFlowerClient cf_client)
            throws JSONException {

        String response_msg = cf_client.upateJobCML(job_id, cml);
        JSONObject obj =  ParsingUtilities.evaluateJsonStringToObject(response_msg);
        JSONObject result = new JSONObject();
        
        result.put("status", obj.getString("status"));
        
        if(obj.has("response") && !obj.isNull("response")) {
            JSONObject obj2 = obj.getJSONObject("response");
            
            if(obj2.has("id") && !obj2.isNull("id")) {
                result.put("job_id", obj2.get("id"));
            } 
            else {
                result.put("message", "No job id was retruned by CrowdFlower service");
            }
        }
        else {
            result.put("message", obj.getJSONObject("error").get("message"));
        }

        return result;

    }

    
    
    
    
}
